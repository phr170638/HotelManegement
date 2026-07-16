const imageBase = 'https://picsum.photos'
const legacyImageHost = 'https://coresg-normal.trae.ai/api/ide/v1/text_to_image'

function toSeed(value, fallback = 1) {
  const input = String(value || fallback)
  let hash = 0
  for (let index = 0; index < input.length; index += 1) {
    hash = ((hash << 5) - hash) + input.charCodeAt(index)
    hash |= 0
  }
  return Math.abs(hash % 1000) + 1
}

function buildImageUrl(seed, width = 1200, height = 800) {
  return `${imageBase}/${width}/${height}?random=${toSeed(seed, 1)}`
}

function isLegacyGeneratedUrl(url) {
  return typeof url === 'string' && url.includes(legacyImageHost)
}

function normalizeImageUrl(url, seed, width = 1200, height = 800) {
  if (url && !isLegacyGeneratedUrl(url)) {
    return url
  }
  return buildImageUrl(seed, width, height)
}

export const heroBannerImage = buildImageUrl('hero-banner', 1440, 900)
export const authVisualImage = buildImageUrl('auth-visual', 900, 1400)
export const adminVisualImage = buildImageUrl('admin-visual', 1440, 900)

export function getCityImage(cityName) {
  return buildImageUrl(`city:${cityName || 'travel-city'}`, 900, 1200)
}

export function getHotelCover(hotel) {
  const baseSeed = `hotel:${hotel?.id || hotel?.nameCn || hotel?.nameEn || 'default'}`
  const directUrl = hotel?.mainImage || hotel?.cover || hotel?.image
  if (directUrl && !isLegacyGeneratedUrl(directUrl)) {
    return directUrl
  }

  const images = Array.isArray(hotel?.images) ? hotel.images : []
  const firstImage = images.length > 0 ? (typeof images[0] === 'string' ? images[0] : images[0]?.url) : ''
  return normalizeImageUrl(firstImage, `${baseSeed}:cover`, 1200, 800)
}

export function getHotelGallery(hotel) {
  const imageItems = Array.isArray(hotel?.images) ? hotel.images : []
  const urls = imageItems
    .map((item) => (typeof item === 'string' ? item : item?.url))
    .filter(Boolean)
  const baseSeed = `gallery:${hotel?.id || hotel?.nameCn || hotel?.nameEn || 'default'}`

  return [0, 1, 2].map((index) => normalizeImageUrl(urls[index], `${baseSeed}:${index + 1}`, 1200, 800))
}

export function getRoomCover(room, hotel) {
  const roomImage = Array.isArray(room?.images) ? room.images[0] : ''
  const hotelSeed = hotel?.id || hotel?.nameCn || hotel?.nameEn || 'default'
  return normalizeImageUrl(roomImage, `room:${room?.id || room?.name || hotelSeed}`, 1200, 800)
}

export function getFacilityList(facilities) {
  if (Array.isArray(facilities) && facilities.length > 0) return facilities.slice(0, 4)
  return ['免费 WiFi', '早餐服务', '24小时前台', '快速入住']
}

function containsChineseText(value) {
  return /[\u3400-\u9fff]/.test(String(value || ''))
}

function isBrokenText(value) {
  const text = String(value || '').trim()
  if (!text) return true
  if (text.includes('�')) return true
  return /^[?？]+$/.test(text)
}

function normalizeDisplayText(value) {
  return isBrokenText(value) ? '' : String(value || '').trim()
}

export function getHotelDisplayName(hotel) {
  const nameCn = normalizeDisplayText(hotel?.nameCn)
  const nameEn = normalizeDisplayText(hotel?.nameEn)
  if (nameCn) return nameCn
  if (nameEn) return nameEn
  return '未命名酒店'
}

export function getHotelSecondaryName(hotel) {
  const nameCn = normalizeDisplayText(hotel?.nameCn)
  const nameEn = normalizeDisplayText(hotel?.nameEn)
  if (!nameCn || !nameEn) return ''
  if (nameCn.toLowerCase() === nameEn.toLowerCase()) return ''
  return containsChineseText(nameCn) ? nameEn : nameCn
}

export function getHotelAddress(hotel) {
  return normalizeDisplayText(hotel?.address) || '核心商圈旁，交通便利，适合商务与休闲出行。'
}

export function getHotelBrand(hotel) {
  return normalizeDisplayText(hotel?.brand) || '精选品牌'
}

export function getHotelDescription(hotel) {
  return normalizeDisplayText(hotel?.description) || '这里展示房型、价格、配套与评价信息，帮助你更快完成预订决策。'
}

export function formatHotelReviewCount(reviewCount) {
  const count = Number(reviewCount || 0)
  return count > 0 ? `${count} 条评价` : '暂无评价'
}

export function formatHotelScore(score) {
  const numericScore = Number(score)
  return numericScore > 0 ? numericScore.toFixed(1) : '暂无评分'
}

export function buildHotelMapUrl(hotel) {
  const name = encodeURIComponent(getHotelDisplayName(hotel))
  const address = [normalizeDisplayText(hotel?.cityName), normalizeDisplayText(hotel?.address)].filter(Boolean).join(' ')
  const searchKeyword = encodeURIComponent(address || getHotelDisplayName(hotel))
  const longitude = Number(hotel?.longitude)
  const latitude = Number(hotel?.latitude)

  if (Number.isFinite(longitude) && Number.isFinite(latitude) && longitude > 0 && latitude > 0) {
    return `https://uri.amap.com/marker?position=${longitude},${latitude}&name=${name}&src=hotel-management&coordinate=gaode&callnative=0`
  }

  return `https://uri.amap.com/search?keyword=${searchKeyword}&src=hotel-management&callnative=0`
}
