// src/pages/public/NotFoundPage.jsx
import { Link } from 'react-router-dom';

const NotFoundPage = () => (
  <div className="page-wrapper" style={{ display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
    <div className="empty-state">
      <div style={{ fontSize: '6rem' }}>⚔️</div>
      <h1 style={{ fontSize: '4rem', color: 'var(--accent-primary)' }}>404</h1>
      <p className="empty-state-title">Page not found</p>
      <p className="empty-state-desc">This page got lost in the anime dimension</p>
      <Link to="/" className="btn btn-primary" style={{ marginTop: '1rem' }}>
        Back to Home
      </Link>
    </div>
  </div>
);

export default NotFoundPage;
