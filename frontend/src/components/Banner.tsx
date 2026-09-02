import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import './Banner.css';

interface Slide {
  image: string
  title: string
  subtitle: string
}

const slides: Slide[] = [
  {
    image: 'https://images.unsplash.com/photo-1492684223066-81342ee5ff30?auto=format&fit=crop&w=1600&q=80',
    title: 'Discover Amazing Events',
    subtitle: 'Concerts, workshops, and meetups happening near you.',
  },
  {
    image: 'https://images.unsplash.com/photo-1531058020387-3be344556be6?auto=format&fit=crop&w=1600&q=80',
    title: 'Book Your Seat Today',
    subtitle: 'Quick and easy booking for all your favorite events.',
  },
  {
    image: 'https://images.unsplash.com/photo-1511578314322-379afb476865?auto=format&fit=crop&w=1600&q=80',
    title: 'Host Your Own Event',
    subtitle: 'Reach thousands of attendees with our organiser tools.',
  },
]

export default function Banner() {
  const [current, setCurrent] = useState(0)
  const navigate = useNavigate()

  const prevSlide = () => {
    setCurrent((prev) => (prev === 0 ? slides.length - 1 : prev - 1))
  }

  const nextSlide = () => {
    setCurrent((prev) => (prev === slides.length - 1 ? 0 : prev + 1))
  }

  // auto-slide every 5s
  useEffect(() => {
    const timer = setInterval(nextSlide, 5000)
    return () => clearInterval(timer)
  }, [])

  return (
    <div className="banner">
      {slides.map((slide, index) => (
        <div
          key={index}
          className={`banner-slide ${index === current ? 'active' : ''}`}
          style={{ backgroundImage: `url(${slide.image})` }}
        >
          <div className="banner-overlay">
            <div className="banner-content">
              <h1>{slide.title}</h1>
              <p>{slide.subtitle}</p>
              <button className="browse-btn" onClick={() => navigate('/')}>
                Browse Events
              </button>
            </div>
          </div>
        </div>
      ))}

      <button className="banner-arrow left" onClick={prevSlide} aria-label="Previous slide">
        &#10094;
      </button>
      <button className="banner-arrow right" onClick={nextSlide} aria-label="Next slide">
        &#10095;
      </button>

      <div className="banner-dots">
        {slides.map((_, index) => (
          <span
            key={index}
            className={`dot ${index === current ? 'active' : ''}`}
            onClick={() => setCurrent(index)}
          />
        ))}
      </div>
    </div>
  )
}