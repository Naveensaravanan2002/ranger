import { defineConfig, devices } from '@playwright/test'
export default defineConfig({testDir:'./e2e', timeout:90000, expect:{timeout:15000}, fullyParallel:false, workers:1,
  use:{baseURL:process.env.RANGER_E2E_BASE_URL || 'http://127.0.0.1:5173',trace:'retain-on-failure'},
  projects:[{name:'desktop',use:{...devices['Desktop Chrome']}},{name:'mobile',use:{...devices['iPhone 13'],browserName:'chromium'}}],
})
