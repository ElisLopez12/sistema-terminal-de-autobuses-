import { useCallback, useEffect, useState } from 'react'
import { getHorarios, createHorario, updateHorario, deleteHorario } from '../../services/adminApi'
import { getRutas, getTerminales } from '../../services/adminApi'
import { extractError } from '../../services/errorUtils'
import Modal from '../../components/Modal'

const DIAS = ['LUNES', 'MARTES', 'MIERCOLES', 'JUEVES', 'VIERNES', 'SABADO', 'DOMINGO']
const emptyForm = { rutaId: '', terminalOrigenId: '', diasSemana: [], horaInicio: '', horaFin: '', intervaloMinutos: '', activo: true }

export default function HorariosPage() {
  const [rows, setRows] = useState([])
  const [page, setPage] = useState(0)
  const [totalPages, setTotalPages] = useState(0)
  const [totalElements, setTotalElements] = useState(0)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const [modalOpen, setModalOpen] = useState(false)
  const [editing, setEditing] = useState(null)
  const [form, setForm] = useState(emptyForm)
  const [saving, setSaving] = useState(false)
  const [deleteTarget, setDeleteTarget] = useState(null)
  const [rutas, setRutas] = useState([])
  const [terminales, setTerminales] = useState([])

  const fetchData = useCallback((p = page) => {
    setLoading(true); setError(null)
    getHorarios({ page: p, size: 20, sort: 'id,asc' })
      .then((res) => {
        setRows(res.data.content ?? [])
        setTotalPages(res.data.totalPages ?? 0)
        setTotalElements(res.data.totalElements ?? 0)
      })
      .catch((err) => setError(extractError(err)))
      .finally(() => setLoading(false))
  }, [page])

  useEffect(() => { fetchData() }, [fetchData])

  // carga selects al abrir modal
  useEffect(() => {
    if (!modalOpen) return
    if (rutas.length === 0) getRutas({ page: 0, size: 100 }).then(r => setRutas(r.data.content ?? [])).catch(() => {})
    if (terminales.length === 0) getTerminales(0, 100).then(r => setTerminales(r.data.content ?? [])).catch(() => {})
  }, [modalOpen, rutas.length, terminales.length])

  function openCreate() { setEditing(null); setForm(emptyForm); setModalOpen(true) }
  function openEdit(row) {
    setEditing(row)
    setForm({
      rutaId: row.rutaId ?? '', terminalOrigenId: row.terminalOrigenId ?? '',
      diasSemana: [], diaSemana: row.diaSemana, horaInicio: row.horaInicio, horaFin: row.horaFin,
      intervaloMinutos: row.intervaloMinutos ?? '',
      activo: row.activo,
    })
    setModalOpen(true)
  }

  function handleSave(e) {
    e.preventDefault(); setSaving(true); setError(null)
    const payload = {
      horaInicio: form.horaInicio, horaFin: form.horaFin,
      activo: form.activo,
    }
    if (form.rutaId) payload.rutaId = Number(form.rutaId)
    if (form.terminalOrigenId) payload.terminalOrigenId = Number(form.terminalOrigenId)
    if (form.intervaloMinutos !== '') payload.intervaloMinutos = Number(form.intervaloMinutos)
    if (editing) {
      payload.diaSemana = form.diaSemana
    } else {
      payload.diasSemana = form.diasSemana
    }
    const promise = editing ? updateHorario(editing.id, payload) : createHorario(payload)
    promise
      .then(() => { setModalOpen(false); fetchData(editing ? page : 0) })
      .catch((err) => setError(extractError(err)))
      .finally(() => setSaving(false))
  }

  function confirmDelete(row) { setDeleteTarget(row) }
  function handleDelete() {
    if (!deleteTarget) return
    deleteHorario(deleteTarget.id)
      .then(() => { setDeleteTarget(null); fetchData(page) })
      .catch((err) => setError(extractError(err)))
  }

  const inputCls = 'mt-1 block w-full rounded-lg border border-gray-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent'

  return (
    <div>
      <div className="flex items-center justify-between mb-6">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">Horarios</h1>
          <p className="text-sm text-gray-500">{totalElements} registros</p>
        </div>
        <button onClick={openCreate} className="bg-blue-600 hover:bg-blue-700 text-white text-sm font-medium px-4 py-2 rounded-lg cursor-pointer">+ Nuevo horario</button>
      </div>
      {error && <p className="text-sm text-red-700 bg-red-50 rounded-lg px-4 py-2 mb-4">{error}</p>}
      {loading ? <p className="text-center text-gray-500 py-12">Cargando...</p>
      : rows.length === 0 ? (
        <div className="text-center py-12">
          <p className="text-gray-500 mb-2">No hay horarios registrados</p>
          <button onClick={openCreate} className="text-blue-600 text-sm font-medium cursor-pointer">Crear el primer horario</button>
        </div>
      ) : (
        <>
          <div className="bg-white rounded-xl shadow-sm overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-gray-200 bg-gray-50 text-left text-gray-500 font-medium">
                  <th className="px-4 py-3">ID</th>
                  <th className="px-4 py-3">Ruta</th>
                  <th className="px-4 py-3">Terminal</th>
                  <th className="px-4 py-3">Día</th>
                  <th className="px-4 py-3">Horario</th>
                  <th className="px-4 py-3">Intervalo</th>
                  <th className="px-4 py-3">Estado</th>
                  <th className="px-4 py-3 text-right">Acciones</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-100">
                {rows.map((row) => (
                  <tr key={row.id} className="hover:bg-gray-50">
                    <td className="px-4 py-3 text-gray-400">{row.id}</td>
                    <td className="px-4 py-3 font-medium text-gray-900">{row.rutaNombre ?? '—'}</td>
                    <td className="px-4 py-3 text-gray-600">{row.terminalOrigenNombre ?? '—'}</td>
                    <td className="px-4 py-3 text-gray-600">{row.diaSemana}</td>
                    <td className="px-4 py-3 text-gray-600 whitespace-nowrap">{row.horaInicio} - {row.horaFin}</td>
                    <td className="px-4 py-3 text-gray-600">{row.intervaloMinutos != null ? `${row.intervaloMinutos} min` : '—'}</td>
                    <td className="px-4 py-3">
                      <span className={`inline-block text-xs font-medium px-2 py-0.5 rounded-full ${row.activo ? 'bg-green-100 text-green-700' : 'bg-red-100 text-red-700'}`}>
                        {row.activo ? 'Activo' : 'Inactivo'}
                      </span>
                    </td>
                    <td className="px-4 py-3 text-right space-x-2">
                      <button onClick={() => openEdit(row)} className="text-blue-600 hover:text-blue-700 text-sm cursor-pointer">Editar</button>
                      <button onClick={() => confirmDelete(row)} className="text-red-600 hover:text-red-700 text-sm cursor-pointer">Eliminar</button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          <div className="flex items-center justify-between mt-4 text-sm text-gray-500">
            <span>Página {page + 1} de {totalPages}</span>
            <div className="flex gap-2">
              <button disabled={page === 0} onClick={() => setPage(p => Math.max(0, p - 1))} className="px-3 py-1.5 rounded-lg border border-gray-300 disabled:opacity-40 hover:bg-gray-100 cursor-pointer disabled:cursor-default">Anterior</button>
              <button disabled={page >= totalPages - 1} onClick={() => setPage(p => p + 1)} className="px-3 py-1.5 rounded-lg border border-gray-300 disabled:opacity-40 hover:bg-gray-100 cursor-pointer disabled:cursor-default">Siguiente</button>
            </div>
          </div>
        </>
      )}

      <Modal open={modalOpen} onClose={() => setModalOpen(false)} title={editing ? 'Editar horario' : 'Nuevo horario'}>
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
          {editing ? (
            <label className="block"><span className="text-sm font-medium text-gray-700">Día de la semana *</span>
              <select value={form.diaSemana} onChange={(e) => setForm({...form, diaSemana: e.target.value})} className={inputCls}>
                {DIAS.map(d => <option key={d} value={d}>{d}</option>)}
              </select></label>
          ) : (
            <fieldset className="block">
              <legend className="text-sm font-medium text-gray-700 mb-1">Días de la semana *</legend>
              <div className="flex flex-wrap gap-4">
                {DIAS.map(d => (
                  <label key={d} className="flex items-center gap-1.5 text-sm">
                    <input type="checkbox" checked={form.diasSemana.includes(d)}
                      onChange={(e) => setForm({
                        ...form,
                        diasSemana: e.target.checked
                          ? [...form.diasSemana, d]
                          : form.diasSemana.filter(x => x !== d)
                      })}
                      className="rounded border-gray-300" />
                    {d}
                  </label>
                ))}
              </div>
            </fieldset>
          )}
          <div className="grid grid-cols-2 gap-4">
            <label className="block"><span className="text-sm font-medium text-gray-700">Hora inicio *</span>
              <input type="time" value={form.horaInicio} onChange={(e) => setForm({...form, horaInicio: e.target.value})} className={inputCls} required /></label>
            <label className="block"><span className="text-sm font-medium text-gray-700">Hora fin *</span>
              <input type="time" value={form.horaFin} onChange={(e) => setForm({...form, horaFin: e.target.value})} className={inputCls} required /></label>
          </div>
          <label className="block"><span className="text-sm font-medium text-gray-700">Intervalo (minutos)</span>
            <input type="number" min="1" value={form.intervaloMinutos} onChange={(e) => setForm({...form, intervaloMinutos: e.target.value})} className={inputCls} /></label>
          <label className="flex items-center gap-2">
            <input type="checkbox" checked={form.activo} onChange={(e) => setForm({...form, activo: e.target.checked})} className="rounded border-gray-300" />
            <span className="text-sm font-medium text-gray-700">Activo</span>
          </label>
          <div className="flex justify-end gap-3 pt-2">
            <button type="button" onClick={() => setModalOpen(false)} className="px-4 py-2 text-sm font-medium text-gray-700 bg-gray-100 hover:bg-gray-200 rounded-lg cursor-pointer">Cancelar</button>
            <button type="submit" disabled={saving} className="px-4 py-2 text-sm font-medium text-white bg-blue-600 hover:bg-blue-700 disabled:bg-blue-400 rounded-lg cursor-pointer">
              {saving ? 'Guardando...' : editing ? 'Guardar cambios' : 'Crear horario'}
            </button>
          </div>
        </form>
      </Modal>

      <Modal open={!!deleteTarget} onClose={() => setDeleteTarget(null)} title="Confirmar eliminación">
        <p className="text-sm text-gray-600 mb-6">¿Estás seguro de eliminar este horario? Esta acción no se puede deshacer.</p>
        <div className="flex justify-end gap-3">
          <button onClick={() => setDeleteTarget(null)} className="px-4 py-2 text-sm font-medium text-gray-700 bg-gray-100 hover:bg-gray-200 rounded-lg cursor-pointer">Cancelar</button>
          <button onClick={handleDelete} className="px-4 py-2 text-sm font-medium text-white bg-red-600 hover:bg-red-700 rounded-lg cursor-pointer">Eliminar</button>
        </div>
      </Modal>
    </div>
  )
}
