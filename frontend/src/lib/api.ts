import type { Snapshot } from './types'
const base = (import.meta.env.VITE_API_URL || '').replace(/\/$/, '')
export class ApiError extends Error { status: number; constructor(message:string,status=0){super(message);this.status=status} }
export async function api<T>(path: string, options: RequestInit = {}, token?: string, timeout=12000): Promise<T> {
  const controller=new AbortController(), timer=window.setTimeout(()=>controller.abort(), timeout)
  const external=options.signal
  const abort=()=>controller.abort()
  if(external?.aborted)controller.abort();else external?.addEventListener('abort',abort,{once:true})
  try {
    const headers=new Headers(options.headers)
    if(options.body) headers.set('Content-Type','application/json')
    if(token) headers.set('Authorization',`Bearer ${token}`)
    const res=await fetch(`${base}${path}`,{...options,headers,signal:controller.signal,cache:'no-store'})
    if(!res.ok) {
      let message='The trail is unavailable right now. Please try again.'
      try {const body=await res.json();if(typeof body.error==='string')message=body.error} catch { /* Render can return HTML while waking */ }
      throw new ApiError(message,res.status)
    }
    if(res.status===204) return undefined as T
    const content=res.headers.get('content-type') || ''
    if(!content.includes('application/json')) throw new ApiError('The trail is still waking up.')
    return await res.json() as T
  } finally {window.clearTimeout(timer);external?.removeEventListener('abort',abort)}
}
export const getDemo = () => api<Snapshot>('/api/public/demo')
export const getLedger = (token:string, timezone:string, start:string, end:string, signal?:AbortSignal) =>
  api<Snapshot>(`/api/private/ledger?${new URLSearchParams({timezone,start,end})}`,{signal},token)
/** Only called during a real visit; no background keep-alive after readiness. */
export async function wakeBackend(onStatus:(status:'waking'|'ready'|'offline')=>void, signal:AbortSignal):Promise<'ready'|'offline'|'aborted'> {
  if(signal.aborted)return 'aborted'
  onStatus('waking')
  const deadline=Date.now()+90000
  while(!signal.aborted && Date.now()<deadline) {
    try {const data=await api<{status:string}>('/api/public/health',{signal},undefined,8000); if(!signal.aborted && data.status==='ready'){onStatus('ready');return 'ready'}}
    catch { /* a cold service may return 503, HTML, a timeout or a CORS error */ }
    if(signal.aborted)return 'aborted'
    await new Promise<void>(resolve=>{
      const done=()=>{window.clearTimeout(timer);signal.removeEventListener('abort',done);resolve()}
      const timer=window.setTimeout(done,5000);signal.addEventListener('abort',done,{once:true})
    })
  }
  if(signal.aborted)return 'aborted'
  onStatus('offline');return 'offline'
}

/** Retry one private read after a bounded, verified wake; never retry authentication or validation errors. */
export async function getLedgerWithWake(token:string, timezone:string, start:string, end:string,
    onStatus:(status:'waking'|'ready'|'offline')=>void, signal:AbortSignal):Promise<Snapshot> {
  try {return await getLedger(token,timezone,start,end,signal)}
  catch(error){
    const mayBeCold=error instanceof ApiError ? error.status===0 || error.status===502 || error.status===503 || error.status===504 : true
    if(!mayBeCold || signal.aborted)throw error
    const wake=await wakeBackend(onStatus,signal)
    if(wake==='aborted')throw new DOMException('Request cancelled.','AbortError')
    if(wake!=='ready')throw new Error('The backend did not become ready. Please retry or explore the demo.')
    return getLedger(token,timezone,start,end,signal)
  }
}
