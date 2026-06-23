// src/pages/user/OrdersPage.jsx
import { useState, useEffect, useCallback } from 'react';
import { Link, useLocation } from 'react-router-dom';
import { orderApi } from '../../api/apiCollections';
import Loader from '../../components/common/Loader';
import toast from 'react-hot-toast';
import { MdInfo, MdInventory2, MdImage, MdLock, MdLocalShipping } from 'react-icons/md';
import './OrdersPage.css';

const statusBadge = {
  PENDING:        'badge-red',
  CONFIRMED:      'badge-blue',
  PROCESSING:     'badge-purple',
  SHIPPED:        'badge-gold',
  DELIVERED:      'badge-green',
  CANCELLED:      'badge-red',
  PAYMENT_FAILED: 'badge-red',
};

const OrdersPage = () => {
  const location = useLocation();
  const [orders, setOrders] = useState([]);
  const [pagination, setPagination] = useState({});
  const [loading, setLoading] = useState(true);
  const [page, setPage] = useState(0);
  const [cancellingId, setCancellingId] = useState(null);
  
  // ── Catch the trigger flag from CheckoutPage ──
  const [showStars, setShowStars] = useState(location.state?.showStars || false);

  const fetchOrders = useCallback(async () => {
    setLoading(true);
    try {
      const res = await orderApi.getMyOrders({ page, size: 10 });
      setOrders(res.data.data?.content || []);
      setPagination(res.data.data || {});
    } catch { toast.error('Failed to load orders'); }
    finally { setLoading(false); }
  }, [page]);

  useEffect(() => { fetchOrders(); }, [fetchOrders]);

  // ── Handle Animation Cleanup ──
  useEffect(() => {
    if (showStars) {
      // Clear router state so it doesn't replay on page refresh
      window.history.replaceState({}, document.title);
      
      // Remove animation from DOM after 4 seconds (3.5s animation + 0.5s fade)
      const timer = setTimeout(() => setShowStars(false), 4000);
      return () => clearTimeout(timer);
    }
  }, [showStars]);

  const handleCancel = async (orderId) => {
    if (!window.confirm('Cancel this order?')) return;
    setCancellingId(orderId);
    try {
      await orderApi.cancelOrder(orderId);
      toast.success('Order cancelled');
      fetchOrders();
    } catch (err) {
      toast.error(err.response?.data?.message || 'Cannot cancel this order');
    } finally { setCancellingId(null); }
  };

  if (loading) return <Loader fullPage />;

  return (
    <div className="page-wrapper">
      
      {/* ── EPIC SUCCESS OVERLAY ── */}
      {showStars && (
        <div className="epic-success-overlay">
          {/* Falling Glowing Stars */}
          <div className="stars-container">
            {[...Array(40)].map((_, i) => {
              const size = Math.random() * 2 + 0.5; // Random size
              return (
                <div 
                  key={i} 
                  className="epic-star" 
                  style={{ 
                    left: `${Math.random() * 100}%`, 
                    animationDelay: `${Math.random() * 3}s`,
                    animationDuration: `${2 + Math.random() * 3}s`,
                    fontSize: `${size}rem`,
                    opacity: Math.random() * 0.8 + 0.2
                  }}
                >
                  ★
                </div>
              );
            })}
          </div>

          {/* The Glowing Skewed Banner */}
          <div className="epic-yellow-banner">
            <div className="epic-banner-content">
              <h1>CONGRATULATIONS !!</h1>
              <p>Your order has been placed</p>
            </div>
          </div>
        </div>
      )}

      {/* ── MAIN ORDERS CONTENT ── */}
      <div className="container orders-page">
        <h1 className="orders-title">My Orders</h1>

        {/* Cancellation policy notice */}
        <div style={{
          background: 'rgba(233,69,96,0.08)',
          border: '1px solid rgba(233,69,96,0.25)',
          borderRadius: 8,
          padding: '10px 16px',
          marginBottom: 'var(--space-6)',
          fontSize: 'var(--text-sm)',
          color: 'var(--text-muted)',
          display: 'flex',
          alignItems: 'center',
          gap: 8,
        }}>
          <MdInfo size={18} color="var(--accent-primary)" />
          <span>
            Orders can be cancelled only when in <strong>Pending</strong> or <strong>Confirmed</strong> status.
            Once an order moves to Processing, cancellation is not possible.&nbsp;
            Need help? Email us at{' '}
            <a href="mailto:zenenationstore@gmail.com" style={{ color: 'var(--accent-primary)' }}>
              zenenationstore@gmail.com
            </a>
          </span>
        </div>

        {orders.length === 0 ? (
          <div className="empty-state">
            <div className="empty-state-icon"><MdInventory2 size={64} color="var(--text-muted)" /></div>
            <p className="empty-state-title">No orders yet</p>
            <p className="empty-state-desc">Start shopping to see your orders here</p>
            <Link to="/products" className="btn btn-primary" style={{ marginTop: '1rem' }}>
              Shop Now
            </Link>
          </div>
        ) : (
          <>
            <div className="orders-list">
              {orders.map(order => (
                <div key={order.id} className="order-card">
                  <div className="order-card-header">
                    <div>
                      <span className="order-number">{order.orderNumber}</span>
                      <span className="text-muted text-xs" style={{ marginLeft: 12 }}>
                        {new Date(order.createdAt).toLocaleDateString('en-IN', { day: 'numeric', month: 'long', year: 'numeric' })}
                      </span>
                    </div>
                    <div style={{ display: 'flex', gap: 8, alignItems: 'center' }}>
                      <span className={`badge ${statusBadge[order.status] || 'badge-blue'}`}>
                        {order.status}
                      </span>
                      <span className={`badge ${order.paymentStatus === 'PAID' ? 'badge-green' : 'badge-red'}`}>
                        {order.paymentMethod} · {order.paymentStatus}
                      </span>
                    </div>
                  </div>

                  {/* Items preview */}
                  <div className="order-card-items">
                    {order.orderItems?.slice(0, 3).map(item => (
                      <div key={item.id} className="order-item-preview">
                        {item.productImageUrl
                          ? <img src={item.productImageUrl} alt={item.productName} />
                          : <div className="order-item-no-img"><MdImage size={24} color="var(--text-muted)" /></div>
                        }
                        <div>
                          <p className="order-item-name">{item.productName}</p>
                          <p className="text-xs text-muted">Qty: {item.quantity} × ₹{Number(item.priceAtPurchase).toLocaleString('en-IN')}</p>
                        </div>
                      </div>
                    ))}
                    {order.orderItems?.length > 3 && (
                      <p className="text-xs text-muted" style={{ padding: 'var(--space-2)' }}>
                        +{order.orderItems.length - 3} more items
                      </p>
                    )}
                  </div>

                  <div className="order-card-footer">
                    <div style={{ display: 'flex', flexDirection: 'column', gap: '4px' }}>
                      <div>
                        <span className="text-muted text-sm">Total: </span>
                        <span className="text-gold" style={{ fontWeight: 700, fontSize: 'var(--text-lg)' }}>
                          ₹{Number(order.totalAmount).toLocaleString('en-IN')}
                        </span>
                      </div>
                      
                      {/* Static Delivery Note */}
                      {order.status !== 'CANCELLED' && (
                        <div style={{ 
                          display: 'flex', 
                          alignItems: 'center', 
                          gap: '6px', 
                          color: 'var(--accent-green)', 
                          fontSize: 'var(--text-sm)',
                          fontWeight: 500
                        }}>
                          <MdLocalShipping size={16} />
                          <span>Estimated delivery: 5-10 Days</span>
                        </div>
                      )}
                    </div>

                    <div style={{ display: 'flex', gap: 8, alignItems: 'center' }}>
                      {(order.status === 'PENDING' || order.status === 'CONFIRMED') && (
                        <button className="btn btn-sm"
                          style={{ background: 'rgba(244,67,54,0.1)', color: 'var(--accent-red)', border: '1px solid rgba(244,67,54,0.2)' }}
                          onClick={() => handleCancel(order.id)}
                          disabled={cancellingId === order.id}>
                          {cancellingId === order.id ? 'Cancelling...' : 'Cancel Order'}
                        </button>
                      )}
                      {(order.status === 'PROCESSING' || order.status === 'SHIPPED' || order.status === 'DELIVERED') && (
                        <span className="text-xs text-muted" style={{ fontStyle: 'italic' }}>
                          <MdLock size={14} style={{ verticalAlign: 'middle', marginRight: 4 }} /> Cannot cancel — already {order.status.toLowerCase()}
                        </span>
                      )}
                    </div>
                  </div>
                </div>
              ))}
            </div>

            {pagination.totalPages > 1 && (
              <div className="pagination" style={{ marginTop: 'var(--space-8)' }}>
                <button className="btn btn-ghost btn-sm" disabled={pagination.isFirst} onClick={() => setPage(p => p - 1)}>← Prev</button>
                <span className="text-muted text-sm">Page {page + 1} of {pagination.totalPages}</span>
                <button className="btn btn-ghost btn-sm" disabled={pagination.isLast} onClick={() => setPage(p => p + 1)}>Next →</button>
              </div>
            )}
          </>
        )}
      </div>
    </div>
  );
};

export default OrdersPage;