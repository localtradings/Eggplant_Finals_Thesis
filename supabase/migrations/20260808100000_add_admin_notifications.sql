-- Published administrator messages for the mobile notification inbox.
-- The table is intentionally service-role-only: admin and mobile API routes
-- authenticate the caller first, then read/write through trusted server code.
create table if not exists public.admin_notifications (
  id uuid primary key default gen_random_uuid(),
  idempotency_key uuid not null unique,
  category text not null check (category in ('announcement', 'update', 'tip', 'alert')),
  title_en text not null check (char_length(btrim(title_en)) between 2 and 120),
  body_en text not null check (char_length(btrim(body_en)) between 2 and 2000),
  title_fil text not null check (char_length(btrim(title_fil)) between 2 and 120),
  body_fil text not null check (char_length(btrim(body_fil)) between 2 and 2000),
  status text not null default 'published' check (status in ('published', 'archived')),
  published_at timestamptz not null default now(),
  expires_at timestamptz,
  created_by uuid references auth.users(id) on delete set null,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create index if not exists admin_notifications_published_idx
  on public.admin_notifications (status, published_at desc);

create trigger admin_notifications_set_updated_at
before update on public.admin_notifications
for each row execute function app_private.set_updated_at();

alter table public.admin_notifications enable row level security;
revoke all privileges on public.admin_notifications from public, anon, authenticated;
grant select, insert, update on public.admin_notifications to service_role;
