# 04 — Experimental Gmail: a separate, listed-tester environment only

> **Not live-verified.** This code has not been exercised with a valid Google access token, real Firebase user, real mailbox or deployed origin. Do not connect sensitive inboxes until the implementation, data handling and threat model have been reviewed and the complete flow is verified using dedicated test mailboxes. The public site keeps Gmail off.

## Scope, consent and eligibility

Google's `https://www.googleapis.com/auth/gmail.readonly` is a **restricted** scope and permits broad reading of a user's mailbox, even though Ranger's current test code searches only a small subset. A separate genuine **External / Testing** OAuth project and an explicit list of tester accounts may be used under Google's current testing rules (generally up to 100 listed test users, with warnings and testing limitations). **Testing is not a workaround for public production.** Public server-backed use generally requires Google's restricted-scope verification and may require an independent recurring security assessment that can cost money. Confirm the current policy and cost before changing launch scope: [Gmail scopes](https://developers.google.com/workspace/gmail/api/auth/scopes), [restricted-scope verification](https://developers.google.com/identity/protocols/oauth2/production-readiness/restricted-scope-verification), [Google User Data Policy](https://developers.google.com/workspace/workspace-api-user-data-developer-policy).

**Eligibility is also unresolved:** Google's policy permits only specified Gmail use cases, including certain reporting/monitoring that improves the email experience, and excludes certain one-time/manual email-export applications. Ranger extracts spending fields rather than offering a general email export, but whether this particular use case qualifies is Google's decision; verification or a Testing project does not guarantee eligibility. Confirm the intended use case with Google before enabling real inbox access. See the [approved Gmail use cases](https://developers.google.com/workspace/workspace-api-user-data-developer-policy).

Firebase sign-in verifies identity; Google Gmail authorization is a separate permission. In the test build, the sign-in modal contains the privacy disclosure and then offers a Google authorization/sync button **in the same modal** after identity verification. Google may display another consent/account chooser; there is no extra Ranger consent form. A user may continue without Gmail. Because the code uses short-lived access tokens and does not persist refresh tokens, a later on-demand sync may require another user click/Google prompt from Settings. It does not run a background or scheduled sync, especially while Render sleeps.

## A. Isolate the environment

1. Create separate test frontend/backend deployments, Firebase project and test database/project or sanitized branch. Do not copy real users' data into the test setup; Neon branches may inherit their parent's data. Limit who can access the test accounts and database. A public/demo and a test Render service share the free workspace-hours allowance.
2. Review [security and limitations](05_SECURITY_AND_LIMITS.md). Use only fictitious receipts in a dedicated tester inbox during initial validation, not your personal mailbox. Publish a deployment-specific privacy policy, real contact information and any Google-required details; the included privacy text is a draft.
3. In a separate Google Cloud project for Gmail testing, configure OAuth consent as **External**, choose **Testing**, add each intended Google test account as a test user, and enable the **Gmail API**. Add the required `openid`, `email` and `gmail.readonly` scopes as appropriate in the current Google console. Google can display warnings and request additional setup. Its rules, including refresh-token restrictions, can change; this implementation does not use a refresh token.
4. Create an OAuth **Web application** client for Google Identity Services. Add the exact **Authorized JavaScript origins** you will use, for example `http://localhost:5173` and the HTTPS test-frontend origin. Origins have scheme + host (+ port when needed), not paths or trailing slashes. Do not use a public raw IP; localhost has special development treatment. The token-model client does not use a backend OAuth redirect URI. Google project/client requirements should be checked against current docs.

## B. Configure test-only variables

On the **test frontend** build only:

```text
VITE_ENABLE_GMAIL_TEST=true
VITE_GMAIL_TEST_CLIENT_ID=<testing OAuth web client ID>
VITE_API_URL=<test backend origin>
# Also configure the separate test Firebase web app values.
```

On the **test backend** only:

```text
RANGER_GMAIL_TEST_MODE=true
RANGER_GMAIL_CLIENT_ID=<the same testing OAuth web client ID>
RANGER_GMAIL_TESTERS=<comma-separated verified account email addresses>
FIREBASE_PROJECT_ID=<test Firebase project ID>
RANGER_CORS_ORIGINS=<exact test frontend origin>
SPRING_PROFILES_ACTIVE=neon
# RANGER_DB_* values for the isolated test database, set privately.
```

The OAuth client ID is public by design; the tester list is personal information, and database passwords/tokens are sensitive. Keep the list and credentials out of Git/logs. Redeploy the frontend after changing any `VITE_*` value. The backend checks its enable flag, listed and verified Firebase email, Google token scope/client, Google verified email and Gmail profile, and linked Google identity when present; it refuses a mismatched mailbox. These checks are **untested with real credentials** and are not an independent security assessment. The test-only Google `tokeninfo` check is not a production verification architecture.

## C. What the connector does—and does not do

When a listed tester explicitly authorizes and triggers a sync, the server searches at most **25** Gmail messages matching order/receipt/invoice subjects from the last **90 days**; it then fetches supported HTML message bodies. It does not paginate the rest of the mailbox or handle attachments/PDFs. HTML must match the narrow parser shapes; unsupported receipts are skipped and extracted imports are marked for review. The response body is bounded to 512 KiB, the sync has an approximate 35-second overall budget with 10-second per-Google-request limits, and raw HTML/access tokens are not intentionally stored. A timeout can happen after some imports succeeded: retrying uses the stable mailbox/message ID to avoid sequential duplicates. Test concurrency separately before trusting this claim under load.

One user's Google mailbox email must match the verified Firebase email; a Gmail user signing in with email/password still has to authorize that matching Google mailbox. There is no guarantee arbitrary merchant email templates will parse correctly. Users can revoke the Google grant at [Google Account permissions](https://myaccount.google.com/permissions); signing out of Firebase alone does not necessarily revoke Google permission.

## D. Test acceptance checklist

- No Google token/Gmail permission is requested on the public demo.
- Test login disclosure is accurate; Google explicitly asks before broad read-only access; denial or popup closure is handled.
- A nonlisted test account, unverified Firebase email, wrong OAuth client/scope, wrong mailbox, and mismatched linked Google identity all fail closed.
- A listed test account can sync a fake supported receipt; the same message is not duplicated on repeated sequential sync; an unsupported template is skipped.
- Another tester cannot view/change/delete those records; timeouts, token expiry, partial sync and revoke/re-consent work as expected.
- Inspect app/server/host logs and backups for accidental raw message/token disclosure; test deletion and document retained backups.

If Google blocks the app, the user sees an unverified-app warning, or an API policy requires verification, follow Google's current requirements. Do not claim verification, production readiness or zero assessment cost without evidence.
