import { useEffect, useState } from 'react'
import { getTerminales, getRutas, getSalidas } from '../services/publicApi'
import TerminalCard from '../components/TerminalCard'

export default function MainPage() {
  const [terminales, setTerminales] = useState([])
  const [rutas, setRutas] = useState([])
  const [nextSalidas, setNextSalidas] = useState({})
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)

  useEffect(() => {
    getTerminales()
      .then((terminalesRes) => {
        const terminales = terminalesRes.data.content ?? []
        setTerminales(terminales)

        // Fetch routes per terminal + salidas in parallel
        const routePromises = terminales.map((t) =>
          getRutas({ origenId: t.id })
        )
        return Promise.all([...routePromises, getSalidas({ size: 50 })]).then(
          (results) => {
            const salidasRes = results[results.length - 1]
            const routeResponses = results.slice(0, -1)
            const allRoutes = routeResponses.flatMap(
              (r) => r.data.content ?? []
            )
            setRutas(allRoutes)

            // armar mapa: rutaId -> próxima salida (la más próxima en el futuro)
            const now = new Date()
            const map = {}
            for (const s of salidasRes.data.content ?? []) {
              const horaReal = new Date(s.horaReal ?? s.horaProgramada)
              if (horaReal < now) continue
              if (!map[s.rutaId]) map[s.rutaId] = s
            }
            setNextSalidas(map)
          }
        )
      })
      .catch((err) => {
        setError(err.response?.data?.message ?? err.message)
      })
      .finally(() => setLoading(false))
  }, [])

  const rutasPorTerminal = {}
  for (const ruta of rutas) {
    const key = ruta.origenId
    if (!rutasPorTerminal[key]) rutasPorTerminal[key] = []
    rutasPorTerminal[key].push(ruta)
  }

  if (loading) {
    return (
      <p className="text-center text-gray-500 py-12 text-lg">
        Cargando...
      </p>
    )
  }

  if (error) {
    return (
      <p className="text-center text-red-700 py-12 text-lg">{error}</p>
    )
  }

  return (
    <main className="max-w-3xl mx-auto px-4 py-8">
      <h1 className="text-2xl font-bold text-gray-900">Terminal de Autobuses</h1>
      <p className="text-gray-500 mb-8">
        Consultá rutas, horarios y salidas desde cada terminal
      </p>

      {terminales.length === 0 ? (
        <p className="text-center text-gray-500 py-12">
          No hay terminales registradas
        </p>
      ) : (
        <div className="flex flex-col gap-4">
          {terminales.map((terminal) => (
            <TerminalCard
              key={terminal.id}
              terminal={terminal}
              rutas={rutasPorTerminal[terminal.id] ?? []}
              nextSalidas={nextSalidas}
            />
          ))}
        </div>
      )}

      <footer className="mt-16 text-center">
        <a
          href="/login"
          className="text-xs text-gray-300 hover:text-gray-400 transition-colors no-underline"
        >
          Acceso administrativo
        </a>
      </footer>
    </main>
  )
}
