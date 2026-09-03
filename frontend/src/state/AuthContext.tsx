import React, { createContext, useState, useContext, useCallback } from 'react'

export type AuthState = {
  token?: string
  email?: string
  name?: string
  role?: 'ATTENDEE' | 'ORGANISER' | 'ADMIN'
}

/** Matches the backend AuthResponse record. */
export type AuthResponse = {
  token: string
  name: string
  role: AuthState['role']
  email: string
}

type AuthContextValue = {
  auth: AuthState
  login: (response: AuthResponse) => void
  logout: () => void
  isOrganiser: boolean
  isAdmin: boolean
}

const STORAGE_KEY = 'eventzone.auth'

// Persisted so a page refresh does not silently log the user out. This keeps the
// JWT in localStorage, which is readable by any script on the page -- acceptable
// for local development; a production build should move to an httpOnly cookie.
function loadStoredAuth(): AuthState {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    return raw ? (JSON.parse(raw) as AuthState) : {}
  } catch {
    return {}
  }
}

function storeAuth(state: AuthState) {
  try {
    if (state.token) localStorage.setItem(STORAGE_KEY, JSON.stringify(state))
    else localStorage.removeItem(STORAGE_KEY)
  } catch {
    // Private-mode / blocked storage: stay in memory only.
  }
}

const AuthContext = createContext<AuthContextValue | null>(null)

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [auth, setAuth] = useState<AuthState>(loadStoredAuth)

  const login = useCallback((response: AuthResponse) => {
    const next: AuthState = {
      token: response.token,
      email: response.email,
      name: response.name,
      role: response.role,
    }
    setAuth(next)
    storeAuth(next)
  }, [])

  const logout = useCallback(() => {
    setAuth({})
    storeAuth({})
  }, [])

  const value: AuthContextValue = {
    auth,
    login,
    logout,
    isOrganiser: auth.role === 'ORGANISER' || auth.role === 'ADMIN',
    isAdmin: auth.role === 'ADMIN',
  }

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export const useAuth = (): AuthContextValue => {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth must be used inside an AuthProvider')
  return ctx
}
