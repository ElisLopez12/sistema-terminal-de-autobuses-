import { useCallback, useEffect, useState } from 'react'
import { getRutas, getRuta, createRuta, updateRuta, deleteRuta } from '../../services/adminApi'
import { getTerminales } from '../../services/adminApi'
import { extractError } from '../../services/errorUtils'
import Modal from '../../components/Modal'

const emptyForm = { nombre: '', destinoNombre: '', destinoUbicacion: '', terminalOrigenId: '', precioBase: '', distanciaKm: '', duracionEstimadaMin: '', activo: true }

function paradaVacia() { return { nombre: '', orden: 0 } }

export default function RutasPage() {
  const [rows, setRows] = useState([])
  const [page, setPage] = useState(0)
  const [totalPages, setTotalPages] = useState(0)
  const [totalElements, setTotalElements] = useState(0)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const [modalOpen, setModalOpen] = useState(false)
  const [editing, setEditing] = useState(null)
  const [form, setForm] = useState(emptyForm)
  const [formParadas, setFormParadas] = useState([])
  const [saving, setSaving] = useState(false)
  const [deleteTarget, setDeleteTarget] = useState(null)
  const [terminales, setTerminales] = useState([])

  const fetchData = useCallback((p = page) => {
    setLoading(true); setError(null)
    getRutas({ page: p, size: 20, sort: 'id,asc' })
      .then((res) => {
        setRows(res.data.content ?? [])
        setTotalPages(res.data.totalPages ?? 0)
        setTotalElements(res.data.totalElements ?? 0)
      })
      .catch((err) => setError(extractError(err)))
      .finally(() => setLoading(false))
  }, [page])

  useEffect(() => { fetchData() }, [fetchData])
  useEffect(() => {
    if (modalOpen && terminales.length === 0) {
      getTerminales(0, 100).then((r) => setTerminales(r.data.content ?? [])).catch(() => {})
    }
  }, [modalOpen, terminales.length])

  function openCreate() { setEditing(null); setForm(emptyForm); setFormParadas([]); setModalOpen(true) }
  function openEdit(row) {
    setEditing(row)
    setForm({
      nombre: row.nombre, destinoNombre: row.destinoNombre,
      destinoUbicacion: row.destinoUbicacion ?? '',
      terminalOrigenId: row.origenId ?? '', precioBase: row.precioBase ?? '',
      distanciaKm: row.distanciaKm ?? '', duracionEstimadaMin: row.duracionEstimadaMin ?? '',
      activo: row.activo,
    })
    getRuta(row.id).then((res) => {
      const detail = res.data
      setFormParadas((detail.paradas ?? []).map(p => ({
        nombre: p.nombre, orden: p.orden,
      })))
    }).catch(() => setFormParadas([]))
    setModalOpen(true)
  }

  function agregarParada() { setFormParadas([...formParadas, paradaVacia()]) }
  function eliminarParada(i) {
    const copia = [...formParadas]; copia.splice(i, 1);
    setFormParadas(copia.map((p, idx) => ({ ...p, orden: idx + 1 })))
  }
  function actualizarParada(i, campo, valor) {
    const copia = [...formParadas]; copia[i][campo] = valor;
    setFormParadas(copia)
  }

  function handleSave(e) {
    e.preventDefault(); setSaving(true); setError(null)
    const payload = {
      nombre: form.nombre.trim(), destinoNombre: form.destinoNombre.trim(),
      activo: form.activo,
    }
    if (form.terminalOrigenId) payload.origenId = Number(form.terminalOrigenId)
    if (form.precioBase !== '') payload.precioBase = Number(form.precioBase)
    if (form.destinoUbicacion.trim()) payload.destinoUbicacion = form.destinoUbicacion.trim()
    if (form.distanciaKm !== '') payload.distanciaKm = Number(form.distanciaKm)
    if (form.duracionEstimadaMin !== '') payload.duracionEstimadaMin = Number(form.duracionEstimadaMin)

    // paradas: solo las que tienen nombre
    const paradas = formParadas
      .filter(p => p.nombre.trim())
      .map((p, idx) => ({ nombre: p.nombre.trim(), orden: idx + 1 }))
    if (paradas.length > 0) payload.paradas = paradas

    const promise = editing ? updateRuta(editing.id, payload) : createRuta(payload)
    promise
      .then(() => { setModalOpen(false); fetchData(editing ? page : 0) })
      .catch((err) => setError(extractError(err)))
      .finally(() => setSaving(false))
  }

  function confirmDelete(row) { setDeleteTarget(row) }
  function handleDelete() {
    if (!deleteTarget) return
    deleteRuta(deleteTarget.id)
      .then(() => { setDeleteTarget(null); fetchData(page) })
      .catch((err) => setError(extractError(err)))
  }

  const inputCls = 'mt-1 block w-full rounded-lg border border-gray-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent'

  return (
    <div>
      <div className="flex items-center justify-between mb-6">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">Rutas</h1>
          <p className="text-sm text-gray-500">{totalElements} registros</p>
        </div>
        <button onClick={openCreate} className="bg-blue-600 hover:bg-blue-700 text-white text-sm font-medium px-4 py-2 rounded-lg cursor-pointer">+ Nueva ruta</button>
      </div>
      {error && <p className="text-sm text-red-700 bg-red-50 rounded-lg px-4 py-2 mb-4">{error}</p>}
      {loading ? <p className="text-center text-gray-500 py-12">Cargando...</p>
      : rows.length === 0 ? (
        <div className="text-center py-12">
          <p className="text-gray-500 mb-2">No hay rutas registradas</p>
          <button onClick={openCreate} className="text-blue-600 text-sm font-medium cursor-pointer">Crear la primera ruta</button>
        </div>
      ) : (
        <>
          <div className="bg-white rounded-xl shadow-sm overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-gray-200 bg-gray-50 text-left text-gray-500 font-medium">
                  <th className="px-4 py-3">ID</th>
                  <th className="px-4 py-3">Nombre</th>
                  <th className="px-4 py-3">Destino</th>
                  <th className="px-4 py-3">Ubicación</th>
                  <th className="px-4 py-3">Distancia</th>
                  <th className="px-4 py-3">Duración</th>
                  <th className="px-4 py-3">Terminal origen</th>
                  <th className="px-4 py-3">Precio base</th>
                  <th className="px-4 py-3">Estado</th>
                  <th className="px-4 py-3 text-right">Acciones</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-100">
                {rows.map((row) => (
                  <tr key={row.id} className="hover:bg-gray-50">
                    <td className="px-4 py-3 text-gray-400">{row.id}</td>
                    <td className="px-4 py-3 font-medium text-gray-900">{row.nombre}</td>
                    <td className="px-4 py-3 text-gray-600">{row.destinoNombre}</td>
                    <td className="px-4 py-3 text-gray-500 max-w-[180px] truncate" title={row.destinoUbicacion ?? ''}>{row.destinoUbicacion ?? '—'}</td>
                    <td className="px-4 py-3 text-gray-600">{row.distanciaKm != null ? `${row.distanciaKm} km` : '—'}</td>
                    <td className="px-4 py-3 text-gray-600">{row.duracionEstimadaMin != null ? `${row.duracionEstimadaMin} min` : '—'}</td>
                    <td className="px-4 py-3 text-gray-600">{row.origenNombre ?? '—'}</td>
                    <td className="px-4 py-3 text-gray-600">{row.precioBase != null ? `$${row.precioBase.toFixed(2)}` : '—'}</td>
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

      <Modal open={modalOpen} onClose={() => { setModalOpen(false); setFormParadas([]) }} title={editing ? 'Editar ruta' : 'Nueva ruta'}>
        <form onSubmit={handleSave} className="space-y-4">
          <label className="block"><span className="text-sm font-medium text-gray-700">Nombre *</span>
            <input type="text" value={form.nombre} onChange={(e) => setForm({...form, nombre: e.target.value})} className={inputCls} required /></label>
          <label className="block"><span className="text-sm font-medium text-gray-700">Destino *</span>
            <input type="text" value={form.destinoNombre} onChange={(e) => setForm({...form, destinoNombre: e.target.value})} className={inputCls} required /></label>
          <label className="block"><span className="text-sm font-medium text-gray-700">Ubicación del destino</span>
            <input type="text" value={form.destinoUbicacion} onChange={(e) => setForm({...form, destinoUbicacion: e.target.value})} className={inputCls} placeholder="Dirección o referencia" /></label>
          <div className="grid grid-cols-2 gap-4">
            <label className="block"><span className="text-sm font-medium text-gray-700">Distancia (km)</span>
              <input type="number" step="0.1" min="0" value={form.distanciaKm} onChange={(e) => setForm({...form, distanciaKm: e.target.value})} className={inputCls} /></label>
            <label className="block"><span className="text-sm font-medium text-gray-700">Duración estimada (min)</span>
              <input type="number" min="1" value={form.duracionEstimadaMin} onChange={(e) => setForm({...form, duracionEstimadaMin: e.target.value})} className={inputCls} /></label>
          </div>
          <label className="block"><span className="text-sm font-medium text-gray-700">Terminal origen *</span>
            <select value={form.terminalOrigenId} onChange={(e) => setForm({...form, terminalOrigenId: e.target.value})} className={inputCls} required>
              <option value="">Seleccionar terminal</option>
              {terminales.map(t => <option key={t.id} value={t.id}>{t.nombre}</option>)}
            </select></label>
          <label className="block"><span className="text-sm font-medium text-gray-700">Precio base</span>
            <input type="number" step="0.01" min="0" value={form.precioBase} onChange={(e) => setForm({...form, precioBase: e.target.value})} className={inputCls} /></label>
          <label className="flex items-center gap-2">
            <input type="checkbox" checked={form.activo} onChange={(e) => setForm({...form, activo: e.target.checked})} className="rounded border-gray-300" />
            <span className="text-sm font-medium text-gray-700">Activo</span>
          </label>

          {/* Paradas de la ruta */}
          <div>
            <div className="flex items-center justify-between">
              <span className="text-sm font-medium text-gray-700">Paradas</span>
              <button type="button" onClick={agregarParada} className="text-xs text-blue-600 hover:text-blue-700 font-medium cursor-pointer">+ Agregar parada</button>
            </div>
            {formParadas.length === 0 && <p className="text-xs text-gray-400 mt-1">Sin paradas intermedias</p>}
            <div className="mt-1 space-y-2">
              {formParadas.map((p, i) => (
                <div key={i} className="flex items-center gap-2 bg-gray-50 rounded-lg p-2">
                  <span className="text-xs text-gray-400 font-medium w-4">{i + 1}.</span>
                  <input type="text" placeholder="Nombre" value={p.nombre}
                    onChange={(e) => actualizarParada(i, 'nombre', e.target.value)}
                    className="flex-1 rounded border border-gray-300 px-2 py-1 text-xs focus:outline-none focus:ring-1 focus:ring-blue-500" />
                  <button type="button" onClick={() => eliminarParada(i)}
                    className="text-red-500 hover:text-red-700 text-sm cursor-pointer">✕</button>
                </div>
              ))}
            </div>
          </div>

          <div className="flex justify-end gap-3 pt-2">
            <button type="button" onClick={() => { setModalOpen(false); setFormParadas([]) }} className="px-4 py-2 text-sm font-medium text-gray-700 bg-gray-100 hover:bg-gray-200 rounded-lg cursor-pointer">Cancelar</button>
            <button type="submit" disabled={saving} className="px-4 py-2 text-sm font-medium text-white bg-blue-600 hover:bg-blue-700 disabled:bg-blue-400 rounded-lg cursor-pointer">
              {saving ? 'Guardando...' : editing ? 'Guardar cambios' : 'Crear ruta'}
            </button>
          </div>
        </form>
      </Modal>

      <Modal open={!!deleteTarget} onClose={() => setDeleteTarget(null)} title="Confirmar eliminación">
        <p className="text-sm text-gray-600 mb-6">¿Estás seguro de eliminar la ruta <strong>{deleteTarget?.nombre}</strong>? Esta acción no se puede deshacer.</p>
        <div className="flex justify-end gap-3">
          <button onClick={() => setDeleteTarget(null)} className="px-4 py-2 text-sm font-medium text-gray-700 bg-gray-100 hover:bg-gray-200 rounded-lg cursor-pointer">Cancelar</button>
          <button onClick={handleDelete} className="px-4 py-2 text-sm font-medium text-white bg-red-600 hover:bg-red-700 rounded-lg cursor-pointer">Eliminar</button>
        </div>
      </Modal>
    </div>
  )
}
