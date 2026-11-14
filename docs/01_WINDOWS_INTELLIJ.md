# 01 — Start on Windows with IntelliJ

## 1. Protect the work you already have

The delivered source was based on the GitHub starter at commit `b1da6a6`; it does **not** contain your uncommitted Windows changes or IntelliJ settings. If your existing project is `D:\ranger`, first copy that entire folder to a safe backup location. In IntelliJ, use **Git → Commit** or shelve your changes, as appropriate, before merging. Do not commit a password or full database URL. If you are unsure, keep the backup and compare files before overwriting anything.

Extract `Ranger-Complete-Source.zip` into a **new folder**, e.g. `D:\ranger-delivery`, and open the extracted `Ranger-Project` folder in IntelliJ. You can evaluate it there without touching `D:\ranger`. Later, use IntelliJ's comparison tools to merge the delivered `backend`, `frontend`, docs and configuration into your existing repository. Preserve your original `.git` and local IDE configurations; the ZIP intentionally has neither. The separate component ZIPs are optional, not needed for this first run.

## 2. Verify the free prerequisites

- A **Java 21 JDK** (not just a JRE); IntelliJ **File → Project Structure → Project SDK** should select it. Your Temurin 21 installation is suitable.
- **Node.js** 20.19+ or 22.12+; Node 24 is suitable. Confirm in IntelliJ Terminal: `node -v` and `npm -v`.
- IntelliJ IDEA, Internet to download dependencies the first time, and the Maven wrapper included in `backend`. No separate Maven installation is necessary.
- No Firebase, Neon, Gmail or paid account is needed for the synthetic demo.

## 3. Run the backend

Option A — IntelliJ: open the project; select JDK 21; allow the Maven import when prompted. Open `backend/src/main/java/com/ranger/backend/BackendApplication.java` and run its `main` method. The working directory should be the `backend` folder. Do **not** set the `neon` profile for the first demo run. Wait for `Started BackendApplication`.

Option B — IntelliJ Terminal, PowerShell:

```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

Go to `http://localhost:8080/api/public/health`; a ready backend returns JSON with `status: ready`. This default uses an **in-memory H2** database; it is deleted at shutdown. The default backend listens only on loopback (`127.0.0.1`).

If port 8080 is already occupied, either stop the other process or set `PORT=8081` in the IntelliJ run configuration **and** start the frontend with `RANGER_DEV_BACKEND=http://127.0.0.1:8081`. Do not share an H2 preview as a durable database.

## 4. Run the frontend

Open another terminal in the extracted project root:

```powershell
cd frontend
npm ci
npm run dev
```

Open `http://localhost:5173`. The dev server is local-only by default and proxies `/api` to `http://127.0.0.1:8080`; the **browser** does not call a separate localhost API in a hosted deployment. `VITE_API_URL` is left blank for local development. Click **Explore the demo** and look for “Fictional receipts · processed by the real parser.” If the API is down, the page remains useful but honestly labels the preloaded sample as offline.

Explore Camp → Trail (search, filter, open a receipt, switch a junk tag) → Stash. Click Settings to change timezone/late hours. Demo edits are in memory only. The all-time synthetic receipts are dated October 2026, so a time filter can legitimately show zero later.

## 5. Optional: run checks

In a terminal at `backend`, `./mvnw.cmd test` (PowerShell accepts `.\mvnw.cmd test`) uses isolated, ephemeral H2 by default. **Do not point tests at your real Neon database.**

In `frontend`:

```powershell
npm test
npm run build
# Optional desktop/mobile browser checks, with both local servers already running:
npx playwright install chromium
npm run test:e2e
```

Browser tests download a free Chromium binary. On Linux, Playwright might also need OS packages; follow Playwright's installation message.

## 6. Bring it into your GitHub repo

After you are happy with the extracted copy and have backed up your old project, compare/merge it into `D:\ranger` in IntelliJ. Keep the existing `.git` folder and review the **Commit** window carefully. `.env.local`, secrets, `node_modules`, `dist`, `target`, logs and IDE files should not be committed; `frontend/.env.example` is intentionally safe to commit. Set the Git author **for this repository only**, not globally, in IntelliJ's Git settings or its repository-specific configuration if needed. Then commit and push through IntelliJ.

If IntelliJ reports a wrong JDK or the wrapper cannot download, check Project SDK, Maven runner JDK and your network. If the public sample works but sign-in says setup is required, that's expected: continue to [database + authentication](02_DATABASE_AND_AUTH.md).
