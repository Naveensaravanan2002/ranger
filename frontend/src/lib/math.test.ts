import { describe,it,expect } from 'vitest'
import { inr, investment, isLate, summarize } from './math'
import { offlineReceipts } from './demo'
const settings={timezone:'Asia/Kolkata',start:'23:00',end:'04:00'}
describe('Ranger calculations',()=>{
  it('uses real sample amounts without double-counting fees',()=>{
    const s=summarize(offlineReceipts,settings)
    expect(s.totalSpend).toBe(2036); expect(s.extraFees).toBe(159); expect(s.lateNight).toBe(1087); expect(s.junkFood).toBe(979); expect(s.investments).toBe(1200)
  })
  it('includes 23:00 and excludes 04:00',()=>{
    expect(isLate('2026-10-01T17:30:00Z',settings)).toBe(true)
    expect(isLate('2026-10-01T22:30:00Z',settings)).toBe(false)
  })
  it('treats no return as contributions only',()=>{expect(investment(1200,36,0).balance).toBe(43200)})
  it('calculates a labelled hypothetical annuity',()=>{expect(Math.round(investment(1200,36,8).balance)).toBe(48643)})
  it('formats rupees',()=>{expect(inr(1200)).toContain('1,200')})
})
