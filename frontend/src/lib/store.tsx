import { createContext, useCallback, useContext, useEffect, useMemo, useRef, useState, type ReactNode } from 'react'
import { onAuthStateChanged, type User } from 'firebase/auth'
import { api, getDemo, getLedgerWithWake, wakeBackend } from './api'
import { auth } from './firebase'
import { offlineReceipts } from './demo'
import type { Mode, Receipt, Settings } from './types'

type ServerStatus='waking'|'ready'|'offline'
const DEFAULT:Settings={timezone:'Asia/Kolkata',start:'23:00',end:'04:00'}
// Remember the selected view per tab; this stores no account data or credentials.
const VIEW_KEY='ranger-view-mode'
function readViewPreference():Mode|null {
  try {const value=window.sessionStorage.getItem(VIEW_KEY);return value==='demo'||value==='account'?value:null}
  catch {return null}
}
function writeViewPreference(value:Mode|null) {
  try {if(value)window.sessionStorage.setItem(VIEW_KEY,value);else window.sessionStorage.removeItem(VIEW_KEY)}
  catch { /* session storage can be disabled; the current tab still works */ }
}
interface Store {
  mode:Mode; setMode:(mode:Mode)=>void; user:User|null; authLoading:boolean
  rows:Receipt[]; setRows:(r:Receipt[])=>void; demoLive:boolean; server:ServerStatus; retryWake:()=>void
  settings:Settings; saveSettings:(v:Settings)=>void; busy:boolean; error:string; setError:(v:string)=>void
  refresh:()=>Promise<void>; changeJunk:(receiptId:string,itemId:string,junk:boolean)=>Promise<void>; resetDemo:()=>void
}
const Context=createContext<Store | null>(null)
export function RangerProvider({children}:{children:ReactNode}) {
  const [user,setUser]=useState<User|null>(null), [authLoading,setAuthLoading]=useState(Boolean(auth))
  // Start in the synthetic view until Firebase has restored and verified the identity.
  const [mode,setModeState]=useState<Mode>('demo'), [demoRows,setDemoRows]=useState<Receipt[]>(offlineReceipts), [accountRows,setAccountRows]=useState<Receipt[]>([])
  const viewPreference=useRef<Mode|null>(readViewPreference())
  const setMode=useCallback((next:Mode)=>{viewPreference.current=next;writeViewPreference(next);setModeState(next)},[])
  const [demoLive,setDemoLive]=useState(false), [server,setServer]=useState<ServerStatus>('waking'), [retry,setRetry]=useState(0)
  const [busy,setBusy]=useState(false),[error,setError]=useState('')
  const edited=useRef(false)
  // In-flight private requests must never repopulate another user's view after sign-out.
  const currentUser=useRef<User|null>(null), identityVersion=useRef(0), ledgerReadVersion=useRef(0)
  const identityAbort=useRef(new AbortController())
  const [settings,setSettings]=useState<Settings>(()=>{
    try { const s=JSON.parse(localStorage.getItem('ranger-display-settings')||'{}') as Partial<Settings>
      const zone=['Asia/Kolkata','UTC','Europe/London','America/New_York'].includes(s.timezone||'') ? s.timezone! : DEFAULT.timezone
      const valid=(v:unknown):v is string=>typeof v==='string' && /^([01]\d|2[0-3]):[0-5]\d$/.test(v)
      const start=valid(s.start)?s.start:DEFAULT.start, end=valid(s.end)?s.end:DEFAULT.end
      return {timezone:zone,start:start===end?DEFAULT.start:start,end:start===end?DEFAULT.end:end}
    } catch {return DEFAULT}
  })
  useEffect(()=>{
    if(!auth){setAuthLoading(false);return}
    return onAuthStateChanged(auth,u=>{
      if(currentUser.current?.uid!==u?.uid){identityVersion.current++;identityAbort.current.abort();identityAbort.current=new AbortController();setAccountRows([]);setBusy(false);setError('')}
      currentUser.current=u;setUser(u);setAuthLoading(false)
      if(!u){
        setModeState('demo');setAccountRows([])
        if(viewPreference.current==='account'){viewPreference.current=null;writeViewPreference(null)}
      } else if(u.emailVerified) {
        // An explicitly chosen demo stays isolated; otherwise restore the signed-in ledger.
        setModeState(viewPreference.current==='demo'?'demo':'account')
      } else setModeState('demo')
    })
  },[])
  useEffect(()=>{const controller=new AbortController();void wakeBackend(setServer,controller.signal);return()=>controller.abort()},[retry])
  useEffect(()=>{
    if(server!=='ready' || edited.current) return
    let alive=true
    void getDemo().then(data=>{if(alive && !edited.current){setDemoRows(data.receipts);setDemoLive(true)}}).catch(()=>{})
    return()=>{alive=false}
  },[server])
  const refresh=useCallback(async()=>{
    if(mode==='demo') {
      try {const data=await getDemo();setDemoRows(data.receipts);setDemoLive(true);edited.current=false;setError('')}
      catch {setError('The live parser is still waking up. The labelled offline sample is available.')}
      return
    }
    if(!user){setError('Sign in to open your private ledger.');return}
    const version=identityVersion.current, uid=user.uid, readVersion=++ledgerReadVersion.current
    // A slower pre-sync response must never overwrite a newer post-sync ledger.
    const stillCurrent=()=>identityVersion.current===version && currentUser.current?.uid===uid && ledgerReadVersion.current===readVersion
    setBusy(true);setError('')
    try {
      const token=await user.getIdToken()
      const data=await getLedgerWithWake(token,settings.timezone,settings.start,settings.end,
        status=>{if(stillCurrent())setServer(status)},identityAbort.current.signal)
      if(stillCurrent())setAccountRows(data.receipts)
    }
    catch(e){if(stillCurrent())setError(e instanceof Error?e.message:'Could not load your ledger.')}
    finally{if(stillCurrent())setBusy(false)}
  },[mode,user,settings])
  useEffect(()=>{if(mode==='account' && user)void refresh()},[mode,user,refresh])
  const setRows=(r:Receipt[])=>{if(mode==='demo'){edited.current=true;setDemoRows(r)}else setAccountRows(r)}
  const changeJunk=async(receiptId:string,itemId:string,junk:boolean)=>{
    if(mode==='demo'){
      edited.current=true;setDemoRows(prev=>prev.map(r=>r.id===receiptId?{...r,items:r.items.map(i=>i.id===itemId?{...i,junkFood:junk}:i)}:r));return
    }
    if(!user)throw new Error('Sign in first.')
    const version=identityVersion.current, uid=user.uid
    await api(`/api/private/receipts/${receiptId}/items/${itemId}`,{method:'PATCH',body:JSON.stringify({junkFood:junk})},await user.getIdToken())
    if(identityVersion.current===version && currentUser.current?.uid===uid)
      setAccountRows(prev=>prev.map(r=>r.id===receiptId?{...r,items:r.items.map(i=>i.id===itemId?{...i,junkFood:junk}:i)}:r))
  }
  const saveSettings=(s:Settings)=>{setSettings(s);localStorage.setItem('ranger-display-settings',JSON.stringify(s))}
  const value=useMemo<Store>(()=>({mode,setMode,user,authLoading,rows:mode==='demo'?demoRows:accountRows,setRows,demoLive,server,retryWake:()=>{setRetry(x=>x+1);if(mode==='account' && user)void refresh()},settings,saveSettings,busy,error,setError,refresh,changeJunk,resetDemo:()=>{edited.current=false;setDemoRows(offlineReceipts);setDemoLive(false);void getDemo().then(d=>{setDemoRows(d.receipts);setDemoLive(true)}).catch(()=>{})}}),
    // Functions are intentionally recreated when relevant state changes.
    // eslint-disable-next-line react-hooks/exhaustive-deps
    [mode,user,authLoading,demoRows,accountRows,demoLive,server,settings,busy,error,refresh])
  return <Context.Provider value={value}>{children}</Context.Provider>
}
export function useRanger(){const c=useContext(Context);if(!c)throw new Error('RangerProvider missing');return c}
