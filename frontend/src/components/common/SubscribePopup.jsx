// src/components/common/SubscribePopup.jsx
import { useState, useEffect } from 'react';
import { announcementApi } from '../../api/apiCollections';
import { MdClose, MdEmail, MdCardGiftcard } from 'react-icons/md';
import toast from 'react-hot-toast';
import './SubscribePopup.css';

const SubscribePopup = () => {
  const [show, setShow] = useState(false);
  const [email, setEmail] = useState('');
  const [name, setName] = useState('');
  const [loading, setLoading] = useState(false);
  const [subscribed, setSubscribed] = useState(false);

  useEffect(() => {
    // Show popup after 3 seconds on first visit
    const alreadySeen = localStorage.getItem('subscribePopupSeen');
    if (!alreadySeen) {
      const timer = setTimeout(() => setShow(true), 3000);
      return () => clearTimeout(timer);
    }
  }, []);

  const handleClose = () => {
    setShow(false);
    localStorage.setItem('subscribePopupSeen', 'true');
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    try {
      await announcementApi.subscribe({ email, name });
      setSubscribed(true);
      localStorage.setItem('subscribePopupSeen', 'true');
      toast.success('Subscribed! Check your email 🎉');
    } catch (err) {
      toast.error(err.response?.data?.message || 'Failed to subscribe');
    } finally { setLoading(false); }
  };

  if (!show) return null;

  return (
    <div className="subscribe-overlay" onClick={handleClose}>
      <div className="subscribe-popup" onClick={e => e.stopPropagation()}>
        <button className="subscribe-close" onClick={handleClose}>
          <MdClose size={20} />
        </button>

        {subscribed ? (
          <div className="subscribe-success">
            <div className="subscribe-success-icon">🎌</div>
            <h3>You're subscribed!</h3>
            <p>Thanks for joining! Check your email for a welcome message.</p>
            <button className="btn btn-primary btn-sm" onClick={handleClose}>Start Shopping</button>
          </div>
        ) : (
          <>
            <div className="subscribe-header">
              <div className="subscribe-icon"><MdEmail size={40} color="var(--accent-primary)" /></div>
              <h2>Stay in the Loop!</h2>
              <p>Subscribe to get exclusive deals, new arrivals, and anime merch updates.</p>
            </div>

            <div className="subscribe-perks">
              <div className="subscribe-perk">
                <MdCardGiftcard size={18} color="var(--accent-secondary)" />
                <span>Exclusive member deals</span>
              </div>
              <div className="subscribe-perk">
                <span>📦</span>
                <span>New product alerts</span>
              </div>
              <div className="subscribe-perk">
                <span>⚡</span>
                <span>Flash sale notifications</span>
              </div>
            </div>

            <form onSubmit={handleSubmit} className="subscribe-form">
              <input className="form-input" type="text" placeholder="Your name (optional)"
                value={name} onChange={e => setName(e.target.value)} />
              <input className="form-input" type="email" placeholder="your@email.com" required
                value={email} onChange={e => setEmail(e.target.value)} />
              <button type="submit" className="btn btn-primary btn-full" disabled={loading}>
                {loading ? 'Subscribing...' : '🎌 Subscribe Now'}
              </button>
            </form>

            <button className="subscribe-skip" onClick={handleClose}>
              No thanks, I'll miss out
            </button>
          </>
        )}
      </div>
    </div>
  );
};

export default SubscribePopup;