import { describe, expect, it } from 'vitest'
import { getOrderStatusMeta } from './order'

describe('getOrderStatusMeta', () => {
  it('returns waiting payment metadata', () => {
    const meta = getOrderStatusMeta(0)
    expect(meta.label).toBe('待支付')
    expect(meta.tone).toBe('warning')
  })

  it('returns fallback metadata for unknown status', () => {
    const meta = getOrderStatusMeta(99)
    expect(meta.label).toBe('未知状态')
    expect(meta.tone).toBe('danger')
  })
})
