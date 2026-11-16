import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { api } from '../api'
import { useAuth } from '../auth'
import {
  AiChip, Banner, CATEGORIES, PRIORITIES, PriorityBadge, STATUSES, StatusBadge,
  formatDate, label, timeFromNow,
} from '../components/ui'

export default function TicketDetail() {
  const { id } = useParams()
  const { isStaff } = useAuth()

  const [ticket, setTicket] = useState(null)
  const [agents, setAgents] = useState([])
  const [similar, setSimilar] = useState([])
  const [error, setError] = useState('')

  const [reply, setReply] = useState('')
  const [internal, setInternal] = useState(false)
  const [sending, setSending] = useState(false)

  const [summary, setSummary] = useState(null)
  const [aiBusy, setAiBusy] = useState('') // '', 'reply' or 'summary'
  const [replySource, setReplySource] = useState(null)

  // Load the ticket, and for staff also the agent list and possible duplicates.
  useEffect(() => {
    setTicket(null); setSummary(null); setSimilar([]); setError(''); setReply(''); setReplySource(null)
    api.ticket(id).then(setTicket).catch((e) => setError(e.message))
    if (isStaff) {
      api.assignableUsers().then(setAgents).catch(() => {})
      api.similarTickets(id).then(setSimilar).catch(() => {})
    }
  }, [id, isStaff])

  const update = async (changes) => {
    setError('')
    try {
      setTicket(await api.updateTicket(id, changes))
    } catch (e) {
      setError(e.message)
    }
  }

  const send = async (ev) => {
    ev.preventDefault()
    if (!reply.trim()) return
    setSending(true)
    setError('')
    try {
      setTicket(await api.addComment(id, { body: reply, internal }))
      setReply(''); setInternal(false); setReplySource(null)
    } catch (e) {
      setError(e.message)
    } finally {
      setSending(false)
    }
  }

  const suggestReply = async () => {
    setAiBusy('reply'); setError('')
    try {
      const res = await api.aiReply(id)
      setReply(res.text); setReplySource(res.source); setInternal(false)
    } catch (e) {
      setError(e.message)
    } finally {
      setAiBusy('')
    }
  }

  const summarise = async () => {
    setAiBusy('summary'); setError('')
    try {
      setSummary(await api.aiSummary(id))
    } catch (e) {
      setError(e.message)
    } finally {
      setAiBusy('')
    }
  }

  if (!ticket) return error ? <Banner>{error}</Banner> : <p className="muted">Loading ticket…</p>

  const closed = ticket.status === 'CLOSED'
  const done = ticket.status === 'RESOLVED' || closed

  return (
    <>
      <p className="crumb"><Link to="/tickets">← Back to tickets</Link></p>
      <header className="page-head">
        <div>
          <h1>#{ticket.id} {ticket.subject}</h1>
          <p className="muted">
            Raised by {ticket.requesterName} · {formatDate(ticket.createdAt)}
          </p>
        </div>
        <div className="head-badges">
          <StatusBadge status={ticket.status} />
          <PriorityBadge priority={ticket.priority} />
        </div>
      </header>

      <Banner>{error}</Banner>

      <div className="detail">
        {/* ---------- left: conversation ---------- */}
        <div className="detail-main">
          <section className="card">
            <h2>Description</h2>
            <p className="pre">{ticket.description}</p>
          </section>

          <section className="card">
            <h2>Conversation</h2>
            {ticket.comments.length === 0 && <p className="muted">No replies yet.</p>}
            <div className="thread">
              {ticket.comments.map((c) => (
                <article key={c.id} className={`msg ${c.internal ? 'internal' : ''} ${c.authorRole === 'CUSTOMER' ? 'customer' : 'staff'}`}>
                  <header>
                    <strong>{c.authorName}</strong>
                    <span className="muted">{label(c.authorRole)} · {formatDate(c.createdAt)}</span>
                    {c.internal && <span className="badge note">Internal note</span>}
                  </header>
                  <p className="pre">{c.body}</p>
                </article>
              ))}
            </div>

            {closed ? (
              <p className="muted">This ticket is closed. Open a new ticket if you need more help.</p>
            ) : (
              <form className="reply" onSubmit={send}>
                <textarea rows={5} value={reply} onChange={(e) => setReply(e.target.value)}
                  placeholder={internal ? 'Private note for your team…' : 'Write a reply…'} />
                <div className="reply-bar">
                  {isStaff && (
                    <>
                      <button type="button" className="btn" onClick={suggestReply} disabled={aiBusy === 'reply'}>
                        {aiBusy === 'reply' ? 'Drafting…' : 'Suggest reply with AI'}
                      </button>
                      {replySource && <AiChip source={replySource} />}
                      <label className="check">
                        <input type="checkbox" checked={internal} onChange={(e) => setInternal(e.target.checked)} />
                        Internal note
                      </label>
                    </>
                  )}
                  <button className="btn primary push" disabled={sending || !reply.trim()}>
                    {sending ? 'Sending…' : internal ? 'Save note' : 'Send reply'}
                  </button>
                </div>
              </form>
            )}
          </section>
        </div>

        {/* ---------- right: details, AI tools, history ---------- */}
        <aside className="detail-side">
          <section className="card">
            <h2>Details</h2>
            {isStaff ? (
              <div className="props">
                <label className="field">
                  <span>Status</span>
                  <select value={ticket.status} onChange={(e) => update({ status: e.target.value })}>
                    {STATUSES.map((s) => <option key={s} value={s}>{label(s)}</option>)}
                  </select>
                </label>
                <label className="field">
                  <span>Priority</span>
                  <select value={ticket.priority} onChange={(e) => update({ priority: e.target.value })}>
                    {PRIORITIES.map((p) => <option key={p} value={p}>{label(p)}</option>)}
                  </select>
                </label>
                <label className="field">
                  <span>Category</span>
                  <select value={ticket.category} onChange={(e) => update({ category: e.target.value })}>
                    {CATEGORIES.map((c) => <option key={c} value={c}>{label(c)}</option>)}
                  </select>
                </label>
                <label className="field">
                  <span>Assignee</span>
                  <select value={ticket.assigneeId || ''}
                    onChange={(e) => update(e.target.value ? { assigneeId: Number(e.target.value) } : { unassign: true })}>
                    <option value="">Unassigned</option>
                    {agents.map((a) => <option key={a.id} value={a.id}>{a.name}</option>)}
                  </select>
                </label>
              </div>
            ) : (
              <dl className="facts">
                <dt>Category</dt><dd>{label(ticket.category)}</dd>
                <dt>Assigned to</dt><dd>{ticket.assigneeName || 'Not yet assigned'}</dd>
              </dl>
            )}
            <dl className="facts">
              <dt>SLA deadline</dt>
              <dd className={ticket.slaBreached ? 'overdue' : ''}>
                {formatDate(ticket.dueAt)}
                {ticket.slaBreached ? ' · missed' : done ? ' · met' : ` · ${timeFromNow(ticket.dueAt)}`}
              </dd>
              {ticket.resolvedAt && (<><dt>Resolved</dt><dd>{formatDate(ticket.resolvedAt)}</dd></>)}
              {isStaff && (<><dt>Requester email</dt><dd>{ticket.requesterEmail}</dd></>)}
            </dl>
          </section>

          {isStaff && (
            <section className="card">
              <h2>AI assistant</h2>
              <button className="btn block" onClick={summarise} disabled={aiBusy === 'summary'}>
                {aiBusy === 'summary' ? 'Summarising…' : 'Summarise this ticket'}
              </button>
              {summary && (
                <div className="ai-box">
                  <div className="ai-box-head"><strong>Summary</strong><AiChip source={summary.source} /></div>
                  <p className="pre">{summary.text}</p>
                </div>
              )}

              <h3>Possible duplicates</h3>
              {similar.length === 0 ? (
                <p className="muted">No similar tickets found.</p>
              ) : (
                <ul className="similar">
                  {similar.map((s) => (
                    <li key={s.id}>
                      <Link to={`/tickets/${s.id}`}>#{s.id} {s.subject}</Link>
                      <span className="muted">{s.matchPercent}% match · {label(s.status)}</span>
                    </li>
                  ))}
                </ul>
              )}
            </section>
          )}

          <section className="card">
            <h2>History</h2>
            <ol className="timeline">
              {ticket.events.map((e) => (
                <li key={e.id} className={e.type === 'SLA_BREACH' ? 'alert' : e.type === 'AI_TRIAGE' ? 'ai' : ''}>
                  <p>{e.detail}</p>
                  <span className="muted">{e.actorName} · {formatDate(e.createdAt)}</span>
                </li>
              ))}
            </ol>
          </section>
        </aside>
      </div>
    </>
  )
}
