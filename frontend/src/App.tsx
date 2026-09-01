import React from 'react'
import { Routes, Route, Link } from 'react-router-dom'
import Home from './pages/Home'
import EventDetail from './pages/EventDetail'
import MyBookings from './pages/MyBookings'
import OrganiserDashboard from './pages/OrganiserDashboard'
import AdminPanel from './pages/AdminPanel'
import { AuthProvider } from './state/AuthContext'

export default function App(){
  return (
    <AuthProvider>
      <nav>
        <Link to='/'>Home</Link> | <Link to='/bookings'>My Bookings</Link> | <Link to='/organiser'>Organiser</Link> | <Link to='/admin'>Admin</Link>
      </nav>
      <Routes>
        <Route path='/' element={<Home/>} />
        <Route path='/events/:id' element={<EventDetail/>} />
        <Route path='/bookings' element={<MyBookings/>} />
        <Route path='/organiser' element={<OrganiserDashboard/>} />
        <Route path='/admin' element={<AdminPanel/>} />
      </Routes>
    </AuthProvider>
  )
}
