import type { User } from 'firebase/auth'
import { api } from './api'
interface TokenResponse { access_token?: string; error?:string; scope?:string }
interface GoogleWindow extends Window {google?:{accounts:{oauth2:{initTokenClient:(config:{client_id:string;scope:string;login_hint?:string;callback:(response:TokenResponse)=>void;error_callback:(response:{type:string})=>void})=>{requestAccessToken:(override?:{prompt:string})=>void}}}}}
export const gmailTestEnabled = import.meta.env.VITE_ENABLE_GMAIL_TEST === 'true'
export const gmailClientId = import.meta.env.VITE_GMAIL_TEST_CLIENT_ID || ''
export function loadGoogleScript():Promise<void>{
  if((window as GoogleWindow).google?.accounts.oauth2)return Promise.resolve()
  return new Promise((resolve,reject)=>{
    const existing=document.querySelector<HTMLScriptElement>('script[data-ranger-gis]')
    const s=existing || document.createElement('script')
    s.onload=()=>resolve();s.onerror=()=>reject(new Error('Could not load Google authorization.'))
    if(!existing){s.src='https://accounts.google.com/gsi/client';s.async=true;s.dataset.rangerGis='true';document.head.appendChild(s)}
  })
}
/** Must be invoked directly from a click after the GIS script has loaded. */
export function startGmailSync(user:User,done:(message:string)=>void,fail:(message:string)=>void){
  const google=(window as GoogleWindow).google
  if(!gmailTestEnabled || !gmailClientId || !google){fail('The separate Gmail testing environment is not configured.');return}
  const client=google.accounts.oauth2.initTokenClient({
    client_id:gmailClientId, scope:'openid email https://www.googleapis.com/auth/gmail.readonly',login_hint:user.email||undefined,
    callback:response=>{
      if(response.error || !response.access_token){fail('Google permission was not granted. No inbox was synced.');return}
      // Do not persist, log or put this short-lived token in a URL.
      void user.getIdToken().then(token=>api<{scanned:number;imported:number;duplicates:number;skipped:number;message:string}>('/api/private/test-gmail/sync',
        {method:'POST',body:JSON.stringify({accessToken:response.access_token})},token,90000))
        .then(result=>done(`Scanned ${result.scanned}; imported ${result.imported}; duplicates ${result.duplicates}; unsupported ${result.skipped}.`))
        .catch(e=>fail(e instanceof Error?e.message:'Gmail sync could not be completed.'))
    },
    error_callback:()=>fail('Google authorization was closed or blocked. No inbox was synced.')
  })
  client.requestAccessToken({prompt:'select_account'})
}
