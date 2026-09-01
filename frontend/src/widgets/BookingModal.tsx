import React, { useState } from 'react'
import { useAuth } from '../state/AuthContext'

export default function BookingModal({ ticket } : { ticket:any }){
  const [open,setOpen] = useState(false)
  const [qty,setQty] = useState(1)
  const { auth } = useAuth()
  const confirm = async ()=>{
    const API_BASE = (import.meta as any).env.VITE_API_BASE || ''
    const res = await fetch(API_BASE + '/api/bookings',{method:'POST',headers:{'Content-Type':'application/json','Authorization': auth.token ? 'Bearer '+auth.token : ''},body:JSON.stringify({ticketCategoryId: ticket.id, quantity: qty})})
    if (res.ok) { alert('Booked'); setOpen(false) } else { alert('Failed') }
  }
  return (
    <div>
      <button onClick={()=>setOpen(true)}>Book</button>
      {open && (
        <div className='card'>
          <div>Category: {ticket.name}</div>
          <div>Price: {ticket.price}</div>
          <div>Qty: <select value={qty} onChange={e=>setQty(Number(e.target.value))}>{[1,2,3,4,5].map(n=> <option key={n} value={n}>{n}</option>)}</select></div>
          <div><button onClick={confirm}>Confirm</button> <button onClick={()=>setOpen(false)}>Close</button></div>
        </div>
      )}
    </div>
  )
}
