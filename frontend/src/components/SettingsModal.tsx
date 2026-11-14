import { useEffect, useState } from 'react'
import { signOut } from 'firebase/auth'
import { ExternalLink, LogOut, Trash2, Link2, AlertCircle } from 'lucide-react'
import { auth } from '../lib/firebase'
import { api } from '../lib/api'
import { gmailClientId, gmailTestEnabled, loadGoogleScript, startGmailSync } from '../lib/gmail'
import { useRanger } from '../lib/store'
import { Modal } from './Modal'

export function SettingsModal({onClose,onSignIn}:{onClose:()=>void;onSignIn:()=>void}){
  const {settings,saveSettings,user,mode,setMode,refresh}=useRanger()
  const [zone,setZone]=useState(settings.timezone),[start,setStart]=useState(settings.start),[end,setEnd]=useState(settings.end)
  const [message,setMessage]=useState(''),[gisReady,setGisReady]=useState(false),[syncing,setSyncing]=useState(false)
  useEffect(()=>{if(gmailTestEnabled && gmailClientId && user)void loadGoogleScript().then(()=>setGisReady(true)).catch(()=>setGisReady(false))},[user])
  const save=()=>{if(start===end){setMessage('Start and end times must be different.');return}saveSettings({timezone:zone,start,end});setMessage('Display settings saved in this browser.')}
  const clear=async()=>{
    if(!user || !window.confirm('Delete all imported Ranger ledger records for this account? This cannot be undone.'))return
    try{await api('/api/private/data',{method:'DELETE'},await user.getIdToken());await refresh();setMessage('Your Ranger ledger records were deleted. Your Firebase account still exists.')}
    catch(e){setMessage(e instanceof Error?e.message:'Could not delete ledger data.')}
  }
  const sync=()=>{
    if(!user || syncing)return
    const uid=user.uid
    setSyncing(true);setMessage('')
    startGmailSync(user,m=>{
      setSyncing(false)
      if(auth?.currentUser?.uid!==uid)return
      setMessage(m)
      // Switching from demo triggers a private read; an already-open ledger needs an explicit refresh.
      if(mode==='account')void refresh();else setMode('account')
    },m=>{
      setSyncing(false)
      if(auth?.currentUser?.uid!==uid)return
      setMessage(m)
      // A timeout may occur after some messages were committed; show any partial imports.
      if(mode==='account')void refresh()
    })
  }
  return <Modal title="Make it yours." eyebrow="CAMP SETTINGS" onClose={onClose}>
    <p className="modal-intro">Set your local clock. Late night starts at your chosen time and ends just before the end time.</p>
    <div className="settings-grid"><label>Time zone<select value={zone} onChange={e=>setZone(e.target.value)}><option value="Asia/Kolkata">India · Asia/Kolkata</option><option value="UTC">UTC</option><option value="Europe/London">Europe/London</option><option value="America/New_York">America/New_York</option></select></label><div className="time-row"><label>Starts<input type="time" value={start} onChange={e=>setStart(e.target.value)}/></label><label>Ends before<input type="time" value={end} onChange={e=>setEnd(e.target.value)}/></label></div></div>
    <button className="btn btn-dark btn-full" onClick={save}>Save preferences</button>
    <div className="modal-divider"/>
    <div className="setting-section"><span className="eyebrow">YOUR ACCOUNT</span>{user?<><strong>{user.email}</strong><small>{mode==='demo'?'You are viewing synthetic demo data.':'You are viewing your private ledger.'}</small><button className="settings-action" onClick={()=>{setMode(mode==='demo'?'account':'demo');onClose()}}>{mode==='demo'?'Open my ledger':'Return to demo'}</button><button className="settings-action" onClick={()=>void (auth?signOut(auth):Promise.resolve()).then(onClose)}><LogOut size={16}/> Sign out</button><button className="settings-action danger" onClick={()=>void clear()}><Trash2 size={16}/> Delete my Ranger ledger data</button></>:<><small>The demo is entirely synthetic and needs no account.</small><button className="settings-action" onClick={onSignIn}>Sign in / create account</button></>}</div>
    <div className="modal-divider"/>
    <div className="setting-section"><span className="eyebrow">GMAIL {gmailTestEnabled?'· TEST ENVIRONMENT':'· NOT CONNECTED'}</span>
      {gmailTestEnabled?<><div className="notice notice--warm"><AlertCircle size={18}/><span><strong>Listed testers only.</strong> Google asks for broad read-only Gmail access. Ranger searches at most 25 recent matching messages and processes supported layouts. This experimental connector needs review and live testing before using real inbox data.</span></div><button className="settings-action" disabled={!user||!gisReady||syncing} onClick={sync}><Link2 size={16}/>{syncing?'Syncing…':'Authorize with Google & sync'}</button><small>Google may ask again; no refresh token is stored. Grants can be revoked in your Google Account.</small></>:<small>Real Gmail sync is available only in a separate configured tester environment—not in the public demo.</small>}
      <a className="settings-action" href="https://myaccount.google.com/permissions" target="_blank" rel="noreferrer">Manage Google permissions <ExternalLink size={14}/></a>
    </div>
    {message&&<p className="form-message" role="status">{message}</p>}
  </Modal>
}
