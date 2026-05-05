// src/pages/public/ProductDetailPage.jsx
import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { productApi } from '../../api/productApi';
import { useCart } from '../../context/CartContext';
import { useAuth } from '../../context/AuthContext';
import Loader from '../../components/common/Loader';
import StarRating from '../../components/product/StarRating';
import ProductReviews from '../../components/product/ProductReviews';
import { MdShoppingCart } from 'react-icons/md';
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
                : <div className="product-detail-no-img">🎌</div>
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

            {/* Rating preview */}
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

            <div className="product-detail-stock">
              {product.stockQuantity > 0
                ? <span className="badge badge-green">✓ In Stock ({product.stockQuantity} available)</span>
                : <span className="badge badge-red">✗ Out of Stock</span>
              }
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

        {/* ── Reviews Section ── */}
        <ProductReviews productId={product.id} />
      </div>
    </div>
  );
};

export default ProductDetailPage;