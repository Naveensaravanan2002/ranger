import { useEffect, useRef, useState } from 'react'
import { NavLink, Outlet, useLocation, useNavigate } from 'react-router-dom'
import { Compass, Tent, Wallet, Settings2, ArrowUpRight, LogIn, Plus, RotateCw, WifiOff, CircleCheck } from 'lucide-react'
import { Brand } from './Brand'
import { ProfileMenu } from './ProfileMenu'
import { useRanger } from '../lib/store'
import type { Period } from '../lib/types'

const links=[{to:'/app/camp',label:'The Camp',caption:'OVERVIEW',Icon:Tent},{to:'/app/trail',label:'The Trail',caption:'TRANSACTIONS',Icon:Compass},{to:'/app/stash',label:'The Stash',caption:'WHAT-IFS',Icon:Wallet}]
export interface DashboardContext {period:Period}
export function Dashboard({onSignIn,onSettings,onImport}:{onSignIn:()=>void;onSettings:()=>void;onImport:()=>void}){
  const {mode,setMode,user,server,demoLive,busy,error,retryWake,refresh}=useRanger(), [period,setPeriod]=useState<Period>('all'),loc=useLocation(),navigate=useNavigate()
  const handledSync=useRef<string|null>(null)
  const candidate=loc.state?.gmailSync
  const syncResult=mode==='account' && user && candidate?.uid===user.uid && typeof candidate?.message==='string' ? candidate as {uid:string;message:string} : null
  useEffect(()=>{
    // Run a fresh private read after sync, even if account mode was already active.
    if(!syncResult || handledSync.current===loc.key)return
    handledSync.current=loc.key
    void refresh()
  },[syncResult,loc.key,refresh])
  const page=links.find(l=>loc.pathname.startsWith(l.to))?.label || 'The Camp'
  const openAccount=()=>{if(user)setMode('account');else onSignIn()}
  const dismissSync=()=>navigate(`${loc.pathname}${loc.search}${loc.hash}`,{replace:true,state:null})
  return <div className="app-shell">
    <aside className="sidebar"><div className="side-brand"><Brand/></div><div className="side-label">EXPLORE RANGER <span>✳</span></div><nav className="side-nav" aria-label="App navigation">{links.map(({to,label,caption,Icon},i)=><NavLink key={to} to={to} className={({isActive})=>`side-link ${isActive?'active':''}`}><span className="side-icon"><Icon size={20}/></span><span><strong>{label}</strong><small>{caption}</small></span><i>0{i+1}</i></NavLink>)}</nav>
      <div className="sidebar-bottom"><div className="side-goat-card"><img src="/assets/ranger-key.webp" alt=""/><div><span>YOUR TRAIL GUIDE</span><b>A little curious.<br/>A lot helpful.</b></div><span className="side-star">✳</span></div><button className="side-settings" onClick={onSettings}><Settings2 size={18}/> Settings <ArrowUpRight size={15}/></button><div className="side-foot">MADE FOR THE CURIOUS <span>© RANGER</span></div></div></aside>
    <div className="app-main"><header className="app-top"><div className="mobile-brand"><Brand/></div><div className="breadcrumbs"><span>RANGER</span><span>/</span><strong>{page.toUpperCase()}</strong></div><div className="top-actions"><label className="period-select"><span>SHOWING</span><select aria-label="Choose time period" value={period} onChange={e=>setPeriod(e.target.value as Period)}><option value="all">All time</option><option value="30days">Last 30 days</option><option value="month">This month</option></select></label><button className="top-icon" aria-label="Settings" onClick={onSettings}><Settings2 size={19}/></button>{user&&mode==='account'?<button className="btn btn-dark btn-small import-action" onClick={onImport}><Plus size={16}/> Import</button>:<button className="btn btn-dark btn-small top-login" onClick={openAccount}><LogIn size={15}/> {user?'My ledger':'Sign in'}</button>}{user&&<ProfileMenu user={user} onSettings={onSettings}/>}</div></header>
      <div className={`mode-banner ${mode==='demo'?'demo-banner':'account-banner'}`}><span className="status-dot"/><strong>{mode==='demo'?'SYNTHETIC DEMO':'YOUR PRIVATE LEDGER'}</strong><span>{mode==='demo'?(demoLive?'Fictional receipts · processed by the real parser':'Fictional offline sample · server may be waking'):'Only your account’s imported receipts'}</span>{mode==='demo'&&<button onClick={openAccount}>{user?'OPEN MY LEDGER':'SIGN IN'} <ArrowUpRight size={13}/></button>}</div>
      {mode==='account'&&busy?<div className="app-content"><div className="wake-card" role="status" aria-live="polite"><div className="wake-stars">✦ &nbsp; ✳ &nbsp; ✦</div><img src="/assets/ranger-key.webp" alt=""/><span className="eyebrow">THE TRAIL IS WAKING</span><h2>Setting up<br/>your camp<span>.</span></h2><p>The first visit after a quiet spell can take around a minute. You can keep exploring the synthetic demo while we wait.</p><button className="btn btn-dark" onClick={()=>setMode('demo')}>Explore the demo <ArrowUpRight size={16}/></button></div></div>:<div className="app-content">{syncResult&&<div className="page-wrap"><div className="notice" role="status" aria-live="polite"><CircleCheck size={18}/><span><strong>Gmail sync complete.</strong> {syncResult.message}</span><button className="settings-action" aria-label="Dismiss Gmail sync result" onClick={dismissSync}>Dismiss</button></div></div>}{mode==='account'&&error&&<div className="app-alert"><WifiOff size={18}/><span>{error}</span><button onClick={retryWake}><RotateCw size={14}/> Retry wake</button></div>}{server==='offline'&&mode==='demo'&&<div className="app-alert"><WifiOff size={18}/><span>Showing the offline synthetic sample; the backend could not be reached.</span><button onClick={retryWake}><RotateCw size={14}/> Retry</button></div>}<Outlet context={{period} satisfies DashboardContext}/></div>}
      <footer className="app-footer"><span>RANGER / YOUR MONEY, IN THE WILD</span><span>HONEST NUMBERS. PLAYFUL GOAT. ✳</span></footer>
    </div>
    <nav className="mobile-nav" aria-label="Mobile app navigation">{links.map(({to,label,Icon})=><NavLink key={to} to={to} className={({isActive})=>isActive?'active':''}><Icon size={20}/><span>{label.replace('The ','')}</span></NavLink>)}<button onClick={onSettings}><Settings2 size={20}/><span>Settings</span></button></nav>
  </div>
}
