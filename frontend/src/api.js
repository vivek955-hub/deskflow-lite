// Every call to the backend goes through this file.
const BASE = '/api'
const TOKEN_KEY = 'deskflow_token'

export const getToken = () => localStorage.getItem(TOKEN_KEY)
export const setToken = (token) =>
  token ? localStorage.setItem(TOKEN_KEY, token) : localStorage.removeItem(TOKEN_KEY)

async function request(method, path, body) {
  const headers = { 'Content-Type': 'application/json' }
  const token = getToken()
  if (token) headers.Authorization = `Bearer ${token}`

  let res
  try {
    res = await fetch(BASE + path, { method, headers, body: body ? JSON.stringify(body) : undefined })
  } catch {
    throw new Error('Cannot reach the server. Is the backend running on port 8080?')
  }

  if (res.status === 401 && token) {
    // The token expired: forget it and go back to the login page.
    setToken(null)
    window.location.assign('/login')
    throw new Error('Your session expired. Please log in again.')
  }

  const data = await res.json().catch(() => null)
  if (!res.ok) {
    const error = new Error(data?.message || `Request failed (HTTP ${res.status})`)
    error.fields = data?.fields || {}
    throw error
  }
  return data
}

// Builds "?status=OPEN&page=0" and skips empty values.
function query(params) {
  const search = new URLSearchParams()
  Object.entries(params).forEach(([key, value]) => {
    if (value !== '' && value !== null && value !== undefined && value !== false) search.set(key, value)
  })
  const text = search.toString()
  return text ? `?${text}` : ''
}

export const api = {
  login: (body) => request('POST', '/auth/login', body),
  registerOrg: (body) => request('POST', '/auth/register-org', body),
  me: () => request('GET', '/auth/me'),

  tickets: (params) => request('GET', '/tickets' + query(params)),
  ticket: (id) => request('GET', `/tickets/${id}`),
  createTicket: (body) => request('POST', '/tickets', body),
  updateTicket: (id, body) => request('PATCH', `/tickets/${id}`, body),
  addComment: (id, body) => request('POST', `/tickets/${id}/comments`, body),
  similarTickets: (id) => request('GET', `/tickets/${id}/similar`),

  aiTriage: (body) => request('POST', '/ai/triage', body),
  aiReply: (id) => request('POST', `/tickets/${id}/ai/reply`),
  aiSummary: (id) => request('POST', `/tickets/${id}/ai/summary`),

  dashboard: () => request('GET', '/dashboard'),

  users: () => request('GET', '/users'),
  assignableUsers: () => request('GET', '/users/assignable'),
  createUser: (body) => request('POST', '/users', body),
  setUserActive: (id, active) => request('PATCH', `/users/${id}/active`, { active }),
}
