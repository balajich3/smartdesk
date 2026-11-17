import axios from 'axios'

const notificationApi = axios.create({
  baseURL: import.meta.env.VITE_NOTIFICATION_API_BASE_URL || '/notification-service',
  headers: { 'Content-Type': 'application/json' },
})

notificationApi.interceptors.request.use((config) => {
  const token = localStorage.getItem('accessToken')
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

export default notificationApi
