import { defineStore } from 'pinia'
import { searchHotels } from '@/api/search'

export const useSearchStore = defineStore('search', {
  state: () => ({
    params: {
      cityId: null,
      keyword: '',
      checkInDate: '',
      checkOutDate: '',
      starLevel: null,
      minPrice: null,
      maxPrice: null,
      page: 1,
      size: 20
    },
    results: [],
    total: 0,
    loading: false
  }),

  actions: {
    async search(params = {}) {
      this.loading = true
      this.params = { ...this.params, ...params }
      try {
        const data = await searchHotels(this.params)
        this.results = data.records || []
        this.total = data.total || 0
      } finally {
        this.loading = false
      }
    },

    setCity(cityId) {
      this.params.cityId = cityId
    },

    setKeyword(keyword) {
      this.params.keyword = keyword
    },

    clearFilters() {
      this.params.starLevel = null
      this.params.minPrice = null
      this.params.maxPrice = null
    }
  }
})
