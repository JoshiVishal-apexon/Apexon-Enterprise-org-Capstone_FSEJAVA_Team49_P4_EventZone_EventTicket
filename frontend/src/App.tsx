import React, { useState } from 'react'
import { Routes, Route, Link } from 'react-router-dom'
import Home from './pages/Home'
import EventDetail from './pages/EventDetail'
import MyBookings from './pages/MyBookings'
import OrganiserDashboard from './pages/OrganiserDashboard'
import AdminPanel from './pages/AdminPanel'
import { AuthProvider } from './state/AuthContext'
import type { CSSProperties } from 'react'
import './App.css'
import Banner from './components/Banner'
import EventCards from './components/EventCards'
import Footer from './components/Footer'

export default function App() {
  const [menuOpen, setMenuOpen] = useState(false)

  const closeMenu = () => setMenuOpen(false)

  return (
    <AuthProvider>
      <nav className="navbar">
        <Link to="/" className="brand" onClick={closeMenu}>
          EventZone
        </Link>

        <button
          className={`nav-toggle ${menuOpen ? 'open' : ''}`}
          onClick={() => setMenuOpen(!menuOpen)}
          aria-label="Toggle navigation"
          aria-expanded={menuOpen}
        >
          <span></span>
          <span></span>
          <span></span>
        </button>

        <ul className={`nav-links ${menuOpen ? 'active' : ''}`}>
          <li><Link to="/" onClick={closeMenu}>Home</Link></li>
          <li><Link to="/bookings" onClick={closeMenu}>My Bookings</Link></li>
          <li><Link to="/organiser" onClick={closeMenu}>Organiser</Link></li>
          <li><Link to="/admin" onClick={closeMenu}>Admin</Link></li>
        </ul>
      </nav>

      <Banner />
      <EventCards />

      <Routes>
        <Route path="/" element={<Home />} />
        <Route path="/events/:id" element={<EventDetail />} />
        <Route path="/bookings" element={<MyBookings />} />
        <Route path="/organiser" element={<OrganiserDashboard />} />
        <Route path="/admin" element={<AdminPanel />} />
      </Routes>
      <Footer />
    </AuthProvider>
  )
}