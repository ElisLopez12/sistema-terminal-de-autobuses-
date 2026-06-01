import { useCallback, useEffect, useState } from 'react'
import { getUsuarios, createUsuario, updateUsuario, deleteUsuario } from '../../services/adminApi'
import { getTerminales } from '../../services/adminApi'
import { extractError } from '../../services/errorUtils'
import Modal from '../../components/Modal'

const emptyForm = { username: '', password: '', rol: 'TERMINAL_ADMIN', terminalId: '', activo: true }

export default function UsuariosPage() {
  const [rows, setRows] = useState([])
  const [page, setPage] = useState(0)
  const [totalPages, setTotalPages] = useState(0)
  const [totalElements, setTotalElements] = useState(0)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)

  // modal
  const [modalOpen, setModalOpen] = useState(false)
  const [editing, setEditing] = useState(null)
  const [form, setForm] = useState(emptyForm)
  const [saving, setSaving] = useState(false)

  // delete
  const [deleteTarget, setDeleteTarget] = useState(null)

  // terminales para el selector
  const [terminales, setTerminales] = useState([])

  // password: en edit mode, mostrar campo solo si quiere cambiarla
  const [changePassword, setChangePassword] = useState(false)

  const fetchData = useCallback((p = page) => {
    setLoading(true)
    setError(null)
    getUsuarios(p)
      .then((res) => {
        setRows(res.data.content ?? [])
        setTotalPages(res.data.totalPages ?? 0)
        setTotalElements(res.data.totalElements ?? 0)
      })
      .catch((err) => setError(extractError(err)))
      .finally(() => setLoading(false))
  }, [page])

  useEffect(() => { fetchData() }, [fetchData])

  // carga terminales solo cuando abre el modal
  useEffect(() => {
    if (modalOpen && terminales.length === 0) {
      getTerminales(0, 100).then((res) => {
        setTerminales(res.data.content ?? [])
      }).catch(() => {})
    }
  }, [modalOpen, terminales.length])

  /* ── form ── */
  function openCreate() {
    setEditing(null)
    setForm(emptyForm)
    setChangePassword(false)
    setModalOpen(true)
  }

  function openEdit(row) {
    setEditing(row)
    setForm({
      username: row.username,
      password: '',
      rol: row.rol,
      terminalId: row.terminalId ?? '',
      activo: row.activo,
    })
    setChangePassword(false)
    setModalOpen(true)
  }

  function handleSave(e) {
    e.preventDefault()
    setSaving(true)
    setError(null)

    const payload = {
      username: form.username.trim(),
      rol: form.rol,
      activo: form.activo,
    }

    // password solo si es create o si explícitamente quiere cambiarla
    if (!editing) {
      payload.password = form.password.trim()
    } else if (changePassword) {
      payload.password = form.password.trim()
    }

    if (form.rol === 'TERMINAL_ADMIN' && form.terminalId) {
      payload.terminalId = Number(form.terminalId)
    }

    const promise = editing
      ? updateUsuario(editing.id, payload)
      : createUsuario(payload)

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
    deleteUsuario(deleteTarget.id)
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
          <h1 className="text-2xl font-bold text-gray-900">Usuarios</h1>
          <p className="text-sm text-gray-500">{totalElements} registros</p>
        </div>
        <button
          onClick={openCreate}
          className="bg-blue-600 hover:bg-blue-700 text-white text-sm font-medium px-4 py-2 rounded-lg transition-colors cursor-pointer"
        >
          + Nuevo usuario
        </button>
      </div>

      {error && (
        <p className="text-sm text-red-700 bg-red-50 rounded-lg px-4 py-2 mb-4">{error}</p>
      )}

      {loading ? (
        <p className="text-center text-gray-500 py-12">Cargando...</p>
      ) : rows.length === 0 ? (
        <div className="text-center py-12">
          <p className="text-gray-500 mb-2">No hay usuarios registrados</p>
          <button onClick={openCreate} className="text-blue-600 hover:text-blue-700 text-sm font-medium cursor-pointer">
            Crear el primer usuario
          </button>
        </div>
      ) : (
        <>
          <div className="bg-white rounded-xl shadow-sm overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-gray-200 bg-gray-50 text-left text-gray-500 font-medium">
                  <th className="px-4 py-3">ID</th>
                  <th className="px-4 py-3">Usuario</th>
                  <th className="px-4 py-3">Rol</th>
                  <th className="px-4 py-3">Terminal</th>
                  <th className="px-4 py-3">Estado</th>
                  <th className="px-4 py-3 text-right">Acciones</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-100">
                {rows.map((row) => (
                  <tr key={row.id} className="hover:bg-gray-50 transition-colors">
                    <td className="px-4 py-3 text-gray-400">{row.id}</td>
                    <td className="px-4 py-3 font-medium text-gray-900">{row.username}</td>
                    <td className="px-4 py-3">
                      <span className={`inline-block text-xs font-medium px-2 py-0.5 rounded-full ${
                        row.rol === 'CENTRAL_ADMIN' ? 'bg-purple-100 text-purple-700' : 'bg-blue-100 text-blue-700'
                      }`}>
                        {row.rol === 'CENTRAL_ADMIN' ? 'Central Admin' : 'Terminal Admin'}
                      </span>
                    </td>
                    <td className="px-4 py-3 text-gray-600">{row.terminalNombre ?? '—'}</td>
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
      <Modal open={modalOpen} onClose={() => setModalOpen(false)} title={editing ? 'Editar usuario' : 'Nuevo usuario'}>
        <form onSubmit={handleSave} className="space-y-4">
          <label className="block">
            <span className="text-sm font-medium text-gray-700">Usuario *</span>
            <input
              type="text"
              autoComplete="off"
              value={form.username}
              onChange={(e) => setForm({ ...form, username: e.target.value })}
              className="mt-1 block w-full rounded-lg border border-gray-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent"
              required
            />
          </label>

          {editing && (
            <label className="flex items-center gap-2">
              <input
                type="checkbox"
                checked={changePassword}
                onChange={(e) => {
                  setChangePassword(e.target.checked)
                  if (!e.target.checked) setForm({ ...form, password: '' })
                }}
                className="rounded border-gray-300"
              />
              <span className="text-sm font-medium text-gray-700">Cambiar contraseña</span>
            </label>
          )}

          {(!editing || changePassword) && (
            <label className="block">
              <span className="text-sm font-medium text-gray-700">Contraseña *</span>
              <input
                type="password"
                autoComplete="new-password"
                value={form.password}
                onChange={(e) => setForm({ ...form, password: e.target.value })}
                className="mt-1 block w-full rounded-lg border border-gray-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                required
              />
            </label>
          )}

          <label className="block">
            <span className="text-sm font-medium text-gray-700">Rol *</span>
            <select
              value={form.rol}
              onChange={(e) => setForm({ ...form, rol: e.target.value })}
              className="mt-1 block w-full rounded-lg border border-gray-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent"
            >
              <option value="TERMINAL_ADMIN">Terminal Admin</option>
              <option value="CENTRAL_ADMIN">Central Admin</option>
            </select>
          </label>

          {form.rol === 'TERMINAL_ADMIN' && (
            <label className="block">
              <span className="text-sm font-medium text-gray-700">Terminal *</span>
              <select
                value={form.terminalId}
                onChange={(e) => setForm({ ...form, terminalId: e.target.value })}
                className="mt-1 block w-full rounded-lg border border-gray-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                required
              >
                <option value="">Seleccionar terminal</option>
                {terminales.map((t) => (
                  <option key={t.id} value={t.id}>{t.nombre}</option>
                ))}
              </select>
            </label>
          )}

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
              {saving ? 'Guardando...' : editing ? 'Guardar cambios' : 'Crear usuario'}
            </button>
          </div>
        </form>
      </Modal>

      {/* confirmación de borrado */}
      <Modal open={!!deleteTarget} onClose={() => setDeleteTarget(null)} title="Confirmar eliminación">
        <p className="text-sm text-gray-600 mb-6">
          ¿Estás seguro de eliminar el usuario <strong>{deleteTarget?.username}</strong>? Esta acción no se puede deshacer.
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
