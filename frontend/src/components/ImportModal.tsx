import { useState } from 'react'
import { UploadCloud, ArrowRight } from 'lucide-react'
import { api } from '../lib/api'
import { useRanger } from '../lib/store'
import { Modal } from './Modal'
export function ImportModal({onClose}:{onClose:()=>void}){
  const {user,refresh}=useRanger(),[html,setHtml]=useState(''),[busy,setBusy]=useState(false),[message,setMessage]=useState('')
  const submit=async()=>{if(!user)return;setBusy(true);setMessage('');try{
    const result=await api<{message:string}>('/api/private/import',{method:'POST',body:JSON.stringify({html,receivedAt:new Date().toISOString()})},await user.getIdToken())
    setMessage(result.message);await refresh()
  }catch(e){setMessage(e instanceof Error?e.message:'Could not import this receipt.')}finally{setBusy(false)}}
  return <Modal title="Bring a receipt." eyebrow="MANUAL HTML IMPORT" onClose={onClose}>
    <p className="modal-intro">Paste supported receipt HTML. Ranger processes it on the backend and stores extracted fields, not the raw HTML. Real layouts vary; review every result.</p>
    <div className="notice"><UploadCloud size={18}/><span>Only import data you’re comfortable sending to your configured backend. This is not Gmail sync.</span></div>
    <label className="block-label">Receipt HTML<textarea maxLength={100000} rows={8} value={html} onChange={e=>setHtml(e.target.value)} placeholder="<html>…receipt…</html>"/></label>
    <button className="btn btn-dark btn-full" disabled={!user||!html.trim()||busy} onClick={()=>void submit()}>{busy?'Reading receipt…':'Parse and import'} <ArrowRight size={17}/></button>
    {message&&<p className="form-message" role="status">{message}</p>}
  </Modal>
}
