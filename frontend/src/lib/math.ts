import type { Period, Receipt, Settings, Summary } from './types'
const money = (amount: number) => Math.round((amount + Number.EPSILON) * 100)
export const inr = (amount: number) => new Intl.NumberFormat('en-IN', {style:'currency',currency:'INR',maximumFractionDigits: amount % 1 === 0 ? 0 : 2}).format(amount)
export const shortDate = (iso: string, timezone: string) => new Intl.DateTimeFormat('en-IN',{day:'numeric',month:'short',year:'numeric',timeZone:timezone}).format(new Date(iso))
export const timeLabel = (iso: string, timezone: string) => new Intl.DateTimeFormat('en-IN',{hour:'numeric',minute:'2-digit',hour12:true,timeZone:timezone}).format(new Date(iso))
const clock = (time: string) => { const [h,m] = time.split(':').map(Number); return h*60+m }
export function isLate(iso: string, settings: Settings): boolean {
  const parts = new Intl.DateTimeFormat('en-GB',{hour:'2-digit',minute:'2-digit',hourCycle:'h23',timeZone:settings.timezone}).formatToParts(new Date(iso))
  const h=Number(parts.find(x=>x.type==='hour')?.value), m=Number(parts.find(x=>x.type==='minute')?.value)
  const t=h*60+m, a=clock(settings.start), b=clock(settings.end)
  if (a===b) return false
  return a<b ? t>=a && t<b : t>=a || t<b
}
export function periodRows(rows: Receipt[], period: Period, now = new Date(), timezone = 'Asia/Kolkata'): Receipt[] {
  if (period==='all') return rows
  if (period==='30days') return rows.filter(r => {const d=new Date(r.occurredAt);return d<=now && d.getTime()>=now.getTime()-30*86400000})
  const monthKey=(date:Date)=>new Intl.DateTimeFormat('en-CA',{timeZone:timezone,year:'numeric',month:'2-digit'}).format(date)
  return rows.filter(r => monthKey(new Date(r.occurredAt))===monthKey(now))
}
export function summarize(rows: Receipt[], settings: Settings): Summary {
  let total=0,fees=0,late=0,junk=0,unknown=0,invest=0
  for (const r of rows) {
    if(r.category==='INVESTMENT'){invest+=money(r.total);continue}
    total+=money(r.total); fees+=money(r.fees); if(isLate(r.occurredAt,settings)) late+=money(r.total)
    for(const item of r.items) if(item.junkFood) { if(item.amount===null) unknown++; else junk+=money(item.amount) }
  }
  return {totalSpend:total/100,extraFees:fees/100,lateNight:late/100,junkFood:junk/100,junkItemsWithoutPrice:unknown,investments:invest/100,receiptCount:rows.length,timezone:settings.timezone,lateStart:settings.start,lateEnd:settings.end}
}
export function investment(monthly: number, months: number, annual: number) {
  const m=Math.max(0,monthly), n=Math.max(0,Math.min(600,Math.floor(months))), r=Math.max(0,annual)/100/12
  const contributed=m*n, balance=r===0?contributed:m*(Math.pow(1+r,n)-1)/r
  return {contributed, balance, gain:balance-contributed}
}
