import { describe, expect, it } from 'vitest'
import {
  buildSearchRequestParams,
  buildSearchRouteQuery,
  DEFAULT_PAGE,
  DEFAULT_PAGE_SIZE,
  DEFAULT_PRICE_RANGE,
  DEFAULT_SORT_BY,
  isAbortError,
  isSameRouteQuery,
  normalizeSearchState
} from './state'

describe('search state helpers', () => {
  it('normalizes query values and trims keyword', () => {
    const state = normalizeSearchState({
      keyword: '  西湖  ',
      page: '0',
      size: '-3',
      minPrice: '1600',
      maxPrice: '600',
      sortBy: 'unknown'
    })

    expect(state.searchParams.keyword).toBe('西湖')
    expect(state.searchParams.page).toBe(DEFAULT_PAGE)
    expect(state.searchParams.size).toBe(DEFAULT_PAGE_SIZE)
    expect(state.filters.sortBy).toBe(DEFAULT_SORT_BY)
    expect(state.priceRange).toEqual([600, 1600])
  })

  it('builds compact route query without default values', () => {
    const query = buildSearchRouteQuery(
      { cityId: 2, keyword: '  国贸 ', page: DEFAULT_PAGE, size: DEFAULT_PAGE_SIZE },
      { starLevel: null, sortBy: DEFAULT_SORT_BY },
      [...DEFAULT_PRICE_RANGE]
    )

    expect(query).toEqual({
      cityId: 2,
      keyword: '国贸',
      page: undefined,
      size: undefined,
      minPrice: undefined,
      maxPrice: undefined,
      starLevel: undefined,
      sortBy: undefined
    })
  })

  it('builds request params for API calls', () => {
    const params = buildSearchRequestParams(
      { cityId: null, keyword: '  静安寺  ', page: 2, size: 9 },
      { starLevel: 5, sortBy: 'price' },
      [300, 1200]
    )

    expect(params).toMatchObject({
      keyword: '静安寺',
      page: 2,
      size: 9,
      minPrice: 300,
      maxPrice: 1200,
      starLevel: 5,
      sortBy: 'price'
    })
  })

  it('compares route queries by effective values only', () => {
    expect(
      isSameRouteQuery(
        { keyword: '外滩', page: undefined, size: undefined },
        { keyword: '外滩' }
      )
    ).toBe(true)
  })

  it('recognizes canceled requests', () => {
    expect(isAbortError({ code: 'ERR_CANCELED' })).toBe(true)
    expect(isAbortError(new Error('boom'))).toBe(false)
  })
})
