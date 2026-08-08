-- Allow disease requests to use a photo selected from the Android gallery.
-- This is a forward-only contract change: existing rows, quotas, RLS, and
-- storage paths remain unchanged.

begin;

alter table public.disease_request_photos
  drop constraint if exists disease_request_photos_capture_source_check;

alter table public.disease_request_photos
  add constraint disease_request_photos_capture_source_check
  check (capture_source in ('live', 'capture', 'gallery'));

create or replace function public.create_disease_request_with_quota_v2(
  p_owner_id uuid,
  p_client_request_id uuid,
  p_requested_name text,
  p_notes text,
  p_model_version text,
  p_rights_consent boolean,
  p_training_consent boolean,
  p_photo_hashes text[],
  p_photo_sources text[],
  p_rate_subject text
)
returns table (request_id uuid, request_status text, outcome text)
language plpgsql
security definer
set search_path = ''
as $$
declare
  existing public.disease_requests%rowtype;
  created public.disease_requests%rowtype;
  ip_count integer;
  expected_paths text[];
  recorded_paths text[];
  normalized_name text := nullif(trim(coalesce(p_requested_name, '')), '');
  normalized_notes text := nullif(trim(coalesce(p_notes, '')), '');
begin
  if (select auth.role()) <> 'service_role' then
    raise insufficient_privilege using message = 'service role required';
  end if;
  if p_rate_subject !~ '^[a-f0-9]{64}$'
    or not p_rights_consent
    or p_training_consent
    or cardinality(p_photo_hashes) not between 1 and 3
    or cardinality(p_photo_sources) <> cardinality(p_photo_hashes)
    or exists (select 1 from unnest(p_photo_hashes) as photo_hash where photo_hash !~ '^[a-f0-9]{64}$')
    or exists (select 1 from unnest(p_photo_sources) as source where source not in ('live', 'capture', 'gallery'))
    or (normalized_name is not null and char_length(normalized_name) not between 2 and 120)
    or (normalized_notes is not null and char_length(normalized_notes) > 200) then
    raise exception using errcode = '22023', message = 'invalid disease request';
  end if;

  perform pg_advisory_xact_lock(hashtextextended('ip:' || p_rate_subject, 0));
  perform pg_advisory_xact_lock(hashtextextended('owner:' || p_owner_id::text, 0));

  select * into existing
  from public.disease_requests
  where owner_id = p_owner_id and client_request_id = p_client_request_id;
  if found then
    select array_agg(format('requests/%s/%s/%s-%s.jpg', p_owner_id, existing.id, photo.ordinality - 1, photo.hash) order by photo.ordinality)
      into expected_paths
    from unnest(p_photo_hashes) with ordinality as photo(hash, ordinality);
    select array_agg(object_path order by position) into recorded_paths
    from public.disease_request_photos
    where request_id = existing.id and owner_id = p_owner_id;
    if existing.requested_name is not distinct from normalized_name
      and existing.notes is not distinct from normalized_notes
      and existing.model_version = p_model_version
      and existing.rights_consent = p_rights_consent
      and existing.training_consent = p_training_consent
      and recorded_paths is not distinct from expected_paths then
      return query select existing.id, existing.status, 'existing'::text;
    else
      return query select existing.id, existing.status, 'conflict'::text;
    end if;
    return;
  end if;

  select coalesce(request_count, 0) into ip_count
  from public.api_rate_limits
  where action = 'disease_request' and window_start = current_date and subject_hash = p_rate_subject;
  if (select count(*) from public.disease_requests where owner_id = p_owner_id and created_at >= now() - interval '24 hours') >= 5
    or coalesce(ip_count, 0) >= 15 then
    return query select null::uuid, null::text, 'quota'::text;
    return;
  end if;

  insert into public.disease_requests (
    owner_id, client_request_id, requested_name, notes, model_version, rights_consent, training_consent
  ) values (
    p_owner_id, p_client_request_id, normalized_name, normalized_notes, p_model_version, p_rights_consent, p_training_consent
  ) returning * into created;

  insert into public.disease_request_photos (request_id, owner_id, position, object_path, capture_source)
  select created.id, p_owner_id, (photo.ordinality - 1)::smallint,
    format('requests/%s/%s/%s-%s.jpg', p_owner_id, created.id, photo.ordinality - 1, photo.hash),
    p_photo_sources[photo.ordinality]
  from unnest(p_photo_hashes) with ordinality as photo(hash, ordinality);

  insert into public.api_rate_limits (action, window_start, subject_hash, request_count)
  values ('disease_request', current_date, p_rate_subject, 1)
  on conflict (action, window_start, subject_hash) do update
    set request_count = public.api_rate_limits.request_count + 1, updated_at = now();

  return query select created.id, created.status, 'created'::text;
end;
$$;

revoke all on function public.create_disease_request_with_quota_v2(uuid, uuid, text, text, text, boolean, boolean, text[], text[], text) from public, anon, authenticated;
grant execute on function public.create_disease_request_with_quota_v2(uuid, uuid, text, text, text, boolean, boolean, text[], text[], text) to service_role;

commit;
