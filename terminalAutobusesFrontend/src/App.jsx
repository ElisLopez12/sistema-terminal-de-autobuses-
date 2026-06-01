import { Routes, Route } from 'react-router-dom'
import MainPage from './pages/MainPage'
import LoginPage from './pages/LoginPage'
import ProtectedRoute from './components/ProtectedRoute'
import DashboardLayout from './pages/admin/DashboardLayout'
import DashboardPage from './pages/admin/DashboardPage'
import TerminalesPage from './pages/admin/TerminalesPage'
import UsuariosPage from './pages/admin/UsuariosPage'
import RutasPage from './pages/admin/RutasPage'
import AutobusesPage from './pages/admin/AutobusesPage'
import ChoferesPage from './pages/admin/ChoferesPage'
import ColectoresPage from './pages/admin/ColectoresPage'
import HorariosPage from './pages/admin/HorariosPage'
import SalidasPage from './pages/admin/SalidasPage'


export default function App() {
  return (
    <Routes>
      <Route path="/" element={<MainPage />} />
      <Route path="/login" element={<LoginPage />} />
      <Route
        path="/admin"
        element={
          <ProtectedRoute>
            <DashboardLayout />
          </ProtectedRoute>
        }
      >
        <Route index element={<DashboardPage />} />
        <Route path="terminales" element={<TerminalesPage />} />
        <Route path="usuarios" element={<UsuariosPage />} />
        <Route path="rutas" element={<RutasPage />} />
        <Route path="autobuses" element={<AutobusesPage />} />
        <Route path="choferes" element={<ChoferesPage />} />
        <Route path="colectores" element={<ColectoresPage />} />
        <Route path="horarios" element={<HorariosPage />} />
        <Route path="salidas" element={<SalidasPage />} />
      </Route>
    </Routes>
  )
}
