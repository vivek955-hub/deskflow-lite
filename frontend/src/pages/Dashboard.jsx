import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { api } from '../api'
import { Banner, label } from '../components/ui'

// Horizontal bars drawn with plain CSS: no chart library needed.
function BarList({ title, data, tone }) {
  const max = Math.max(1, ...Object.values(data))
  return (
    <section className="card">
      <h2>{title}</h2>
      <div className="bars">
        {Object.entries(data).map(([name, count]) => (
          <div className="bar-row" key={name}>
            <span className="bar-label">{label(name)}</span>
            <div className="bar-track">
              <div className={`bar-fill ${tone}`} style={{ width: `${(count / max) * 100}%` }} />
            </div>
            <span className="bar-value">{count}</span>
          </div>
        ))}
      </div>
    </section>
  )
}

export default function Dashboard() {
  const [data, setData] = useState(null)
  const [error, setError] = useState('')

  useEffect(() => {
    api.dashboard().then(setData).catch((e) => setError(e.message))
  }, [])

  if (error) return <Banner>{error}</Banner>
  if (!data) return <p className="muted">Loading dashboard…</p>

  const maxDay = Math.max(1, ...data.last7Days.map((d) => d.count))
  const aiShare = data.total ? Math.round((data.aiTriaged / data.total) * 100) : 0

  return (
    <>
      <header className="page-head">
        <div>
          <h1>Dashboard</h1>
          <p className="muted">How your helpdesk is doing right now.</p>
        </div>
        <Link className="btn primary" to="/tickets/new">New ticket</Link>
      </header>

      <div className="stats">
        <div className="stat"><span>Active tickets</span><strong>{data.active}</strong><small>of {data.total} total</small></div>
        <div className="stat"><span>Unassigned</span><strong>{data.unassigned}</strong><small>waiting for an owner</small></div>
        <div className={`stat ${data.slaBreached ? 'warn' : ''}`}><span>SLA missed</span><strong>{data.slaBreached}</strong><small>past their deadline</small></div>
        <div className="stat"><span>Avg. resolution</span><strong>{data.avgResolutionHours} h</strong><small>for resolved tickets</small></div>
        <div className="stat"><span>Triaged by AI</span><strong>{aiShare}%</strong><small>{data.aiTriaged} tickets</small></div>
      </div>

      <div className="grid-2">
        <section className="card">
          <h2>New tickets, last 7 days</h2>
          <div className="columns">
            {data.last7Days.map((d) => (
              <div className="column" key={d.day}>
                <span className="column-value">{d.count}</span>
                <div className="column-bar" style={{ height: `${(d.count / maxDay) * 100}%` }} />
                <span className="column-label">
                  {new Date(d.day + 'T00:00:00').toLocaleDateString('en-IN', { weekday: 'short' })}
                </span>
              </div>
            ))}
          </div>
        </section>

        <section className="card">
          <h2>Active tickets per agent</h2>
          {data.agentLoad.length === 0 ? (
            <p className="muted">No tickets are assigned yet.</p>
          ) : (
            <div className="bars">
              {data.agentLoad.map((a) => (
                <div className="bar-row" key={a.agent}>
                  <span className="bar-label">{a.agent}</span>
                  <div className="bar-track">
                    <div className="bar-fill teal"
                      style={{ width: `${(a.activeTickets / Math.max(...data.agentLoad.map((x) => x.activeTickets))) * 100}%` }} />
                  </div>
                  <span className="bar-value">{a.activeTickets}</span>
                </div>
              ))}
            </div>
          )}
        </section>

        <BarList title="By status" data={data.byStatus} tone="indigo" />
        <BarList title="By priority" data={data.byPriority} tone="amber" />
        <BarList title="By category" data={data.byCategory} tone="teal" />
      </div>
    </>
  )
}
