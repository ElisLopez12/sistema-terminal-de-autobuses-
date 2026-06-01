import { useCallback, useEffect, useState } from 'react'
import {
  getSalidas, createSalida, updateSalida, deleteSalida,
  getRutas, getTerminales, getHorarios, getAutobuses,
  asignarAutobus, ajustarRetraso, cambiarEstadoSalida, generarSalidasDelDia,
} from '../../services/adminApi'
import { extractError } from '../../services/errorUtils'
import Modal from '../../components/Modal'

const ESTADOS = ['PROGRAMADA', 'ABORDAJE', 'EN_RUTA', 'CANCELADA']
const emptyForm = {
  rutaId: '', horarioId: '', terminalOrigenId: '', autobusId: '',
  horaProgramada: '', retrasoMinutos: '0', estado: 'PROGRAMADA',
}

export default function SalidasPage() {
  const [rows, setRows] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const [modalOpen, setModalOpen] = useState(false)
  const [editing, setEditing] = useState(null)
  const [form, setForm] = useState(emptyForm)
  const [saving, setSaving] = useState(false)
  const [deleteTarget, setDeleteTarget] = useState(null)
  const [rutas, setRutas] = useState([])
  const [terminales, setTerminales] = useState([])
  const [horarios, setHorarios] = useState([])
  const [autobuses, setAutobuses] = useState([])

  /* ─── Acordeón ─── */
  const [rutaAccordionOpen, setRutaAccordionOpen] = useState({})

  /* ─── Filtros ─── */
  const [filtroSinAutobus, setFiltroSinAutobus] = useState(false)

  /* ─── Generar salidas ─── */
  const [generando, setGenerando] = useState(false)

  /* ─── Asignar autobús ─── */
  const [asignarTarget, setAsignarTarget] = useState(null)
  const [asignarAutobusId, setAsignarAutobusId] = useState('')
  const [autobusesDisponibles, setAutobusesDisponibles] = useState([])
  const [rutaConflict, setRutaConflict] = useState(null) // { autobus, salida } cuando hay mismatch

  /* ─── Ajustar retraso ─── */
  const [retrasoTarget, setRetrasoTarget] = useState(null)
  const [retrasoMinutosInput, setRetrasoMinutosInput] = useState('0')

  const fetchData = useCallback(() => {
    setLoading(true); setError(null)
    getSalidas({ page: 0, size: 500, sort: 'horaProgramada,asc' })
      .then((res) => setRows(res.data.content ?? []))
      .catch((err) => setError(extractError(err)))
      .finally(() => setLoading(false))
  }, [])

  useEffect(() => { fetchData() }, [fetchData])
  useEffect(() => {
    if (!modalOpen) return
    if (rutas.length === 0) getRutas({ page: 0, size: 100 }).then(r => setRutas(r.data.content ?? [])).catch(() => {})
    if (terminales.length === 0) getTerminales(0, 100).then(r => setTerminales(r.data.content ?? [])).catch(() => {})
    if (horarios.length === 0) getHorarios({ page: 0, size: 100 }).then(r => setHorarios(r.data.content ?? [])).catch(() => {})
    if (autobuses.length === 0) getAutobuses({ page: 0, size: 100 }).then(r => setAutobuses(r.data.content ?? [])).catch(() => {})
  }, [modalOpen, rutas.length, terminales.length, horarios.length, autobuses.length])

  function openCreate() { setEditing(null); setForm(emptyForm); setModalOpen(true) }
  function openEdit(row) {
    setEditing(row)
    setForm({
      rutaId: row.rutaId ?? '', horarioId: row.horarioId ?? '',
      terminalOrigenId: row.terminalOrigenId ?? '', autobusId: row.autobusId ?? '',
      horaProgramada: row.horaProgramada ? row.horaProgramada.slice(0, 16) : '',
      retrasoMinutos: row.retrasoMinutos ?? '0',
      estado: row.estado,
    })
    setModalOpen(true)
  }

  function handleSave(e) {
    e.preventDefault(); setSaving(true); setError(null)
    const payload = {
      horaProgramada: form.horaProgramada, estado: form.estado,
    }
    if (form.rutaId) payload.rutaId = Number(form.rutaId)
    if (form.terminalOrigenId) payload.terminalOrigenId = Number(form.terminalOrigenId)
    if (form.horarioId) payload.horarioId = Number(form.horarioId)
    if (form.autobusId) payload.autobusId = Number(form.autobusId)
    if (form.retrasoMinutos !== '') payload.retrasoMinutos = Number(form.retrasoMinutos)
    const promise = editing ? updateSalida(editing.id, payload) : createSalida(payload)
    promise
      .then(() => { setModalOpen(false); fetchData() })
      .catch((err) => setError(extractError(err)))
      .finally(() => setSaving(false))
  }

  /* ─── Generar salidas del día ─── */
  /* ─── Cambio rápido de estado ─── */
  function handleCambiarEstado(salidaId, nuevoEstado) {
    setSaving(true)
    cambiarEstadoSalida(salidaId, { estado: nuevoEstado })
      .then(() => fetchData())
      .catch((err) => setError(extractError(err)))
      .finally(() => setSaving(false))
  }

  /* ─── Marcar salida como EN_RUTA (auto-detecta retraso) ─── */
  function handleSalirTerminal(row) {
    setSaving(true)
    cambiarEstadoSalida(row.id, { estado: 'EN_RUTA' })
      .then(() => fetchData())
      .catch((err) => setError(extractError(err)))
      .finally(() => setSaving(false))
  }

  function handleGenerar() {
    setGenerando(true); setError(null)
    generarSalidasDelDia()
      .then(() => fetchData())
      .catch((err) => setError(extractError(err)))
      .finally(() => setGenerando(false))
  }

  /* ─── Asignar autobús ─── */
  function openAsignar(row) {
    setAsignarTarget(row)
    setAsignarAutobusId('')
    getAutobuses({ page: 0, size: 100 })
      .then(r => setAutobusesDisponibles(r.data.content ?? []))
      .catch(() => {})
  }

  function handleAsignar() {
    if (!asignarTarget || !asignarAutobusId) return
    const bus = autobusesDisponibles.find(a => a.id === Number(asignarAutobusId))
    const mismaRuta = bus?.rutaId === asignarTarget.rutaId
    if (!mismaRuta && bus?.rutaNombre) {
      setRutaConflict({ bus, salida: asignarTarget })
      return
    }
    ejecutarAsignacion(Number(asignarAutobusId), false)
  }

  function confirmarReasignarRuta() {
    if (!rutaConflict) return
    setRutaConflict(null)
    ejecutarAsignacion(rutaConflict.bus.id, true)
  }

  function ejecutarAsignacion(autobusId, sobreescribirRuta) {
    setSaving(true); setError(null)
    asignarAutobus(asignarTarget.id, { autobusId, sobreescribirRuta })
      .then(() => { setAsignarTarget(null); fetchData() })
      .catch((err) => setError(extractError(err)))
      .finally(() => setSaving(false))
  }

  /* ─── Ajustar retraso ─── */
  function openAjustarRetraso(row) {
    setRetrasoTarget(row)
    setRetrasoMinutosInput(String(row.retrasoMinutos ?? 0))
  }

  function handleAjustarRetraso() {
    if (!retrasoTarget) return
    setSaving(true); setError(null)
    ajustarRetraso(retrasoTarget.id, { retrasoMinutos: Number(retrasoMinutosInput) })
      .then(() => { setRetrasoTarget(null); fetchData() })
      .catch((err) => setError(extractError(err)))
      .finally(() => setSaving(false))
  }

  function confirmDelete(row) { setDeleteTarget(row) }
  function handleDelete() {
    if (!deleteTarget) return
    deleteSalida(deleteTarget.id)
      .then(() => { setDeleteTarget(null); fetchData() })
      .catch((err) => setError(extractError(err)))
  }

  const inputCls = 'mt-1 block w-full rounded-lg border border-gray-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent'

  /* ─── Filtrar días anteriores ─── */
  const hoy = new Date(); hoy.setHours(0, 0, 0, 0)
  const ahora = new Date()

  // Hora efectiva = horaProgramada + retrasoMinutos
  function horaEfectiva(row) {
    if (!row.horaProgramada) return null
    const h = new Date(row.horaProgramada)
    h.setMinutes(h.getMinutes() + (row.retrasoMinutos ?? 0))
    return h
  }

  // Determinar si una salida ya pasó (para opacar visualmente)
  function esPasada(row) {
    const hora = horaEfectiva(row)
    return hora !== null && hora < ahora && row.estado !== 'ABORDAJE' && row.estado !== 'EN_RUTA'
  }

  const filteredRows = (filtroSinAutobus
    ? rows.filter(r => r.tieneAutobus === false)
    : rows
  ).filter(r => !r.horaProgramada || new Date(r.horaProgramada) >= hoy)

  /* ─── Agrupar por ruta con próxima salida destacada ─── */
  const grupos = {}
  for (const salida of filteredRows) {
    const key = salida.rutaNombre ?? 'Sin ruta'
    if (!grupos[key]) grupos[key] = []
    grupos[key].push(salida)
  }

  // Para cada grupo, ordenar con la próxima salida primera
  const gruposOrdenados = Object.entries(grupos).map(([rutaNombre, salidas]) => {
    const copia = [...salidas]
    // Sort: futuras PROGRAMADA/ABORDAJE primero (por hora efectiva), luego el resto
    copia.sort((a, b) => {
      const ha = horaEfectiva(a)
      const hb = horaEfectiva(b)
      const aProx = (a.estado === 'PROGRAMADA' || a.estado === 'ABORDAJE') && ha && ha >= ahora
      const bProx = (b.estado === 'PROGRAMADA' || b.estado === 'ABORDAJE') && hb && hb >= ahora
      if (aProx && !bProx) return -1
      if (!aProx && bProx) return 1
      return (ha?.getTime() ?? 0) - (hb?.getTime() ?? 0)
    })
    const primera = copia[0]
    const hPrimera = primera ? horaEfectiva(primera) : null
    const esProxima = primera && (primera.estado === 'PROGRAMADA' || primera.estado === 'ABORDAJE') && hPrimera && hPrimera >= ahora
    return { rutaNombre, salidas: copia, proximaId: esProxima ? primera.id : null }
  })

  return (
    <div>
      <div className="flex items-center justify-between mb-6">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">Salidas</h1>
          <p className="text-sm text-gray-500">{filteredRows.length} registros mostrados{rows.length !== filteredRows.length ? ` (${rows.length - filteredRows.length} de días anteriores ocultos)` : ''}{filtroSinAutobus && `, sin autobús`}</p>
        </div>
        <div className="flex items-center gap-3">
          <button
            onClick={handleGenerar}
            disabled={generando}
            className="bg-emerald-600 hover:bg-emerald-700 disabled:bg-emerald-400 text-white text-sm font-medium px-4 py-2 rounded-lg cursor-pointer"
          >
            {generando ? 'Generando...' : '+ Generar salidas del día'}
          </button>
          <button onClick={openCreate} className="bg-blue-600 hover:bg-blue-700 text-white text-sm font-medium px-4 py-2 rounded-lg cursor-pointer">+ Nueva salida</button>
        </div>
      </div>
      {error && <p className="text-sm text-red-700 bg-red-50 rounded-lg px-4 py-2 mb-4">{error}</p>}
      {loading ? <p className="text-center text-gray-500 py-12">Cargando...</p>
      : rows.length === 0 ? (
        <div className="text-center py-12">
          <p className="text-gray-500 mb-2">No hay salidas registradas</p>
          <button onClick={openCreate} className="text-blue-600 text-sm font-medium cursor-pointer">Crear la primera salida</button>
        </div>
      ) : filteredRows.length === 0 ? (
        <div className="text-center py-12">
          <p className="text-gray-500 mb-2">Todas las salidas son de días anteriores</p>
          <button onClick={() => fetchData()} className="text-blue-600 text-sm font-medium cursor-pointer">Recargar</button>
        </div>
      ) : (
        <>
          <div className="flex items-center gap-2 mb-3">
            <label className="flex items-center gap-1.5 text-sm text-gray-600 cursor-pointer">
              <input
                type="checkbox"
                checked={filtroSinAutobus}
                onChange={(e) => setFiltroSinAutobus(e.target.checked)}
                className="rounded border-gray-300"
              />
              Solo sin autobús
            </label>
          </div>

          {gruposOrdenados.map(({ rutaNombre, salidas, proximaId }) => {
            const isOpen = rutaAccordionOpen[rutaNombre] ?? false
            return (
              <div key={rutaNombre} className="mb-4 bg-white rounded-xl shadow-sm overflow-hidden">
                <button
                  onClick={() => setRutaAccordionOpen(prev => ({ ...prev, [rutaNombre]: !isOpen }))}
                  className="w-full flex items-center justify-between px-4 py-3 text-left hover:bg-gray-50 cursor-pointer"
                >
                  <h2 className="text-lg font-semibold text-gray-800">{rutaNombre}</h2>
                  <span className="text-sm text-gray-400 flex items-center gap-2">
                    {salidas.length} salidas
                    <svg className={`w-4 h-4 transition-transform ${isOpen ? 'rotate-180' : ''}`} fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 9l-7 7-7-7" />
                    </svg>
                  </span>
                </button>
                {isOpen && (
                  <div className="overflow-x-auto border-t border-gray-100">
                    <table className="w-full text-sm">
                      <thead>
                        <tr className="border-b border-gray-200 bg-gray-50 text-left text-gray-500 font-medium">
                          <th className="px-4 py-3">ID</th>
                          <th className="px-4 py-3">Terminal</th>
                          <th className="px-4 py-3">Salida</th>
                          <th className="px-4 py-3">Autobús</th>
                          <th className="px-4 py-3">Estado</th>
                          <th className="px-4 py-3 text-right">Acciones</th>
                        </tr>
                      </thead>
                      <tbody className="divide-y divide-gray-100">
                          {salidas.map((row) => {
                            const esProxima = row.id === proximaId
                            const pasada = esPasada(row)
                            return (
                          <tr key={row.id} className={`transition-colors ${esProxima ? 'bg-gradient-to-r from-blue-50/80 to-indigo-50/80' : pasada ? 'opacity-50' : ''} hover:bg-gray-100`}>
                            <td className="px-4 py-3 text-gray-400">{row.id}</td>
                            <td className="px-4 py-3 text-gray-600">{row.terminalOrigenNombre ?? '—'}</td>
                            <td className="px-4 py-3 text-gray-600 whitespace-nowrap">
                              {(() => {
                                const h = horaEfectiva(row)
                                return h ? h.toLocaleString('es-ES') : '—'
                              })()}
                              {row.retrasoMinutos > 0 && (
                                <span className="ml-2 text-xs text-amber-600 bg-amber-50 border border-amber-200 px-1.5 py-0.5 rounded-full">+{row.retrasoMinutos}min</span>
                              )}
                            </td>
                            <td className="px-4 py-3 text-gray-600">{row.autobusNumeroUnidad ?? '—'}</td>
                            <td className="px-4 py-3">
                              <EstadoBadge estado={row.estado} tieneAutobus={row.tieneAutobus} />
                              {esProxima && <span className="ml-1.5 text-xs font-bold text-blue-600 bg-blue-100 px-1.5 py-0.5 rounded-full uppercase tracking-wide">Próxima</span>}
                            </td>
                            <td className="px-4 py-3 text-right space-x-1.5 whitespace-nowrap">
                              {row.estado === 'PROGRAMADA' && (
                                <>
                                  {!row.tieneAutobus ? (
                                    <button onClick={() => openAsignar(row)} className="text-indigo-600 hover:text-indigo-700 text-xs font-medium cursor-pointer">Asignar bus</button>
                                  ) : (
                                    <button onClick={() => handleCambiarEstado(row.id, 'ABORDAJE')} disabled={saving} className="text-amber-600 hover:text-amber-700 text-xs font-medium cursor-pointer">Abordaje</button>
                                  )}
                                  <button onClick={() => handleCambiarEstado(row.id, 'CANCELADA')} disabled={saving} className="text-red-600 hover:text-red-700 text-xs cursor-pointer">Cancelar</button>
                                </>
                              )}
                              {row.estado === 'ABORDAJE' && (
                                <>
                                  <button onClick={() => handleSalirTerminal(row)} disabled={saving} className="text-emerald-600 hover:text-emerald-700 text-xs font-medium cursor-pointer">Salió del terminal</button>
                                  <button onClick={() => handleCambiarEstado(row.id, 'CANCELADA')} disabled={saving} className="text-red-600 hover:text-red-700 text-xs cursor-pointer">Cancelar</button>
                                </>
                              )}
                              {row.estado === 'EN_RUTA' && (
                                <button onClick={() => handleCambiarEstado(row.id, 'CANCELADA')} disabled={saving} className="text-red-600 hover:text-red-700 text-xs cursor-pointer">Cancelar</button>
                              )}
                              <button onClick={() => openAjustarRetraso(row)} className="text-amber-600 hover:text-amber-700 text-xs cursor-pointer">Ajustar retraso</button>
                              <button onClick={() => openEdit(row)} className="text-blue-600 hover:text-blue-700 text-xs cursor-pointer">Editar</button>
                              <button onClick={() => confirmDelete(row)} className="text-red-600 hover:text-red-700 text-xs cursor-pointer">Eliminar</button>
                            </td>
                          </tr>
                            )})}
                      </tbody>
                    </table>
                  </div>
                )}
              </div>
            )
          })}

          {filteredRows.length > 0 && (
            <p className="text-sm text-gray-400 text-center mt-4">{filteredRows.length} salidas en total</p>
          )}
        </>
      )}

      <Modal open={modalOpen} onClose={() => setModalOpen(false)} title={editing ? 'Editar salida' : 'Nueva salida'}>
        <form onSubmit={handleSave} className="space-y-4">
          <label className="block"><span className="text-sm font-medium text-gray-700">Ruta *</span>
            <select value={form.rutaId} onChange={(e) => setForm({...form, rutaId: e.target.value})} className={inputCls} required>
              <option value="">Seleccionar ruta</option>
              {rutas.map(r => <option key={r.id} value={r.id}>{r.nombre}</option>)}
            </select></label>
          <label className="block"><span className="text-sm font-medium text-gray-700">Terminal origen *</span>
            <select value={form.terminalOrigenId} onChange={(e) => setForm({...form, terminalOrigenId: e.target.value})} className={inputCls} required>
              <option value="">Seleccionar terminal</option>
              {terminales.map(t => <option key={t.id} value={t.id}>{t.nombre}</option>)}
            </select></label>
          <label className="block"><span className="text-sm font-medium text-gray-700">Horario (opcional)</span>
            <select value={form.horarioId} onChange={(e) => setForm({...form, horarioId: e.target.value})} className={inputCls}>
              <option value="">Sin horario base</option>
              {horarios.filter(h => h.activo).map(h => (
                <option key={h.id} value={h.id}>{h.rutaNombre} - {h.diaSemana} {h.horaInicio}-{h.horaFin}</option>
              ))}
            </select></label>
          <label className="block"><span className="text-sm font-medium text-gray-700">Autobús (opcional)</span>
            <select value={form.autobusId} onChange={(e) => setForm({...form, autobusId: e.target.value})} className={inputCls}>
              <option value="">Sin asignar</option>
              {autobuses.filter(a => a.activo).map(a => (
                <option key={a.id} value={a.id}>{a.numeroUnidad} - {a.matricula}</option>
              ))}
            </select></label>
          <label className="block"><span className="text-sm font-medium text-gray-700">Fecha y hora programada *</span>
            <input type="datetime-local" value={form.horaProgramada} onChange={(e) => setForm({...form, horaProgramada: e.target.value})} className={inputCls} required /></label>
          <div className="grid grid-cols-2 gap-4">
            <label className="block"><span className="text-sm font-medium text-gray-700">Retraso (min)</span>
              <input type="number" min="0" value={form.retrasoMinutos} onChange={(e) => setForm({...form, retrasoMinutos: e.target.value})} className={inputCls} /></label>
            <label className="block"><span className="text-sm font-medium text-gray-700">Estado *</span>
              <select value={form.estado} onChange={(e) => setForm({...form, estado: e.target.value})} className={inputCls}>
                {ESTADOS.map(e => <option key={e} value={e}>{e.replace('_', ' ')}</option>)}
              </select></label>
          </div>
          <div className="flex justify-end gap-3 pt-2">
            <button type="button" onClick={() => setModalOpen(false)} className="px-4 py-2 text-sm font-medium text-gray-700 bg-gray-100 hover:bg-gray-200 rounded-lg cursor-pointer">Cancelar</button>
            <button type="submit" disabled={saving} className="px-4 py-2 text-sm font-medium text-white bg-blue-600 hover:bg-blue-700 disabled:bg-blue-400 rounded-lg cursor-pointer">
              {saving ? 'Guardando...' : editing ? 'Guardar cambios' : 'Crear salida'}
            </button>
          </div>
        </form>
      </Modal>

      <Modal open={!!deleteTarget} onClose={() => setDeleteTarget(null)} title="Confirmar eliminación">
        <p className="text-sm text-gray-600 mb-6">¿Estás seguro de eliminar esta salida? Esta acción no se puede deshacer.</p>
        <div className="flex justify-end gap-3">
          <button onClick={() => setDeleteTarget(null)} className="px-4 py-2 text-sm font-medium text-gray-700 bg-gray-100 hover:bg-gray-200 rounded-lg cursor-pointer">Cancelar</button>
          <button onClick={handleDelete} className="px-4 py-2 text-sm font-medium text-white bg-red-600 hover:bg-red-700 rounded-lg cursor-pointer">Eliminar</button>
        </div>
      </Modal>

      <Modal open={!!asignarTarget} onClose={() => setAsignarTarget(null)} title="Asignar autobús">
        <p className="text-sm text-gray-600 mb-4">
          Asignar autobús a la salida <strong>#{asignarTarget?.id}</strong>
        </p>
        <label className="block mb-6">
          <span className="text-sm font-medium text-gray-700">Autobús</span>
          <select value={asignarAutobusId} onChange={(e) => setAsignarAutobusId(e.target.value)} className={inputCls}>
            <option value="">Seleccionar autobús</option>
            {autobusesDisponibles.filter(a => a.activo).map(a => (
              <option key={a.id} value={a.id}>
                U{a.numeroUnidad} — {a.rutaNombre ?? 'Sin ruta'}{a.choferNombre ? ` — Chofer: ${a.choferNombre}` : ''}
              </option>
            ))}
          </select>
        </label>
        <div className="flex justify-end gap-3">
          <button type="button" onClick={() => setAsignarTarget(null)} className="px-4 py-2 text-sm font-medium text-gray-700 bg-gray-100 hover:bg-gray-200 rounded-lg cursor-pointer">Cancelar</button>
          <button
            onClick={handleAsignar}
            disabled={!asignarAutobusId || saving}
            className="px-4 py-2 text-sm font-medium text-white bg-indigo-600 hover:bg-indigo-700 disabled:bg-indigo-400 rounded-lg cursor-pointer"
          >
            {saving ? 'Asignando...' : 'Asignar'}
          </button>
        </div>
      </Modal>

      <Modal open={!!rutaConflict} onClose={() => setRutaConflict(null)} title="Cambiar ruta del autobús">
        <p className="text-sm text-gray-600 mb-4">
          El autobús <strong>U{rutaConflict?.bus?.numeroUnidad}</strong> está asignado a{' '}
          <strong>{rutaConflict?.bus?.rutaNombre}</strong>, pero la salida <strong>#{rutaConflict?.salida?.id}</strong>
          {' '}es de la ruta <strong>{rutaConflict?.salida?.rutaNombre}</strong>.
        </p>
        <p className="text-sm text-gray-600 mb-6">
          ¿Querés reasignar el autobús a la ruta <strong>{rutaConflict?.salida?.rutaNombre}</strong>?
        </p>
        <div className="flex justify-end gap-3">
          <button onClick={() => setRutaConflict(null)} className="px-4 py-2 text-sm font-medium text-gray-700 bg-gray-100 hover:bg-gray-200 rounded-lg cursor-pointer">Cancelar</button>
          <button onClick={confirmarReasignarRuta} className="px-4 py-2 text-sm font-medium text-white bg-amber-600 hover:bg-amber-700 rounded-lg cursor-pointer">Reasignar ruta</button>
        </div>
      </Modal>

      <Modal open={!!retrasoTarget} onClose={() => setRetrasoTarget(null)} title="Ajustar retraso">
        <p className="text-sm text-gray-600 mb-4">
          Ajustar retraso para la salida <strong>#{retrasoTarget?.id}</strong>
        </p>
        <label className="block mb-6">
          <span className="text-sm font-medium text-gray-700">Retraso (minutos)</span>
          <input
            type="number"
            min="0"
            value={retrasoMinutosInput}
            onChange={(e) => setRetrasoMinutosInput(e.target.value)}
            className={inputCls}
          />
        </label>
        <div className="flex justify-end gap-3">
          <button type="button" onClick={() => setRetrasoTarget(null)} className="px-4 py-2 text-sm font-medium text-gray-700 bg-gray-100 hover:bg-gray-200 rounded-lg cursor-pointer">Cancelar</button>
          <button
            onClick={handleAjustarRetraso}
            disabled={saving}
            className="px-4 py-2 text-sm font-medium text-white bg-amber-600 hover:bg-amber-700 disabled:bg-amber-400 rounded-lg cursor-pointer"
          >
            {saving ? 'Ajustando...' : 'Ajustar'}
          </button>
        </div>
      </Modal>
    </div>
  )
}

function EstadoBadge({ estado, tieneAutobus }) {
  const colors = {
    PROGRAMADA: tieneAutobus ? 'bg-blue-200 text-blue-800 border border-blue-300' : 'bg-blue-50 text-blue-500 border border-blue-200',
    ABORDAJE: 'bg-amber-100 text-amber-700 border border-amber-300',
    EN_RUTA: 'bg-green-100 text-green-700',
    CANCELADA: 'bg-red-100 text-red-700',
  }
  return (
    <span className={`inline-block text-xs font-medium px-2 py-0.5 rounded-full ${colors[estado] ?? 'bg-gray-100 text-gray-700'}`}>
      {estado === 'ABORDAJE' ? 'Abordaje' : estado === 'EN_RUTA' ? 'En ruta' : estado?.replace('_', ' ')}
      {estado === 'PROGRAMADA' && tieneAutobus && ' ✓'}
    </span>
  )
}
