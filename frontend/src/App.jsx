import { Navigate, Route, Routes } from 'react-router-dom'
import { useAuth } from './auth'
import Layout from './components/Layout'
import Login from './pages/Login'
import Register from './pages/Register'
import Dashboard from './pages/Dashboard'
import Tickets from './pages/Tickets'
import NewTicket from './pages/NewTicket'
import TicketDetail from './pages/TicketDetail'
import Team from './pages/Team'

// Wraps pages that need a login. "allow" limits a page to certain roles.
function Protected({ allow, children }) {
  const { user, loading } = useAuth()
  if (loading) return <div className="center-screen">Loading…</div>
  if (!user) return <Navigate to="/login" replace />
  if (allow && !allow.includes(user.role)) return <Navigate to="/tickets" replace />
  return <Layout>{children}</Layout>
}

export default function App() {
  const { user } = useAuth()
  const home = user?.role === 'CUSTOMER' ? '/tickets' : '/dashboard'

  return (
    <Routes>
      <Route path="/login" element={user ? <Navigate to={home} replace /> : <Login />} />
      <Route path="/register" element={user ? <Navigate to={home} replace /> : <Register />} />
      <Route path="/dashboard" element={<Protected allow={['ADMIN', 'AGENT']}><Dashboard /></Protected>} />
      <Route path="/tickets" element={<Protected><Tickets /></Protected>} />
      <Route path="/tickets/new" element={<Protected><NewTicket /></Protected>} />
      <Route path="/tickets/:id" element={<Protected><TicketDetail /></Protected>} />
      <Route path="/team" element={<Protected allow={['ADMIN']}><Team /></Protected>} />
      <Route path="*" element={<Navigate to={user ? home : '/login'} replace />} />
    </Routes>
  )
}
