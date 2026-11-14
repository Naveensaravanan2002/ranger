import { Component, useState, type ReactNode } from 'react'
import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom'
import { RangerProvider } from './lib/store'
import { Landing } from './pages/Landing'
import { Camp } from './pages/Camp'
import { Trail } from './pages/Trail'
import { Stash } from './pages/Stash'
import { Privacy } from './pages/Privacy'
import { Dashboard } from './components/Dashboard'
import { AuthModal } from './components/AuthModal'
import { SettingsModal } from './components/SettingsModal'
import { ImportModal } from './components/ImportModal'

class ErrorBoundary extends Component<{children:ReactNode},{failed:boolean}>{
  state={failed:false}
  static getDerivedStateFromError(){return {failed:true}}
  render(){return this.state.failed?<div className="fatal-error"><span>✳</span><h1>We lost the trail.</h1><p>Something unexpected happened. Try refreshing the page.</p><button className="btn btn-dark" onClick={()=>window.location.reload()}>Refresh Ranger</button></div>:this.props.children}
}
function RoutesAndModals(){
  const [modal,setModal]=useState<'auth'|'settings'|'import'|null>(null)
  return <><Routes><Route path="/" element={<Landing onSignIn={()=>setModal('auth')}/>}/><Route path="/privacy" element={<Privacy/>}/><Route path="/app" element={<Dashboard onSignIn={()=>setModal('auth')} onSettings={()=>setModal('settings')} onImport={()=>setModal('import')}/>}><Route index element={<Navigate to="camp" replace/>}/><Route path="camp" element={<Camp/>}/><Route path="trail" element={<Trail/>}/><Route path="stash" element={<Stash/>}/></Route><Route path="*" element={<Navigate to="/" replace/>}/></Routes>
    {modal==='auth'&&<AuthModal onClose={()=>setModal(null)}/>}{modal==='settings'&&<SettingsModal onClose={()=>setModal(null)} onSignIn={()=>setModal('auth')}/>}{modal==='import'&&<ImportModal onClose={()=>setModal(null)}/>}
  </>
}
export default function App(){return <ErrorBoundary><BrowserRouter><RangerProvider><RoutesAndModals/></RangerProvider></BrowserRouter></ErrorBoundary>}
