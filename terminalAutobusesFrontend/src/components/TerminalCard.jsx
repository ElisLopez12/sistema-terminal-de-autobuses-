import { useState } from 'react'

const estados = {
  PROGRAMADA: 'text-blue-600 bg-blue-50',
  ABORDAJE: 'text-amber-600 bg-amber-50',
  EN_RUTA: 'text-green-600 bg-green-50',
  CANCELADA: 'text-red-600 bg-red-50',
}

function formatearHora(iso) {
  const d = new Date(iso)
  const horas = String(d.getHours()).padStart(2, '0')
  const mins = String(d.getMinutes()).padStart(2, '0')
  return `${horas}:${mins}`
}

function formatearFecha(iso) {
  const d = new Date(iso)
  const dia = String(d.getDate()).padStart(2, '0')
  const mes = String(d.getMonth() + 1).padStart(2, '0')
  const horas = String(d.getHours()).padStart(2, '0')
  const mins = String(d.getMinutes()).padStart(2, '0')
  return `${dia}/${mes} ${horas}:${mins}`
}

export default function TerminalCard({ terminal, rutas, nextSalidas }) {
  const [terminalOpen, setTerminalOpen] = useState(false)

  return (
    <div className="bg-white rounded-xl shadow-sm overflow-hidden">
      {/* Terminal header */}
      <button
        onClick={() => setTerminalOpen(!terminalOpen)}
        className="w-full flex items-center justify-between px-6 py-4 text-left hover:bg-gray-50 transition-colors cursor-pointer"
      >
        <div>
          <h2 className="text-lg font-semibold text-gray-900">{terminal.nombre}</h2>
          {terminal.ubicacion && (
            <p className="text-sm text-gray-500">{terminal.ubicacion}</p>
          )}
        </div>
        <div className="flex items-center gap-2">
          <span className="text-xs text-gray-400 font-medium">
            {rutas.length} ruta{rutas.length !== 1 && 's'}
          </span>
          <svg
            className={`w-5 h-5 text-gray-400 transition-transform ${terminalOpen ? 'rotate-180' : ''}`}
            fill="none" stroke="currentColor" viewBox="0 0 24 24"
          >
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 9l-7 7-7-7" />
          </svg>
        </div>
      </button>

      {/* Routes list */}
      {terminalOpen && (
        <div className="border-t border-gray-100">
          {rutas.length === 0 ? (
            <p className="text-sm text-gray-400 italic px-6 py-4">Sin rutas disponibles</p>
          ) : (
            <div className="divide-y divide-gray-50">
              {rutas.map((ruta) => (
                <RouteRow key={ruta.id} ruta={ruta} next={nextSalidas?.[ruta.id]} />
              ))}
            </div>
          )}
        </div>
      )}
    </div>
  )
}

function RouteRow({ ruta, next }) {
  const [open, setOpen] = useState(false)

  return (
    <div>
      {/* Route header */}
      <button
        onClick={() => setOpen(!open)}
        className="w-full flex items-center justify-between px-6 py-3 text-left hover:bg-gray-50 transition-colors cursor-pointer"
      >
        <div className="flex items-center gap-3 min-w-0">
          <span className="text-sm font-medium text-gray-800 truncate">{ruta.destinoNombre}</span>
          {ruta.precioBase != null && (
            <span className="text-xs text-green-700 font-semibold shrink-0">
              ${ruta.precioBase.toFixed(2)}
            </span>
          )}
        </div>
        <div className="flex items-center gap-2 shrink-0">
          {next && (
            <span className="text-xs tabular-nums text-gray-500">
              {formatearHora(next.horaReal ?? next.horaProgramada)}
            </span>
          )}
          <svg
            className={`w-4 h-4 text-gray-400 transition-transform ${open ? 'rotate-180' : ''}`}
            fill="none" stroke="currentColor" viewBox="0 0 24 24"
          >
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 9l-7 7-7-7" />
          </svg>
        </div>
      </button>

      {/* Route detail */}
      {open && (
        <div className="px-6 pb-4 space-y-3">
          {/* Next departure */}
          {next && (
            <div className="bg-blue-50 rounded-lg p-3 flex items-center justify-between">
              <div>
                <p className="text-xs font-medium text-blue-700 mb-0.5">Próxima salida</p>
                <p className="text-lg font-bold text-blue-900 tabular-nums">
                  {formatearFecha(next.horaReal ?? next.horaProgramada)}
                </p>
                {next.retrasoMinutos > 0 && (
                  <p className="text-xs text-red-600 font-medium mt-0.5">
                    Atrasado {next.retrasoMinutos} min
                  </p>
                )}
              </div>
              <span className={`px-2 py-1 rounded text-xs font-semibold ${estados[next.estado] ?? 'text-gray-500 bg-gray-100'}`}>
                {next.estado.replace(/_/g, ' ')}
              </span>
            </div>
          )}
          {!next && (
            <p className="text-xs text-gray-400 italic">Sin salidas programadas</p>
          )}

          {/* Stops */}
          {ruta.paradas && ruta.paradas.length > 0 && (
            <div>
              <p className="text-xs font-medium text-gray-500 mb-1.5">Paradas ({ruta.paradas.length})</p>
              <ol className="space-y-0.5">
                {ruta.paradas.map((p) => (
                  <li key={p.id ?? p.orden} className="flex items-center gap-2 text-sm text-gray-600">
                    <span className="w-5 h-5 rounded-full bg-gray-100 text-gray-500 flex items-center justify-center text-[10px] font-medium shrink-0">
                      {p.orden}
                    </span>
                    <span>{p.nombre}</span>
                  </li>
                ))}
              </ol>
            </div>
          )}
        </div>
      )}
    </div>
  )
}
