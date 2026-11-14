# 02 — Neon persistence and Firebase Authentication

These steps use your own consoles. We cannot see or verify your accounts. **Do not paste passwords, full URLs, tokens or private keys into chat.**

## A. Choose the right Neon branch and back it up

1. In Neon, confirm the exact **project, database and branch** you intend to modify. Your earlier setup used `ranger-db` in AWS Singapore and a `development` child branch, but its present status is unknown.
2. Important: Neon Console-created **child branches may default to expiring after one day**. Open **Branches → development → ⋮ → Set expiration** and turn off any expiry if you intend to keep that branch. The default parent branch does not expire this way. Verify the UI for your account. A child branch is separate from normal compute scale-to-zero; expiry can delete the branch. Keep independent secure backups. See [Neon branch expiration](https://neon.com/docs/guides/branch-expiration).
3. Back up before applying schema. In the Neon SQL Editor, connect explicitly to the **development branch/database**, then review and run `backend/src/main/resources/db/schema/001_initial.sql` once. It creates `ranger_receipts`, `ranger_items` and `ranger_fees` and their indexes. `IF NOT EXISTS` does not repair an incompatible older schema; Hibernate validation should reveal a mismatch.
4. For a tester environment use a separate, access-controlled project/database or a truly sanitized branch. A branch can inherit data from its parent; **do not clone real users' data into a test environment**. Never run the test suite against an important database.

There is no Flyway. On `neon`, Hibernate uses `ddl-auto=validate`, not automatic schema mutation. For a future schema change, author/review a versioned SQL change, back up, apply it deliberately to each intended environment and deploy compatible code together. The default local H2 profile uses `create-drop` and is not persistent.

## B. Configure IntelliJ backend

**Run → Edit Configurations → BackendApplication**; select JDK 21. Set the following **environment variables** using IntelliJ's environment editor. Enter their values privately; examples are names, not actual credentials.

| Name | Value / purpose |
|---|---|
| `SPRING_PROFILES_ACTIVE` | `neon` |
| `RANGER_DB_HOST` | Your Neon **host name only**; no `https://`, database path, query string or credentials. |
| `RANGER_DB_NAME` | Your database name, e.g. `neondb`. |
| `RANGER_DB_USER` | Your database role. |
| `RANGER_DB_PASSWORD` | Its password, entered privately in the run configuration. |
| `FIREBASE_PROJECT_ID` | The exact Firebase project ID, after Part C; leave empty while testing a demo-only backend. |
| `RANGER_CORS_ORIGINS` | `http://localhost:5173` for local development; exact comma-separated frontend origins for deployment. |

Restart the backend and verify startup succeeds with profile `neon`, Hikari, schema validation and a connection to the **intended** database. The profile uses TLS `sslmode=verify-full`, the Java SSL factory and required SCRAM channel binding, as in your earlier successful connection. A fresh SQL + local PostgreSQL schema test passed in our sandbox, but **your delivered configuration and current Neon branch have not been re-tested here**. If TLS, role, project/branch or network settings differ, use Neon's current JDBC connection guidance; do not post the connection string. IntelliJ run configurations can contain sensitive values on your machine: do not share or commit them.

## C. Create/configure Firebase Auth on Spark

1. Create/select a Firebase project on the **Spark** plan. Under **Authentication → Sign-in method**, enable **Google** and **Email/Password**. Configure the requested public support details.
2. Add a Firebase **Web app** and obtain its web configuration. Under Authentication settings, add the frontend's exact authorized domain(s), including `localhost` for local testing and your actual static-host domain after deployment. Newly created projects may not include localhost automatically.
3. In `frontend`, copy `.env.example` to `.env.local`, then fill `VITE_FIREBASE_API_KEY`, `VITE_FIREBASE_AUTH_DOMAIN`, `VITE_FIREBASE_PROJECT_ID`, and `VITE_FIREBASE_APP_ID` from your Firebase web app. Leave `VITE_API_URL` blank locally and both Gmail test values disabled/empty. Restart Vite after editing the file.
4. Set `FIREBASE_PROJECT_ID` in the backend to the **same exact project ID** as the frontend. The backend rejects private authentication when the ID is missing; it validates Firebase ID-token signature, issuer, audience, subject, time and verified-email claim for configured projects. No Firebase service-account key is needed for this implementation.
5. Test both providers with accounts you control. Password sign-up sends an email verification link; the user must verify the email before accessing the private API. Password reset is handled by Firebase. A Google sign-in identifies the user; **it is not Gmail permission**.

The `VITE_*` Firebase **web** values and OAuth client IDs are intentionally visible in browser code; they are not administrative credentials. Do not place passwords, service-account keys or tokens in frontend variables. Configure Firebase authorized domains, appropriate API restrictions, email templates and abuse protections; avoid publishing an unrestricted project without review. See [Firebase Auth](https://firebase.google.com/docs/auth) and [Firebase pricing](https://firebase.google.com/pricing).

## D. Verify private storage carefully

After sign-in, open **My ledger → Import**, paste only a **fictional** supported receipt first, and check Camp/Trail. A simple supported example is in [testing notes](06_TESTING_AND_ENGINEERING.md). Restart the backend and confirm it remains only when the correct Neon profile is active. Test with a second account that neither can see, update or delete the other's receipt. Then test deletion; deleting the Ranger ledger does not delete the Firebase account or your external backups. Actual Firebase signature validation and cross-account checks with real provider tokens remain your deployment acceptance tests.
