import React, { useCallback, useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { apiFetch } from '../lib/api'
import { useAuth } from '../state/AuthContext'

type Booking = {
  id: string
  quantity: number
  status: string
  bookingRef: string
  ticketCategory?: { id: string; name: string; price: number; eventTitle?: string }
}

export default function MyBookings() {
  const { auth } = useAuth()
  const [bookings, setBookings] = useState<Booking[]>([])
  const [error, setError] = useState('')
  const [cancellingId, setCancellingId] = useState<string | null>(null)

  const reload = useCallback(async () => {
    if (!auth.token) return
    try {
      setBookings(await apiFetch<Booking[]>('/api/bookings/mine', { token: auth.token }))
    } catch (err) {
      setError((err as Error).message)
    }
  }, [auth.token])

  useEffect(() => { void reload() }, [reload])

  const cancel = async (b: Booking) => {
    if (!window.confirm(`Cancel booking ${b.bookingRef}? The seats will be released.`)) return
    setError('')
    setCancellingId(b.id)
    try {
      await apiFetch(`/api/bookings/${b.id}/cancel`, { method: 'PUT', token: auth.token })
      await reload()
    } catch (err) {
      setError((err as Error).message)
    } finally {
      setCancellingId(null)
    }
  }

  if (!auth.token) {
    return (
      <div style={{ padding: 16 }}>
        <h1>My Bookings</h1>
        <div className="card">Please <Link to="/login">log in</Link> to see your bookings.</div>
      </div>
    )
  }

  return (
    <div style={{ padding: 16 }}>
      <h1>My Bookings</h1>

      {error && <div className="card" style={{ color: 'crimson' }}>{error}</div>}

      <div style={{ overflowX: 'auto' }}>
        <table>
          <thead>
            <tr><th>Event</th><th>Category</th><th>Qty</th><th>Ref</th><th>Status</th><th>Action</th></tr>
          </thead>
          <tbody>
            {bookings.map(b => (
              <tr key={b.id}>
                <td>{b.ticketCategory?.eventTitle}</td>
                <td>{b.ticketCategory?.name}</td>
                <td>{b.quantity}</td>
                <td>{b.bookingRef}</td>
                <td>{b.status}</td>
                <td>
                  {b.status === 'CONFIRMED' && (
                    <button onClick={() => cancel(b)} disabled={cancellingId === b.id}>
                      {cancellingId === b.id ? 'Cancelling…' : 'Cancel'}
                    </button>
                  )}
                </td>
              </tr>
            ))}
            {bookings.length === 0 && <tr><td colSpan={6}>No bookings yet.</td></tr>}
          </tbody>
        </table>
      </div>
    </div>
  )
}
