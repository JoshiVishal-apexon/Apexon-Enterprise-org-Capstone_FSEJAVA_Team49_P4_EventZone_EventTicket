import React, { useCallback, useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { apiFetch, type AppUser, type Category, type EventItem, type Role } from '../lib/api'
import { useAuth } from '../state/AuthContext'

export default function AdminPanel() {
  const { auth, isAdmin } = useAuth()

  const [categories, setCategories] = useState<Category[]>([])
  const [events, setEvents] = useState<EventItem[]>([])
  const [users, setUsers] = useState<AppUser[]>([])
  const [newName, setNewName] = useState('')
  const [editingId, setEditingId] = useState<string | null>(null)
  const [editingName, setEditingName] = useState('')
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  const loadCategories = useCallback(async () => {
    try {
      setCategories(await apiFetch<Category[]>('/api/categories'))
    } catch (err) {
      setError((err as Error).message)
    }
  }, [])

  const loadEvents = useCallback(async () => {
    if (!auth.token) return
    try {
      // /all includes deactivated events, which the public list hides.
      setEvents(await apiFetch<EventItem[]>('/api/events/all', { token: auth.token }))
    } catch (err) {
      setError((err as Error).message)
    }
  }, [auth.token])

  const loadUsers = useCallback(async () => {
    if (!auth.token) return
    try {
      setUsers(await apiFetch<AppUser[]>('/api/users', { token: auth.token }))
    } catch (err) {
      setError((err as Error).message)
    }
  }, [auth.token])

  useEffect(() => { void loadCategories() }, [loadCategories])
  useEffect(() => { void loadEvents() }, [loadEvents])
  useEffect(() => { void loadUsers() }, [loadUsers])

  const create = async (e: React.FormEvent) => {
    e.preventDefault()
    setError('')
    setBusy(true)
    try {
      await apiFetch('/api/categories', { method: 'POST', body: { name: newName }, token: auth.token })
      setNewName('')
      await loadCategories()
    } catch (err) {
      setError((err as Error).message)
    } finally {
      setBusy(false)
    }
  }

  const rename = async (id: string) => {
    setError('')
    try {
      await apiFetch(`/api/categories/${id}`, {
        method: 'PUT', body: { name: editingName }, token: auth.token,
      })
      setEditingId(null)
      setEditingName('')
      await Promise.all([loadCategories(), loadEvents()])
    } catch (err) {
      setError((err as Error).message)
    }
  }

  const removeCategory = async (c: Category) => {
    if (!window.confirm(`Delete category "${c.name}"?`)) return
    setError('')
    try {
      await apiFetch(`/api/categories/${c.id}`, { method: 'DELETE', token: auth.token })
      await loadCategories()
    } catch (err) {
      // 409 when events still reference it -- show the backend's reason.
      setError((err as Error).message)
    }
  }

  const toggleActive = async (ev: EventItem) => {
    setError('')
    try {
      await apiFetch(`/api/events/${ev.id}/active`, {
        method: 'PUT', body: { active: !ev.active }, token: auth.token,
      })
      await loadEvents()
    } catch (err) {
      setError((err as Error).message)
    }
  }

  const changeRole = async (u: AppUser, role: Role) => {
    setError('')
    try {
      await apiFetch(`/api/users/${u.id}/role`, { method: 'PUT', body: { role }, token: auth.token })
      await loadUsers()
    } catch (err) {
      setError((err as Error).message)
    }
  }

  if (!auth.token) {
    return (
      <div style={{ padding: 16 }}>
        <h1>Admin Panel</h1>
        <div className="card">Please <Link to="/login">log in</Link> as an admin.</div>
      </div>
    )
  }

  if (!isAdmin) {
    return (
      <div style={{ padding: 16 }}>
        <h1>Admin Panel</h1>
        <div className="card">
          This area is for admins. You are signed in as <strong>{auth.role}</strong>.
        </div>
      </div>
    )
  }

  return (
    <div style={{ padding: 16 }}>
      <h1>Admin Panel</h1>
      <p>Signed in as {auth.name} ({auth.role})</p>

      {error && <div className="card" style={{ color: 'crimson' }}>{error}</div>}

      <h2>Event categories ({categories.length})</h2>
      <div className="card" style={{ maxWidth: 560 }}>
        <div style={{ overflowX: 'auto' }}>
          <table>
            <thead><tr><th>Name</th><th>Action</th></tr></thead>
            <tbody>
              {categories.map(c => (
                <tr key={c.id}>
                  <td>
                    {editingId === c.id
                      ? <input value={editingName} onChange={e => setEditingName(e.target.value)} />
                      : c.name}
                  </td>
                  <td style={{ whiteSpace: 'nowrap' }}>
                    {editingId === c.id ? (
                      <>
                        <button onClick={() => rename(c.id)}>Save</button>
                        <button onClick={() => { setEditingId(null); setEditingName('') }} style={{ marginLeft: 4 }}>
                          Cancel
                        </button>
                      </>
                    ) : (
                      <>
                        <button onClick={() => { setEditingId(c.id); setEditingName(c.name); setError('') }}>
                          Rename
                        </button>
                        <button onClick={() => removeCategory(c)} style={{ marginLeft: 4 }}>Delete</button>
                      </>
                    )}
                  </td>
                </tr>
              ))}
              {categories.length === 0 && <tr><td colSpan={2}>No categories yet.</td></tr>}
            </tbody>
          </table>
        </div>

        <form onSubmit={create} style={{ marginTop: 8, display: 'flex', gap: 6, alignItems: 'flex-end' }}>
          <label>New category<br />
            <input value={newName} onChange={e => setNewName(e.target.value)} required />
          </label>
          <button type="submit" disabled={busy}>{busy ? 'Adding…' : 'Add'}</button>
        </form>
      </div>

      <h2>Users ({users.length})</h2>
      <p style={{ fontSize: 13 }}>
        Anyone can register as an attendee or organiser. Use this table to promote or
        demote an account. You cannot remove your own admin role.
      </p>
      <div className="card">
        <div style={{ overflowX: 'auto' }}>
          <table>
            <thead><tr><th>Name</th><th>Email</th><th>Role</th><th>Change role</th></tr></thead>
            <tbody>
              {users.map(u => (
                <tr key={u.id}>
                  <td>{u.name}</td>
                  <td>{u.email}</td>
                  <td><strong>{u.role}</strong></td>
                  <td>
                    <select
                      value={u.role}
                      onChange={e => changeRole(u, e.target.value as Role)}
                      disabled={u.email === auth.email}
                      title={u.email === auth.email ? 'You cannot change your own role' : undefined}
                    >
                      <option value="ATTENDEE">ATTENDEE</option>
                      <option value="ORGANISER">ORGANISER</option>
                      <option value="ADMIN">ADMIN</option>
                    </select>
                  </td>
                </tr>
              ))}
              {users.length === 0 && <tr><td colSpan={4}>No users yet.</td></tr>}
            </tbody>
          </table>
        </div>
      </div>

      <h2>Events ({events.length})</h2>
      <p style={{ fontSize: 13 }}>
        Deactivated events stay in the database and keep their bookings, but are hidden
        from the public event list.
      </p>
      <div className="card">
        <div style={{ overflowX: 'auto' }}>
          <table>
            <thead>
              <tr><th>Title</th><th>Organiser</th><th>Category</th><th>Status</th><th>Action</th></tr>
            </thead>
            <tbody>
              {events.map(ev => (
                <tr key={ev.id}>
                  <td><Link to={'/events/' + ev.id}>{ev.title}</Link></td>
                  <td>{ev.organiserName}</td>
                  <td>{ev.categoryName}</td>
                  <td style={{ color: ev.active ? 'green' : 'crimson' }}>
                    {ev.active ? 'Active' : 'Inactive'}
                  </td>
                  <td>
                    <button onClick={() => toggleActive(ev)}>
                      {ev.active ? 'Deactivate' : 'Activate'}
                    </button>
                  </td>
                </tr>
              ))}
              {events.length === 0 && <tr><td colSpan={5}>No events yet.</td></tr>}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  )
}
