# Ranger 🐐

**A portfolio-ready prototype for following the trail from receipts to spending insights.**
Ranger pairs a colourful, responsive React/Vite front end with a Java 21 + Spring Boot API and PostgreSQL. The full-body 2D cowboy goat and temporary palette are deliberately editable.

> **Release status:** the synthetic demo, parser, arithmetic and local database paths have been tested. Firebase sign-in, a live Neon database in this delivered build, Render deployment/cold starts and the experimental Gmail integration still require your own configuration and live verification. **Do not invite people to connect real inboxes or describe Gmail as production-ready yet.**

## Choose your starting point

1. **Try it locally, without accounts:** [Windows + IntelliJ start here](docs/01_WINDOWS_INTELLIJ.md).
2. **Keep data and enable sign-in:** [Neon + Firebase setup](docs/02_DATABASE_AND_AUTH.md).
3. **Publish a free public demo:** [Render + Vercel deployment](docs/03_FREE_DEPLOYMENT.md).
4. **Only later, for specifically listed Gmail testers:** [Separate Gmail testing environment](docs/04_GMAIL_TESTING_ONLY.md).
5. **Before any real personal data:** read [Security, privacy and limitations](docs/05_SECURITY_AND_LIMITS.md) and [Tests + engineering notes](docs/06_TESTING_AND_ENGINEERING.md).

The complete ZIP is the recommended download. Separate backend/frontend ZIPs are optional copies of its two subfolders; they are not extra features.

## What's inside

- **Landing** — responsive, layered scrolling animation, original 2D goat artwork, privacy route.
- **The Camp** — spending, fees, late-night purchases, editable-tag totals and explainable, playful copy.
- **The Trail** — searchable/filterable receipts, extracted item/fee details, item tag correction and deletion.
- **The Stash** — explicitly hypothetical gym, gold, investment and sleep comparisons; not medical or financial advice.
- **Synthetic demo** — no login, no real inbox. The backend parses seven fictional HTML fixtures. An independently labelled offline sample remains usable while the API is asleep/offline.
- **Accounts / manual HTML import** — Firebase Auth + server-side ID-token verification and owner-scoped storage are implemented, **but require setup and live testing**. The parser accepts specific HTML shapes, not arbitrary bank/email formats.
- **Gmail** — disabled by default; an experimental, on-demand connector is present for a *separate genuine testing environment* with explicitly listed testers. It has never been tested here with a valid Google grant or real mailbox. This is **not** a public Gmail release.

### What the numbers mean

The all-time demo reports **₹2,036 recorded spend**, **₹159 extracted fees**, **₹1,087 late-night orders** and **₹979 priced junk-food items**. All figures are fictional. Fees are generally already in the order total; late-night and junk-food categories overlap, so do not add them to spend. Investment contributions are not spend. Unknown item prices are not invented. The default late interval is **23:00 inclusive to 04:00 exclusive**, interpreted in the selected timezone. A receipt's explicit order time is preferred; otherwise the time is labelled approximate.

The demo fixtures are dated October 2026. “All time” is stable; “this month” and “last 30 days” depend on the current date. Your display settings live in your browser.

## Quick local demo

Requirements: **JDK 21**, **Node.js 20.19+ or 22.12+** (Node 24 is fine), Internet access for first dependency download, and IntelliJ IDEA or a terminal. No accounts, passwords or paid APIs are needed for the demo.

Open two terminals in the extracted project. On Windows PowerShell:

```powershell
# Terminal 1 — backend (H2 is disposable and used for this demo only)
cd backend
.\mvnw.cmd spring-boot:run
```

```powershell
# Terminal 2 — frontend
cd frontend
npm ci
npm run dev
```

Visit **http://localhost:5173**. Click **Explore the demo**. The badge says whether the sample came from the real parser or the offline fallback. Backend health: **http://localhost:8080/api/public/health**. Default servers listen on your own computer, not the public Internet. When you stop the backend, its default in-memory H2 data is lost; use the documented Neon profile for persistence.

Linux/macOS: use `./mvnw spring-boot:run` instead. First Maven and npm runs download free open-source dependencies.

## Repository map

```text
backend/                  Java 21 / Spring Boot 4.1.1 / Maven wrapper
  src/main/java/...       REST API, security, JPA, parser, analytics, tester Gmail
  src/main/resources/demo/             seven fictional receipts
  src/main/resources/db/schema/        manually reviewed PostgreSQL DDL; no Flyway
  src/test/               parser, arithmetic, isolation, body-boundary tests
  Dockerfile              Render-oriented container; requires persistent profile
frontend/                 React 18 / TypeScript / Vite / Tailwind / Firebase client
  src/pages/              landing, Camp, Trail, Stash, privacy
  src/styles.css          starter colour tokens at the top of the file
  public/assets/          selected mascot, scenery and locally hosted fonts
  e2e/                    Playwright desktop/mobile checks
  .env.example            documented public frontend configuration

docs/                     installation, accounts, deployment and known limits
```

## Cost and responsible use

The local synthetic demo does not require paid AI services. Firebase Spark, Neon Free, Render Free and Vercel Hobby are intended starting tiers, **not guarantees of unlimited usage, uptime, current eligibility or future pricing**. A free backend can sleep; actual Ranger wake time and its 512 MB fit are untested. Set limits/alerts and check current provider terms before enabling services. Vercel Hobby is for personal/non-commercial use. Google also restricts eligible Gmail use cases; Ranger’s eligibility is unconfirmed. Restricted Gmail scopes can require Google verification and a paid independent assessment for public server-backed access; the genuine testing allowance does **not** turn this into an unrestricted free production release.

Never send passwords, full database connection strings, OAuth tokens, private keys or unredacted email contents in chat or commit them to Git. If a password or token has been exposed, rotate it in the relevant console.
