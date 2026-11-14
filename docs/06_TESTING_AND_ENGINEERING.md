# 06 — Tests and engineering tour

## What was tested in the build workspace

As of 29 October 2026, on a sandbox using JDK 21 and Node 20:

- **Backend:** 11 JUnit tests passed against ephemeral H2: context startup, parser examples/edge cases, late-night and money analytics, public demo totals, unauthorized/unverified access, mocked-JWT owner isolation, sequential import idempotency, cross-owner write/delete denial, disabled Gmail, the 1 MiB request cap, container-profile guard and bounded Google response subscriber.
- **PostgreSQL:** the provided SQL was applied to a local PostgreSQL 17 test database; the `neon` profile's JPA schema validation and owner-scoped CRUD tests with mock JWTs passed against it. This is not a test of your real Neon project, its TLS settings or provider uptime.
- **Frontend:** TypeScript/Vite production build and eight Vitest tests passed (five arithmetic and three simulated wake/retry/cancellation tests); npm audit reported no known advisories at the time of the check.
- **Browser:** eight Playwright tests passed across desktop Chromium and a mobile-sized Chromium context: landing/demo, live synthetic parser badge and totals, Trail search/filters/details/tag edits, calculator, privacy, unconfigured login messaging, settings, horizontal overflow and offline fallback. Mobile viewport testing is not a substitute for physical iOS/Android or every browser.
- Public demo/health returned HTTP 200 and a private endpoint returned 401 with no valid token. A local two-origin browser smoke test reached the API with the allowed CORS origin; an unlisted origin was rejected. This does not verify deployed CORS. The Google Firebase JWK endpoint was reachable, but **no valid Firebase ID token was used**.

Not tested here: deployed Render memory/cold start/CORS under a real hostname, your current Neon branch/password/TLS, real Firebase Google/password sign-in or signed-token verification, real Gmail OAuth/tokens/mailbox/consent/revocation, production security, payment/medical outcomes or regulatory compliance. Re-run all acceptance checks with your own accounts and test data.

## Run the checks

Windows, backend terminal (never run tests using a real/important Neon profile):

```powershell
cd backend
.\mvnw.cmd test
.\mvnw.cmd -DskipTests package
```

Frontend terminal:

```powershell
cd frontend
npm ci
npm test
npm run build
npm audit
# Optional browser tests; start backend and frontend first in separate terminals.
npx playwright install chromium
npm run test:e2e
```

The browser tests default to `http://127.0.0.1:5173`; override `RANGER_E2E_BASE_URL` if necessary. They require the backend for the live-parser test, and also deliberately simulate an offline backend. `npm test` runs the Vitest files under `src`; Playwright specs live under `e2e` and are run separately. Test traces/reports are disposable and ignored by Git.

## Try a fictional manual import after configuring sign-in

This is **test data**, not a real email. Once Firebase and persistent storage are set up and tested, use **My ledger → Import** and paste:

```html
<div data-merchant="Example Cafe" data-category="FOOD"></div>
<div data-total="125.00"></div>
<time datetime="2026-10-22T23:00:00+05:30"></time>
<div data-item>
  <span class="item-name">Burger</span>
  <span class="item-amount">100.00</span>
</div>
<div data-fee>
  <span class="fee-name">Delivery</span>
  <span class="fee-amount">25.00</span>
</div>
```

Expected: ₹125 recorded spend, ₹25 included fees, ₹100 initially tagged as junk and, at the default India interval, ₹125 late-night. Reimporting the exact same HTML sequentially for the same account is treated as a duplicate; a different account gets its own scoped record. This does not prove arbitrary merchant emails will parse.

## Follow one request through the code

1. `frontend/src/lib/api.ts` calls the API; `store.tsx` separates synthetic data from the account ledger and guards against stale user responses after sign-out.
2. `SecurityConfig` verifies a Firebase bearer ID token (when configured), then requires a verified email. `LedgerController` passes the token's subject to service methods. There is no client-supplied owner parameter.
3. `ReceiptParser` uses Jsoup to strip executable elements and extract supported shapes into Java records. `LedgerService` associates deterministic source IDs with the owner; JPA persists `LedgerReceipt`, `LedgerItem`, `LedgerFee` with the `(owner_uid, source_key)` uniqueness constraint. Per-value money is `BigDecimal`/`NUMERIC(12,2)` and times are `Instant`/`TIMESTAMPTZ`.
4. `AnalyticsService` computes summaries using the requested timezone and an inclusive-start/exclusive-end interval. Hibernate `@BatchSize(50)` batches the two child collections to reduce N+1 queries, but unpaginated ledger loading still needs scale work.
5. React's Camp/Trail/Stash consume DTOs, never raw JPA entities. The selected period is calculated in the UI using the chosen timezone; API summaries are for the requested interval and all fetched records.

The frontend uses a relative `/api` proxy only in local development. A published static bundle uses the configured public `VITE_API_URL` backend origin. The API validates exact CORS origins and is stateless; it does not rely on browser auth cookies. The supplied Dockerfile is not proof that Render's Free machine has enough resources.

## Where to customize

- Starter colours and fonts: first `:root` declaration and `@font-face` rules in `frontend/src/styles.css`. Some decorative rules use literal colours too; review contrast after changing them.
- Goat and scenery: `frontend/public/assets/`; keep the mascot 2D if following the agreed vision.
- Synthetic fixtures: `backend/src/main/resources/demo/` and the matching offline sample `frontend/src/lib/demo.ts`; update the expected tests if changing the examples.
- Supported receipt formats: `backend/src/main/java/com/ranger/backend/parser/ReceiptParser.java`; add redacted fixtures and tests, don't accept scripts or claim unsupported formats.
- Default late interval: frontend `store.tsx`, backend controller defaults and analytics tests. Timezone settings are a browser preference, not a synced user profile.
