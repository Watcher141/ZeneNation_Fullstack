// src/components/product/ProductCard.jsx
import { useState } from 'react';
import { Link } from 'react-router-dom';
import { useCart } from '../../context/CartContext';
import { useAuth } from '../../context/AuthContext';
import { useNavigate } from 'react-router-dom';
import toast from 'react-hot-toast';
import { MdImage } from 'react-icons/md';
import './ProductCard.css';

const ProductCard = ({ product }) => {
  const { addToCart } = useCart();
  const { isAuthenticated } = useAuth();
  const navigate = useNavigate();
  
  // NEW: State to track if this specific button is currently loading
  const [isAdding, setIsAdding] = useState(false);

  const handleAddToCart = async (e) => {
    e.preventDefault(); // Prevents the <Link> from triggering navigation
    
    // Prevent duplicate clicks if already processing
    if (isAdding) return;

    if (!isAuthenticated()) {
      toast.error('Please login to add items to cart');
      navigate('/login');
      return;
    }
    
    // Lock the button
    setIsAdding(true);
    
    try {
      await addToCart(product.id, 1);
      toast.success(`${product.name} added to cart!`);
    } catch {
      toast.error('Failed to add to cart');
    } finally {
      // Unlock the button regardless of success or failure
      setIsAdding(false);
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
          <div className="product-card-no-image"><MdImage size={36} color="var(--text-muted)" /></div>
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
          // Disabled if out of stock OR currently adding to cart
          disabled={product.stockQuantity === 0 || isAdding}
          style={{ cursor: isAdding ? 'wait' : 'pointer' }}
        >
          {isAdding 
            ? 'Adding...' 
            : product.stockQuantity === 0 
              ? 'Out of Stock' 
              : 'Add to Cart'
          }
        </button>
      </div>
    </Link>
  );
};

export default ProductCard;