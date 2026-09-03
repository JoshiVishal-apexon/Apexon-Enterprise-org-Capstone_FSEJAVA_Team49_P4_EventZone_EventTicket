import React, { useCallback, useEffect, useState } from 'react'
import { apiFetch, type TicketCategory } from '../lib/api'
import { useAuth } from '../state/AuthContext'

type TicketForm = { name: string; price: string; totalSeats: string }

const emptyForm: TicketForm = { name: '', price: '', totalSeats: '' }

/**
 * Add / edit / delete the ticket categories of one event, and show how many
 * seats each has actually sold. `onChanged` lets the parent dashboard refresh
 * its own per-event totals after a write.
 */
export default function TicketCategoryManager({
  eventId,
  onChanged,
}: {
  eventId: string
  onChanged?: () => void | Promise<void>
}) {
  const { auth } = useAuth()
  const [tickets, setTickets] = useState<TicketCategory[]>([])
  const [form, setForm] = useState<TicketForm>(emptyForm)
  const [editingId, setEditingId] = useState<string | null>(null)
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  const reload = useCallback(async () => {
    try {
      setTickets(await apiFetch<TicketCategory[]>(`/api/events/${eventId}/ticket-categories`))
    } catch (err) {
      setError((err as Error).message)
    }
  }, [eventId])

  useEffect(() => { void reload() }, [reload])

  const resetForm = () => { setForm(emptyForm); setEditingId(null); setError('') }

  const startEdit = (t: TicketCategory) => {
    setEditingId(t.id)
    setError('')
    setForm({ name: t.name, price: String(t.price), totalSeats: String(t.totalSeats) })
  }

  const save = async (e: React.FormEvent) => {
    e.preventDefault()
    setError('')
    setBusy(true)
    try {
      const body = {
        name: form.name,
        price: Number(form.price),
        totalSeats: Number(form.totalSeats),
      }
      if (editingId) {
        await apiFetch(`/api/ticket-categories/${editingId}`, { method: 'PUT', body, token: auth.token })
      } else {
        await apiFetch(`/api/events/${eventId}/ticket-categories`, { method: 'POST', body, token: auth.token })
      }
      resetForm()
      await reload()
      await onChanged?.()
    } catch (err) {
      // e.g. 409 "Total seats cannot be less than the 4 seat(s) already booked"
      setError((err as Error).message)
    } finally {
      setBusy(false)
    }
  }

  const remove = async (t: TicketCategory) => {
    if (!window.confirm(`Delete ticket category "${t.name}"?`)) return
    setError('')
    try {
      await apiFetch(`/api/ticket-categories/${t.id}`, { method: 'DELETE', token: auth.token })
      if (editingId === t.id) resetForm()
      await reload()
      await onChanged?.()
    } catch (err) {
      setError((err as Error).message)
    }
  }

  return (
    <div style={{ marginTop: 12, borderTop: '1px solid #ddd', paddingTop: 12 }}>
      <strong>Ticket categories</strong>

      {error && <div style={{ color: 'crimson', margin: '8px 0' }}>{error}</div>}

      <div style={{ overflowX: 'auto' }}>
        <table>
          <thead>
            <tr>
              <th>Name</th><th>Price</th><th>Total</th><th>Available</th><th>Booked</th><th>Action</th>
            </tr>
          </thead>
          <tbody>
            {tickets.map(t => (
              <tr key={t.id}>
                <td>{t.name}</td>
                <td>Rs {t.price}</td>
                <td>{t.totalSeats}</td>
                <td>{t.availableSeats}</td>
                <td>{t.bookedQuantity}</td>
                <td style={{ whiteSpace: 'nowrap' }}>
                  <button onClick={() => startEdit(t)}>Edit</button>
                  <button onClick={() => remove(t)} style={{ marginLeft: 4 }}>Delete</button>
                </td>
              </tr>
            ))}
            {tickets.length === 0 && (
              <tr><td colSpan={6}>No ticket categories yet.</td></tr>
            )}
          </tbody>
        </table>
      </div>

      <form onSubmit={save} style={{ marginTop: 8, display: 'flex', gap: 6, flexWrap: 'wrap', alignItems: 'flex-end' }}>
        <label>Name<br />
          <input value={form.name} onChange={e => setForm({ ...form, name: e.target.value })} required />
        </label>
        <label>Price<br />
          <input type="number" min="0" step="0.01" value={form.price}
                 onChange={e => setForm({ ...form, price: e.target.value })} required />
        </label>
        <label>Total seats<br />
          <input type="number" min="1" value={form.totalSeats}
                 onChange={e => setForm({ ...form, totalSeats: e.target.value })} required />
        </label>
        <button type="submit" disabled={busy}>
          {busy ? 'Saving…' : editingId ? 'Save' : 'Add'}
        </button>
        {editingId && <button type="button" onClick={resetForm}>Cancel</button>}
      </form>
    </div>
  )
}
