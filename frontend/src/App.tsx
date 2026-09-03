import React, { useState } from 'react'
import { Routes, Route, Link } from 'react-router-dom'
import Home from './pages/Home'
import EventDetail from './pages/EventDetail'
import MyBookings from './pages/MyBookings'
import OrganiserDashboard from './pages/OrganiserDashboard'
import AdminPanel from './pages/AdminPanel'
import Login from './pages/Login'
import { AuthProvider, useAuth } from './state/AuthContext'
import './App.css'
import Banner from './components/Banner'
import EventCards from './components/EventCards'
import Footer from './components/Footer'

function Nav() {
  const [menuOpen, setMenuOpen] = useState(false)
  const { auth, isOrganiser, isAdmin, logout } = useAuth()
  const closeMenu = () => setMenuOpen(false)

  return (
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
        {auth.token && <li><Link to="/bookings" onClick={closeMenu}>My Bookings</Link></li>}
        {/* Role-gated so an attendee is not shown areas they cannot use. */}
        {isOrganiser && <li><Link to="/organiser" onClick={closeMenu}>Organiser</Link></li>}
        {isAdmin && <li><Link to="/admin" onClick={closeMenu}>Admin</Link></li>}
        {auth.token
          ? <li><button onClick={() => { logout(); closeMenu() }}>Log out ({auth.name})</button></li>
          : <li><Link to="/login" onClick={closeMenu}>Log in</Link></li>}
      </ul>
    </nav>
  )
}

export default function App() {
  return (
    <AuthProvider>
      <Nav />

      <Banner />
      <EventCards />

      <Routes>
        <Route path="/" element={<Home />} />
        <Route path="/events/:id" element={<EventDetail />} />
        <Route path="/bookings" element={<MyBookings />} />
        <Route path="/organiser" element={<OrganiserDashboard />} />
        <Route path="/admin" element={<AdminPanel />} />
        <Route path="/login" element={<Login />} />
      </Routes>
      <Footer />
    </AuthProvider>
  )
}
