import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { ApiError, getLedgerWithWake, wakeBackend } from './api'

describe('bounded backend wake',()=>{
  beforeEach(()=>{vi.useFakeTimers();vi.stubGlobal('window',globalThis)})
  afterEach(()=>{vi.useRealTimers();vi.unstubAllGlobals();vi.restoreAllMocks()})
  const json=(body:unknown,status=200)=>new Response(JSON.stringify(body),{status,headers:{'content-type':'application/json'}})
  it('waits through a 503, then retries the private ledger after real readiness',async()=>{
    const fetchMock=vi.fn().mockResolvedValueOnce(json({error:'sleeping'},503))
      .mockResolvedValueOnce(json({error:'starting'},503)).mockResolvedValueOnce(json({status:'ready'}))
      .mockResolvedValueOnce(json({demo:false,receipts:[],summary:{}}))
    vi.stubGlobal('fetch',fetchMock)
    const statuses:string[]=[], controller=new AbortController()
    const result=getLedgerWithWake('test-only','Asia/Kolkata','23:00','04:00',s=>statuses.push(s),controller.signal)
    await vi.advanceTimersByTimeAsync(5000)
    expect((await result).receipts).toEqual([])
    expect(statuses).toEqual(['waking','ready']);expect(fetchMock).toHaveBeenCalledTimes(4)
  })
  it('does not retry an unauthorized private request',async()=>{
    const fetchMock=vi.fn().mockResolvedValue(json({error:'Unauthorized'},401));vi.stubGlobal('fetch',fetchMock)
    await expect(getLedgerWithWake('bad','UTC','23:00','04:00',()=>{},new AbortController().signal)).rejects.toBeInstanceOf(ApiError)
    expect(fetchMock).toHaveBeenCalledTimes(1)
  })
  it('cancels an in-flight wake when the user signs out',async()=>{
    vi.stubGlobal('fetch',vi.fn((_url:string,options:RequestInit)=>new Promise((_resolve,reject)=>{
      options.signal?.addEventListener('abort',()=>reject(new DOMException('aborted','AbortError')),{once:true})
    })))
    const controller=new AbortController(), statuses:string[]=[]
    const pending=wakeBackend(s=>statuses.push(s),controller.signal);controller.abort()
    await expect(pending).resolves.toBe('aborted');expect(statuses).toEqual(['waking'])
  })
})
