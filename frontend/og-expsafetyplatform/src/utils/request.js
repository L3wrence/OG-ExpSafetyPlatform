 import axios from 'axios'
 import { ElMessage } from 'element-plus'
 import { useAuthStore } from '@/stores/authStore'
 import router from '@/router'
 
 const request = axios.create({
   baseURL: '/api',
   timeout: 10000,
 })

 function isLoginRequest(config) {
   return String(config?.url || '').includes('/auth/login')
 }

 function getBusinessMessage(payload, fallback = '请求失败') {
   return payload?.message || payload?.msg || fallback
 }

 function getNetworkMessage(error) {
   if (error.code === 'ECONNABORTED') {
     return '请求超时，请检查后端服务或网络连接'
   }
   return getBusinessMessage(error.response?.data, error.message || '网络错误')
 }

 function handleUnauthorized(config) {
   if (isLoginRequest(config)) return
   const authStore = useAuthStore()
   authStore.logout()
   if (router.currentRoute.value.path !== '/login') {
     router.push('/login').catch(() => {})
   }
 }
 
 // Request interceptor
 request.interceptors.request.use(
   (config) => {
     const authStore = useAuthStore()
     if (authStore.token) {
       config.headers.Authorization = authStore.token
     }
     return config
   },
   (error) => Promise.reject(error)
 )
 
 // Response interceptor
 request.interceptors.response.use(
  (response) => {
    if (response.config?.responseType === 'blob') {
      return response.data
    }
    const { code, data } = response.data || {}
     if (code === 0 || code === 200) {
       return data
     }
     if (code === 401) {
       handleUnauthorized(response.config)
     }
     const message = getBusinessMessage(response.data)
     if (!response.config?.silent) {
       ElMessage.error(message)
     }
     return Promise.reject(new Error(message))
   },
   (error) => {
     if (error.response?.status === 401) {
       handleUnauthorized(error.config)
     }
     const message = getNetworkMessage(error)
     if (!error.config?.silent) {
       ElMessage.error(message)
     }
     return Promise.reject(new Error(message))
   }
 )
 
 export default request
