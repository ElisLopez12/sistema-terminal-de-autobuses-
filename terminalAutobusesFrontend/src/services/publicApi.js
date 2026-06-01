import api from './api'

export function getTerminales() {
  return api.get('/public/terminales')
}

export function getRutas({ origenId } = {}) {
  const params = {}
  if (origenId) params.origenId = origenId
  return api.get('/public/rutas', { params })
}

export function getSalidas({ rutaId, origenId, size } = {}) {
  const params = {}
  if (rutaId) params.rutaId = rutaId
  if (origenId) params.origenId = origenId
  if (size) params.size = size
  return api.get('/public/salidas', { params })
}
