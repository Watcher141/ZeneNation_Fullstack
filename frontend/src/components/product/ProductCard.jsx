// src/components/product/ProductCard.jsx
import { Link } from 'react-router-dom';
import { useCart } from '../../context/CartContext';
import { useAuth } from '../../context/AuthContext';
import { useNavigate } from 'react-router-dom';
import toast from 'react-hot-toast';
import './ProductCard.css';

const ProductCard = ({ product }) => {
  const { addToCart } = useCart();
  const { isAuthenticated } = useAuth();
  const navigate = useNavigate();

  const handleAddToCart = async (e) => {
    e.preventDefault();
    if (!isAuthenticated()) {
      toast.error('Please login to add items to cart');
      navigate('/login');
      return;
    }
    try {
      await addToCart(product.id, 1);
      toast.success(`${product.name} added to cart!`);
    } catch {
      toast.error('Failed to add to cart');
    }
  };

  const hasDiscount = product.discountPercent > 0;

  return (
    <Link to={`/products/${product.slug}`} className="product-card">
      {/* Image */}
      <div className="product-card-image">
        {product.primaryImageUrl ? (
          <img src={product.primaryImageUrl} alt={product.name} loading="lazy" />
        ) : (
          <div className="product-card-no-image">🎌</div>
        )}
        {hasDiscount && (
          <span className="product-card-discount">-{Math.round(product.discountPercent)}%</span>
        )}
        {product.stockQuantity === 0 && (
          <div className="product-card-out">Out of Stock</div>
        )}
      </div>

      {/* Info */}
      <div className="product-card-info">
        <span className="product-card-category">{product.category?.name}</span>
        <h3 className="product-card-name">{product.name}</h3>

        <div className="product-card-pricing">
          <span className="price">₹{Number(product.discountedPrice || product.price).toLocaleString('en-IN')}</span>
          {hasDiscount && (
            <span className="price-original">₹{Number(product.price).toLocaleString('en-IN')}</span>
          )}
        </div>

        <button
          className="product-card-btn btn btn-primary btn-sm btn-full"
          onClick={handleAddToCart}
          disabled={product.stockQuantity === 0}
        >
          {product.stockQuantity === 0 ? 'Out of Stock' : 'Add to Cart'}
        </button>
      </div>
    </Link>
  );
};

export default ProductCard;
