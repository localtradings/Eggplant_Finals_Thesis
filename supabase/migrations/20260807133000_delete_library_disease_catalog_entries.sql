-- Additive catalog deletion contract.
-- Applying this migration does not delete any existing row. A delete request is
-- still rejected for detector classes or for catalog entries referenced by
-- shared scans, pending shares, or the ranking ledger.

alter table public.admin_action_receipts
  drop constraint if exists admin_action_receipts_action_check;

alter table public.admin_action_receipts
  add constraint admin_action_receipts_action_check
  check (action in (
    'catalog_publish',
    'request_review',
    'scan_moderation',
    'cloud_writes_toggle',
    'catalog_delete'
  ));

create or replace function app_private.claim_admin_action(
  p_admin_id uuid,
  p_action text,
  p_resource_key text,
  p_idempotency_key uuid,
  p_payload_hash text
)
returns text
language plpgsql
security definer
set search_path = ''
as $$
declare
  inserted integer;
  existing_hash text;
begin
  if (select auth.role()) <> 'service_role'
    or not exists (select 1 from public.admin_members where user_id = p_admin_id)
    or p_action not in ('catalog_publish', 'request_review', 'scan_moderation', 'cloud_writes_toggle', 'catalog_delete')
    or p_idempotency_key is null
    or p_payload_hash !~ '^[a-f0-9]{64}$' then
    raise insufficient_privilege using message = 'valid authorized admin action required';
  end if;

  insert into public.admin_action_receipts (
    admin_id, action, resource_key, idempotency_key, payload_hash, outcome
  ) values (
    p_admin_id, p_action, p_resource_key, p_idempotency_key, p_payload_hash, 'unchanged'
  ) on conflict (admin_id, action, resource_key, idempotency_key) do nothing;
  get diagnostics inserted = row_count;
  if inserted = 1 then return 'new'; end if;

  select payload_hash into existing_hash
  from public.admin_action_receipts
  where admin_id = p_admin_id
    and action = p_action
    and resource_key = p_resource_key
    and idempotency_key = p_idempotency_key;
  if existing_hash is distinct from p_payload_hash then return 'conflict'; end if;
  return 'replay';
end;
$$;

revoke all on function app_private.claim_admin_action(uuid, text, text, uuid, text) from public, anon, authenticated;

create or replace function public.delete_disease_catalog_entry_v1(
  p_disease_id text,
  p_admin_id uuid,
  p_idempotency_key uuid,
  p_payload_hash text,
  p_destructive_approval text
)
returns jsonb
language plpgsql
security definer
set search_path = ''
as $$
declare
  catalog public.disease_catalog%rowtype;
  claim text;
  prior_outcome text;
begin
  if (select auth.role()) <> 'service_role'
    or not exists (
      select 1 from public.admin_members
      where user_id = p_admin_id and role in ('owner', 'admin')
    )
    or p_disease_id is null
    or p_disease_id !~ '^[a-z0-9]+(-[a-z0-9]+)*$'
    or p_idempotency_key is null
    or p_payload_hash !~ '^[a-f0-9]{64}$'
    or p_destructive_approval is distinct from 'I approve this destructive database action.' then
    raise insufficient_privilege using message = 'exact destructive approval and authorized owner/admin action required';
  end if;

  select * into catalog
  from public.disease_catalog
  where id = p_disease_id
  for update;

  if not found then
    claim := app_private.claim_admin_action(p_admin_id, 'catalog_delete', p_disease_id, p_idempotency_key, p_payload_hash);
    if claim = 'conflict' then
      raise exception using errcode = '23505', message = 'idempotency key conflicts with another catalog deletion';
    end if;
    if claim = 'replay' then
      select outcome into prior_outcome
      from public.admin_action_receipts
      where admin_id = p_admin_id
        and action = 'catalog_delete'
        and resource_key = p_disease_id
        and idempotency_key = p_idempotency_key;
      return jsonb_build_object('outcome', coalesce(prior_outcome, 'unchanged'), 'artwork_path', null);
    end if;
    perform app_private.complete_admin_action(p_admin_id, 'catalog_delete', p_disease_id, p_idempotency_key, 'unchanged');
    return jsonb_build_object('outcome', 'missing', 'artwork_path', null);
  end if;

  if catalog.detector_supported is distinct from false then
    raise exception using errcode = '42501', message = 'detector-supported catalog classes cannot be deleted';
  end if;

  if exists (select 1 from public.scan_contributions where disease_id = p_disease_id)
    or exists (select 1 from public.global_share_intents where disease_id = p_disease_id)
    or exists (select 1 from public.global_ranking_ledger where disease_id = p_disease_id) then
    raise exception using errcode = '23503', message = 'this disease is referenced by existing scan data and cannot be deleted';
  end if;

  claim := app_private.claim_admin_action(p_admin_id, 'catalog_delete', p_disease_id, p_idempotency_key, p_payload_hash);
  if claim = 'conflict' then
    raise exception using errcode = '23505', message = 'idempotency key conflicts with another catalog deletion';
  end if;
  if claim = 'replay' then
    select outcome into prior_outcome
    from public.admin_action_receipts
    where admin_id = p_admin_id
      and action = 'catalog_delete'
      and resource_key = p_disease_id
      and idempotency_key = p_idempotency_key;
    return jsonb_build_object('outcome', coalesce(prior_outcome, 'unchanged'), 'artwork_path', null);
  end if;

  -- Localizations use ON DELETE RESTRICT, so remove the dependent content in
  -- the same transaction before removing the library-only catalog row.
  delete from public.disease_localizations where disease_id = p_disease_id;
  delete from public.disease_signs where disease_id = p_disease_id;
  delete from public.disease_references where disease_id = p_disease_id;
  delete from public.disease_catalog where id = p_disease_id;

  update public.app_config
  set catalog_version = catalog_version + 1, updated_at = now()
  where id = true;

  insert into public.moderation_actions (admin_id, resource_type, resource_key, action, reason)
  values (
    p_admin_id,
    'disease_catalog',
    p_disease_id,
    'catalog_deleted',
    'Library-only disease entry permanently removed by an authorized owner/admin'
  );

  perform app_private.complete_admin_action(p_admin_id, 'catalog_delete', p_disease_id, p_idempotency_key, 'applied');
  return jsonb_build_object('outcome', 'applied', 'artwork_path', catalog.artwork_path);
end;
$$;

revoke all on function public.delete_disease_catalog_entry_v1(text, uuid, uuid, text, text) from public, anon, authenticated;
grant execute on function public.delete_disease_catalog_entry_v1(text, uuid, uuid, text, text) to service_role;
