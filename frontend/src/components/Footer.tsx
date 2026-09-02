import React from 'react'
import { Link } from 'react-router-dom'
import './Footer.css'

export default function Footer() {
  const year = new Date().getFullYear()

  return (
    <footer className="footer">
      <div className="footer-container">
        <div className="footer-col">
          <h3 className="footer-brand">My Events</h3>
          <p className="footer-about">
            Discover, book, and host amazing events near you. Your one-stop
            platform for concerts, workshops, and meetups.
          </p>
        </div>

        <div className="footer-col">
          <h4 className="footer-heading">Quick Links</h4>
          <ul className="footer-links">
            <li><Link to="/">Home</Link></li>
            <li><Link to="/bookings">My Bookings</Link></li>
            <li><Link to="/organiser">Organiser</Link></li>
            <li><Link to="/admin">Admin</Link></li>
          </ul>
        </div>

        <div className="footer-col">
          <h4 className="footer-heading">Support</h4>
          <ul className="footer-links">
            <li><a href="#">Help Center</a></li>
            <li><a href="#">Terms of Service</a></li>
            <li><a href="#">Privacy Policy</a></li>
            <li><a href="#">Contact Us</a></li>
          </ul>
        </div>

        <div className="footer-col">
          <h4 className="footer-heading">Follow Us</h4>
          <div className="footer-socials">
            <a href="#" aria-label="Facebook">📘</a>
            <a href="#" aria-label="Instagram">📷</a>
            <a href="#" aria-label="Twitter">🐦</a>
            <a href="#" aria-label="LinkedIn">💼</a>
          </div>
        </div>
      </div>

      <div className="footer-bottom">
        <p>© {year} My Events. All rights reserved.</p>
      </div>
    </footer>
  )
}