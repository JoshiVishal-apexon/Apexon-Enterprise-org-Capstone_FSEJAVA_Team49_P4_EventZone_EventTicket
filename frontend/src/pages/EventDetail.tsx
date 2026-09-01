import React, { useEffect, useState } from 'react'
import { useParams } from 'react-router-dom'
import BookingModal from '../widgets/BookingModal'

export default function EventDetail(){
  const { id } = useParams()
  const [event, setEvent] = useState<any>(null)
  const API_BASE = (import.meta as any).env.VITE_API_BASE || ''
  useEffect(()=>{ if (id) fetch(API_BASE + '/api/events/'+id).then(r=>r.json()).then(setEvent).catch(e=>console.error(e)) },[id])
  if (!event) return <div>Loading...</div>
  return (
    <div>
      <h1>{event.title}</h1>
      <img src={event.coverImageUrl} style={{width:400}} />
      <p>{event.description}</p>
      <h3>Tickets</h3>
      {event.ticketCategories?.map((tc:any)=> (
        <div key={tc.id} className='card' style={{marginBottom:8}}>
          <div>{tc.name} - Rs {tc.price} - Available: {tc.availableSeats}</div>
          <BookingModal ticket={tc} />
        </div>
      ))}
    </div>
  )
}
