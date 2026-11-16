import { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { api } from '../api'
import { useAuth } from '../auth'
import { Banner, PRIORITIES, PriorityBadge, STATUSES, StatusBadge, label, timeFromNow } from '../components/ui'

const PAGE_SIZE = 10

export default function Tickets() {
  const { isStaff } = useAuth()
  const navigate = useNavigate()
  const [filters, setFilters] = useState({ q: '', status: '', priority: '', mine: false })
  const [page, setPage] = useState(0)
  const [result, setResult] = useState(null)
  const [error, setError] = useState('')

  // Reload whenever a filter or the page changes. The short delay avoids a request per keystroke.
  useEffect(() => {
    let cancelled = false
    const timer = setTimeout(() => {
      api.tickets({ ...filters, page, size: PAGE_SIZE })
        .then((res) => { if (!cancelled) { setResult(res); setError('') } })
        .catch((e) => { if (!cancelled) setError(e.message) })
    }, 250)
    return () => { cancelled = true; clearTimeout(timer) }
  }, [filters, page])

  const change = (key, value) => {
    setFilters({ ...filters, [key]: value })
    setPage(0)
  }

  return (
    <>
      <header className="page-head">
        <div>
          <h1>{isStaff ? 'Tickets' : 'My tickets'}</h1>
          <p className="muted">{result ? `${result.totalElements} ticket${result.totalElements === 1 ? '' : 's'}` : ' '}</p>
        </div>
        <Link className="btn primary" to="/tickets/new">New ticket</Link>
      </header>

      <div className="filters">
        <input className="grow" placeholder="Search by subject…" value={filters.q}
          onChange={(e) => change('q', e.target.value)} />
        <select value={filters.status} onChange={(e) => change('status', e.target.value)}>
          <option value="">All statuses</option>
          {STATUSES.map((s) => <option key={s} value={s}>{label(s)}</option>)}
        </select>
        <select value={filters.priority} onChange={(e) => change('priority', e.target.value)}>
          <option value="">All priorities</option>
          {PRIORITIES.map((p) => <option key={p} value={p}>{label(p)}</option>)}
        </select>
        {isStaff && (
          <label className="check">
            <input type="checkbox" checked={filters.mine} onChange={(e) => change('mine', e.target.checked)} />
            Assigned to me
          </label>
        )}
      </div>

      <Banner>{error}</Banner>

      <section className="card flush">
        {!result ? (
          <p className="muted pad">Loading tickets…</p>
        ) : result.content.length === 0 ? (
          <p className="muted pad">No tickets match these filters.</p>
        ) : (
          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>#</th><th>Subject</th><th>Status</th><th>Priority</th><th>Category</th>
                  {isStaff && <th>Requester</th>}
                  <th>Assignee</th><th>Due</th>
                </tr>
              </thead>
              <tbody>
                {result.content.map((t) => (
                  <tr key={t.id} onClick={() => navigate(`/tickets/${t.id}`)}>
                    <td className="mono">{t.id}</td>
                    <td className="subject">
                      <Link to={`/tickets/${t.id}`} onClick={(e) => e.stopPropagation()}>{t.subject}</Link>
                      {t.aiTriaged && <span className="chip-ai" title="Priority and category were set by AI">AI</span>}
                    </td>
                    <td><StatusBadge status={t.status} /></td>
                    <td><PriorityBadge priority={t.priority} /></td>
                    <td>{label(t.category)}</td>
                    {isStaff && <td>{t.requesterName}</td>}
                    <td>{t.assigneeName || <span className="muted">Unassigned</span>}</td>
                    <td className={t.slaBreached ? 'overdue' : ''}>
                      {t.slaBreached ? 'SLA missed' : ['RESOLVED', 'CLOSED'].includes(t.status) ? 'Done' : timeFromNow(t.dueAt)}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>

      {result && result.totalPages > 1 && (
        <div className="pager">
          <button className="btn small" disabled={page === 0} onClick={() => setPage(page - 1)}>Previous</button>
          <span>Page {page + 1} of {result.totalPages}</span>
          <button className="btn small" disabled={page + 1 >= result.totalPages} onClick={() => setPage(page + 1)}>Next</button>
        </div>
      )}
    </>
  )
}
