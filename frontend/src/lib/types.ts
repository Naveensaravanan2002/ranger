export type Category = 'FOOD' | 'GROCERIES' | 'SHOPPING' | 'INVESTMENT' | 'OTHER'
export interface ReceiptItem { id: string; name: string; amount: number | null; junkFood: boolean }
export interface Fee { label: string; amount: number }
export interface Receipt {
  id: string; merchant: string; category: Category; occurredAt: string; approximateTime: boolean
  total: number; fees: number; items: ReceiptItem[]; feeDetails: Fee[]; source: string; reviewRequired: boolean
}
export interface Summary {
  totalSpend: number; extraFees: number; lateNight: number; junkFood: number; junkItemsWithoutPrice: number
  investments: number; receiptCount: number; timezone: string; lateStart: string; lateEnd: string
}
export interface Snapshot { demo: boolean; summary: Summary; receipts: Receipt[] }
export type Period = 'all' | 'month' | '30days'
export type Mode = 'demo' | 'account'
export interface Settings { timezone: string; start: string; end: string }
