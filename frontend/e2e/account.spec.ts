import { test, expect, type Page } from '@playwright/test'

// Test-only module substitution: this never loads real Google credentials or calls Firebase.
async function mockFirebase(page:Page,options:{noPhoto?:boolean;failSignOut?:boolean}={}){
  await page.route('**/src/lib/firebase.ts',route=>route.fulfill({
    contentType:'application/javascript',body:`export const firebaseConfigured = true; export const auth = { currentUser: null };`
  }))
  await page.route(/\/node_modules\/\.vite\/deps\/firebase_auth\.js(?:\?.*)?$/,route=>route.fulfill({
    contentType:'application/javascript',body:`
      import {auth} from '/src/lib/firebase.ts';
      const listeners = new Set();
      const options = ${JSON.stringify(options)};
      const photo = options.noPhoto ? null : 'data:image/svg+xml,%3Csvg xmlns="http://www.w3.org/2000/svg" width="40" height="40"%3E%3Crect width="40" height="40" fill="%23a5c649"/%3E%3C/svg%3E';
      const makeUser = () => ({uid:'fake-account', email:'alex@example.test', emailVerified:true, displayName:'Alex Test', photoURL:photo, providerData:[{providerId:'google.com', photoURL:photo}], getIdToken:async()=> 'test-only-token'});
      if(sessionStorage.getItem('test-only-firebase-user')) auth.currentUser=makeUser();
      const emit = () => listeners.forEach(listener=>listener(auth.currentUser));
      export class GoogleAuthProvider {}
      export function onAuthStateChanged(_auth, listener) {listeners.add(listener); queueMicrotask(()=>{if(listeners.has(listener)) listener(auth.currentUser)}); return ()=>listeners.delete(listener)}
      export async function signInWithPopup(){auth.currentUser=makeUser();sessionStorage.setItem('test-only-firebase-user','1');emit();return {user:auth.currentUser}}
      export async function signInWithEmailAndPassword(){return signInWithPopup()}
      export async function createUserWithEmailAndPassword(){return signInWithPopup()}
      export async function sendEmailVerification(){}
      export async function sendPasswordResetEmail(){}
      export async function signOut(){if(options.failSignOut)throw new Error('simulated');auth.currentUser=null;sessionStorage.removeItem('test-only-firebase-user');emit()}
    `
  }))
  // Test the public identity flow independently of any local Gmail tester configuration.
  await page.route('**/src/lib/gmail.ts',route=>route.fulfill({contentType:'application/javascript',body:`
    export const gmailClientId='', gmailTestEnabled=false;
    export const loadGoogleScript=async()=>{}; export const startGmailSync=()=>{};
  `}))
  await page.route('**/api/public/health',route=>route.fulfill({json:{status:'ready'}}))
  await page.route('**/api/public/demo',route=>route.fulfill({json:{receipts:[]}}))
  await page.route('**/api/private/ledger**',route=>route.fulfill({json:{receipts:[]}}))
}

test('sign-in goes to Camp; account menu, refresh, demo and sign-out work',async({page},testInfo)=>{
  await mockFirebase(page)
  const errors:string[]=[];page.on('pageerror',error=>errors.push(error.message))
  if(testInfo.project.name==='mobile'){
    await page.goto('/app/stash')
    await page.locator('.app-top .top-login').click()
  } else {
    await page.goto('/')
    await page.getByRole('button',{name:'Sign in',exact:true}).click()
  }
  const modal=page.getByRole('dialog')
  await modal.locator('.disclosure input').check()
  await modal.getByRole('button',{name:/Continue with Google/}).click()
  await expect(page).toHaveURL(/\/app\/camp$/)
  await expect(page.getByText('YOUR PRIVATE LEDGER')).toBeVisible()
  await expect(page.locator('.total-amount')).toContainText('₹0')
  const account=page.getByRole('button',{name:'Account menu'})
  await expect(account.locator('img')).toBeVisible()

  await account.click()
  const options=page.getByRole('group',{name:'Your account options'})
  await expect(options.getByText('Alex Test')).toBeVisible()
  await expect(options.getByText('alex@example.test')).toBeVisible()
  await page.keyboard.press('Escape')
  await expect(options).toHaveCount(0)
  await expect(account).toBeFocused()

  await account.click()
  await options.getByRole('button',{name:'Settings'}).click()
  await expect(page.getByRole('dialog').getByText('CAMP SETTINGS')).toBeVisible()
  await page.getByRole('dialog').getByRole('button',{name:'Close'}).click()
  await page.getByRole('button',{name:'Import',exact:false}).click()
  await expect(page.getByRole('dialog').getByText('MANUAL HTML IMPORT')).toBeVisible()
  await page.getByRole('dialog').getByRole('button',{name:'Close'}).click()

  await page.reload()
  await expect(page.getByText('YOUR PRIVATE LEDGER')).toBeVisible()
  await account.click()
  await options.getByRole('button',{name:'Explore the demo'}).click()
  await expect(page.getByText('SYNTHETIC DEMO')).toBeVisible()
  await page.reload()
  await expect(page.getByText('SYNTHETIC DEMO')).toBeVisible()
  await account.click()
  await options.getByRole('button',{name:'Open my ledger'}).click()
  await expect(page.getByText('YOUR PRIVATE LEDGER')).toBeVisible()

  // The signed-in header and opened menu must fit even on a narrow mobile viewport.
  await page.setViewportSize({width:320,height:800})
  await account.click()
  const box=await options.boundingBox()
  expect(box).not.toBeNull()
  expect(box!.x).toBeGreaterThanOrEqual(0)
  expect(box!.x+box!.width).toBeLessThanOrEqual(320)
  const dimensions=await page.evaluate(()=>({scroll:document.documentElement.scrollWidth,client:document.documentElement.clientWidth}))
  expect(dimensions.scroll).toBeLessThanOrEqual(dimensions.client+1)

  await options.getByRole('button',{name:'Sign out'}).click()
  await expect(page).toHaveURL(/\/$/)
  await expect(page.getByRole('button',{name:'Account menu'})).toHaveCount(0)
  expect(errors).toEqual([])
})

test('restored account gets an initial when there is no photo, and sign-out failure stays safe',async({page})=>{
  await mockFirebase(page,{noPhoto:true,failSignOut:true})
  await page.addInitScript(()=>{sessionStorage.setItem('test-only-firebase-user','1');sessionStorage.removeItem('ranger-view-mode')})
  await page.goto('/app/camp')
  await expect(page.getByText('YOUR PRIVATE LEDGER')).toBeVisible()
  const account=page.getByRole('button',{name:'Account menu'})
  await expect(account.locator('.profile-avatar')).toHaveText('A')
  await account.click()
  await page.getByRole('group',{name:'Your account options'}).getByRole('button',{name:'Sign out'}).click()
  await expect(page.getByRole('alert')).toHaveText('Could not sign out. Please try again.')
  await expect(page.getByText('YOUR PRIVATE LEDGER')).toBeVisible()
})
