import axios from 'axios'

const api = axios.create({
  baseURL: 'http://localhost:8080/api/v1',
  headers: { 'Content-Type': 'application/json' },
})

// interceptor: agrega el token JWT a cada request si está guardado
api.interceptors.request.use((config) => {
  try {
    const stored = localStorage.getItem('auth')
    if (stored) {
      const { token } = JSON.parse(stored)
      if (token) {
        config.headers.Authorization = `Bearer ${token}`
      }
    }
  } catch {
    // si el localStorage está corrupto, simplemente no manda token
  }
  return config
})

export default api
