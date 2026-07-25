// src/pages/public/TrackOrderPage.jsx
import { useState, useRef } from 'react';
import { MdLocalShipping, MdContentPaste, MdOpenInNew, MdSearch, MdInfo, MdCheckCircle, MdClose, MdEmail, MdInventory2 } from 'react-icons/md';
import { FaBoxOpen, FaTruck, FaWhatsapp, FaClipboardCheck, FaIndustry, FaGift } from 'react-icons/fa';
import { MdChat } from 'react-icons/md';
import './TrackOrderPage.css';

const STEPS = [
  { icon: FaBoxOpen,        color: '#f5a623', label: 'Order Placed', desc: 'Your order has been received' },
  { icon: FaClipboardCheck, color: '#4fc3f7', label: 'Confirmed',    desc: 'Order confirmed & being prepared' },
  { icon: FaIndustry,       color: '#7c4dff', label: 'Processing',   desc: 'Your items are being packed' },
  { icon: FaTruck,          color: '#e94560', label: 'Shipped',      desc: 'Package handed to courier' },
  { icon: FaGift,           color: '#4caf50', label: 'Delivered',    desc: 'Enjoy your anime merch!' },
];

const isValidJetpostLink = (url) => {
  try {
    const u = new URL(url);
    return (
      u.protocol === 'https:' &&
      (u.hostname.includes('jetpost') ||
        u.hostname.includes('thejetpost') ||
        u.hostname.includes('track') ||
        // allow any https link that could be a branded tracking link
        u.protocol === 'https:')
    );
  } catch {
    return false;
  }
};

const TrackOrderPage = () => {
  const [trackingLink, setTrackingLink] = useState('');
  const [submitted, setSubmitted] = useState(false);
  const [error, setError] = useState('');
  const [iframeError, setIframeError] = useState(false);
  const inputRef = useRef(null);

  const handlePaste = async () => {
    try {
      const text = await navigator.clipboard.readText();
      setTrackingLink(text.trim());
      setError('');
    } catch {
      inputRef.current?.focus();
    }
  };

  const handleTrack = (e) => {
    e.preventDefault();
    const link = trackingLink.trim();

    if (!link) {
      setError('Please enter your tracking link.');
      return;
    }
    if (!link.startsWith('http://') && !link.startsWith('https://')) {
      setError('Please paste the full tracking link (starting with https://)');
      return;
    }
    setError('');
    setSubmitted(true);
    setIframeError(false);
  };

  const handleReset = () => {
    setSubmitted(false);
    setTrackingLink('');
    setIframeError(false);
    setError('');
  };

  const handleOpenInNewTab = () => {
    window.open(trackingLink, '_blank', 'noopener,noreferrer');
  };

  return (
    <div className="track-page">
      {/* ── Hero Banner ── */}
      <div className="track-hero">
        <div className="track-hero-glow" />
        <div className="container track-hero-inner">
          <div className="track-hero-icon">
            <MdLocalShipping size={40} />
          </div>
          <h1 className="track-hero-title">Track Your Package</h1>
          <p className="track-hero-subtitle">
            Paste the tracking link sent to you via WhatsApp or SMS by JetPost to see your package's live status.
          </p>
        </div>
      </div>

      <div className="container track-content">

        {/* ── How It Works ── */}
        {!submitted && (
          <div className="track-info-banner">
            <MdInfo size={20} color="var(--accent-blue)" style={{ flexShrink: 0 }} />
            <div>
              <strong>How it works:</strong> After your order is shipped, JetPost sends a tracking link directly to your
              WhatsApp or registered mobile number. Paste that link below to track your parcel in real time.
            </div>
          </div>
        )}

        {/* ── Search Box ── */}
        {!submitted ? (
          <div className="track-card">
            <div className="track-card-header">
              <FaTruck size={22} color="var(--accent-primary)" />
              <h2>Enter Your Tracking Link</h2>
            </div>

            <form onSubmit={handleTrack} className="track-form">
              <div className="track-input-wrapper">
                <input
                  ref={inputRef}
                  type="url"
                  className={`track-input ${error ? 'error' : ''}`}
                  placeholder="https://track.thejetpost.in/... or your JetPost tracking link"
                  value={trackingLink}
                  onChange={(e) => { setTrackingLink(e.target.value); setError(''); }}
                  id="tracking-link-input"
                  autoComplete="off"
                />
                <button
                  type="button"
                  className="track-paste-btn"
                  onClick={handlePaste}
                  title="Paste from clipboard"
                >
                  <MdContentPaste size={20} />
                  <span>Paste</span>
                </button>
              </div>
              {error && (
                <p className="track-error">
                  <MdClose size={14} /> {error}
                </p>
              )}
              <button type="submit" className="btn btn-primary track-submit-btn" id="track-submit">
                <MdSearch size={20} />
                Track My Package
              </button>
            </form>

            {/* WhatsApp tip */}
            <div className="track-tip">
              <FaWhatsapp size={16} color="#25D366" />
              <span>
                <strong>Tip:</strong> Your tracking link is in the WhatsApp or SMS message from JetPost. Long-press the link → Copy Link → Paste here.
              </span>
            </div>
          </div>
        ) : (
          /* ── Tracking Result ── */
          <div className="track-result-section">
            <div className="track-result-header">
              <div className="track-result-title">
                <MdCheckCircle size={22} color="var(--accent-green)" />
                <span>Tracking your shipment…</span>
              </div>
              <button className="btn btn-ghost btn-sm" onClick={handleReset}>
                ← Track Another
              </button>
            </div>

            {/* Link display */}
            <div className="track-link-display">
              <span className="track-link-label">Tracking URL:</span>
              <span className="track-link-url">{trackingLink}</span>
              <button className="btn btn-sm track-open-btn" onClick={handleOpenInNewTab} title="Open in new tab">
                <MdOpenInNew size={16} />
                Open
              </button>
            </div>

            {/* Iframe embed */}
            {!iframeError ? (
              <div className="track-iframe-wrapper">
                <div className="track-iframe-loader">
                  <div className="spinner" />
                  <span>Loading tracking info…</span>
                </div>
                <iframe
                  key={trackingLink}
                  src={trackingLink}
                  title="Package Tracking"
                  className="track-iframe"
                  sandbox="allow-scripts allow-same-origin allow-forms allow-popups"
                  onError={() => setIframeError(true)}
                  onLoad={(e) => {
                    // Hide loader once loaded
                    const loader = e.target.parentElement.querySelector('.track-iframe-loader');
                    if (loader) loader.style.display = 'none';
                  }}
                />
              </div>
            ) : null}

            {/* Fallback: open in new tab */}
            <div className="track-fallback-card">
              <FaBoxOpen size={32} color="var(--accent-secondary)" />
              <div>
                <p className="track-fallback-title">Can't display inline?</p>
                <p className="track-fallback-desc">
                  Some tracking pages block embedding. Click below to open it in a new tab for the full experience.
                </p>
                <button className="btn btn-primary" onClick={handleOpenInNewTab} id="open-tracking-tab">
                  <MdOpenInNew size={18} />
                  Open Tracking Page
                </button>
              </div>
            </div>
          </div>
        )}

        {/* ── Order Journey Steps ── */}
        <div className="track-journey">
          <h3 className="track-journey-title">Order Journey</h3>
          <div className="track-steps">
            {STEPS.map((step, idx) => {
              const StepIcon = step.icon;
              return (
                <div key={idx} className="track-step">
                  <div className="track-step-icon">
                    <StepIcon size={22} color={step.color} />
                  </div>
                  <div className="track-step-info">
                    <span className="track-step-label">{step.label}</span>
                    <span className="track-step-desc">{step.desc}</span>
                  </div>
                  {idx < STEPS.length - 1 && <div className="track-step-connector" />}
                </div>
              );
            })}
          </div>
        </div>

        {/* ── Help Section ── */}
        <div className="track-help-grid">
          <div className="track-help-card">
            <div className="track-help-icon"><MdChat size={28} color="var(--accent-blue)" /></div>
            <h4>Didn't receive a tracking link?</h4>
            <p>It's usually sent within 24–48 hours of your order being shipped. Check your WhatsApp and SMS inbox.</p>
          </div>
          <div className="track-help-card">
            <div className="track-help-icon"><MdInventory2 size={28} color="var(--accent-secondary)" /></div>
            <h4>Track via My Orders</h4>
            <p>Log in to your account and visit <strong>My Orders</strong> to see your order status updated by our team.</p>
          </div>
          <div className="track-help-card">
            <div className="track-help-icon"><MdEmail size={28} color="var(--accent-primary)" /></div>
            <h4>Need Help?</h4>
            <p>
              Contact us at{' '}
              <a href="mailto:zenenationstore@gmail.com" style={{ color: 'var(--accent-primary)' }}>
                zenenationstore@gmail.com
              </a>{' '}
              and we'll assist you right away.
            </p>
          </div>
        </div>

      </div>
    </div>
  );
};

export default TrackOrderPage;
