import { createContext, useContext, useEffect, useState } from 'react'
import { api, getToken, setToken } from './api'

const AuthContext = createContext(null)

// Keeps the logged-in user in one place so every page can read it with useAuth().
export function AuthProvider({ children }) {
  const [user, setUser] = useState(null)
  const [company, setCompany] = useState('')
  const [loading, setLoading] = useState(true)

  // On page refresh: if a token is saved, ask the backend who it belongs to.
  useEffect(() => {
    if (!getToken()) {
      setLoading(false)
      return
    }
    api.me()
      .then((res) => { setUser(res.user); setCompany(res.companyName) })
      .catch(() => setToken(null))
      .finally(() => setLoading(false))
  }, [])

  const accept = (res) => {
    setToken(res.token)
    setUser(res.user)
    setCompany(res.companyName)
    return res.user
  }

  const value = {
    user,
    company,
    loading,
    isStaff: user?.role === 'ADMIN' || user?.role === 'AGENT',
    isAdmin: user?.role === 'ADMIN',
    login: async (email, password) => accept(await api.login({ email, password })),
    registerOrg: async (form) => accept(await api.registerOrg(form)),
    logout: () => { setToken(null); setUser(null); setCompany('') },
  }

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export const useAuth = () => useContext(AuthContext)
