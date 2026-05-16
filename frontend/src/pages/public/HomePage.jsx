// src/pages/public/HomePage.jsx
import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { productApi } from '../../api/productApi';
import { homeSectionApi } from '../../api/homeSectionApi';
import ProductCard from '../../components/product/ProductCard';
import HorizontalScroll from '../../components/common/HorizontalScroll';
import Loader from '../../components/common/Loader';
import { announcementApi } from '../../api/apiCollections';
import './HomePage.css';

const HomePage = () => {
  const [sections, setSections] = useState([]);
  const [announcements, setAnnouncements] = useState([]);
  const [annIndex, setAnnIndex] = useState(0);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    announcementApi.getActive()
      .then(res => setAnnouncements(res.data.data || []))
      .catch(() => {});
  }, []);

  useEffect(() => {
    if (announcements.length <= 1) return;
    const timer = setInterval(() => setAnnIndex(i => (i + 1) % announcements.length), 4000);
    return () => clearInterval(timer);
  }, [announcements.length]);

  useEffect(() => {
    homeSectionApi.getActive()
      .then(res => setSections(res.data.data || []))
      .catch(() => {})
      .finally(() => setLoading(false));
  }, []);

  if (loading) return <Loader fullPage />;

  return (
    <div className="home-page">

      {/* ── Featured Banner ── */}
      <section className="featured-carousel-section">
        <div className="outside-aurora aurora-left" />
        <div className="outside-aurora aurora-bottom" />
        <div className="outside-aurora aurora-right" />
        <div className="featured-grid-overlay" />
        <div className="container">
          <div className="featured-carousel">
            <img
              src="https://images.unsplash.com/photo-1578632767115-351597cf2477?q=80&w=1600&auto=format&fit=crop"
              alt="Anime Banner"
              className="featured-carousel-image"
            />
            <div className="featured-carousel-overlay" />
            <div className="featured-carousel-content">
              <div className="featured-carousel-badge">⚔️ Featured Collection</div>
              <h2 className="featured-carousel-title">
                Legendary <span>Anime Weapons</span>
              </h2>
              <p className="featured-carousel-desc">
                Explore premium LED katanas, collector swords,
                anime replicas and exclusive collectibles.
              </p>
              <div className="featured-carousel-actions">
                <Link to="/search?keyword=katana" className="btn btn-primary btn-lg">Explore Collection</Link>
                <Link to="/products" className="btn btn-secondary btn-lg">View Products</Link>
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* ── Dynamic Sections from Admin ── */}
      {sections.map(section => (
        <section key={section.id} className="section">
          <div className="container">
            <div className="section-header">
              <div>
                {section.subtitle && <p className="section-subtitle" style={{ marginBottom: 4 }}>{section.subtitle}</p>}
                <h2 className="section-title">{section.title}</h2>
              </div>
              {section.viewAllUrl && (
                <Link to={section.viewAllUrl} className="btn btn-ghost btn-sm">View All →</Link>
              )}
            </div>

            {section.products?.length > 0 ? (
              <HorizontalScroll>
                {section.products.map(p => (
                  <div key={p.id} className="product-card-scroll-wrap">
                    <ProductCard product={p} />
                  </div>
                ))}
              </HorizontalScroll>
            ) : (
              <div className="empty-state" style={{ padding: '24px 0' }}>
                <p className="empty-state-title" style={{ fontSize: 14 }}>No products in this section yet</p>
                <p className="empty-state-desc">Add products from the admin panel → Home Sections</p>
              </div>
            )}
          </div>
        </section>
      ))}

      {/* ── Promo Banner ── */}
      <section className="promo-banner">
        <div className="container promo-inner">
          {announcements.length > 0 ? (
            <>
              <div className="promo-content">
                {announcements.length > 1 && (
                  <div className="promo-dots">
                    {announcements.map((_, i) => (
                      <button key={i} className={`promo-dot ${i === annIndex ? 'active' : ''}`}
                        onClick={() => setAnnIndex(i)} />
                    ))}
                  </div>
                )}
                <div className="promo-type-badge">{
                  announcements[annIndex]?.type === 'DEAL' ? '🎉 Deal' :
                  announcements[annIndex]?.type === 'SUCCESS' ? '✅ Update' :
                  announcements[annIndex]?.type === 'WARNING' ? '⚠️ Notice' : '📢 Announcement'
                }</div>
                <h2>{announcements[annIndex]?.title}</h2>
                <p className="text-secondary">{announcements[annIndex]?.message}</p>
              </div>
              <Link to="/products" className="btn btn-gold btn-lg">Shop Now</Link>
            </>
          ) : (
            <>
              <div className="promo-content">
                <div className="promo-type-badge">🚚 Shipping</div>
                <h2>Free Delivery on orders above ₹500</h2>
                <p className="text-secondary">Shop your favourite anime merch with free shipping!</p>
              </div>
              <Link to="/products" className="btn btn-gold btn-lg">Shop Now</Link>
            </>
          )}
        </div>
      </section>
    </div>
  );
};

export default HomePage;