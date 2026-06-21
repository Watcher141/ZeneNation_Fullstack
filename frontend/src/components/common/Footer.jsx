// src/components/common/Footer.jsx
import { useState } from 'react';
import { Link } from 'react-router-dom';
import { announcementApi } from '../../api/apiCollections';
import { MdEmail, MdSend, MdCheckCircle } from 'react-icons/md';
import toast from 'react-hot-toast';
import './Footer.css';

const Footer = () => {
  const [email, setEmail] = useState('');
  const [name, setName] = useState('');
  const [loading, setLoading] = useState(false);
  const [subscribed, setSubscribed] = useState(false);

  const handleSubscribe = async (e) => {
    e.preventDefault();
    if (!email.trim()) return;
    setLoading(true);
    try {
      await announcementApi.subscribe({ email, name });
      setSubscribed(true);
      setEmail('');
      setName('');
      toast.success('Subscribed! Check your email for a welcome message.');
    } catch (err) {
      const msg = err.response?.data?.message || 'Failed to subscribe';
      // If already subscribed, still show success-ish
      if (msg.toLowerCase().includes('already')) {
        toast('You are already subscribed!', { icon: '✓' });
      } else {
        toast.error(msg);
      }
    } finally { setLoading(false); }
  };

  return (
    <footer className="footer">

      {/* ── Newsletter Strip (UNCHANGED) ── */}
      <div className="footer-newsletter">
        <div className="container newsletter-inner">
          <div className="newsletter-text">
            <div className="newsletter-icon"><MdEmail size={28} /></div>
            <div>
              <h3 className="newsletter-title">Stay in the Loop</h3>
              <p className="newsletter-subtitle">Get exclusive deals, new arrivals and anime merch drops straight to your inbox.</p>
            </div>
          </div>

          {subscribed ? (
            <div className="newsletter-success">
              <MdCheckCircle size={24} color="var(--accent-green)" />
              <span>You're subscribed! Welcome to the Zenenation family</span>
            </div>
          ) : (
            <form onSubmit={handleSubscribe} className="newsletter-form">
              <input
                type="text"
                placeholder="Your name (optional)"
                value={name}
                onChange={e => setName(e.target.value)}
                className="newsletter-input"
              />
              <input
                type="email"
                placeholder="your@email.com"
                value={email}
                onChange={e => setEmail(e.target.value)}
                className="newsletter-input"
                required
              />
              <button type="submit" className="newsletter-btn" disabled={loading}>
                {loading ? '...' : <><MdSend size={18} /> Subscribe</>}
              </button>
            </form>
          )}
        </div>
      </div>

      {/* ── Main Footer (UPDATED: IMAGE LOGO & GROUPED LINKS) ── */}
      <div className="container footer-inner-compact">
        <div className="footer-brand-compact">
          <Link to="/" className="footer-logo" style={{ display: 'inline-block', marginBottom: '8px' }}>
            <img 
              src="/button1.png" 
              alt="Zenenation Logo" 
              style={{ 
                height: '140px',          
                width: 'auto', 
                objectFit: 'contain',
                transform: 'scale(1.6)', 
                transformOrigin: 'left center' 
              }} 
            />
          </Link>
          <p className="footer-tagline">Your ultimate anime merchandise destination</p>
        </div>

        <div className="footer-rows">
          <div className="footer-row">
            <span className="footer-row-title">Shop:</span>
            <div className="footer-links-group">
              <Link to="/products">All Products</Link>
              <Link to="/products?sortBy=createdAt&sortDir=desc">New Arrivals</Link>
              <Link to="/search?keyword=figure">Figures</Link>
              <Link to="/search?keyword=katana">Katana</Link>
            </div>
          </div>
          <div className="footer-row">
            <span className="footer-row-title">Account:</span>
            <div className="footer-links-group">
              <Link to="/profile">My Profile</Link>
              <Link to="/orders">My Orders</Link>
              <Link to="/cart">Cart</Link>
            </div>
          </div>
          <div className="footer-row">
            <span className="footer-row-title">Support:</span>
            <div className="footer-links-group">
              <Link to="/forgot-password">Forgot Password</Link>
              <a href="mailto:support@zenenation.com">Contact Us</a>
            </div>
          </div>
        </div>
      </div>

      <div className="footer-bottom">
        <div className="container">
          <p>© 2025 Zenenation. All rights reserved.</p>
          <p className="text-muted text-sm">Built with ❤️ for anime fans</p>
        </div>
      </div>
    </footer>
  );
};

export default Footer;