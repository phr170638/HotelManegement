const imageBase = 'https://coresg-normal.trae.ai/api/ide/v1/text_to_image?prompt='

function buildImageUrl(prompt, imageSize = 'landscape_16_9') {
  return `${imageBase}${encodeURIComponent(prompt)}&image_size=${imageSize}`
}

export const heroBannerImage = buildImageUrl(
  'boutique hotel lobby with warm gold lighting, polished stone floor, elegant reception desk, modern chinese luxury hospitality aesthetic, cinematic realistic photography',
  'landscape_16_9'
)

export const authVisualImage = buildImageUrl(
  'luxury hotel corridor with soft ambient lighting, calm hospitality atmosphere, warm beige and navy palette, realistic interior photography',
  'portrait_16_9'
)

export const adminVisualImage = buildImageUrl(
  'modern hotel operations center, elegant hospitality management office, dark navy workspace with warm brass accents, realistic editorial photography',
  'landscape_16_9'
)

export function getCityImage(cityName) {
  return buildImageUrl(
    `${cityName || 'famous travel city'} skyline at dusk, luxury travel postcard, warm lights, cinematic realistic city photography`,
    'portrait_4_3'
  )
}

export function getHotelCover(hotel) {
  const directUrl = hotel?.mainImage || hotel?.cover || hotel?.image
  if (directUrl) return directUrl

  const images = hotel?.images
  if (Array.isArray(images) && images.length > 0) {
    return typeof images[0] === 'string' ? images[0] : images[0]?.url
  }

  const hotelName = hotel?.nameCn || hotel?.nameEn || 'boutique hotel'
  const cityName = hotel?.cityName || 'travel city'
  return buildImageUrl(
    `${hotelName}, ${cityName}, boutique hotel exterior at sunset, elegant facade, premium travel photography, realistic`,
    'landscape_16_9'
  )
}

export function getHotelGallery(hotel) {
  const imageItems = Array.isArray(hotel?.images) ? hotel.images : []
  const urls = imageItems
    .map((item) => (typeof item === 'string' ? item : item?.url))
    .filter(Boolean)

  if (urls.length >= 3) return urls.slice(0, 3)

  const baseName = hotel?.nameCn || hotel?.nameEn || 'hotel suite'
  const prompts = [
    `${baseName} exterior facade at golden hour, realistic hospitality photography`,
    `${baseName} deluxe guest room, warm daylight, elegant bed and window, realistic photography`,
    `${baseName} lobby lounge with brass details and soft fabric seating, realistic photography`
  ]

  return prompts.map((prompt, index) => urls[index] || buildImageUrl(prompt, 'landscape_16_9'))
}

export function getFacilityList(facilities) {
  if (Array.isArray(facilities) && facilities.length > 0) return facilities.slice(0, 4)
  return ['免费 WiFi', '早餐服务', '24小时前台', '快速入住']
}
