// src/pages/public/ProductDetailPage.jsx
import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { productApi } from '../../api/productApi';
import { useCart } from '../../context/CartContext';
import { useAuth } from '../../context/AuthContext';
import Loader from '../../components/common/Loader';
import StarRating from '../../components/product/StarRating';
import ProductReviews from '../../components/product/ProductReviews';
import FrequentlyBoughtTogether from '../../components/product/FrequentlyBoughtTogether';
import { MdShoppingCart, MdImage, MdCheckCircle, MdCancel, MdFitnessCenter } from 'react-icons/md';
import HorizontalScroll from '../../components/common/HorizontalScroll';
import ProductCard from '../../components/product/ProductCard';
import toast from 'react-hot-toast';
import './ProductDetailPage.css';

const ProductDetailPage = () => {
  const { slug } = useParams();
  const navigate = useNavigate();
  const { addToCart } = useCart();
  const { isAuthenticated } = useAuth();
  const [product, setProduct] = useState(null);
  const [loading, setLoading] = useState(true);
  const [selectedImage, setSelectedImage] = useState(0);
  const [quantity, setQuantity] = useState(1);
  const [adding, setAdding] = useState(false);
  const [suggestions, setSuggestions] = useState([]);

  useEffect(() => {
    if (!product) return;
    const fetchSuggestions = async () => {
      try {
        const catId = product.category?.parentId || product.category?.id;
        if (catId) {
          const res = await productApi.getByCategory(catId, { page: 0, size: 20 });
          const all = res.data.data?.content || [];
          const filtered = all.filter(p => p.id !== product.id);
          if (filtered.length >= 3) {
            setSuggestions(filtered.slice(0, 10));
            return;
          }
        }
        const res = await productApi.getAll({ page: 0, size: 12, sortBy: 'createdAt', sortDir: 'desc' });
        setSuggestions((res.data.data?.content || []).filter(p => p.id !== product.id).slice(0, 10));
      } catch {
        // silent fail
      }
    };
    fetchSuggestions();
  }, [product?.id]);

  useEffect(() => {
    productApi.getBySlug(slug)
      .then(r => setProduct(r.data.data))
      .catch(() => navigate('/404'))
      .finally(() => setLoading(false));
  }, [slug]);

  const handleAddToCart = async () => {
    if (!isAuthenticated()) {
      toast.error('Please login to add items to cart');
      navigate('/login');
      return;
    }
    setAdding(true);
    try {
      await addToCart(product.id, quantity);
      toast.success(`${product.name} added to cart!`);
    } catch {
      toast.error('Failed to add to cart');
    } finally {
      setAdding(false);
    }
  };

  if (loading) return <Loader fullPage />;
  if (!product) return null;

  const hasDiscount = product.discountPercent > 0;
  const images = product.images || [];
  const currentImage = images[selectedImage];

  // Use parent category ID for FBT lookup; fall back to own category if already a parent
  const fbtCategoryId = product.category?.parentId || product.category?.id;

  return (
    <div className="page-wrapper">
      <div className="container">
        {/* ── Product Details ── */}
        <div className="product-detail">
          {/* Images */}
          <div className="product-detail-gallery">
            <div className="product-detail-main-img">
              {currentImage
                ? <img src={currentImage.imageUrl} alt={product.name} />
                : <div className="product-detail-no-img"><MdImage size={64} color="var(--text-muted)" /></div>
              }
            </div>
            {images.length > 1 && (
              <div className="product-detail-thumbs">
                {images.map((img, i) => (
                  <button key={img.id}
                    className={`product-detail-thumb ${selectedImage === i ? 'active' : ''}`}
                    onClick={() => setSelectedImage(i)}>
                    <img src={img.imageUrl} alt={`${product.name} ${i + 1}`} />
                  </button>
                ))}
              </div>
            )}
          </div>

          {/* Info */}
          <div className="product-detail-info">
            <span className="product-detail-category">{product.category?.name}</span>
            <h1 className="product-detail-name">{product.name}</h1>
            {product.tagline && (
              <p className="product-detail-tagline">"{product.tagline}"</p>
            )}

            {product.averageRating > 0 && (
              <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                <StarRating rating={product.averageRating} size={18} showNumber />
                <span className="text-muted text-sm">({product.reviewCount} reviews)</span>
              </div>
            )}

            <div className="product-detail-pricing">
              <span className="price" style={{ fontSize: '2rem' }}>
                ₹{Number(product.discountedPrice || product.price).toLocaleString('en-IN')}
              </span>
              {hasDiscount && (
                <>
                  <span className="price-original" style={{ fontSize: '1rem' }}>
                    ₹{Number(product.price).toLocaleString('en-IN')}
                  </span>
                  <span className="discount-badge">{Math.round(product.discountPercent)}% OFF</span>
                </>
              )}
            </div>

            <div className="product-detail-stock" style={{ display: 'flex', gap: 8, flexWrap: 'wrap' }}>
              {product.stockQuantity > 0
                ? <span className="badge badge-green"><MdCheckCircle size={13} style={{ verticalAlign: 'middle', marginRight: 4 }} />In Stock ({product.stockQuantity} available)</span>
                : <span className="badge badge-red"><MdCancel size={13} style={{ verticalAlign: 'middle', marginRight: 4 }} />Out of Stock</span>
              }
              {product.weightGrams > 0 && (
                <span className="badge badge-blue" style={{ display: 'flex', alignItems: 'center', gap: 4 }}>
                  <MdFitnessCenter size={13} style={{ verticalAlign: 'middle', marginRight: 4 }} />
                  {product.weightGrams >= 1000 ? `${(product.weightGrams / 1000).toFixed(2)} kg` : `${product.weightGrams}g`}
                </span>
              )}
            </div>

            {product.stockQuantity > 0 && (
              <div className="product-detail-quantity">
                <label className="form-label">Quantity</label>
                <div className="quantity-selector">
                  <button onClick={() => setQuantity(q => Math.max(1, q - 1))} className="qty-btn">−</button>
                  <span className="qty-value">{quantity}</span>
                  <button onClick={() => setQuantity(q => Math.min(product.stockQuantity, q + 1))} className="qty-btn">+</button>
                </div>
              </div>
            )}

            <button className="btn btn-primary btn-lg btn-full"
              style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 8 }}
              onClick={handleAddToCart}
              disabled={product.stockQuantity === 0 || adding}>
              <MdShoppingCart size={20} />
              {adding ? 'Adding...' : product.stockQuantity === 0 ? 'Out of Stock' : 'Add to Cart'}
            </button>

            <div className="divider" />

            <div className="product-detail-desc">
              <h3>About this product</h3>
              <p>{product.description}</p>
            </div>
          </div>
        </div>

        {/* ── Frequently Bought Together ── */}
        {/* ── Frequently Bought Together ── */}
        {product.category?.id && (
        <FrequentlyBoughtTogether
          categoryId={product.category.id}
          parentCategoryId={product.category.parentId || product.category.id}
        />
        )}

        {/* ── You May Also Like ── */}
        {suggestions.length > 0 && (
          <div className="suggestions-section">
            <div className="section-header" style={{ marginBottom: 'var(--space-6)' }}>
              <h2 className="section-title">You May Also Like</h2>
              <a href={`/products?categoryId=${product.category?.id}`} className="btn btn-ghost btn-sm">
                View All →
              </a>
            </div>
            <HorizontalScroll>
              {suggestions.map(p => (
                <div key={p.id} className="product-card-scroll-wrap">
                  <ProductCard product={p} />
                </div>
              ))}
            </HorizontalScroll>
          </div>
        )}

        {/* ── Reviews Section ── */}
        <ProductReviews productId={product.id} />
      </div>
    </div>
  );
};

export default ProductDetailPage;