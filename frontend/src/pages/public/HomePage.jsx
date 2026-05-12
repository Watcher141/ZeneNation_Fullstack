// src/pages/public/HomePage.jsx
import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { productApi } from '../../api/productApi';
import { categoryApi } from '../../api/categoryApi';
import ProductCard from '../../components/product/ProductCard';
import Loader from '../../components/common/Loader';
import { announcementApi } from '../../api/apiCollections';
import './HomePage.css';

const HomePage = () => {
  const [products, setProducts] = useState([]);
  const [categories, setCategories] = useState([]);
  const [loading, setLoading] = useState(true);
  const [announcements, setAnnouncements] = useState([]);
  const [annIndex, setAnnIndex] = useState(0);
  const [preorders, setPreorders] = useState([]);

  useEffect(() => {
    announcementApi.getActive()
      .then(res => setAnnouncements(res.data.data || []))
      .catch(() => {});
    productApi.getPreorders()
      .then(res => setPreorders(res.data.data || []))
      .catch(() => {});
  }, []);

  // Auto-rotate announcements every 4 seconds
  useEffect(() => {
    if (announcements.length <= 1) return;
    const timer = setInterval(() => setAnnIndex(i => (i + 1) % announcements.length), 4000);
    return () => clearInterval(timer);
  }, [announcements.length]);

  useEffect(() => {
    const fetchData = async () => {
      try {
        const [prodRes, catRes] = await Promise.all([
          productApi.getAll({ page: 0, size: 8, sortBy: 'createdAt', sortDir: 'desc' }),
          categoryApi.getAll(),
        ]);
        setProducts(prodRes.data.data?.content || []);
        setCategories(catRes.data.data || []);
      } catch {
        // silent
      } finally {
        setLoading(false);
      }
    };
    fetchData();
  }, []);

  if (loading) return <Loader fullPage />;

  return (
    <div className="home-page">
      Hero
      {/* <section className="hero">
        <div className="container hero-inner">
          <div className="hero-content">
            <div className="hero-badge">🎌 Anime Merchandise</div>
            <h1 className="hero-title">
              Your Ultimate<br />
              <span className="hero-accent">Anime Store</span>
            </h1>
            <p className="hero-desc">
              Figures, wooden katanas, keychains and more —
              hand-picked for true anime fans.
            </p>
            <div className="hero-actions">
              <Link to="/products" className="btn btn-primary btn-lg">
                Shop Now
              </Link>
              <Link to="/search?keyword=figure" className="btn btn-secondary btn-lg">
                Explore Figures
              </Link>
            </div>
          </div>
          <div className="hero-visual">
            <div className="hero-orb" />
            <span className="hero-emoji">⚔️</span>
          </div>
        </div>
      </section> */}

            {/* Featured Banner Carousel */}
      <section className="featured-carousel-section">
        <div className="container">
          <div className="featured-carousel">

            <div className="featured-carousel-overlay" />

            <img
              src="https://images.unsplash.com/photo-1578632767115-351597cf2477?q=80&w=1600&auto=format&fit=crop"
              alt="Anime Banner"
              className="featured-carousel-image"
            />

            <div className="featured-carousel-content">
              <div className="featured-carousel-badge">
                ⚔️ Featured Collection
              </div>

              <h2 className="featured-carousel-title">
                Legendary <span>Anime Weapons</span>
              </h2>

              <p className="featured-carousel-desc">
                Explore premium LED katanas, collector swords,
                anime replicas and exclusive collectibles.
              </p>

              <div className="featured-carousel-actions">
                <Link
                  to="/search?keyword=katana"
                  className="btn btn-primary btn-lg"
                >
                  Explore Collection
                </Link>

                <Link
                  to="/products"
                  className="btn btn-secondary btn-lg"
                >
                  View Products
                </Link>
              </div>
            </div>

          </div>
        </div>
      </section>

      {/* Categories */}
      {categories.length > 0 && (
        <section className="section">
          <div className="container">
            <div className="section-header">
              <h2 className="section-title">Shop by Category</h2>
              <Link to="/products" className="btn btn-ghost btn-sm">View All</Link>
            </div>
            <div className="category-grid">
              {categories.map((cat) => (
                <Link to={`/products?categoryId=${cat.id}`} key={cat.id} className="category-card">
                  {cat.imageUrl ? (
                    <img src={cat.imageUrl} alt={cat.name} />
                  ) : (
                    <div className="category-icon">🎌</div>
                  )}
                  <span className="category-name">{cat.name}</span>
                </Link>
              ))}
            </div>
          </div>
        </section>
      )}

      {/* Latest Products */}
      <section className="section">
        <div className="container">
          <div className="section-header">
            <h2 className="section-title">New Arrivals</h2>
            <Link to="/products" className="btn btn-ghost btn-sm">View All</Link>
          </div>
          {products.length > 0 ? (
            <div className="grid-products">
              {products.map((p) => <ProductCard key={p.id} product={p} />)}
            </div>
          ) : (
            <div className="empty-state">
              <div className="empty-state-icon">🎌</div>
              <p className="empty-state-title">No products yet</p>
              <p className="empty-state-desc">Check back soon — exciting products are coming!</p>
            </div>
          )}
        </div>
      </section>

      {/* Preorder Section */}
      {preorders.length > 0 && (
        <section className="preorder-section">
          <div className="container">
            <div className="section-header">
              <div>
                <div className="preorder-section-badge">🚀 Coming Soon</div>
                <h2 className="section-title">Preorder Collection</h2>
                <p className="section-subtitle">Reserve upcoming anime collectibles before they're gone</p>
              </div>
              <Link to="/preorder" className="btn btn-ghost btn-sm">View All →</Link>
            </div>
            <div className="preorder-home-grid">
              {preorders.slice(0, 4).map(product => (
                <Link to={`/products/${product.slug}`} key={product.id} className="preorder-home-card">
                  <div className="preorder-home-img">
                    {product.primaryImageUrl
                      ? <img src={product.primaryImageUrl} alt={product.name} />
                      : <span>🎌</span>}
                    <div className="preorder-home-badge">PREORDER</div>
                  </div>
                  <div className="preorder-home-info">
                    <p className="preorder-home-name">{product.name}</p>
                    {product.estimatedShipDate && (
                      <p className="preorder-home-date">Ships {new Date(product.estimatedShipDate).toLocaleDateString('en-IN', { month: 'short', year: 'numeric' })}</p>
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
            </div>
          </div>
        </section>
      )}

      {/* Dynamic Announcements / Deals Banner */}
      <section className="promo-banner">
        <div className="container promo-inner">
          {announcements.length > 0 ? (
            <>
              <div className="promo-content">
                {announcements.length > 1 && (
                  <div className="promo-dots">
                    {announcements.map((_, i) => (
                      <button key={i}
                        className={`promo-dot ${i === annIndex ? 'active' : ''}`}
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