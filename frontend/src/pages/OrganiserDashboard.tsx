import React, { useCallback, useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { apiFetch, type Category, type EventItem } from '../lib/api'
import { useAuth } from '../state/AuthContext'
import TicketCategoryManager from '../widgets/TicketCategoryManager'

type EventForm = {
  title: string
  description: string
  eventDate: string
  venue: string
  coverImageUrl: string
  categoryId: string
}

const emptyForm: EventForm = {
  title: '', description: '', eventDate: '', venue: '', coverImageUrl: '', categoryId: '',
}

export default function OrganiserDashboard() {
  const { auth, isOrganiser } = useAuth()
  const [events, setEvents] = useState<EventItem[]>([])
  const [categories, setCategories] = useState<Category[]>([])
  const [form, setForm] = useState<EventForm>(emptyForm)
  const [editingId, setEditingId] = useState<string | null>(null)
  const [expandedId, setExpandedId] = useState<string | null>(null)
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  const reload = useCallback(async () => {
    if (!auth.token) return
    try {
      const mine = await apiFetch<EventItem[]>('/api/events/mine', { token: auth.token })
      setEvents(mine)
    } catch (err) {
      setError((err as Error).message)
    }
  }, [auth.token])

  useEffect(() => { void reload() }, [reload])

  useEffect(() => {
    apiFetch<Category[]>('/api/categories')
      .then(setCategories)
      .catch(err => setError((err as Error).message))
  }, [])

  const set = (field: keyof EventForm) => (
    e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement | HTMLSelectElement>,
  ) => setForm({ ...form, [field]: e.target.value })

  const resetForm = () => { setForm(emptyForm); setEditingId(null); setError('') }

  const startEdit = (ev: EventItem) => {
    setEditingId(ev.id)
    setError('')
    setForm({
      title: ev.title,
      description: ev.description,
      eventDate: ev.eventDate?.slice(0, 16) ?? '', // trim seconds for datetime-local
      venue: ev.venue,
      coverImageUrl: ev.coverImageUrl ?? '',
      categoryId: ev.categoryId ?? '',
    })
  }

  const save = async (e: React.FormEvent) => {
    e.preventDefault()
    setError('')
    setBusy(true)
    try {
      const body = {
        title: form.title,
        description: form.description,
        // datetime-local yields "yyyy-MM-ddTHH:mm"; the API wants LocalDateTime.
        eventDate: form.eventDate.length === 16 ? form.eventDate + ':00' : form.eventDate,
        venue: form.venue,
        coverImageUrl: form.coverImageUrl || null,
        categoryId: form.categoryId,
      }
      if (editingId) {
        await apiFetch(`/api/events/${editingId}`, { method: 'PUT', body, token: auth.token })
      } else {
        await apiFetch('/api/events', { method: 'POST', body, token: auth.token })
      }
      resetForm()
      await reload()
    } catch (err) {
      setError((err as Error).message)
    } finally {
      setBusy(false)
    }
  }

  const remove = async (ev: EventItem) => {
    if (!window.confirm(`Delete "${ev.title}"?`)) return
    setError('')
    try {
      await apiFetch(`/api/events/${ev.id}`, { method: 'DELETE', token: auth.token })
      if (editingId === ev.id) resetForm()
      await reload()
    } catch (err) {
      // e.g. 409 when bookings exist -- show the backend's reason.
      setError((err as Error).message)
    }
  }

  if (!auth.token) {
    return (
      <div style={{ padding: 16 }}>
        <h1>Organiser Dashboard</h1>
        <div className="card">Please <Link to="/login">log in</Link> to manage your events.</div>
      </div>
    )
  }

  if (!isOrganiser) {
    return (
      <div style={{ padding: 16 }}>
        <h1>Organiser Dashboard</h1>
        <div className="card">
          This area is for organisers. You are signed in as <strong>{auth.role}</strong>.
        </div>
      </div>
    )
  }

  return (
    <div style={{ padding: 16 }}>
      <h1>Organiser Dashboard</h1>
      <p>Signed in as {auth.name} ({auth.role})</p>

      {error && (
        <div className="card" style={{ color: 'crimson' }}>{error}</div>
      )}

      <h2>{editingId ? 'Edit event' : 'Create event'}</h2>
      <form className="card" onSubmit={save} style={{ maxWidth: 560 }}>
        <div style={{ marginBottom: 8 }}>
          <label>Title<br />
            <input value={form.title} onChange={set('title')} required style={{ width: '100%' }} />
          </label>
        </div>
        <div style={{ marginBottom: 8 }}>
          <label>Description<br />
            <textarea value={form.description} onChange={set('description')} required rows={3} style={{ width: '100%' }} />
          </label>
        </div>
        <div style={{ marginBottom: 8 }}>
          <label>Date &amp; time<br />
            <input type="datetime-local" value={form.eventDate} onChange={set('eventDate')} required />
          </label>
        </div>
        <div style={{ marginBottom: 8 }}>
          <label>Venue<br />
            <input value={form.venue} onChange={set('venue')} required style={{ width: '100%' }} />
          </label>
        </div>
        <div style={{ marginBottom: 8 }}>
          <label>Cover image URL<br />
            <input value={form.coverImageUrl} onChange={set('coverImageUrl')} style={{ width: '100%' }} />
          </label>
        </div>
        <div style={{ marginBottom: 8 }}>
          <label>Category<br />
            <select value={form.categoryId} onChange={set('categoryId')} required>
              <option value="">Select a category…</option>
              {categories.map(c => <option key={c.id} value={c.id}>{c.name}</option>)}
            </select>
          </label>
        </div>
        <button type="submit" disabled={busy}>
          {busy ? 'Saving…' : editingId ? 'Save changes' : 'Create event'}
        </button>
        {editingId && (
          <button type="button" onClick={resetForm} style={{ marginLeft: 8 }}>Cancel</button>
        )}
      </form>

      <h2>My events ({events.length})</h2>
      {events.length === 0 && <div className="card">No events yet. Create one above.</div>}

      {events.map(ev => (
        <div key={ev.id} className="card" style={{ marginBottom: 8 }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', gap: 8, flexWrap: 'wrap' }}>
            <div>
              <strong><Link to={'/events/' + ev.id}>{ev.title}</Link></strong>
              <div style={{ fontSize: 13 }}>
                {ev.venue} — {ev.eventDate} — {ev.categoryName}
              </div>
              <div style={{ fontSize: 13 }}>
                {ev.ticketCategories.length} ticket category(ies),{' '}
                {ev.ticketCategories.reduce((sum, t) => sum + t.bookedQuantity, 0)} seat(s) booked
              </div>
            </div>
            <div style={{ whiteSpace: 'nowrap' }}>
              <button onClick={() => setExpandedId(expandedId === ev.id ? null : ev.id)}>
                {expandedId === ev.id ? 'Hide tickets' : 'Tickets'}
              </button>
              <button onClick={() => startEdit(ev)} style={{ marginLeft: 4 }}>Edit</button>
              <button onClick={() => remove(ev)} style={{ marginLeft: 4 }}>Delete</button>
            </div>
          </div>

          {expandedId === ev.id && (
            <TicketCategoryManager eventId={ev.id} onChanged={reload} />
          )}
        </div>
      ))}
    </div>
  )
}
