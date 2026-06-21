// src/pages/user/CartPage.jsx
import { Link, useNavigate } from 'react-router-dom';
import { useCart } from '../../context/CartContext';
import Loader from '../../components/common/Loader';
import toast from 'react-hot-toast';
import { MdShoppingCart, MdImage, MdClose, MdLocalShipping, MdCheckCircle } from 'react-icons/md';
import './CartPage.css';

const CartPage = () => {
  const { cart, loading, updateItem, removeItem, clearCart } = useCart();
  const navigate = useNavigate();

  const handleQtyChange = async (cartItemId, qty) => {
    try {
      await updateItem(cartItemId, qty);
    } catch {
      toast.error('Failed to update quantity');
    }
  };

  const handleRemove = async (cartItemId) => {
    try {
      await removeItem(cartItemId);
      toast.success('Item removed');
    } catch {
      toast.error('Failed to remove item');
    }
  };

  const handleClear = async () => {
    if (!window.confirm('Clear entire cart?')) return;
    try {
      await clearCart();
      toast.success('Cart cleared');
    } catch {
      toast.error('Failed to clear cart');
    }
  };

  if (loading) return <Loader fullPage />;

  const items = cart?.items || [];
  const subtotal = cart?.subtotal || 0;
  //const deliveryCharge = subtotal >= 500 ? 0 : 49;
  const deliveryCharge = 0;
  const total = Number(subtotal) + deliveryCharge;

  return (
    <div className="page-wrapper">
      <div className="container cart-page">
        <h1 className="cart-title">My Cart {items.length > 0 && <span className="text-muted">({cart.totalQuantity} items)</span>}</h1>

        {items.length === 0 ? (
          <div className="empty-state">
            <div className="empty-state-icon"><MdShoppingCart size={64} color="var(--text-muted)" /></div>
            <p className="empty-state-title">Your cart is empty</p>
            <p className="empty-state-desc">Add some amazing anime products!</p>
            <Link to="/products" className="btn btn-primary" style={{ marginTop: '1rem' }}>
              Shop Now
            </Link>
          </div>
        ) : (
          <div className="cart-layout">
            {/* Items */}
            <div className="cart-items">
              <div className="cart-items-header">
                <span>Product</span>
                <button className="btn btn-ghost btn-sm" onClick={handleClear}>Clear All</button>
              </div>

              {items.map(item => (
                <div key={item.cartItemId} className="cart-item">
                  <div className="cart-item-image">
                    {item.primaryImageUrl
                      ? <img src={item.primaryImageUrl} alt={item.productName} />
                      : <span><MdImage size={28} color="var(--text-muted)" /></span>}
                  </div>

                  <div className="cart-item-info">
                    <Link to={`/products/${item.productSlug}`} className="cart-item-name">
                      {item.productName}
                    </Link>
                    <div className="cart-item-price">
                      ₹{Number(item.discountedPrice).toLocaleString('en-IN')} each
                    </div>
                    {!item.isAvailable && (
                      <span className="badge badge-red">No longer available</span>
                    )}
                  </div>

                  <div className="quantity-selector">
                    <button className="qty-btn" onClick={() => handleQtyChange(item.cartItemId, item.quantity - 1)}>−</button>
                    <span className="qty-value">{item.quantity}</span>
                    <button
                      className="qty-btn"
                      onClick={() => handleQtyChange(item.cartItemId, item.quantity + 1)}
                      disabled={item.quantity >= item.availableStock}
                    >+</button>
                  </div>

                  <div className="cart-item-total">
                    ₹{Number(item.totalPrice).toLocaleString('en-IN')}
                  </div>

                  <button className="cart-item-remove" onClick={() => handleRemove(item.cartItemId)}><MdClose size={16} /></button>
                </div>
              ))}
            </div>

            {/* Summary */}
            <div className="cart-summary">
              <h3>Order Summary</h3>
              <div className="summary-row">
                <span>Subtotal</span>
                <span>₹{Number(subtotal).toLocaleString('en-IN')}</span>
              </div>
              <div className="summary-row">
                <span>Delivery</span>
                <span><span className="text-success" style={{ display: 'flex', alignItems: 'center', gap: 4 }}><MdCheckCircle size={14} /> Free Delivery</span></span>
              </div>
              <div className="divider" />
              <div className="summary-row summary-total">
                <span>Total</span>
                <span className="text-gold">₹{total.toLocaleString('en-IN')}</span>
              </div>
              <button
                className="btn btn-primary btn-full btn-lg"
                onClick={() => navigate('/checkout')}
              >
                Proceed to Checkout →
              </button>
              <Link to="/products" className="btn btn-ghost btn-full">
                Continue Shopping
              </Link>
            </div>
          </div>
        )}
      </div>
    </div>
  );
};

export default CartPage;
