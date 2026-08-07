-- Library-only disease entries.
-- This file matches the migration version registered in production.
-- These rows are educational catalog content and are intentionally separate
-- from the fixed on-device model class map. They never become detector labels.

alter table public.disease_catalog
  alter column model_class_index drop not null,
  alter column model_label drop not null,
  add column if not exists detector_supported boolean not null default true,
  add column if not exists artwork_path text;

alter table public.disease_catalog
  drop constraint if exists disease_catalog_model_mapping_check;
alter table public.disease_catalog
  add constraint disease_catalog_model_mapping_check check (
    (detector_supported and model_class_index is not null and model_label is not null)
    or (not detector_supported and model_class_index is null and model_label is null)
  );

alter table public.disease_catalog
  drop constraint if exists disease_catalog_artwork_path_check;
alter table public.disease_catalog
  add constraint disease_catalog_artwork_path_check check (
    artwork_path is null
    or (
      char_length(artwork_path) between 1 and 200
      and artwork_path !~ '(^/|(^|/)\.\.(/|$)|[[:cntrl:]])'
      and artwork_path ~ '^catalog/[a-z0-9]+(-[a-z0-9]+)*\.jpg$'
    )
  );

insert into storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
values ('disease-catalog-artwork', 'disease-catalog-artwork', true, 5242880, array['image/jpeg'])
on conflict (id) do update set
  name = excluded.name,
  public = excluded.public,
  file_size_limit = excluded.file_size_limit,
  allowed_mime_types = excluded.allowed_mime_types;

create or replace function public.create_disease_catalog_entry_v1(
  p_disease_id text,
  p_category text,
  p_artwork_path text,
  p_en_content jsonb,
  p_fil_content jsonb,
  p_en_signs jsonb,
  p_fil_signs jsonb,
  p_en_reference jsonb,
  p_fil_reference jsonb,
  p_admin_id uuid,
  p_idempotency_key uuid,
  p_payload_hash text
)
returns text
language plpgsql
security definer
set search_path = ''
as $$
declare
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
    or char_length(p_disease_id) > 80
    or p_category is null
    or p_category not in ('LEAF_DISEASE', 'FRUIT_DISEASE')
    or p_artwork_path is distinct from 'catalog/' || p_disease_id || '.jpg'
    or p_idempotency_key is null
    or p_payload_hash is null
    or p_payload_hash !~ '^[a-f0-9]{64}$' then
    raise insufficient_privilege using message = 'valid owner/admin catalog action required';
  end if;

  if coalesce(jsonb_typeof(p_en_content), '') <> 'object'
    or coalesce(jsonb_typeof(p_fil_content), '') <> 'object' then
    raise exception using errcode = '22023', message = 'bilingual disease content must be objects';
  end if;
  if exists (
    select 1
    from unnest(ARRAY[
      'name', 'description', 'symptom_preview', 'causes', 'recommended_action',
      'prevention', 'guidance', 'when_to_act', 'disclaimer'
    ]::text[]) as field(name)
    where char_length(trim(coalesce(p_en_content ->> field.name, ''))) not between 2 and 4000
      or char_length(trim(coalesce(p_fil_content ->> field.name, ''))) not between 2 and 4000
  ) then
    raise exception using errcode = '22023', message = 'bilingual disease content is incomplete';
  end if;

  if coalesce(jsonb_typeof(p_en_signs), '') <> 'array'
    or coalesce(jsonb_typeof(p_fil_signs), '') <> 'array' then
    raise exception using errcode = '22023', message = 'bilingual symptoms must be arrays';
  end if;
  if jsonb_array_length(p_en_signs) not between 1 and 20
    or jsonb_array_length(p_fil_signs) not between 1 and 20
    or exists (
      select 1 from jsonb_array_elements_text(p_en_signs) as sign(value)
      where char_length(trim(sign.value)) not between 2 and 500
    )
    or exists (
      select 1 from jsonb_array_elements_text(p_fil_signs) as sign(value)
      where char_length(trim(sign.value)) not between 2 and 500
    ) then
    raise exception using errcode = '22023', message = 'invalid bilingual symptoms';
  end if;

  if coalesce(jsonb_typeof(p_en_reference), '') <> 'object'
    or coalesce(jsonb_typeof(p_fil_reference), '') <> 'object'
    or char_length(trim(coalesce(p_en_reference ->> 'publisher', ''))) not between 2 and 500
    or char_length(trim(coalesce(p_fil_reference ->> 'publisher', ''))) not between 2 and 500
    or char_length(trim(coalesce(p_en_reference ->> 'title', ''))) not between 2 and 500
    or char_length(trim(coalesce(p_fil_reference ->> 'title', ''))) not between 2 and 500
    or char_length(trim(coalesce(p_en_reference ->> 'url', ''))) not between 12 and 2048
    or char_length(trim(coalesce(p_fil_reference ->> 'url', ''))) not between 12 and 2048
    or (p_en_reference ->> 'url') !~ '^https://[^[:space:]]+$'
    or (p_fil_reference ->> 'url') !~ '^https://[^[:space:]]+$' then
    raise exception using errcode = '22023', message = 'each disease reference needs a public HTTPS URL';
  end if;

  claim := app_private.claim_admin_action(p_admin_id, 'catalog_publish', p_disease_id, p_idempotency_key, p_payload_hash);
  if claim = 'conflict' then
    raise exception using errcode = '23505', message = 'idempotency key conflicts with another catalog creation';
  end if;
  if claim = 'replay' then
    select outcome into prior_outcome
    from public.admin_action_receipts
    where admin_id = p_admin_id
      and action = 'catalog_publish'
      and resource_key = p_disease_id
      and idempotency_key = p_idempotency_key;
    return coalesce(prior_outcome, 'unchanged');
  end if;

  if not exists (
    select 1 from storage.objects
    where bucket_id = 'disease-catalog-artwork'
      and name = p_artwork_path
  ) then
    raise exception using errcode = '23514', message = 'disease artwork upload is unavailable';
  end if;

  if exists (select 1 from public.disease_catalog where id = p_disease_id) then
    raise exception using errcode = '23505', message = 'disease ID already exists';
  end if;

  insert into public.disease_catalog (
    id, model_class_index, model_label, category, artwork_key, artwork_path,
    detector_supported, content_version, content_hash
  ) values (
    p_disease_id, null, null, p_category, p_disease_id, p_artwork_path,
    false, 1, p_payload_hash
  );

  insert into public.disease_localizations (
    disease_id, language_tag, name, description, symptom_preview, causes,
    recommended_action, prevention, guidance, when_to_act, disclaimer
  ) values
    (
      p_disease_id, 'en', trim(p_en_content ->> 'name'), trim(p_en_content ->> 'description'),
      trim(p_en_content ->> 'symptom_preview'), trim(p_en_content ->> 'causes'),
      trim(p_en_content ->> 'recommended_action'), trim(p_en_content ->> 'prevention'),
      trim(p_en_content ->> 'guidance'), trim(p_en_content ->> 'when_to_act'), trim(p_en_content ->> 'disclaimer')
    ),
    (
      p_disease_id, 'fil', trim(p_fil_content ->> 'name'), trim(p_fil_content ->> 'description'),
      trim(p_fil_content ->> 'symptom_preview'), trim(p_fil_content ->> 'causes'),
      trim(p_fil_content ->> 'recommended_action'), trim(p_fil_content ->> 'prevention'),
      trim(p_fil_content ->> 'guidance'), trim(p_fil_content ->> 'when_to_act'), trim(p_fil_content ->> 'disclaimer')
    );

  insert into public.disease_signs (disease_id, language_tag, position, text)
  select p_disease_id, 'en', (sign.ordinality - 1)::smallint, trim(sign.value)
  from jsonb_array_elements_text(p_en_signs) with ordinality as sign(value, ordinality)
  union all
  select p_disease_id, 'fil', (sign.ordinality - 1)::smallint, trim(sign.value)
  from jsonb_array_elements_text(p_fil_signs) with ordinality as sign(value, ordinality);

  insert into public.disease_references (disease_id, language_tag, position, publisher, title, url)
  values
    (p_disease_id, 'en', 0, trim(p_en_reference ->> 'publisher'), trim(p_en_reference ->> 'title'), trim(p_en_reference ->> 'url')),
    (p_disease_id, 'fil', 0, trim(p_fil_reference ->> 'publisher'), trim(p_fil_reference ->> 'title'), trim(p_fil_reference ->> 'url'));

  update public.app_config
  set catalog_version = catalog_version + 1, updated_at = now()
  where id = true;
  insert into public.moderation_actions (admin_id, resource_type, resource_key, action, reason)
  values (
    p_admin_id, 'disease_catalog', p_disease_id, 'catalog_created',
    'Library-only bilingual disease entry published'
  );
  perform app_private.complete_admin_action(p_admin_id, 'catalog_publish', p_disease_id, p_idempotency_key, 'applied');
  return 'applied';
end;
$$;

revoke all on function public.create_disease_catalog_entry_v1(text, text, text, jsonb, jsonb, jsonb, jsonb, jsonb, jsonb, uuid, uuid, text) from public, anon, authenticated;
grant execute on function public.create_disease_catalog_entry_v1(text, text, text, jsonb, jsonb, jsonb, jsonb, jsonb, jsonb, uuid, uuid, text) to service_role;
