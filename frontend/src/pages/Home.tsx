import React, { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'

export default function Home(){
  const [events, setEvents] = useState<any[]>([])
  const API_BASE = (import.meta as any).env.VITE_API_BASE || ''
  useEffect(()=>{ fetch(API_BASE + '/api/events').then(r=>r.json()).then(setEvents).catch(e=>console.error(e)) },[])
  return (
    <div>
      <div style={{display:'grid',gridTemplateColumns:'repeat(3,1fr)',gap:8}}>
        {events.map(e=> (
          <div key={e.id} className='card'>
            <img src={e.coverImageUrl} style={{width:'100%'}} />
            <h3><Link to={'/events/'+e.id}>{e.title}</Link></h3>
            <div>{e.venue} - {e.eventDate}</div>
          </div>
        ))}
      </div>
    </div>
  )
}
