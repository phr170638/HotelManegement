import { describe, expect, it } from 'vitest'
import { getFacilityList, getHotelCover } from './hotel'

describe('hotel helpers', () => {
  it('uses provided cover image when available', () => {
    const cover = getHotelCover({ mainImage: 'https://example.com/hotel.jpg' })
    expect(cover).toBe('https://example.com/hotel.jpg')
  })

  it('replaces legacy generated image urls with picsum images', () => {
    const cover = getHotelCover({
      id: 1,
      mainImage: 'https://coresg-normal.trae.ai/api/ide/v1/text_to_image?prompt=test&image_size=landscape_16_9'
    })
    expect(cover).toContain('https://picsum.photos/')
  })

  it('returns default facilities when list is empty', () => {
    const facilities = getFacilityList([])
    expect(facilities.length).toBeGreaterThan(0)
    expect(facilities).toContain('免费 WiFi')
  })
})
