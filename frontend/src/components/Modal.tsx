import { useEffect, useRef, type ReactNode } from 'react'
import { createPortal } from 'react-dom'
import { X } from 'lucide-react'
export function Modal({title,eyebrow,onClose,children,wide=false}:{title:string;eyebrow?:string;onClose:()=>void;children:ReactNode;wide?:boolean}){
  const ref=useRef<HTMLDivElement>(null)
  useEffect(()=>{
    const before=document.activeElement as HTMLElement|null, overflow=document.body.style.overflow
    document.body.style.overflow='hidden'
    const t=window.setTimeout(()=>ref.current?.querySelector<HTMLElement>('button, input, select, textarea, a[href]')?.focus(),10)
    const key=(e:KeyboardEvent)=>{
      if(e.key==='Escape'){onClose();return}
      if(e.key==='Tab' && ref.current){const els=[...ref.current.querySelectorAll<HTMLElement>('button:not([disabled]),input:not([disabled]),select:not([disabled]),textarea:not([disabled]),a[href]')]; if(!els.length)return
        if(e.shiftKey && document.activeElement===els[0]){e.preventDefault();els[els.length-1].focus()}
        else if(!e.shiftKey && document.activeElement===els[els.length-1]){e.preventDefault();els[0].focus()}}
    }
    document.addEventListener('keydown',key)
    return()=>{window.clearTimeout(t);document.removeEventListener('keydown',key);document.body.style.overflow=overflow;before?.focus()}
  },[onClose])
  return createPortal(<div className="modal-backdrop" onMouseDown={e=>{if(e.target===e.currentTarget)onClose()}}>
    <div className={`modal-card ${wide?'modal-card--wide':''}`} role="dialog" aria-modal="true" aria-labelledby="modal-title" ref={ref}>
      <div className="modal-head"><div>{eyebrow&&<span className="eyebrow">{eyebrow}</span>}<h2 id="modal-title">{title}</h2></div><button className="icon-button" aria-label="Close" onClick={onClose}><X size={20}/></button></div>
      {children}
    </div>
  </div>,document.body)
}
