import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { api } from '../api'
import { AiChip, Banner, CATEGORIES, PRIORITIES, label } from '../components/ui'

export default function NewTicket() {
  const navigate = useNavigate()
  const [form, setForm] = useState({ subject: '', description: '', priority: '', category: '' })
  const [fields, setFields] = useState({})
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)
  const [preview, setPreview] = useState(null)
  const [previewing, setPreviewing] = useState(false)

  const set = (key) => (e) => {
    setForm({ ...form, [key]: e.target.value })
    if (key === 'subject' || key === 'description') setPreview(null) // text changed, old preview is stale
  }

  const canPreview = form.subject.trim() && form.description.trim()

  const runPreview = async () => {
    setError('')
    setPreviewing(true)
    try {
      setPreview(await api.aiTriage({ subject: form.subject, description: form.description }))
    } catch (e) {
      setError(e.message)
    } finally {
      setPreviewing(false)
    }
  }

  const submit = async (ev) => {
    ev.preventDefault()
    setError('')
    setFields({})
    setBusy(true)
    try {
      // Empty priority/category are sent as null, which tells the backend to let AI decide.
      const ticket = await api.createTicket({
        subject: form.subject,
        description: form.description,
        priority: form.priority || null,
        category: form.category || null,
      })
      navigate(`/tickets/${ticket.id}`)
    } catch (e) {
      setError(e.message)
      setFields(e.fields || {})
      setBusy(false)
    }
  }

  return (
    <>
      <header className="page-head">
        <div>
          <h1>New ticket</h1>
          <p className="muted">Describe the problem. AI picks the priority and category unless you choose them.</p>
        </div>
      </header>

      <form className="card form-narrow" onSubmit={submit}>
        <Banner>{error}</Banner>

        <label className="field">
          <span>Subject</span>
          <input value={form.subject} onChange={set('subject')} maxLength={200}
            placeholder="e.g. Charged twice for my subscription" required />
          {fields.subject && <em>{fields.subject}</em>}
        </label>

        <label className="field">
          <span>Description</span>
          <textarea rows={7} value={form.description} onChange={set('description')} maxLength={5000}
            placeholder="What happened? What did you expect? Any error message?" required />
          {fields.description && <em>{fields.description}</em>}
        </label>

        <div className="row-2">
          <label className="field">
            <span>Priority</span>
            <select value={form.priority} onChange={set('priority')}>
              <option value="">Auto (AI decides)</option>
              {PRIORITIES.map((p) => <option key={p} value={p}>{label(p)}</option>)}
            </select>
          </label>
          <label className="field">
            <span>Category</span>
            <select value={form.category} onChange={set('category')}>
              <option value="">Auto (AI decides)</option>
              {CATEGORIES.map((c) => <option key={c} value={c}>{label(c)}</option>)}
            </select>
          </label>
        </div>

        {preview && (
          <div className="ai-box">
            <div className="ai-box-head"><strong>AI triage preview</strong><AiChip source={preview.source} /></div>
            <p>Category <b>{label(preview.category)}</b>, priority <b>{label(preview.priority)}</b>.</p>
            <p className="muted">{preview.reason}</p>
          </div>
        )}

        <div className="actions">
          <button type="button" className="btn" disabled={!canPreview || previewing} onClick={runPreview}>
            {previewing ? 'Thinking…' : 'Preview AI triage'}
          </button>
          <button className="btn primary" disabled={busy}>{busy ? 'Creating…' : 'Create ticket'}</button>
        </div>
      </form>
    </>
  )
}
