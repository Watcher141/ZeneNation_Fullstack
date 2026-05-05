// src/components/common/AnnouncementBanner.jsx
import { useState, useEffect } from 'react';
import { announcementApi } from '../../api/apiCollections';
import { MdClose, MdCampaign, MdLocalOffer, MdWarning, MdCheckCircle } from 'react-icons/md';
import './AnnouncementBanner.css';

const typeConfig = {
  INFO:    { icon: MdCampaign,    color: 'var(--accent-primary)',   bg: 'rgba(233,69,96,0.08)'   },
  DEAL:    { icon: MdLocalOffer,  color: 'var(--accent-secondary)', bg: 'rgba(245,166,35,0.08)'  },
  WARNING: { icon: MdWarning,     color: '#ff9800',                 bg: 'rgba(255,152,0,0.08)'   },
  SUCCESS: { icon: MdCheckCircle, color: 'var(--accent-green)',     bg: 'rgba(76,175,80,0.08)'   },
};

const AnnouncementBanner = () => {
  const [announcements, setAnnouncements] = useState([]);
  const [dismissed, setDismissed] = useState(() => {
    try { return JSON.parse(localStorage.getItem('dismissedAnnouncements') || '[]'); }
    catch { return []; }
  });
  const [current, setCurrent] = useState(0);

  useEffect(() => {
    announcementApi.getActive()
      .then(res => setAnnouncements(res.data.data || []))
      .catch(() => {});
  }, []);

  const visible = announcements.filter(a => !dismissed.includes(a.id));

  const dismiss = (id) => {
    const next = [...dismissed, id];
    setDismissed(next);
    localStorage.setItem('dismissedAnnouncements', JSON.stringify(next));
    if (current >= visible.length - 1) setCurrent(0);
  };

  if (!visible.length) return null;

  const ann = visible[current] || visible[0];
  if (!ann) return null;

  const config = typeConfig[ann.type] || typeConfig.INFO;
  const Icon = config.icon;

  return (
    <div className="announcement-banner" style={{ background: config.bg, borderBottom: `2px solid ${config.color}` }}>
      <div className="announcement-inner">
        <Icon size={18} color={config.color} style={{ flexShrink: 0 }} />
        <div className="announcement-content">
          {ann.title && <span className="announcement-title" style={{ color: config.color }}>{ann.title}: </span>}
          <span className="announcement-message">{ann.message}</span>
        </div>
        {visible.length > 1 && (
          <div className="announcement-dots">
            {visible.map((_, i) => (
              <button key={i} className={`dot ${i === current ? 'active' : ''}`}
                onClick={() => setCurrent(i)} style={{ background: i === current ? config.color : undefined }} />
            ))}
          </div>
        )}
        <button className="announcement-close" onClick={() => dismiss(ann.id)}>
          <MdClose size={16} />
        </button>
      </div>
    </div>
  );
};

export default AnnouncementBanner;