# Ranger frontend

React 18 / TypeScript / Vite. From this directory: `npm ci`, then `npm run dev` and open `http://localhost:5173`. A local backend on port 8080 activates the real synthetic parser; otherwise an explicitly labelled fictional offline sample works. `npm test`, `npm run build`, and `npm run test:e2e` (after installing Chromium and starting both services) run the checks.

The complete Ranger ZIP includes `../docs/` with setup, Neon, Firebase, deployment and security instructions. `.env.example` lists public browser configuration; do not put passwords, OAuth access tokens, service-account keys or database credentials in `VITE_*`. Firebase and Gmail are unconfigured/untested until you complete your own account-side setup; Gmail is for a separate listed-tester environment only. Colours can be changed near the top of `src/styles.css`.
