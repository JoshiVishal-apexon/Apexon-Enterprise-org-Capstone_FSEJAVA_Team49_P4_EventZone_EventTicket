import React, { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { apiFetch } from '../lib/api'
import { useAuth, type AuthResponse } from '../state/AuthContext'

export default function Login() {
  const { auth, login, logout } = useAuth()
  const navigate = useNavigate()

  const [mode, setMode] = useState<'login' | 'register'>('login')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [name, setName] = useState('')
  const [role, setRole] = useState<'ATTENDEE' | 'ORGANISER'>('ATTENDEE')
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  const submit = async (e: React.FormEvent) => {
    e.preventDefault()
    setError('')
    setBusy(true)
    try {
      if (mode === 'register') {
        // The API accepts ATTENDEE or ORGANISER (never ADMIN); we log in straight
        // after so the returned role drives where we land.
        await apiFetch('/api/auth/register', {
          method: 'POST',
          body: { email, password, name, role },
        })
      }
      const res = await apiFetch<AuthResponse>('/api/auth/login', {
        method: 'POST',
        body: { email, password },
      })
      login(res)
      navigate(res.role === 'ORGANISER' || res.role === 'ADMIN' ? '/organiser' : '/bookings')
    } catch (err) {
      setError((err as Error).message)
    } finally {
      setBusy(false)
    }
  }

  if (auth.token) {
    return (
      <div style={{ padding: 16 }}>
        <h1>Account</h1>
        <div className="card">
          <div>Signed in as <strong>{auth.name}</strong> ({auth.email})</div>
          <div>Role: <strong>{auth.role}</strong></div>
          <button style={{ marginTop: 8 }} onClick={logout}>Log out</button>
        </div>
      </div>
    )
  }

  return (
    <div style={{ padding: 16, maxWidth: 420 }}>
      <h1>{mode === 'login' ? 'Log in' : 'Register'}</h1>
      <form className="card" onSubmit={submit}>
        {mode === 'register' && (
          <>
            <div style={{ marginBottom: 8 }}>
              <label>Name<br />
                <input value={name} onChange={e => setName(e.target.value)} required style={{ width: '100%' }} />
              </label>
            </div>
            <div style={{ marginBottom: 8 }}>
              <label>I am registering as<br />
                <select
                  value={role}
                  onChange={e => setRole(e.target.value as 'ATTENDEE' | 'ORGANISER')}
                  style={{ width: '100%' }}
                >
                  <option value="ATTENDEE">Attendee &mdash; browse and book tickets</option>
                  <option value="ORGANISER">Organiser &mdash; create and manage events</option>
                </select>
              </label>
            </div>
          </>
        )}
        <div style={{ marginBottom: 8 }}>
          <label>Email<br />
            <input type="email" value={email} onChange={e => setEmail(e.target.value)} required style={{ width: '100%' }} />
          </label>
        </div>
        <div style={{ marginBottom: 8 }}>
          <label>Password<br />
            <input type="password" value={password} onChange={e => setPassword(e.target.value)} required style={{ width: '100%' }} />
          </label>
        </div>

        {error && <div style={{ color: 'crimson', marginBottom: 8 }}>{error}</div>}

        <button type="submit" disabled={busy}>
          {busy ? 'Please wait…' : mode === 'login' ? 'Log in' : 'Register & log in'}
        </button>
      </form>

      <p>
        <button
          onClick={() => { setMode(mode === 'login' ? 'register' : 'login'); setError('') }}
          style={{ background: 'none', border: 'none', textDecoration: 'underline', cursor: 'pointer', padding: 0 }}
        >
          {mode === 'login' ? 'Need an account? Register' : 'Already have an account? Log in'}
        </button>
      </p>

      <div className="card" style={{ fontSize: 13 }}>
        <div><strong>Seeded accounts</strong> (password <code>Password123!</code>)</div>
        <div>org1@eventzone.com — organiser</div>
        <div>user1@eventzone.com — attendee</div>
        <div>admin@eventzone.com — admin</div>
      </div>
    </div>
  )
}
