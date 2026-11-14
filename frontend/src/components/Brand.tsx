import { Link } from 'react-router-dom'
export function Brand({light=false}:{light?:boolean}){return <Link to="/" className={`brand ${light?'brand--light':''}`} aria-label="Ranger home"><span className="brand-mark">R<span>✦</span></span><span className="brand-name">ranger<span>.</span></span></Link>}
