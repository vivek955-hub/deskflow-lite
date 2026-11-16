import { NavLink, useNavigate } from 'react-router-dom'
import { useAuth } from '../auth'

export default function Layout({ children }) {
  const { user, company, isStaff, isAdmin, logout } = useAuth()
  const navigate = useNavigate()

  const signOut = () => {
    logout()
    navigate('/login')
  }

  return (
    <div className="app">
      <aside className="sidebar">
        <div className="brand">
          <span className="brand-mark">D</span>
          <div>
            <strong>DeskFlow</strong>
            <small>{company}</small>
          </div>
        </div>

        <nav className="nav">
          {isStaff && <NavLink to="/dashboard">Dashboard</NavLink>}
          <NavLink to="/tickets" end>{isStaff ? 'Tickets' : 'My tickets'}</NavLink>
          <NavLink to="/tickets/new">New ticket</NavLink>
          {isAdmin && <NavLink to="/team">Team</NavLink>}
        </nav>

        <div className="sidebar-user">
          <div className="avatar">{user.name.charAt(0)}</div>
          <div className="sidebar-user-text">
            <strong>{user.name}</strong>
            <small>{user.role.toLowerCase()}</small>
          </div>
          <button className="link-btn" onClick={signOut}>Log out</button>
        </div>
      </aside>

      <main className="main">{children}</main>
    </div>
  )
}
