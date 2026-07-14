import { describe, expect, it } from 'vitest'
import { getFacilityList, getHotelCover } from './hotel'

describe('hotel helpers', () => {
  it('uses provided cover image when available', () => {
    const cover = getHotelCover({ mainImage: 'https://example.com/hotel.jpg' })
    expect(cover).toBe('https://example.com/hotel.jpg')
  })

  it('returns default facilities when list is empty', () => {
    const facilities = getFacilityList([])
    expect(facilities.length).toBeGreaterThan(0)
    expect(facilities).toContain('免费 WiFi')
  })
})
