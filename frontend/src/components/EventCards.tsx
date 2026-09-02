import React, { useState } from 'react'
import './EventCards.css'

interface EventItem {
  id: number
  image: string
  title: string
  date: string
  location: string
  price: string
}

const events: EventItem[] = [
  {
    id: 1,
    image: 'https://images.unsplash.com/photo-1470229722913-7c0e2dbbafd3?auto=format&fit=crop&w=600&q=80',
    title: 'Summer Music Fest',
    date: 'Sep 12, 2026',
    location: 'Chennai, IN',
    price: '₹999',
  },
  {
    id: 2,
    image: 'https://images.unsplash.com/photo-1540575467063-178a50c2df87?auto=format&fit=crop&w=600&q=80',
    title: 'Tech Innovators Summit',
    date: 'Sep 18, 2026',
    location: 'Bengaluru, IN',
    price: '₹1499',
  },
  {
    id: 3,
    image: 'https://images.unsplash.com/photo-1511578314322-379afb476865?auto=format&fit=crop&w=600&q=80',
    title: 'Startup Pitch Night',
    date: 'Sep 22, 2026',
    location: 'Mumbai, IN',
    price: '₹499',
  },
  {
    id: 4,
    image: 'https://images.unsplash.com/photo-1493225457124-a3eb161ffa5f?auto=format&fit=crop&w=600&q=80',
    title: 'Food & Wine Carnival',
    date: 'Sep 25, 2026',
    location: 'Goa, IN',
    price: '₹799',
  },
  {
    id: 5,
    image: 'https://images.unsplash.com/photo-1523580494863-6f3031224c94?auto=format&fit=crop&w=600&q=80',
    title: 'Marathon Championship',
    date: 'Oct 02, 2026',
    location: 'Delhi, IN',
    price: 'Free',
  },
  {
    id: 6,
    image: 'https://images.unsplash.com/photo-1517457373958-b7bdd4587205?auto=format&fit=crop&w=600&q=80',
    title: 'Comedy Night Live',
    date: 'Oct 05, 2026',
    location: 'Hyderabad, IN',
    price: '₹599',
  },
  {
    id: 7,
    image: 'https://images.unsplash.com/photo-1501281668745-f7f57925c3b4?auto=format&fit=crop&w=600&q=80',
    title: 'Art & Craft Expo',
    date: 'Oct 09, 2026',
    location: 'Pune, IN',
    price: '₹299',
  },
  {
    id: 8,
    image: 'https://images.unsplash.com/photo-1516450360452-9312f5e86fc7?auto=format&fit=crop&w=600&q=80',
    title: 'Gaming Convention',
    date: 'Oct 14, 2026',
    location: 'Chennai, IN',
    price: '₹899',
  },
  {
    id: 9,
    image: 'https://images.unsplash.com/photo-1475721027785-f74eccf877e2?auto=format&fit=crop&w=600&q=80',
    title: 'Business Leadership Meet',
    date: 'Oct 20, 2026',
    location: 'Bengaluru, IN',
    price: '₹1999',
  },
  {
    id: 10,
    image: 'https://images.unsplash.com/photo-1524368535928-5b5e00ddc76b?auto=format&fit=crop&w=600&q=80',
    title: 'Photography Workshop',
    date: 'Oct 27, 2026',
    location: 'Kolkata, IN',
    price: '₹399',
  },
  {
    id: 11,
    image: 'https://images.unsplash.com/photo-1508997449629-303059a039c0?auto=format&fit=crop&w=600&q=80',
    title: 'Dance Fusion Night',
    date: 'Nov 02, 2026',
    location: 'Ahmedabad, IN',
    price: '₹549',
  },
  {
    id: 12,
    image: 'https://images.unsplash.com/photo-1483721310020-03333e577078?auto=format&fit=crop&w=600&q=80',
    title: 'Wellness & Yoga Retreat',
    date: 'Nov 08, 2026',
    location: 'Rishikesh, IN',
    price: '₹1199',
  },
  {
    id: 13,
    image: 'https://images.unsplash.com/photo-1531482615713-2afd69097998?auto=format&fit=crop&w=600&q=80',
    title: 'Film Festival Premiere',
    date: 'Nov 15, 2026',
    location: 'Mumbai, IN',
    price: '₹699',
  },
  {
    id: 14,
    image: 'https://images.unsplash.com/photo-1461151304267-38535e780c79?auto=format&fit=crop&w=600&q=80',
    title: 'Book Fair & Author Meet',
    date: 'Nov 20, 2026',
    location: 'Kolkata, IN',
    price: 'Free',
  },
]

const ITEMS_PER_PAGE = 9

export default function EventCards() {
  const [currentPage, setCurrentPage] = useState(1)

  const totalPages = Math.ceil(events.length / ITEMS_PER_PAGE)

  const startIndex = (currentPage - 1) * ITEMS_PER_PAGE
  const currentEvents = events.slice(startIndex, startIndex + ITEMS_PER_PAGE)

  const goToPage = (page: number) => {
    if (page < 1 || page > totalPages) return
    setCurrentPage(page)
    window.scrollTo({ top: document.querySelector('.events-section')?.getBoundingClientRect().top, behavior: 'smooth' })
  }

  const pageNumbers = Array.from({ length: totalPages }, (_, i) => i + 1)

  return (
    <section className="events-section">
      <h2 className="events-heading">Upcoming Events</h2>

      <div className="events-grid">
        {currentEvents.map((event) => (
          <div className="event-card" key={event.id}>
            <div className="event-image-wrapper">
              <img src={event.image} alt={event.title} className="event-image" />
            </div>
            <div className="event-info">
              <h3 className="event-title">{event.title}</h3>
              <p className="event-detail">
                <span className="icon">📅</span> {event.date}
              </p>
              <p className="event-detail">
                <span className="icon">📍</span> {event.location}
              </p>
              <p className="event-price">{event.price}</p>
            </div>
          </div>
        ))}
      </div>

      {totalPages > 1 && (
        <div className="pagination">
          <button
            className="page-btn"
            onClick={() => goToPage(currentPage - 1)}
            disabled={currentPage === 1}
          >
            &#10094; Prev
          </button>

          {pageNumbers.map((num) => (
            <button
              key={num}
              className={`page-btn number ${currentPage === num ? 'active' : ''}`}
              onClick={() => goToPage(num)}
            >
              {num}
            </button>
          ))}

          <button
            className="page-btn"
            onClick={() => goToPage(currentPage + 1)}
            disabled={currentPage === totalPages}
          >
            Next &#10095;
          </button>
        </div>
      )}
    </section>
  )
}