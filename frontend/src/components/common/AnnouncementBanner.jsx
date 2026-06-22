// src/components/common/AnnouncementBanner.jsx
import { useState, useEffect } from 'react';
import { announcementApi } from '../../api/apiCollections';
import { MdClose, MdCampaign, MdLocalOffer, MdWarning, MdCheckCircle } from 'react-icons/md';
import './AnnouncementBanner.css';

const typeConfig = {
  INFO:    { icon: MdCampaign,    color: 'var(--accent-primary, #e94560)',   bg: 'rgba(233,69,96,0.08)'   },
  DEAL:    { icon: MdLocalOffer,  color: 'var(--accent-secondary, #f5a623)', bg: 'rgba(245,166,35,0.08)'  },
  WARNING: { icon: MdWarning,     color: '#ff9800',                          bg: 'rgba(255,152,0,0.08)'   },
  SUCCESS: { icon: MdCheckCircle, color: 'var(--accent-green, #4caf50)',     bg: 'rgba(76,175,80,0.08)'   },
};

const DEFAULT_MESSAGE = "Welcome to zenenation store the ultimate Merchendice Destination";

const AnnouncementBanner = () => {
  const [announcements, setAnnouncements] = useState([]);
  
  const [dismissed, setDismissed] = useState(() => {
    try { return JSON.parse(localStorage.getItem('dismissedAnnouncements') || '[]'); }
    catch { return []; }
  });

  useEffect(() => {
    announcementApi.getActive()
      .then(res => {
        const fetchedData = res?.data?.data || res?.data || [];
        setAnnouncements(Array.isArray(fetchedData) ? fetchedData : []);
      })
      .catch(() => {
        setAnnouncements([]); 
      });
  }, []);

  // Get all announcements that haven't been dismissed
  const visible = announcements.filter(a => !dismissed.includes(a.id));
  const hasAnnouncements = visible.length > 0;

  // When clicking close, dismiss ALL currently showing announcements
  const dismissAll = () => {
    if (!hasAnnouncements) return;
    const visibleIds = visible.map(a => a.id);
    const next = [...dismissed, ...visibleIds];
    setDismissed(next);
    localStorage.setItem('dismissedAnnouncements', JSON.stringify(next));
  };

  // Base colors on the first announcement, or fallback to INFO
  const primaryConfig = hasAnnouncements ? (typeConfig[visible[0].type] || typeConfig.INFO) : typeConfig.INFO;

  return (
    <div className="announcement-banner" style={{ background: primaryConfig.bg, borderBottom: `2px solid ${primaryConfig.color}` }}>
      <div className="announcement-inner">
        
        {/* Fixed Left Icon (Using a generic megaphone) */}
        <div className="announcement-icon-wrapper">
          <MdCampaign size={18} color={primaryConfig.color} />
        </div>
        
        {/* Continuous Sliding Text Window */}
        <div className="announcement-marquee-container">
          <div className="animate-marquee" style={{ animationDuration: `${15 + (visible.length * 15)}s` }}>
            
            {hasAnnouncements ? (
              // Map through EVERY announcement and put them side-by-side
              visible.map((ann, index) => {
                const config = typeConfig[ann.type] || typeConfig.INFO;
                return (
                  <span key={ann.id} className="announcement-item">
                    {ann.title && (
                      <span className="announcement-title" style={{ color: config.color }}>
                        {ann.title}: 
                      </span>
                    )}
                    <span className="announcement-message">{ann.message}</span>
                    
                    {/* Add a dot separator between announcements, except for the last one */}
                    {index < visible.length - 1 && (
                      <span className="announcement-separator">•</span>
                    )}
                  </span>
                );
              })
            ) : (
              // Fallback if no announcements
              <span className="announcement-item">
                <span className="announcement-message">{DEFAULT_MESSAGE}</span>
              </span>
            )}

          </div>
        </div>

        {/* Right Controls - Only the close button remains */}
        {hasAnnouncements && (
          <div className="announcement-controls">
            <button className="announcement-close" onClick={dismissAll} title="Dismiss Announcements">
              <MdClose size={18} color="#ffffff" />
            </button>
          </div>
        )}

      </div>
    </div>
  );
};

export default AnnouncementBanner;