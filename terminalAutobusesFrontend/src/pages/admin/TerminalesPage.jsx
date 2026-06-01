import { useCallback, useEffect, useState } from 'react'
import { getTerminales, createTerminal, updateTerminal, deleteTerminal } from '../../services/adminApi'
import { extractError } from '../../services/errorUtils'
import Modal from '../../components/Modal'

const emptyForm = { nombre: '', ubicacion: '', activo: true }

export default function TerminalesPage() {
  const [rows, setRows] = useState([])
  const [page, setPage] = useState(0)
  const [totalPages, setTotalPages] = useState(0)
  const [totalElements, setTotalElements] = useState(0)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)

  // modal
  const [modalOpen, setModalOpen] = useState(false)
  const [editing, setEditing] = useState(null)   // null = crear, objeto = editando
  const [form, setForm] = useState(emptyForm)
  const [saving, setSaving] = useState(false)

  // confirmación de borrado
  const [deleteTarget, setDeleteTarget] = useState(null)

  const fetchData = useCallback((p = page) => {
    setLoading(true)
    setError(null)
    getTerminales(p)
      .then((res) => {
        const sorted = (res.data.content ?? []).sort((a, b) => {
          if (a.activo === b.activo) return 0
          return a.activo ? -1 : 1
        })
        setRows(sorted)
        setTotalPages(res.data.totalPages ?? 0)
        setTotalElements(res.data.totalElements ?? 0)
      })
      .catch((err) => setError(extractError(err)))
      .finally(() => setLoading(false))
  }, [page])

  useEffect(() => { fetchData() }, [fetchData])

  /* ── form handlers ── */
  function openCreate() {
    setEditing(null)
    setForm(emptyForm)
    setModalOpen(true)
  }

  function openEdit(row) {
    setEditing(row)
    setForm({ nombre: row.nombre, ubicacion: row.ubicacion ?? '', activo: row.activo })
    setModalOpen(true)
  }

  function handleSave(e) {
    e.preventDefault()
    setSaving(true)
    const payload = { nombre: form.nombre.trim(), ubicacion: form.ubicacion.trim() || null, activo: form.activo }
    const promise = editing
      ? updateTerminal(editing.id, payload)
      : createTerminal(payload)

    promise
      .then(() => {
        setModalOpen(false)
        fetchData(editing ? page : 0)
      })
      .catch((err) => setError(extractError(err)))
      .finally(() => setSaving(false))
  }

  function confirmDelete(row) {
    setDeleteTarget(row)
  }

  function handleDelete() {
    if (!deleteTarget) return
    deleteTerminal(deleteTarget.id)
      .then(() => {
        setDeleteTarget(null)
        fetchData(page)
      })
      .catch((err) => setError(extractError(err)))
  }

  return (
    <div>
      <div className="flex items-center justify-between mb-6">
        <div>
          <h1 className="text-2xl font-bold text-gray-900">Terminales</h1>
          <p className="text-sm text-gray-500">{totalElements} registros</p>
        </div>
        <button
          onClick={openCreate}
          className="bg-blue-600 hover:bg-blue-700 text-white text-sm font-medium px-4 py-2 rounded-lg transition-colors cursor-pointer"
        >
          + Nueva terminal
        </button>
      </div>

      {error && (
        <p className="text-sm text-red-700 bg-red-50 rounded-lg px-4 py-2 mb-4">{error}</p>
      )}

      {loading ? (
        <p className="text-center text-gray-500 py-12">Cargando...</p>
      ) : rows.length === 0 ? (
        <div className="text-center py-12">
          <p className="text-gray-500 mb-2">No hay terminales registradas</p>
          <button onClick={openCreate} className="text-blue-600 hover:text-blue-700 text-sm font-medium cursor-pointer">
            Crear la primera terminal
          </button>
        </div>
      ) : (
        <>
          <div className="bg-white rounded-xl shadow-sm overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-gray-200 bg-gray-50 text-left text-gray-500 font-medium">
                  <th className="px-4 py-3">ID</th>
                  <th className="px-4 py-3">Nombre</th>
                  <th className="px-4 py-3">Ubicación</th>
                  <th className="px-4 py-3">Estado</th>
                  <th className="px-4 py-3 text-right">Acciones</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-100">
                {rows.map((row) => (
                  <tr key={row.id} className="hover:bg-gray-50 transition-colors">
                    <td className="px-4 py-3 text-gray-400">{row.id}</td>
                    <td className="px-4 py-3 font-medium text-gray-900">{row.nombre}</td>
                    <td className="px-4 py-3 text-gray-600">{row.ubicacion ?? '—'}</td>
                    <td className="px-4 py-3">
                      <span className={`inline-block text-xs font-medium px-2 py-0.5 rounded-full ${
                        row.activo ? 'bg-green-100 text-green-700' : 'bg-red-100 text-red-700'
                      }`}>
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

          {/* paginación */}
          <div className="flex items-center justify-between mt-4 text-sm text-gray-500">
            <span>Página {page + 1} de {totalPages}</span>
            <div className="flex gap-2">
              <button
                disabled={page === 0}
                onClick={() => setPage((p) => Math.max(0, p - 1))}
                className="px-3 py-1.5 rounded-lg border border-gray-300 disabled:opacity-40 hover:bg-gray-100 transition-colors cursor-pointer disabled:cursor-default"
              >
                Anterior
              </button>
              <button
                disabled={page >= totalPages - 1}
                onClick={() => setPage((p) => p + 1)}
                className="px-3 py-1.5 rounded-lg border border-gray-300 disabled:opacity-40 hover:bg-gray-100 transition-colors cursor-pointer disabled:cursor-default"
              >
                Siguiente
              </button>
            </div>
          </div>
        </>
      )}

      {/* modal crear/editar */}
      <Modal open={modalOpen} onClose={() => setModalOpen(false)} title={editing ? 'Editar terminal' : 'Nueva terminal'}>
        <form onSubmit={handleSave} className="space-y-4">
          <label className="block">
            <span className="text-sm font-medium text-gray-700">Nombre *</span>
            <input
              type="text"
              value={form.nombre}
              onChange={(e) => setForm({ ...form, nombre: e.target.value })}
              className="mt-1 block w-full rounded-lg border border-gray-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent"
              required
            />
          </label>

          <label className="block">
            <span className="text-sm font-medium text-gray-700">Ubicación</span>
            <input
              type="text"
              value={form.ubicacion}
              onChange={(e) => setForm({ ...form, ubicacion: e.target.value })}
              className="mt-1 block w-full rounded-lg border border-gray-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent"
            />
          </label>

          <label className="flex items-center gap-2">
            <input
              type="checkbox"
              checked={form.activo}
              onChange={(e) => setForm({ ...form, activo: e.target.checked })}
              className="rounded border-gray-300"
            />
            <span className="text-sm font-medium text-gray-700">Activo</span>
          </label>

          <div className="flex justify-end gap-3 pt-2">
            <button
              type="button"
              onClick={() => setModalOpen(false)}
              className="px-4 py-2 text-sm font-medium text-gray-700 bg-gray-100 hover:bg-gray-200 rounded-lg transition-colors cursor-pointer"
            >
              Cancelar
            </button>
            <button
              type="submit"
              disabled={saving}
              className="px-4 py-2 text-sm font-medium text-white bg-blue-600 hover:bg-blue-700 disabled:bg-blue-400 rounded-lg transition-colors cursor-pointer"
            >
              {saving ? 'Guardando...' : editing ? 'Guardar cambios' : 'Crear terminal'}
            </button>
          </div>
        </form>
      </Modal>

      {/* confirmación de borrado */}
      <Modal open={!!deleteTarget} onClose={() => setDeleteTarget(null)} title="Confirmar eliminación">
        <p className="text-sm text-gray-600 mb-6">
          ¿Estás seguro de eliminar la terminal <strong>{deleteTarget?.nombre}</strong>? Esta acción no se puede deshacer.
        </p>
        <div className="flex justify-end gap-3">
          <button
            onClick={() => setDeleteTarget(null)}
            className="px-4 py-2 text-sm font-medium text-gray-700 bg-gray-100 hover:bg-gray-200 rounded-lg transition-colors cursor-pointer"
          >
            Cancelar
          </button>
          <button
            onClick={handleDelete}
            className="px-4 py-2 text-sm font-medium text-white bg-red-600 hover:bg-red-700 rounded-lg transition-colors cursor-pointer"
          >
            Eliminar
          </button>
        </div>
      </Modal>
    </div>
  )
}
