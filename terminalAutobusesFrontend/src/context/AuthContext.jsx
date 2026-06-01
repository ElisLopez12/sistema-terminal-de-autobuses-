import { createContext, useContext, useState, useEffect } from 'react'
import { login as loginApi } from '../services/auth'

const AuthContext = createContext(null)

function getUserFromStorage() {
  try {
    const stored = localStorage.getItem('auth')
    return stored ? JSON.parse(stored) : null
  } catch {
    return null
  }
}

export function AuthProvider({ children }) {
  const [user, setUser] = useState(getUserFromStorage)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    // si había un token guardado, ya lo cargamos del localStorage arriba
    setLoading(false)
  }, [])

  function doLogin(username, password) {
    return loginApi({ username, password }).then((res) => {
      const userData = {
        token: res.data.token,
        userId: res.data.userId,
        username: res.data.username,
        rol: res.data.rol,
        terminalId: res.data.terminalId,
        terminalNombre: res.data.terminalNombre,
      }
      localStorage.setItem('auth', JSON.stringify(userData))
      setUser(userData)
      return userData
    })
  }

  function doLogout() {
    localStorage.removeItem('auth')
    setUser(null)
  }

  return (
    <AuthContext.Provider
      value={{ user, loading, login: doLogin, logout: doLogout }}
    >
      {children}
    </AuthContext.Provider>
  )
}

export function useAuth() {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth debe usarse dentro de AuthProvider')
  return ctx
}
