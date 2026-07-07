import { useAuthStore } from '@/stores/authStore'

function hasPermission(value) {
  if (!value) return true
  const required = Array.isArray(value) ? value : [value]
  const authStore = useAuthStore()
  return required.some((code) => authStore.permissions.includes(code))
}

export default {
  mounted(el, binding) {
    if (!hasPermission(binding.value)) {
      el.parentNode?.removeChild(el)
    }
  },
  updated(el, binding) {
    if (!hasPermission(binding.value)) {
      el.parentNode?.removeChild(el)
    }
  },
}
