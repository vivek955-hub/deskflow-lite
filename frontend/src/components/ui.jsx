// Small shared pieces used on several pages.

// "FEATURE_REQUEST" -> "Feature request"
export const label = (value) =>
  value ? value.charAt(0) + value.slice(1).toLowerCase().replaceAll('_', ' ') : '—'

export const formatDate = (iso) =>
  iso ? new Date(iso).toLocaleString('en-IN', { day: 'numeric', month: 'short', hour: '2-digit', minute: '2-digit' }) : '—'

// "3 hours ago", "in 2 days"
export function timeFromNow(iso) {
  if (!iso) return '—'
  const minutes = Math.round((new Date(iso).getTime() - Date.now()) / 60000)
  const abs = Math.abs(minutes)
  let text
  if (abs < 1) return 'just now'
  if (abs < 60) text = `${abs} min`
  else if (abs < 60 * 24) text = `${Math.round(abs / 60)} h`
  else text = `${Math.round(abs / 60 / 24)} d`
  return minutes < 0 ? `${text} ago` : `in ${text}`
}

export const StatusBadge = ({ status }) => (
  <span className={`badge status-${status.toLowerCase()}`}>{label(status)}</span>
)

export const PriorityBadge = ({ priority }) => (
  <span className={`badge priority-${priority.toLowerCase()}`}>{label(priority)}</span>
)

export const AiChip = ({ source }) => (
  <span className="chip-ai" title={source === 'LLM' ? 'Answered by the AI model' : 'Answered by the built-in rule engine'}>
    {source === 'LLM' ? 'AI model' : 'AI rules'}
  </span>
)

export const Banner = ({ kind = 'error', children }) =>
  children ? <div className={`banner ${kind}`} role={kind === 'error' ? 'alert' : 'status'}>{children}</div> : null

export const STATUSES = ['OPEN', 'IN_PROGRESS', 'PENDING', 'RESOLVED', 'CLOSED']
export const PRIORITIES = ['LOW', 'MEDIUM', 'HIGH', 'URGENT']
export const CATEGORIES = ['BILLING', 'TECHNICAL', 'ACCOUNT', 'FEATURE_REQUEST', 'GENERAL']
