import { useRef } from 'react'
import { Link } from 'react-router-dom'
import { motion, useReducedMotion, useScroll, useTransform } from 'framer-motion'
import { ArrowDown, ArrowUpRight, Check, Eye, LockKeyhole, Sparkles, Star, Zap } from 'lucide-react'
import { Brand } from '../components/Brand'
import { useRanger } from '../lib/store'

export function Landing({onSignIn}:{onSignIn:()=>void}){
  const {setMode,server,user}=useRanger(), heroRef=useRef<HTMLElement>(null), reduced=useReducedMotion()
  const {scrollYProgress}=useScroll({target:heroRef,offset:['start start','end start']})
  const goatY=useTransform(scrollYProgress,[0,1],[0,85]), sceneryY=useTransform(scrollYProgress,[0,1],[0,40])
  const appear={initial:{opacity:1,y:30},whileInView:{opacity:1,y:0},viewport:{once:true,amount:0.2},transition:{duration:.65}}
  return <main className="landing">
    <section className="hero" ref={heroRef} id="top">
      <header className="landing-nav wrap"><Brand/><nav aria-label="Main navigation"><a href="#story">THE STORY</a><a href="#features">THE GOODS</a><Link to="/privacy">PRIVACY</Link></nav><div className="nav-actions">{user?<Link className="nav-signin landing-account" to="/app/camp" onClick={()=>setMode('account')}>My ledger</Link>:<button className="nav-signin" onClick={onSignIn}>Sign in</button>}<Link to="/app/camp" onClick={()=>setMode('demo')} className="btn btn-dark btn-small">Try the demo <ArrowUpRight size={16}/></Link></div></header>
      <div className="hero-layout wrap">
        <div className="hero-copy"><motion.div initial={reduced?false:{opacity:0,y:18}} animate={{opacity:1,y:0}} transition={{duration:.6}}>
          <div className="hero-kicker"><span className="dot"/> YOUR RECEIPTS HAVE A STORY <span className="hero-kicker-index">EST. 2026</span></div>
          <h1>YOUR MONEY<span className="headline-period">.</span><br/><em>IN THE</em><br/><span className="hero-word">WILD<span className="headline-period">.</span></span></h1>
          <p>Meet Ranger. He follows the trail of receipts, fees, and questionable midnight snacks—so you don’t have to.</p>
          <div className="hero-buttons"><Link to="/app/camp" onClick={()=>setMode('demo')} className="btn btn-dark btn-large">Explore the demo <ArrowUpRight size={19}/></Link><a className="hero-text-link" href="#story">Meet your ranger <ArrowDown size={15}/></a></div>
          <div className="hero-proof"><div className="proof-icons"><span>✳</span><span>₹</span><span>✦</span></div><span>NO SIGN-UP. NO REAL INBOX.<br/><strong>JUST A VERY CURIOUS GOAT.</strong></span></div>
        </motion.div></div>
        <div className="hero-stage" aria-label="Illustration of Ranger the goat in a hand-drawn desert">
          <div className="hero-sun"/><div className="hero-halo-text">✦ &nbsp; YOUR MONEY HAS A STORY &nbsp; ✦</div>
          <motion.img style={reduced?{}:{y:sceneryY}} className="desert-art" src="/assets/desert-linework.webp" alt="" />
          <motion.img style={reduced?{}:{y:goatY}} initial={reduced?false:{opacity:0,scale:.92,rotate:3}} animate={{opacity:1,scale:1,rotate:0}} transition={{type:'spring',duration:1.1}} className="hero-goat" src="/assets/ranger-key.webp" alt="Ranger, a lanky cowboy goat with a carrot" />
          <motion.div initial={reduced?false:{opacity:0,x:-20,rotate:-10}} animate={{opacity:1,x:0,rotate:-7}} transition={{delay:.5,duration:.5}} className="floating-note note-one"><span className="note-label">TRAIL FIND NO. 01</span><b>₹ 159</b><span>in sneaky fees*</span><i>↗</i></motion.div>
          <motion.div initial={reduced?false:{opacity:0,x:20,rotate:10}} animate={{opacity:1,x:0,rotate:7}} transition={{delay:.7,duration:.5}} className="floating-note note-two"><span className="note-star">✳</span><strong>SNACK<br/>PATROL</strong><small>ON DUTY 24/7**</small></motion.div>
          <span className="hero-spark spark-one">✳</span><span className="hero-spark spark-two">✦</span><span className="hero-spark spark-three">✧</span>
          <span className="hero-stamp">OFFICIAL<br/>TRAIL<br/>GUIDE <b>№ 01</b></span>
        </div>
      </div>
      <div className="hero-foot wrap"><span>SCROLL TO WANDER <ArrowDown size={14}/></span><span>*SYNTHETIC EXAMPLE &nbsp; **THE GOAT SLEEPS. THE JOKES DON’T.</span><span>{server==='ready'?'TRAIL READY':'WAKING THE CAMP'}</span></div>
    </section>
    <div className="ticker" aria-hidden="true"><div><span>HIDDEN FEES ✳</span><span>LATE-NIGHT ORDERS ✳</span><span>SNACK ATTACKS ✳</span><span>BETTER WHAT-IFS ✳</span><span>HIDDEN FEES ✳</span><span>LATE-NIGHT ORDERS ✳</span><span>SNACK ATTACKS ✳</span></div></div>
    <section className="story-section section-pad" id="story"><div className="wrap story-grid">
      <motion.div {...(reduced?{}:appear)} className="story-copy"><span className="section-tag"><span>01</span> THE PROBLEM</span><h2>ALL THOSE<br/><span>LITTLE</span> THINGS<br/>ADD UP<span className="accent-dot">.</span></h2><p>A delivery fee here. A midnight order there. Before you know it, your receipts have written a whole story about your spending.</p><p className="story-emphasis">Ranger helps you actually read it.</p><div className="handwritten">“I’m not judging. I’m observing.” <span>— Ranger</span></div></motion.div>
      <motion.div {...(reduced?{}:appear)} className="story-art"><div className="story-paper"><div className="paper-top"><span>THE RECEIPT REPORT</span><span>✳</span></div><div className="paper-heading">the little<br/><i>things.</i></div><div className="paper-rule"/><div className="paper-row"><span>Delivery & packaging</span><b>₹ 159</b></div><div className="paper-row"><span>After-hours orders</span><b>₹ 1,087</b></div><div className="paper-row"><span>Snack patrol</span><b>₹ 979</b></div><div className="paper-rule"/><div className="paper-foot">ALL SYNTHETIC EXAMPLES <span>✦</span></div></div><div className="story-sticker">WAIT.<br/>THAT WAS<br/><b>EXTRA?</b></div><span className="story-orbit">✦</span></motion.div>
    </div></section>
    <section className="features-section section-pad" id="features"><div className="wrap"><div className="section-heading"><div><span className="section-tag"><span>02</span> THE GOOD STUFF</span><h2>THREE STOPS.<br/><em>ONE SMARTER TRAIL.</em></h2></div><p>A lot less mystery, a little more “oh, that’s where it went.”</p></div>
      <div className="feature-grid">
        <motion.article {...(reduced?{}:appear)} className="feature-card feature-camp"><div className="feature-top"><span>01 / THE OVERVIEW</span><Sparkles size={20}/></div><div className="feature-icon">✳</div><h3>The<br/>Camp<span>.</span></h3><p>Your spending, fees, and late-night habits—all in one honest little snapshot.</p><Link to="/app/camp" onClick={()=>setMode('demo')}>EXPLORE THE CAMP <ArrowUpRight size={18}/></Link></motion.article>
        <motion.article {...(reduced?{}:{...appear,transition:{duration:.65,delay:.1}})} className="feature-card feature-trail"><div className="feature-top"><span>02 / THE DETAILS</span><Eye size={20}/></div><div className="feature-icon">↝</div><h3>The<br/>Trail<span>.</span></h3><p>Follow every transaction, inspect the fees, and fix the tags Ranger got wrong.</p><Link to="/app/trail" onClick={()=>setMode('demo')}>FOLLOW THE TRAIL <ArrowUpRight size={18}/></Link></motion.article>
        <motion.article {...(reduced?{}:{...appear,transition:{duration:.65,delay:.2}})} className="feature-card feature-stash"><div className="feature-top"><span>03 / THE WHAT-IFS</span><Zap size={20}/></div><div className="feature-icon">✺</div><h3>The<br/>Stash<span>.</span></h3><p>Play with the possibilities. Simulations stay hypothetical, not promises.</p><Link to="/app/stash" onClick={()=>setMode('demo')}>OPEN THE STASH <ArrowUpRight size={18}/></Link></motion.article>
      </div><p className="feature-note"><Check size={15}/> Counts are real to the selected data; categories overlap, so don’t add them together.</p></div></section>
    <section className="trust-section section-pad"><div className="wrap trust-grid"><div><span className="section-tag"><span>03</span> YOU’RE IN CONTROL</span><h2>YOUR TRAIL.<br/><em>YOUR RULES.</em></h2><p>Start with a private, no-login synthetic demo. Sign-in is separate from Gmail permission—and public Gmail sync isn’t part of this free release.</p><Link className="plain-arrow" to="/privacy">READ OUR PRIVACY NOTES <ArrowUpRight size={17}/></Link></div><div className="trust-list"><div><LockKeyhole/><span><b>No inbox needed for the demo.</b><small>Every sample receipt is fictional.</small></span><span>01</span></div><div><Eye/><span><b>See what the numbers mean.</b><small>Fees can already be included in totals.</small></span><span>02</span></div><div><Star/><span><b>Curious, not judgmental.</b><small>Roasts are playful. You remain in charge.</small></span><span>03</span></div></div></div></section>
    <section className="last-cta"><div className="wrap last-cta-inner"><div><span className="eyebrow">READY, TRAILBLAZER?</span><h2>LET’S SEE WHERE<br/>YOUR MONEY <em>WENT.</em></h2><Link to="/app/camp" onClick={()=>setMode('demo')} className="btn btn-cream btn-large">Take a look around <ArrowUpRight size={19}/></Link><p>Free demo. No account. One opinionated goat.</p></div><img src="/assets/ranger-key.webp" alt=""/></div></section>
    <footer className="landing-footer"><div className="wrap footer-grid"><Brand light/><p>A little more clarity.<br/>A lot more personality.</p><div><a href="#story">THE STORY</a><a href="#features">THE GOODS</a><Link to="/privacy">PRIVACY</Link></div><span>© 2026 RANGER<br/>BUILT FOR THE CURIOUS ✳</span></div></footer>
  </main>
}
