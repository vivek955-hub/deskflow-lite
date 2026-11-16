import { useState } from 'react'
import { Link } from 'react-router-dom'
import { useAuth } from '../auth'
import { Banner } from '../components/ui'

export default function Register() {
  const { registerOrg } = useAuth()
  const [form, setForm] = useState({ companyName: '', name: '', email: '', password: '' })
  const [fields, setFields] = useState({})
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  const set = (key) => (e) => setForm({ ...form, [key]: e.target.value })

  const submit = async (ev) => {
    ev.preventDefault()
    setError('')
    setFields({})
    setBusy(true)
    try {
      await registerOrg(form)
    } catch (e) {
      setError(e.message)
      setFields(e.fields || {})
      setBusy(false)
    }
  }

  return (
    <div className="auth-screen">
      <form className="auth-card" onSubmit={submit}>
        <h1>Create a workspace</h1>
        <p className="muted">This creates a new company. You become its admin and can invite your team.</p>
        <Banner>{error}</Banner>

        <label className="field">
          <span>Company name</span>
          <input value={form.companyName} onChange={set('companyName')} required />
          {fields.companyName && <em>{fields.companyName}</em>}
        </label>
        <label className="field">
          <span>Your name</span>
          <input value={form.name} onChange={set('name')} required />
          {fields.name && <em>{fields.name}</em>}
        </label>
        <label className="field">
          <span>Email</span>
          <input type="email" value={form.email} onChange={set('email')} required />
          {fields.email && <em>{fields.email}</em>}
        </label>
        <label className="field">
          <span>Password (8 or more characters)</span>
          <input type="password" value={form.password} onChange={set('password')} minLength={8} required />
          {fields.password && <em>{fields.password}</em>}
        </label>
        <button className="btn primary block" disabled={busy}>{busy ? 'Creating…' : 'Create workspace'}</button>

        <p className="auth-foot">Already have an account? <Link to="/login">Log in</Link></p>
      </form>
    </div>
  )
}
