import { useEffect, useState } from 'react'
import { Navigate, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'

export default function LoginPage() {
  const { login, user } = useAuth()
  const navigate = useNavigate()
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState(null)
  const [submitting, setSubmitting] = useState(false)

  // Forzar limpieza de auto-fill del navegador después del mount
  useEffect(() => {
    setUsername('')
    setPassword('')
  }, [])

  // si ya está autenticado, redirige al dashboard
  if (user) {
    return <Navigate to="/admin" replace />
  }

  function handleSubmit(e) {
    e.preventDefault()
    setError(null)
    setSubmitting(true)

    login(username, password)
      .then(() => navigate('/admin', { replace: true }))
      .catch((err) => {
        setError(err.response?.data?.message ?? 'Error al iniciar sesión')
      })
      .finally(() => setSubmitting(false))
  }

  return (
    <div className="min-h-screen flex items-center justify-center bg-gray-100 px-4">
      <form
        onSubmit={handleSubmit}
        className="bg-white rounded-xl shadow-sm p-8 w-full max-w-sm"
      >
        <h1 className="text-xl font-bold text-gray-900 mb-1">
          Acceso administrativo
        </h1>
        <p className="text-sm text-gray-500 mb-6">
          Iniciá sesión para gestionar el sistema
        </p>

        {error && (
          <p className="text-sm text-red-700 bg-red-50 rounded-lg px-3 py-2 mb-4">
            {error}
          </p>
        )}

        <label className="block mb-4">
          <span className="text-sm font-medium text-gray-700">Usuario</span>
          <input
            type="text"
            autoComplete="username"
            value={username}
            onChange={(e) => setUsername(e.target.value)}
            className="mt-1 block w-full rounded-lg border border-gray-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent"
            required
          />
        </label>

        <label className="block mb-6">
          <span className="text-sm font-medium text-gray-700">Contraseña</span>
          <input
            type="password"
            autoComplete="new-password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            className="mt-1 block w-full rounded-lg border border-gray-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent"
            required
          />
        </label>

        <button
          type="submit"
          disabled={submitting}
          className="w-full bg-blue-600 hover:bg-blue-700 disabled:bg-blue-400 text-white font-medium rounded-lg px-4 py-2 text-sm transition-colors cursor-pointer"
        >
          {submitting ? 'Ingresando...' : 'Ingresar'}
        </button>
      </form>
    </div>
  )
}
