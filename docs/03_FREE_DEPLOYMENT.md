# 03 — Publish the demo on free tiers

This is a **deployment recipe, not a claim that Ranger has already been deployed or passed a production review**. Provider plans, quotas and dashboard labels change. Check the current terms and eligibility first. Keep the public demo and the Gmail-testing environment separate.

## 1. Push your reviewed source

After backing up and merging your existing project, commit in IntelliJ and push the repository to your own GitHub account. Do not commit `.env.local`, passwords, full database URLs, OAuth tokens or keys. The ZIP excludes `.git` and local config.

## 2. Create a Render Free web service for Spring

1. In Render, connect the reviewed repository and create a **Web Service**, selecting **Docker** and the **Free** instance if available.
2. For this monorepo, set **Root Directory** to `backend`; Dockerfile path is `./Dockerfile` and build context is `.` relative to that root. Check [Render monorepo support](https://render.com/docs/monorepo-support) and the current dashboard. The Dockerfile runs as a non-root user and sets a limited JVM percentage; whether it fits the real Free instance has not been verified.
3. Set **Health Check Path** to `/api/public/health`. `PORT` is supplied by the platform; the Dockerfile sets `BIND_ADDRESS=0.0.0.0`. Do not force `PORT` to a different value without understanding the host settings.
4. Choose one setup:
   - **Public synthetic demo only, no accounts:** set `RANGER_REQUIRE_PERSISTENT_DB=false`; leave `FIREBASE_PROJECT_ID` empty and Gmail disabled. The service intentionally uses disposable H2. With no Firebase project configured, real bearer authentication is rejected. This explicitly opted-in setting is acceptable only for fictional demo data.
   - **Persistent, authenticated environment:** leave the Docker guard enabled, run the reviewed SQL on the correct Neon database, and set `SPRING_PROFILES_ACTIVE=neon`, `RANGER_DB_HOST`, `RANGER_DB_NAME`, `RANGER_DB_USER`, `RANGER_DB_PASSWORD`, `FIREBASE_PROJECT_ID` and `RANGER_CORS_ORIGINS`. The guard prevents a container from silently falling back to H2. Firebase sign-in and private data need additional live acceptance/security testing before use.
5. Set `RANGER_CORS_ORIGINS` to the exact static frontend origin, e.g. `https://your-own-site.vercel.app` (no trailing slash, no path). For multiple explicit environments, separate origins by commas. There is no wildcard and no authentication cookie. Update it after the frontend URL is known, then redeploy/restart the backend.
6. Leave `RANGER_GMAIL_TEST_MODE=false` and Gmail client/tester variables empty on the public service. Set secrets in the host's protected environment-variable UI—not in Git. Never share the Render env screen if it exposes values.
7. Deploy and open `https://<your-render-host>/api/public/health`; it should return JSON `{"status":"ready"}` after the service starts. The demo endpoint is `/api/public/demo`. If the service fails, inspect logs privately for missing variables/schema mismatch. Do not copy logs containing secrets into public issues.

## 3. Create the static frontend

On Vercel, import the same repository and choose **Root Directory** `frontend`, framework **Vite**, install command `npm ci`, build command `npm run build`, output directory `dist`. The included `vercel.json` supplies the SPA rewrite and some basic headers. Set Vercel's Node version to a compatible current LTS (20.19+, 22.12+, or 24).

At **build time**, set:

- `VITE_API_URL=https://<your-render-host>` — the backend origin only, no `/api` or trailing slash. This is public in the resulting browser bundle.
- For a **demo-only** site, leave all `VITE_FIREBASE_*` values blank and `VITE_ENABLE_GMAIL_TEST=false`.
- If deliberately enabling accounts after completing [Firebase setup](02_DATABASE_AND_AUTH.md), also supply the four Firebase web-config fields. The Firebase project ID must match the backend. Never use service-account keys or database passwords in any `VITE_*` variable.

Deploy, add the actual Vercel hostname to Firebase **authorized domains** if using Firebase, and put the exact `https://…` origin in Render `RANGER_CORS_ORIGINS`. Vite variables are embedded **at build time**: redeploy the frontend after changing them. Vercel Hobby is intended for personal/non-commercial projects; see its current terms.

The app calls the configured backend origin from the browser. Local development uses a Vite `/api` proxy; it does **not** use `localhost` in a remote visitor's browser. Configure an appropriate CSP and additional security headers for your actual hostnames before production; the starter headers alone are not a security review.

## 4. Verify the real deployment

- Test landing, direct `/privacy` and `/app/trail` refresh, desktop/mobile navigation, demo totals, search/filter and tag changes.
- Stop using the service for at least the platform's sleep interval, then load the static site. It should appear immediately, while its bounded health checks wake the backend. The fictional fallback is labelled offline; when `/api/public/demo` succeeds, the badge changes to “processed by the real parser.” A private ledger request shows the goat loader while actually waiting and offers a demo/visible error, not a fake percentage.
- Record actual build time, cold-start time, memory, HTTP 503/HTML behavior, CORS, and failure/retry behavior. The client retries readiness for about 90 seconds with request timeouts; it can report offline even if a particularly slow service comes up later, so use Retry. A static site cannot keep the backend awake without extra traffic, and this app does not run an automatic keep-alive.
- If enabling accounts, test fresh user registration/verification, Google sign-in and real signed Firebase ID tokens, two-account isolation, private import/persistence, settings and deletion. Test from your actual frontend origin, not just localhost. Review logs, backups, rate limits, CSP, incident response and privacy text before any real data.

## 5. Free-tier reality check

At the time these notes were written, [Render Free](https://render.com/docs/free) advertised 512 MB, 0.1 CPU and spin-down after 15 minutes without inbound traffic; wake can take around a minute and actual application readiness can take longer. Free web services share a 750-hour/month workspace allowance; a public service **plus** a separate tester service can consume it faster. Services can be suspended when the allowance is exhausted; billing rules depend on account setup. Runtime filesystem writes are ephemeral. Render Free is not a production SLA. Neon Free has its own usage and restore limits, and free branch expiry must be checked independently. Provider terms can change; set safeguards and verify your actual bill before enabling anything paid.
