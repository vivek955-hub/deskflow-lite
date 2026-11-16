import { useEffect, useState } from 'react'
import { api } from '../api'
import { useAuth } from '../auth'
import { Banner, formatDate, label } from '../components/ui'

const EMPTY = { name: '', email: '', password: '', role: 'AGENT' }

export default function Team() {
  const { user: me } = useAuth()
  const [users, setUsers] = useState(null)
  const [form, setForm] = useState(EMPTY)
  const [fields, setFields] = useState({})
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')
  const [busy, setBusy] = useState(false)

  const load = () => api.users().then(setUsers).catch((e) => setError(e.message))
  useEffect(() => { load() }, [])

  const set = (key) => (e) => setForm({ ...form, [key]: e.target.value })

  const add = async (ev) => {
    ev.preventDefault()
    setError(''); setNotice(''); setFields({})
    setBusy(true)
    try {
      const created = await api.createUser(form)
      setNotice(`${created.name} was added as ${label(created.role).toLowerCase()}.`)
      setForm(EMPTY)
      await load()
    } catch (e) {
      setError(e.message)
      setFields(e.fields || {})
    } finally {
      setBusy(false)
    }
  }

  const toggle = async (u) => {
    setError(''); setNotice('')
    try {
      await api.setUserActive(u.id, !u.active)
      await load()
    } catch (e) {
      setError(e.message)
    }
  }

  return (
    <>
      <header className="page-head">
        <div>
          <h1>Team</h1>
          <p className="muted">Everyone in your company's workspace: admins, agents and customers.</p>
        </div>
      </header>

      <Banner>{error}</Banner>
      <Banner kind="ok">{notice}</Banner>

      <div className="grid-team">
        <section className="card flush">
          {!users ? <p className="muted pad">Loading team…</p> : (
            <div className="table-wrap">
              <table>
                <thead><tr><th>Name</th><th>Email</th><th>Role</th><th>Joined</th><th>Status</th><th /></tr></thead>
                <tbody>
                  {users.map((u) => (
                    <tr key={u.id} className="static">
                      <td>{u.name}</td>
                      <td>{u.email}</td>
                      <td>{label(u.role)}</td>
                      <td>{formatDate(u.createdAt)}</td>
                      <td>{u.active ? 'Active' : <span className="overdue">Deactivated</span>}</td>
                      <td>
                        {u.id !== me.id && (
                          <button className="btn small" onClick={() => toggle(u)}>
                            {u.active ? 'Deactivate' : 'Activate'}
                          </button>
                        )}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </section>

        <form className="card" onSubmit={add}>
          <h2>Add a person</h2>
          <label className="field">
            <span>Name</span>
            <input value={form.name} onChange={set('name')} required />
            {fields.name && <em>{fields.name}</em>}
          </label>
          <label className="field">
            <span>Email</span>
            <input type="email" value={form.email} onChange={set('email')} required />
            {fields.email && <em>{fields.email}</em>}
          </label>
          <label className="field">
            <span>Temporary password</span>
            <input type="password" value={form.password} onChange={set('password')} minLength={8} required />
            {fields.password && <em>{fields.password}</em>}
          </label>
          <label className="field">
            <span>Role</span>
            <select value={form.role} onChange={set('role')}>
              <option value="AGENT">Agent</option>
              <option value="CUSTOMER">Customer</option>
              <option value="ADMIN">Admin</option>
            </select>
          </label>
          <button className="btn primary block" disabled={busy}>{busy ? 'Adding…' : 'Add to team'}</button>
        </form>
      </div>
    </>
  )
}
