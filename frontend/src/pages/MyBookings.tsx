import React, { useEffect, useState } from 'react'
import { useAuth } from '../state/AuthContext'

export default function MyBookings(){
  const { auth } = useAuth()
  const [bookings, setBookings] = useState<any[]>([])
  const API_BASE = (import.meta as any).env.VITE_API_BASE || ''
  useEffect(()=>{ if (auth.token) fetch(API_BASE + '/api/bookings/mine',{headers:{'Authorization':'Bearer '+auth.token}}).then(r=>r.json()).then(setBookings).catch(e=>console.error(e)) },[auth.token])
  return (
    <div>
      <h1>My Bookings</h1>
      <table>
        <thead><tr><th>Event</th><th>Category</th><th>Qty</th><th>Ref</th><th>Status</th><th>Action</th></tr></thead>
        <tbody>
          {bookings.map(b=> (
            <tr key={b.id}>
              <td>{b.ticketCategory?.event?.title}</td>
              <td>{b.ticketCategory?.name}</td>
              <td>{b.quantity}</td>
              <td>{b.bookingRef}</td>
              <td>{b.status}</td>
              <td>{b.status==='CONFIRMED' && <button onClick={()=>{/* cancel */}}>Cancel</button>}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}
