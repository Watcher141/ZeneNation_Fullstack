// src/pages/public/HomePage.jsx
import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { productApi } from '../../api/productApi';
import { categoryApi } from '../../api/categoryApi';
import { homeSectionApi } from '../../api/homeSectionApi';
import ProductCard from '../../components/product/ProductCard';
import HorizontalScroll from '../../components/common/HorizontalScroll';
import Loader from '../../components/common/Loader';
import { announcementApi } from '../../api/apiCollections';
import { MdAutoAwesome, MdImage, MdRocketLaunch, MdCampaign, MdLocalOffer, MdWarning, MdCheckCircle } from 'react-icons/md';
import './HomePage.css';

/* =============================================================================
  ONLINE IMAGE LINKS (KATANA, HOTWHEELS, F1, MARVEL, KNIFE, NARUTO)
=============================================================================
*/
const heroImages = [
  "https://images.unsplash.com/photo-1578632767115-351597cf2477?q=80&w=1600&auto=format&fit=crop", // 1. Katana
  "https://images.unsplash.com/photo-1581235720704-06d3acfcb36f?q=80&w=1600&auto=format&fit=crop", // 2. Hotwheels (Diecast Car)
  "https://images.unsplash.com/photo-1614200187524-dc4b892acf16?q=80&w=1600&auto=format&fit=crop", // 3. F1 Racing Car
  "https://images.unsplash.com/photo-1608889175123-8ee362201f81?q=80&w=1600&auto=format&fit=crop", // 4. Marvel (Spider-Man Figure)
  "https://images.unsplash.com/photo-1590523741831-ab7e8b8f9c7f?q=80&w=1600&auto=format&fit=crop", // 5. Knife (Tactical Blade)
  "https://images.unsplash.com/photo-1613376023733-0a73315d9b06?q=80&w=1600&auto=format&fit=crop"  // 6. Naruto (Anime/Manga Vibe)
];

const HomePage = () => {
  const [sections, setSections] = useState([]);
  const [categories, setCategories] = useState([]);
  const [preorders, setPreorders] = useState([]);
  const [announcements, setAnnouncements] = useState([]);
  const [annIndex, setAnnIndex] = useState(0);
  const [loading, setLoading] = useState(true);
  const [bgIndex, setBgIndex] = useState(0);

  useEffect(() => {
    announcementApi.getActive().then(r => setAnnouncements(r.data.data || [])).catch(() => {});
    categoryApi.getAll().then(r => setCategories(r.data.data || [])).catch(() => {});
    productApi.getPreorders().then(r => setPreorders(r.data.data || [])).catch(() => {});
    homeSectionApi.getActive()
      .then(r => setSections(r.data.data || []))
      .catch(() => {})
      .finally(() => setLoading(false));
  }, []);

  useEffect(() => {
    if (announcements.length <= 1) return;
    const timer = setInterval(() => setAnnIndex(i => (i + 1) % announcements.length), 4000);
    return () => clearInterval(timer);
  }, [announcements.length]);

  useEffect(() => {
    const bgTimer = setInterval(() => {
      setBgIndex(prev => (prev + 1) % heroImages.length);
    }, 5000);
    return () => clearInterval(bgTimer);
  }, []);

  if (loading) return <Loader fullPage />;

  return (
    <div className="home-page">

      {/* ── Hero Banner ── */}
      <section
        className="featured-carousel-section"
        style={{ padding: '1rem', paddingTop: '85px', boxSizing: 'border-box' }}
      >
        <div className="container" style={{ maxWidth: '1400px', margin: '0 auto' }}>
          <div className="featured-carousel" style={{
            position: 'relative',
            width: '100%',
            height: '35vh',       /* Restored to original */
            minHeight: '280px',   /* Restored to original */
            maxHeight: '380px',   /* Restored to original */
            borderRadius: '16px',
            overflow: 'hidden',
            display: 'flex',
            alignItems: 'center',
            boxShadow: '0 20px 40px rgba(0,0,0,0.3)',
            backgroundColor: '#1a1a2e'
          }}>
            {heroImages.map((img, idx) => (
              <img
                key={idx}
                src={img}
                alt={`Featured Banner Image ${idx + 1}`}
                style={{
                  position: 'absolute', top: 0, left: 0,
                  width: '100%', height: '100%',
                  objectFit: 'cover', objectPosition: 'center',
                  zIndex: 0,
                  opacity: bgIndex === idx ? 1 : 0,
                  transition: 'opacity 1.5s ease-in-out'
                }}
              />
            ))}

            <div style={{
              position: 'absolute', top: 0, left: 0,
              width: '100%', height: '100%',
              background: 'linear-gradient(90deg, rgba(15,15,20,0.95) 0%, rgba(15,15,20,0.7) 40%, rgba(15,15,20,0.1) 100%)',
              zIndex: 1
            }} />

            <div className="featured-carousel-content" style={{
              position: 'relative',
              zIndex: 2,
              padding: 'clamp(1rem, 3vw, 2.5rem)',
              maxWidth: '600px',
              width: '100%',
              boxSizing: 'border-box',
              display: 'flex',
              flexDirection: 'column',
              gap: 'clamp(0.4rem, 1.5vw, 0.8rem)' /* Tighter gaps so content doesn't get too tall */
            }}>
              <div style={{
                alignSelf: 'flex-start',
                background: 'rgba(255,255,255,0.1)',
                backdropFilter: 'blur(8px)',
                padding: '4px 12px',
                borderRadius: '30px',
                fontSize: 'clamp(0.6rem, 2vw, 0.7rem)', /* Adjusted min size */
                fontWeight: '700',
                color: '#fff',
                border: '1px solid rgba(255,255,255,0.2)',
                textTransform: 'uppercase',
                letterSpacing: '1px',
                whiteSpace: 'nowrap'
              }}>
                <MdAutoAwesome size={12} style={{ marginRight: 4, verticalAlign: 'middle' }} /> Featured Collection
              </div>

              <h2 style={{
                fontSize: 'clamp(1.4rem, 5vw, 2.8rem)', /* Allows heading to shrink more on mobile */
                color: '#fff',
                margin: '0',
                lineHeight: 1.1,
                fontWeight: 800
              }}>
                Legendary <span style={{ color: 'var(--accent-gold, #FFD700)' }}>Collections</span>
              </h2>

              <p style={{
                color: '#e0e0e0',
                fontSize: 'clamp(0.75rem, 2vw, 1rem)', /* Allows text to shrink more on mobile */
                margin: '0',
                lineHeight: 1.4,
                opacity: 0.9
              }}>
                Explore premium LED katanas, diecast cars, Marvel figures, and exclusive anime replicas designed for true fans.
              </p>

              <div style={{ display: 'flex', gap: '0.6rem', flexWrap: 'wrap', marginTop: '0.2rem' }}>
                <Link to="/products" className="btn btn-primary" style={{ padding: '8px 18px', fontSize: '0.85rem' }}>
                  Explore Collection
                </Link>
                <Link to="/products?newArrivals=true&page=0" className="btn btn-ghost" style={{ padding: '8px 18px', fontSize: '0.85rem', border: '1px solid rgba(255,255,255,0.3)', color: '#fff', background: 'rgba(255,255,255,0.05)' }}>
                  New Arrivals
                </Link>
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* ── Shop by Category ── */}
      {categories.length > 0 && (
        <section className="section">
          <div className="container">
            <div className="section-header">
              <h2 className="section-title">Shop by Category</h2>
              <Link to="/products" className="btn btn-ghost btn-sm">View All →</Link>
            </div>
            <HorizontalScroll>
              {categories.map(cat => (
                <Link to={`/products?categoryId=${cat.id}`} key={cat.id} className="category-card category-card--scroll">
                  {cat.imageUrl
                    ? <img src={cat.imageUrl} alt={cat.name} />
                    : <div className="category-icon"><MdImage size={28} color="var(--text-muted)" /></div>
                  }
                  <span className="category-name">{cat.name}</span>
                </Link>
              ))}
            </HorizontalScroll>
          </div>
        </section>
      )}

      {/* ── Dynamic Home Sections ── */}
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
                <p className="empty-state-desc">Add products from Admin → Home Sections</p>
              </div>
            )}
          </div>
        </section>
      ))}

      {/* ── Preorder Collection ── */}
      {preorders.length > 0 && (
        <section className="preorder-section">
          <div className="container">
            <div className="section-header">
              <div>
                <div className="preorder-section-badge"><MdRocketLaunch size={14} style={{ marginRight: 4, verticalAlign: 'middle' }} /> Coming Soon</div>
                <h2 className="section-title">Preorder Collection</h2>
                <p className="section-subtitle">Reserve upcoming anime collectibles before they're gone</p>
              </div>
              <Link to="/preorder" className="btn btn-ghost btn-sm">View All →</Link>
            </div>
            <HorizontalScroll>
              {preorders.map(product => (
                <Link to={`/products/${product.slug}`} key={product.id} className="preorder-home-card preorder-home-card--scroll">
                  <div className="preorder-home-img">
                    {product.primaryImageUrl
                      ? <img src={product.primaryImageUrl} alt={product.name} />
                      : <span><MdImage size={28} color="var(--text-muted)" /></span>}
                    <div className="preorder-home-badge">PREORDER</div>
                  </div>
                  <div className="preorder-home-info">
                    <p className="preorder-home-name">{product.name}</p>
                    {product.estimatedShipDate && (
                      <p className="preorder-home-date">
                        Ships {new Date(product.estimatedShipDate).toLocaleDateString('en-IN', { month: 'short', year: 'numeric' })}
                      </p>
                    )}
                    <div className="preorder-home-price">
                      <span className="text-muted text-xs">From </span>
                      <span className="text-gold" style={{ fontWeight: 700 }}>
                        ₹{Math.ceil(Number(product.discountedPrice || product.price) / 2).toLocaleString('en-IN')}
                      </span>
                    </div>
                  </div>
                </Link>
              ))}
            </HorizontalScroll>
          </div>
        </section>
      )}

      {/* ── Promo Banner ── */}
      <section className="promo-banner">
        <div className="container promo-inner">
          {announcements.length > 0 ? (
            <>
              <div className="promo-content">
                {announcements.length > 1 && (
                  <div className="promo-dots">
                    {announcements.map((_, i) => (
                      <button
                        key={i}
                        className={`promo-dot ${i === annIndex ? 'active' : ''}`}
                        onClick={() => setAnnIndex(i)}
                      />
                    ))}
                  </div>
                )}
                <div className="promo-type-badge">
                  {announcements[annIndex]?.type === 'DEAL'    ? <><MdLocalOffer size={14} style={{ verticalAlign: 'middle', marginRight: 4 }} />Deal</> :
                   announcements[annIndex]?.type === 'SUCCESS' ? <><MdCheckCircle size={14} style={{ verticalAlign: 'middle', marginRight: 4 }} />Update</> :
                   announcements[annIndex]?.type === 'WARNING' ? <><MdWarning size={14} style={{ verticalAlign: 'middle', marginRight: 4 }} />Notice</> : <><MdCampaign size={14} style={{ verticalAlign: 'middle', marginRight: 4 }} />Announcement</>}
                </div>
                <h2>{announcements[annIndex]?.title}</h2>
                <p className="text-secondary">{announcements[annIndex]?.message}</p>
              </div>
              <Link to="/products" className="btn btn-gold btn-lg">Shop Now</Link>
            </>
          ) : (
            null
          )}
        </div>
      </section>

    </div>
  );
};

export default HomePage;