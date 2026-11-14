import { useEffect, useRef, useState } from 'react'
import { GoogleAuthProvider, createUserWithEmailAndPassword, sendEmailVerification, sendPasswordResetEmail, signInWithEmailAndPassword, signInWithPopup, signOut, type User } from 'firebase/auth'
import { ArrowRight, LockKeyhole } from 'lucide-react'
import { Link, useNavigate } from 'react-router-dom'
import { auth, firebaseConfigured } from '../lib/firebase'
import { gmailClientId, gmailTestEnabled, loadGoogleScript, startGmailSync } from '../lib/gmail'
import { useRanger } from '../lib/store'
import { Modal } from './Modal'

export function AuthModal({onClose}:{onClose:()=>void}){
  const navigate=useNavigate()
  const {mode,setMode,refresh}=useRanger(), [tab,setTab]=useState<'login'|'signup'>('login')
  const [email,setEmail]=useState(''),[password,setPassword]=useState(''),[ack,setAck]=useState(false),[busy,setBusy]=useState(false),[message,setMessage]=useState('')
  const [testUser,setTestUser]=useState<User|null>(null),[gisReady,setGisReady]=useState(false)
  const active=useRef(false)
  useEffect(()=>{active.current=true;return()=>{active.current=false}},[])
  // Load only in the separate Gmail test build. Google permission still requires a direct user click.
  useEffect(()=>{if(gmailTestEnabled && gmailClientId)void loadGoogleScript().then(()=>setGisReady(true)).catch(()=>setGisReady(false))},[])
  const finish=(syncResult?:{uid:string;message:string})=>{
    if(testUser && auth?.currentUser?.uid!==testUser.uid){setTestUser(null);setMessage('Your account changed. Please sign in again.');return}
    setMode('account')
    // A successful Gmail sync is refreshed by the dashboard after navigation.
    // If the user continues after an error, refresh to show any partial commits.
    if(!syncResult && mode==='account')void refresh()
    onClose()
    navigate('/app/camp',syncResult?{state:{gmailSync:syncResult}}:undefined)
  }
  const afterIdentity=(user:User)=>{
    if(gmailTestEnabled){setTestUser(user);setMessage('Identity verified. You may authorize the matching Google mailbox, or continue without Gmail.')}
    else finish()
  }
  const run=async(action:'google'|'email')=>{
    if(!auth || !ack)return; setBusy(true);setMessage('')
    try{
      if(action==='google') {const result=await signInWithPopup(auth,new GoogleAuthProvider());afterIdentity(result.user);return}
      if(tab==='signup'){
        const result=await createUserWithEmailAndPassword(auth,email,password)
        await sendEmailVerification(result.user);await signOut(auth)
        setMessage('Check your inbox for a verification email, then come back and sign in.');setTab('login')
      } else {
        const result=await signInWithEmailAndPassword(auth,email,password)
        if(!result.user.emailVerified){await sendEmailVerification(result.user);await signOut(auth);setMessage('Please verify your email first. A new verification link was sent.');return}
        afterIdentity(result.user)
      }
    }catch(error){
    const code =
        typeof error === 'object' && error !== null && 'code' in error
            ? error.code
            : ''

    setMessage(
        import.meta.env.DEV && typeof code === 'string' && /^auth\/[a-z0-9-]+$/i.test(code)
            ? `Sign-in failed: ${code}`
            : 'Sign-in could not be completed. Please try again.'
    )
  }finally {
    setBusy(false)
  }
  }
  const sync=()=>{
    if(!testUser || auth?.currentUser?.uid!==testUser.uid)return
    const uid=testUser.uid
    setBusy(true);setMessage('')
    startGmailSync(testUser,m=>{
      if(!active.current)return
      setBusy(false)
      if(auth?.currentUser?.uid!==uid)return
      finish({uid,message:m})
    },m=>{
      if(!active.current)return
      setBusy(false)
      if(auth?.currentUser?.uid!==uid)return
      setMessage(m)
    })
  }
  const reset=async()=>{if(!auth || !email){setMessage('Enter your email above to request a reset link.');return}try{await sendPasswordResetEmail(auth,email)}catch{/* do not reveal account existence */}setMessage('If that address is eligible, a password-reset email will be sent.')}
  return <Modal title="Welcome to the camp." eyebrow="YOUR TRAIL STARTS HERE" onClose={onClose}>
    <p className="modal-intro">Sign in to keep your own ledger. Or stay in the synthetic demo—no account needed.</p>
    {!firebaseConfigured&&<div className="notice notice--warm"><LockKeyhole size={18}/><span><strong>Account setup needed.</strong> Firebase hasn’t been configured for this build yet. The demo still works.</span></div>}
    {testUser ? <>
      <div className="notice notice--warm"><LockKeyhole size={18}/><span><strong>Listed Gmail testers only.</strong> Google will ask for broad read-only Gmail access. Choose the same verified mailbox as your Ranger login.</span></div>
      <button className="btn btn-dark btn-full" disabled={!gisReady||busy} onClick={sync}>{busy?'Syncing…':'Authorize with Google & sync'} <ArrowRight size={17}/></button>
      <button className="settings-action" disabled={busy} onClick={()=>finish()}>Continue to my ledger</button>
      {!gisReady&&<p className="tiny-copy">Google authorization is unavailable or still loading. You may continue without it.</p>}
    </> : <>
      <label className="disclosure"><input type="checkbox" checked={ack} onChange={e=>setAck(e.target.checked)}/><span>I understand sign-in identifies me; it does not connect my inbox. {gmailTestEnabled?'Gmail testing requires a separate Google permission and grants broad read access.':'Gmail access is not offered in the public demo.'} <Link to="/privacy" onClick={onClose}>Privacy details</Link>.</span></label>
      <button className="btn btn-outline auth-google" disabled={!firebaseConfigured||!ack||busy} onClick={()=>void run('google')}><span className="google-g">G</span> Continue with Google <ArrowRight size={17}/></button>
      <div className="or-line"><span>or with email</span></div>
      <div className="auth-tabs"><button className={tab==='login'?'active':''} onClick={()=>{setTab('login');setMessage('')}}>Sign in</button><button className={tab==='signup'?'active':''} onClick={()=>{setTab('signup');setMessage('')}}>Create account</button></div>
      <form onSubmit={e=>{e.preventDefault();void run('email')}} className="auth-form">
        <label>Email address<input type="email" autoComplete="email" value={email} onChange={e=>setEmail(e.target.value)} placeholder="you@example.com" required /></label>
        <label>Password<input type="password" autoComplete={tab==='signup'?'new-password':'current-password'} value={password} onChange={e=>setPassword(e.target.value)} placeholder="At least 6 characters" minLength={6} required /></label>
        {tab==='login'&&<button type="button" className="text-link reset-link" onClick={()=>void reset()} disabled={!firebaseConfigured}>Forgot password?</button>}
        <button className="btn btn-dark" disabled={!firebaseConfigured||!ack||busy} type="submit">{busy?'One moment…':tab==='login'?'Sign in':'Create account'} <ArrowRight size={17}/></button>
      </form>
    </>}
    {message&&<p className="form-message" role="status">{message}</p>}
    <p className="tiny-copy">Firebase handles account credentials. Never enter your Google password into Ranger.</p>
  </Modal>
}
