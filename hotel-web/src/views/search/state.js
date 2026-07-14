export const DEFAULT_PRICE_RANGE = [200, 2000]
export const DEFAULT_PAGE = 1
export const DEFAULT_PAGE_SIZE = 9
export const DEFAULT_SORT_BY = 'recommended'

export function toNumber(value, fallback = null) {
  const parsed = Number(value)
  return Number.isFinite(parsed) ? parsed : fallback
}

export function toPositiveInt(value, fallback) {
  const parsed = Math.trunc(Number(value))
  return parsed > 0 ? parsed : fallback
}

export function normalizeKeyword(value) {
  return String(value || '').trim()
}

export function normalizeSortBy(value) {
  return ['recommended', 'price', 'score'].includes(value) ? value : DEFAULT_SORT_BY
}

export function normalizeSearchState(query) {
  const minPrice = toNumber(query.minPrice, DEFAULT_PRICE_RANGE[0])
  const maxPrice = toNumber(query.maxPrice, DEFAULT_PRICE_RANGE[1])

  return {
    searchParams: {
      cityId: toNumber(query.cityId, null),
      keyword: normalizeKeyword(query.keyword),
      page: toPositiveInt(query.page, DEFAULT_PAGE),
      size: toPositiveInt(query.size, DEFAULT_PAGE_SIZE)
    },
    filters: {
      starLevel: toNumber(query.starLevel, null),
      sortBy: normalizeSortBy(String(query.sortBy || DEFAULT_SORT_BY))
    },
    priceRange: [Math.min(minPrice, maxPrice), Math.max(minPrice, maxPrice)]
  }
}

export function buildSearchRouteQuery(searchParams, filters, priceRange) {
  const keyword = normalizeKeyword(searchParams.keyword)

  return {
    cityId: searchParams.cityId || undefined,
    keyword: keyword || undefined,
    page: searchParams.page > DEFAULT_PAGE ? searchParams.page : undefined,
    size: searchParams.size !== DEFAULT_PAGE_SIZE ? searchParams.size : undefined,
    minPrice: priceRange[0] !== DEFAULT_PRICE_RANGE[0] ? priceRange[0] : undefined,
    maxPrice: priceRange[1] !== DEFAULT_PRICE_RANGE[1] ? priceRange[1] : undefined,
    starLevel: filters.starLevel || undefined,
    sortBy: normalizeSortBy(filters.sortBy) !== DEFAULT_SORT_BY ? filters.sortBy : undefined
  }
}

export function isSameRouteQuery(nextQuery, currentQuery) {
  const nextEntries = Object.entries(nextQuery).filter(([, value]) => value !== undefined)
  const currentEntries = Object.entries(currentQuery).filter(([, value]) => value !== undefined)

  if (nextEntries.length !== currentEntries.length) return false
  return nextEntries.every(([key, value]) => String(currentQuery[key]) === String(value))
}

export function buildSearchRequestParams(searchParams, filters, priceRange) {
  const keyword = normalizeKeyword(searchParams.keyword)

  return {
    ...searchParams,
    keyword: keyword || undefined,
    minPrice: priceRange[0],
    maxPrice: priceRange[1],
    starLevel: filters.starLevel,
    sortBy: normalizeSortBy(filters.sortBy) === DEFAULT_SORT_BY ? null : filters.sortBy
  }
}

export function isAbortError(error) {
  return error?.name === 'CanceledError' || error?.code === 'ERR_CANCELED' || error?.message === 'canceled'
}
