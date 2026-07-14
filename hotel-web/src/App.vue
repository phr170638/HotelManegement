<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import SiteHeader from '@/components/SiteHeader.vue'
import SiteFooter from '@/components/SiteFooter.vue'

const route = useRoute()
const isAdminRoute = computed(() => route.path.startsWith('/admin'))
const isAuthRoute = computed(() => ['/login', '/register'].includes(route.path))
</script>

<template>
  <div class="app-shell">
    <SiteHeader v-if="!isAdminRoute && !isAuthRoute" />
    <router-view />
    <SiteFooter v-if="!isAdminRoute && !isAuthRoute" />
  </div>
</template>
