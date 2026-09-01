import React, { createContext, useState, useContext } from 'react'

type AuthState = { token?: string, email?: string }
const AuthContext = createContext<any>(null)

export function AuthProvider({ children } : { children: React.ReactNode }){
  const [auth, setAuth] = useState<AuthState>({})
  const login = (token: string, email?: string) => setAuth({ token, email })
  const logout = () => setAuth({})
  return <AuthContext.Provider value={{ auth, login, logout }}>{children}</AuthContext.Provider>
}

export const useAuth = () => useContext(AuthContext)
