 import axios from 'axios'
 import { ElMessage } from 'element-plus'
 import { useAuthStore } from '@/stores/authStore'
 import router from '@/router'
 
 const request = axios.create({
   baseURL: '/api',
   timeout: 15000,
 })
 
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
     const { code, message, data } = response.data
     if (code === 0 || code === 200) {
       return data
     }
     if (code === 401) {
       const authStore = useAuthStore()
       authStore.logout()
       router.push('/login')
     }
     if (!response.config?.silent) {
       ElMessage.error(message || '请求失败')
     }
     return Promise.reject(new Error(message || '请求失败'))
   },
   (error) => {
     if (error.response?.status === 401) {
       const authStore = useAuthStore()
       authStore.logout()
       router.push('/login')
     }
     if (!error.config?.silent) {
       ElMessage.error(error.response?.data?.message || error.message || '网络错误')
     }
     return Promise.reject(error)
   }
 )
 
 export default request
