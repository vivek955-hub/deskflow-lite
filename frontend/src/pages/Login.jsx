import { useState } from 'react'
import { Link } from 'react-router-dom'
import { useAuth } from '../auth'
import { Banner } from '../components/ui'

const DEMO_PASSWORD = 'Password@123'
const DEMO_USERS = [
  { label: 'Admin', email: 'admin@acme.test' },
  { label: 'Agent', email: 'agent@acme.test' },
  { label: 'Customer', email: 'customer@acme.test' },
  { label: 'Other company', email: 'admin@globex.test' },
]

export default function Login() {
  const { login } = useAuth()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  const submit = async (ev) => {
    ev.preventDefault()
    setError('')
    setBusy(true)
    try {
      await login(email, password) // App.jsx redirects once the user is set
    } catch (e) {
      setError(e.message)
      setBusy(false)
    }
  }

  return (
    <div className="auth-screen">
      <form className="auth-card" onSubmit={submit}>
        <div className="brand auth-brand">
          <span className="brand-mark">D</span>
          <div><strong>DeskFlow</strong><small>Helpdesk for small teams</small></div>
        </div>
        <h1>Log in</h1>
        <Banner>{error}</Banner>

        <label className="field">
          <span>Email</span>
          <input type="email" value={email} onChange={(e) => setEmail(e.target.value)} autoComplete="username" required />
        </label>
        <label className="field">
          <span>Password</span>
          <input type="password" value={password} onChange={(e) => setPassword(e.target.value)} autoComplete="current-password" required />
        </label>
        <button className="btn primary block" disabled={busy}>{busy ? 'Logging in…' : 'Log in'}</button>

        <div className="demo-box">
          <span>Demo accounts (click to fill)</span>
          <div className="demo-row">
            {DEMO_USERS.map((u) => (
              <button key={u.email} type="button" className="btn small"
                onClick={() => { setEmail(u.email); setPassword(DEMO_PASSWORD) }}>
                {u.label}
              </button>
            ))}
          </div>
        </div>

        <p className="auth-foot">New company? <Link to="/register">Create a workspace</Link></p>
      </form>
    </div>
  )
}
