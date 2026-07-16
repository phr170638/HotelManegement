const BACKEND_ORIGIN = 'http://127.0.0.1:8090'

export function resolveMediaUrl(url) {
  if (!url) return ''
  if (/^https?:\/\//i.test(url)) return url
  if (url.startsWith('/')) return `${BACKEND_ORIGIN}${url}`
  return `${BACKEND_ORIGIN}/${url}`
}

export function getUserInitial(name) {
  const value = String(name || 'U').trim()
  return value ? value.slice(0, 1).toUpperCase() : 'U'
}
