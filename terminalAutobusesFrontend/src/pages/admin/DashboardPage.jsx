import { useAuth } from '../../context/AuthContext'

export default function DashboardPage() {
  const { user } = useAuth()

  return (
    <div>
      <h1 className="text-2xl font-bold text-gray-900 mb-1">
        Bienvenido, {user?.username}
      </h1>
      <p className="text-gray-500 mb-8">
        Panel de administración del sistema
      </p>

      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6">
        <WelcomeCard
          title="Centro de control"
          description="Usá el menú de la izquierda para navegar entre las secciones del sistema. Gestioná terminales, usuarios, rutas, autobuses, personal y operaciones."
          color="blue"
        />
      </div>
    </div>
  )
}

function WelcomeCard({ title, description, color }) {
  const colors = {
    blue: 'bg-blue-50 border-blue-200 text-blue-800',
  }
  return (
    <div className={`rounded-xl border p-5 ${colors[color] ?? colors.blue}`}>
      <h2 className="font-semibold mb-2">{title}</h2>
      <p className="text-sm opacity-80 leading-relaxed">{description}</p>
    </div>
  )
}
