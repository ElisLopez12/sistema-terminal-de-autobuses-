import api from './api'

/* ─── Terminales ─── */
export function getTerminales(page = 0, size = 20, sort = 'id,asc') {
  return api.get('/terminales', { params: { page, size, sort } })
}

export function getTerminal(id) {
  return api.get(`/terminales/${id}`)
}

export function createTerminal(data) {
  return api.post('/terminales', data)
}

export function updateTerminal(id, data) {
  return api.put(`/terminales/${id}`, data)
}

export function deleteTerminal(id) {
  return api.delete(`/terminales/${id}`)
}

/* ─── Usuarios ─── */
export function getUsuarios(page = 0, size = 20, sort = 'id,asc') {
  return api.get('/usuarios', { params: { page, size, sort } })
}

export function getUsuario(id) {
  return api.get(`/usuarios/${id}`)
}

export function createUsuario(data) {
  return api.post('/usuarios', data)
}

export function updateUsuario(id, data) {
  return api.put(`/usuarios/${id}`, data)
}

export function deleteUsuario(id) {
  return api.delete(`/usuarios/${id}`)
}

/* ─── Autobuses ─── */
export function getAutobuses(params = {}) {
  return api.get('/autobuses', { params })
}

export function getAutobus(id) {
  return api.get(`/autobuses/${id}`)
}

export function createAutobus(data) {
  return api.post('/autobuses', data)
}

export function updateAutobus(id, data) {
  return api.put(`/autobuses/${id}`, data)
}

export function deleteAutobus(id) {
  return api.delete(`/autobuses/${id}`)
}

/* ─── Choferes ─── */
export function getChoferes(params = {}) {
  return api.get('/choferes', { params })
}

export function getChofer(id) {
  return api.get(`/choferes/${id}`)
}

export function createChofer(data) {
  return api.post('/choferes', data)
}

export function updateChofer(id, data) {
  return api.put(`/choferes/${id}`, data)
}

export function deleteChofer(id) {
  return api.delete(`/choferes/${id}`)
}

/* ─── Colectores ─── */
export function getColectores(params = {}) {
  return api.get('/colectores', { params })
}

export function getColector(id) {
  return api.get(`/colectores/${id}`)
}

export function createColector(data) {
  return api.post('/colectores', data)
}

export function updateColector(id, data) {
  return api.put(`/colectores/${id}`, data)
}

export function deleteColector(id) {
  return api.delete(`/colectores/${id}`)
}

/* ─── Rutas ─── */
export function getRutas(params = {}) {
  return api.get('/rutas', { params })
}

export function getRuta(id) {
  return api.get(`/rutas/${id}`)
}

export function createRuta(data) {
  return api.post('/rutas', data)
}

export function updateRuta(id, data) {
  return api.put(`/rutas/${id}`, data)
}

export function deleteRuta(id) {
  return api.delete(`/rutas/${id}`)
}

/* ─── Paradas ─── */
export function getParadas(params = {}) {
  return api.get('/paradas', { params })
}

export function getParada(id) {
  return api.get(`/paradas/${id}`)
}

export function createParada(data) {
  return api.post('/paradas', data)
}

export function updateParada(id, data) {
  return api.put(`/paradas/${id}`, data)
}

export function deleteParada(id) {
  return api.delete(`/paradas/${id}`)
}

/* ─── Horarios ─── */
export function getHorarios(params = {}) {
  return api.get('/horarios', { params })
}

export function getHorario(id) {
  return api.get(`/horarios/${id}`)
}

export function createHorario(data) {
  return api.post('/horarios', data)
}

export function updateHorario(id, data) {
  return api.put(`/horarios/${id}`, data)
}

export function deleteHorario(id) {
  return api.delete(`/horarios/${id}`)
}

/* ─── Salidas ─── */
export function getSalidas(params = {}) {
  return api.get('/salidas', { params })
}

export function getSalida(id) {
  return api.get(`/salidas/${id}`)
}

export function createSalida(data) {
  return api.post('/salidas', data)
}

export function updateSalida(id, data) {
  return api.put(`/salidas/${id}`, data)
}

export function deleteSalida(id) {
  return api.delete(`/salidas/${id}`)
}

export function asignarAutobus(id, data) {
  return api.put(`/salidas/${id}/asignar-autobus`, data)
}

export function ajustarRetraso(id, data) {
  return api.put(`/salidas/${id}/ajustar-retraso`, data)
}

export function cambiarEstadoSalida(id, data) {
  return api.patch(`/salidas/${id}/estado`, data)
}

export function generarSalidasDelDia() {
  return api.post('/salidas/generar-del-dia')
}
