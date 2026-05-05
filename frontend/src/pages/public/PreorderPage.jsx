// src/pages/public/PreorderPage.jsx
import { useState, useEffect } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { productApi } from '../../api/productApi';
import { useAuth } from '../../context/AuthContext';
import { useCart } from '../../context/CartContext';
import StarRating from '../../components/product/StarRating';
import Loader from '../../components/common/Loader';
import toast from 'react-hot-toast';
import {
  MdRocketLaunch, MdCalendarToday, MdShoppingCart,
  MdInfo, MdCheckCircle,
} from 'react-icons/md';
import './PreorderPage.css';

const PreorderPage = () => {
  const [products, setProducts] = useState([]);
  const [loading, setLoading] = useState(true);
  const { isAuthenticated } = useAuth();
  const { addToCart } = useCart();
  const navigate = useNavigate();
  const [addingId, setAddingId] = useState(null);

  useEffect(() => {
    productApi.getPreorders()
      .then(res => setProducts(res.data.data || []))
      .catch(() => setProducts([]))
      .finally(() => setLoading(false));
  }, []);

  const handleAddToCart = async (product) => {
    if (!isAuthenticated()) {
      toast.error('Please login to preorder');
      navigate('/login');
      return;
    }
    setAddingId(product.id);
    try {
      await addToCart(product.id, 1);
      toast.success(`${product.name} added! Choose payment at checkout.`);
    } catch {
      toast.error('Failed to add to cart');
    } finally { setAddingId(null); }
  };

  if (loading) return <Loader fullPage />;

  return (
    <div className="page-wrapper">
      <div className="container preorder-page">
        {/* Header */}
        <div className="preorder-header">
          <div className="preorder-header-text">
            <div className="preorder-badge"><MdRocketLaunch size={16} /> Coming Soon</div>
            <h1>Preorder Collection</h1>
            <p>Be the first to own these upcoming anime collectibles. Reserve yours now before they're gone!</p>
          </div>
          <div className="preorder-payment-info">
            <h3><MdInfo size={16} /> Flexible Payment</h3>
            <div className="payment-option-info">
              <MdCheckCircle size={14} color="var(--accent-green)" />
              <span><strong>50% Now</strong> — Pay half upfront, rest when shipped</span>
            </div>
            <div className="payment-option-info">
              <MdCheckCircle size={14} color="var(--accent-green)" />
              <span><strong>100% Now</strong> — Pay in full, get priority shipping</span>
            </div>
          </div>
        </div>

        {products.length === 0 ? (
          <div className="empty-state" style={{ marginTop: 'var(--space-12)' }}>
            <MdRocketLaunch size={64} color="var(--text-muted)" />
            <p className="empty-state-title">No preorders available</p>
            <p className="empty-state-desc">Check back soon for upcoming releases!</p>
            <Link to="/products" className="btn btn-primary" style={{ marginTop: '1rem' }}>
              Shop Available Products
            </Link>
          </div>
        ) : (
          <div className="preorder-grid">
            {products.map(product => (
              <div key={product.id} className="preorder-card">
                {/* Image */}
                <Link to={`/products/${product.slug}`} className="preorder-card-image">
                  {product.primaryImageUrl
                    ? <img src={product.primaryImageUrl} alt={product.name} />
                    : <div className="preorder-no-img">🎌</div>
                  }
                  <div className="preorder-label"><MdRocketLaunch size={12} /> PREORDER</div>
                </Link>

                {/* Info */}
                <div className="preorder-card-body">
                  <span className="preorder-category">{product.category?.name}</span>
                  <Link to={`/products/${product.slug}`} className="preorder-name">{product.name}</Link>
                  {product.tagline && <p className="preorder-tagline">"{product.tagline}"</p>}

                  {/* Ship date */}
                  {product.estimatedShipDate && (
                    <div className="preorder-ship-date">
                      <MdCalendarToday size={14} />
                      <span>Est. ships {new Date(product.estimatedShipDate).toLocaleDateString('en-IN', { month: 'long', year: 'numeric' })}</span>
                    </div>
                  )}

                  {product.preorderNote && (
                    <p className="preorder-note">{product.preorderNote}</p>
                  )}

                  {/* Pricing */}
                  <div className="preorder-pricing">
                    <div className="preorder-full-price">
                      <span className="text-muted text-xs">Full price</span>
                      <span className="price">₹{Number(product.discountedPrice || product.price).toLocaleString('en-IN')}</span>
                    </div>
                    <div className="preorder-half-price">
                      <span className="text-muted text-xs">Reserve with</span>
                      <span className="price text-gold">₹{Math.ceil(Number(product.discountedPrice || product.price) / 2).toLocaleString('en-IN')}</span>
                    </div>
                  </div>

                  {/* Payment options */}
                  <div className="preorder-payment-pills">
                    <span className="payment-pill pill-half">50% now</span>
                    <span className="payment-pill-divider">or</span>
                    <span className="payment-pill pill-full">100% now</span>
                  </div>

                  <button className="btn btn-primary btn-full"
                    style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 8 }}
                    onClick={() => handleAddToCart(product)}
                    disabled={addingId === product.id || product.stockQuantity === 0}>
                    <MdShoppingCart size={18} />
                    {addingId === product.id ? 'Adding...' :
                      product.stockQuantity === 0 ? 'Sold Out' : 'Preorder Now'}
                  </button>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
};

export default PreorderPage;