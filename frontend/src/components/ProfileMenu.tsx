import { useEffect, useId, useRef, useState } from 'react'
import { signOut, type User } from 'firebase/auth'
import { Link, useNavigate } from 'react-router-dom'
import { Compass, ExternalLink, LogOut, Settings2, Tent } from 'lucide-react'
import { auth } from '../lib/firebase'
import { useRanger } from '../lib/store'
import './ProfileMenu.css'

/** The account photo comes from Firebase; email/password accounts get an initial instead. */
export function ProfileMenu({user,onSettings}:{user:User;onSettings:()=>void}){
  const {mode,setMode}=useRanger()
  const navigate=useNavigate(), menuId=useId()
  const [open,setOpen]=useState(false),[photoFailed,setPhotoFailed]=useState(false)
  const [signingOut,setSigningOut]=useState(false),[message,setMessage]=useState('')
  const container=useRef<HTMLDivElement>(null),trigger=useRef<HTMLButtonElement>(null)
  const photo=user.photoURL || user.providerData.find(p=>p.providerId==='google.com')?.photoURL || ''
  const name=user.displayName?.trim() || user.email?.split('@')[0] || 'Trailblazer'
  const initial=Array.from(name)[0]?.toLocaleUpperCase() || 'R'

  useEffect(()=>{setPhotoFailed(false)},[user.uid,photo])
  useEffect(()=>{
    if(!open)return
    const outside=(event:PointerEvent)=>{if(!container.current?.contains(event.target as Node))setOpen(false)}
    const keyboard=(event:KeyboardEvent)=>{if(event.key==='Escape'){setOpen(false);trigger.current?.focus()}}
    document.addEventListener('pointerdown',outside)
    document.addEventListener('keydown',keyboard)
    return()=>{document.removeEventListener('pointerdown',outside);document.removeEventListener('keydown',keyboard)}
  },[open])

  const leave=async()=>{
    if(!auth){setMessage('Sign out is unavailable right now. Please try again.');return}
    setSigningOut(true);setMessage('')
    try {await signOut(auth);setMode('demo');setOpen(false);navigate('/')}
    catch {setMessage('Could not sign out. Please try again.')}
    finally {setSigningOut(false)}
  }
  const switchView=()=>{setMode(mode==='demo'?'account':'demo');setOpen(false)}

  return <div className="profile-menu" ref={container}>
    <button ref={trigger} type="button" className="profile-trigger" aria-label="Account menu" aria-expanded={open} aria-controls={open?menuId:undefined} onClick={()=>{setOpen(v=>!v);setMessage('')}}>
      <span className="profile-avatar" aria-hidden="true">
        {photo && !photoFailed?<img src={photo} alt="" referrerPolicy="no-referrer" onError={()=>setPhotoFailed(true)}/>:<span>{initial}</span>}
      </span>
    </button>
    {open&&<div className="profile-popover" id={menuId} role="group" aria-label="Your account options">
      <div className="profile-details"><span>YOUR ACCOUNT</span><strong>{name}</strong>{user.email&&<small>{user.email}</small>}</div>
      <div className="profile-links">
        <button type="button" onClick={switchView}>{mode==='demo'?<Tent size={17}/>:<Compass size={17}/>}<span>{mode==='demo'?'Open my ledger':'Explore the demo'}</span></button>
        <button type="button" onClick={()=>{setOpen(false);onSettings()}}><Settings2 size={17}/><span>Settings</span></button>
        <Link to="/privacy" onClick={()=>setOpen(false)}><ExternalLink size={17}/><span>Privacy</span></Link>
        <button type="button" disabled={signingOut} onClick={()=>void leave()}><LogOut size={17}/><span>{signingOut?'Signing out…':'Sign out'}</span></button>
      </div>
      {message&&<p className="profile-message" role="alert">{message}</p>}
    </div>}
  </div>
}
