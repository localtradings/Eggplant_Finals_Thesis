# Eggplant Disease Detector Admin

Private Next.js administration and mobile API service for the Eggplant Disease Detector. The dashboard reads live Supabase data, protects every administrative page and mutation with both Supabase authentication and `admin_members` authorization, and never substitutes placeholder production metrics.

## Runtime configuration

Configure these values in Vercel encrypted environment variables. Do not commit them:

- `NEXT_PUBLIC_SUPABASE_URL` — public Supabase project URL.
- `NEXT_PUBLIC_SUPABASE_PUBLISHABLE_KEY` — public publishable key used by browser authentication.
- `SUPABASE_SECRET_KEY` — server-only secret key used by protected route handlers and the admin data layer.
- `CRON_SECRET` — secret bearer token used by the daily retention/deletion job.
- `ADMIN_LOGIN_NAME` — server-only login alias, such as `admin`.
- `ADMIN_LOGIN_EMAIL` — server-only email of the existing Supabase Auth user that owns the admin session.

The service starts safely with cloud writes disabled in `app_config`. Read APIs and the offline-first Android client continue to work while writes are paused.

## Local verification

Use Node.js 22 or later:

```bash
npm ci
npm run lint
npm run typecheck
npm test
npm run build
```

Run locally with configured environment variables:

```bash
npm run dev
```

The health endpoint is `GET /api/health`. Mobile endpoints require a valid Supabase anonymous-user bearer token. Administrative routes require a password session whose user ID exists in `admin_members`. The login name is only a server-side alias for the configured Supabase Auth email; passwords are verified and stored by Supabase Auth, never by this dashboard.

## Admin password setup

1. Ensure the configured `ADMIN_LOGIN_EMAIL` already exists in Supabase Auth and has the intended password.
2. Add that Auth user’s UUID to `public.admin_members` with the required role (`owner`, `admin`, or `reviewer`) using the approved Supabase administration process.
3. Set `ADMIN_LOGIN_NAME=admin` and `ADMIN_LOGIN_EMAIL` in the deployment’s encrypted environment variables.
4. To skip email confirmation for this password-based admin flow, disable Supabase Auth’s **Confirm Email** setting for the target project. The versioned local configuration also has confirmations disabled for local development; hosted-project settings must be changed separately.

Do not commit a password or place one in `NEXT_PUBLIC_*` configuration. A short shared credential should only be used temporarily in a private, controlled environment; use a unique longer password before public deployment.

## Deployment

The Vercel project root directory is `admin/`. `vercel.json` schedules the daily maintenance route, which:

- immediately keeps expired photos out of the public feed and retries object cleanup;
- processes scoped user cloud-deletion requests;
- records failed work instead of returning a false success.

Promote a deployment only after authentication, authorization, Storage, feed, reporting, deletion, lint, type-check, test, build, and runtime-header smoke tests pass.
