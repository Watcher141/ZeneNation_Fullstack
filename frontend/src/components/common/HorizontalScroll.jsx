// src/components/common/HorizontalScroll.jsx
import { useRef } from 'react';
import './HorizontalScroll.css';

/**
 * Reusable horizontal scroll row with arrow buttons.
 * Works on both desktop and mobile.
 */
const HorizontalScroll = ({ children, className = '' }) => {
  const scrollRef = useRef(null);

  const scroll = (dir) => {
    if (!scrollRef.current) return;
    const amount = scrollRef.current.offsetWidth * 0.75;
    scrollRef.current.scrollBy({ left: dir === 'left' ? -amount : amount, behavior: 'smooth' });
  };

  return (
    <div className={`hscroll-wrap ${className}`}>
      <button className="hscroll-btn hscroll-btn--left" onClick={() => scroll('left')} aria-label="Scroll left">
        ‹
      </button>
      <div className="hscroll-track" ref={scrollRef}>
        {children}
      </div>
      <button className="hscroll-btn hscroll-btn--right" onClick={() => scroll('right')} aria-label="Scroll right">
        ›
      </button>
    </div>
  );
};

export default HorizontalScroll;