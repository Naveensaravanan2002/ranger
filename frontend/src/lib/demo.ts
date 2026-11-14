import type { Receipt } from './types'

// Offline synthetic preview only. The backend independently parses the HTML fixtures.
const data: Array<Omit<Receipt, 'id' | 'source' | 'reviewRequired' | 'approximateTime' | 'items'> & {items: Array<[string, number | null, boolean]>}> = [
  { merchant: 'Midnight Burger Co.', category: 'FOOD', occurredAt: '2026-10-25T18:12:00Z', total: 350, fees: 40, items: [['Double-stack burger', 240, true], ['Crispy fries', 70, true]], feeDetails: [{label:'Delivery',amount:30},{label:'Platform fee',amount:10}] },
  { merchant: 'Moonlight Pizza', category: 'FOOD', occurredAt: '2026-10-22T19:45:00Z', total: 472, fees: 73, items: [['Farmhouse pizza',399,true]], feeDetails: [{label:'Delivery',amount:48},{label:'Packaging',amount:25}] },
  { merchant: 'Blinkit', category: 'GROCERIES', occurredAt: '2026-10-22T13:42:00Z', total: 160, fees: 16, items: [['Milk',64,false],['Bread',45,false],['Potato chips',35,true]], feeDetails: [{label:'Handling fee',amount:16}] },
  { merchant: 'The Book Stop', category: 'SHOPPING', occurredAt: '2026-10-20T08:36:00Z', total: 599, fees: 0, items: [['The Art of Thinking Clearly',599,false]], feeDetails: [] },
  { merchant: 'Swiggy', category: 'FOOD', occurredAt: '2026-10-17T22:00:00Z', total: 265, fees: 30, items: [['Loaded fries',150,true],['Chocolate cake',85,true]], feeDetails: [{label:'Delivery',amount:24},{label:'Platform fee',amount:6}] },
  { merchant: 'Example Monthly SIP', category: 'INVESTMENT', occurredAt: '2026-10-15T04:30:00Z', total: 1200, fees: 0, items: [['Investment contribution',1200,false]], feeDetails: [] },
  { merchant: 'Night Owl Coffee', category: 'FOOD', occurredAt: '2026-10-12T22:30:00Z', total: 190, fees: 0, items: [['Coffee',190,false]], feeDetails: [] },
]
export const offlineReceipts: Receipt[] = data.map((r, i) => ({
  ...r, id: `offline-${i}`, source: 'SAMPLE', approximateTime: false, reviewRequired: false,
  items: r.items.map(([name,amount,junkFood], j) => ({id:`offline-${i}-${j}`,name,amount,junkFood})),
}))
