// src/pages/user/OrdersPage.jsx
import { useState, useEffect, useCallback } from 'react';
import { Link, useLocation } from 'react-router-dom';
import { orderApi } from '../../api/apiCollections';
import Loader from '../../components/common/Loader';
import toast from 'react-hot-toast';
import { MdInfo, MdInventory2, MdImage, MdLock, MdLocalShipping, MdWarning, MdClose, MdCancel } from 'react-icons/md';
import './OrdersPage.css';

// ── M-05: Added PAYMENT_PENDING to status badge map ──
const statusBadge = {
  PENDING:         'badge-red',
  CONFIRMED:       'badge-blue',
  PROCESSING:      'badge-purple',
  SHIPPED:         'badge-gold',
  DELIVERED:       'badge-green',
  CANCELLED:       'badge-red',
  PAYMENT_FAILED:  'badge-red',
  PAYMENT_PENDING: 'badge-red',   // was missing — caused default 'badge-blue' fallback
};

// ── M-04: In-app Cancel Confirmation Modal ──
const CancelConfirmModal = ({ orderNumber, onConfirm, onCancel, loading }) => (
  <div className="cancel-modal-overlay" role="dialog" aria-modal="true" aria-label="Cancel order confirmation">
    <div className="cancel-modal">
      <div className="cancel-modal-icon">
        <MdWarning size={32} color="var(--accent-red)" />
      </div>
      <h3 className="cancel-modal-title">Cancel Order?</h3>
      <p className="cancel-modal-body">
        Are you sure you want to cancel order <strong>{orderNumber}</strong>?
        This action cannot be undone.
      </p>
      <div className="cancel-modal-actions">
        <button
          className="btn btn-ghost"
          onClick={onCancel}
          disabled={loading}
          id="cancel-modal-keep"
        >
          <MdClose size={16} /> Keep Order
        </button>
        <button
          className="btn"
          style={{ background: 'rgba(244,67,54,0.15)', color: 'var(--accent-red)', border: '1px solid rgba(244,67,54,0.3)' }}
          onClick={onConfirm}
          disabled={loading}
          id="cancel-modal-confirm"
        >
          <MdCancel size={16} /> {loading ? 'Cancelling…' : 'Yes, Cancel'}
        </button>
      </div>
    </div>
  </div>
);

const OrdersPage = () => {
  const location = useLocation();
  const [orders, setOrders] = useState([]);
  const [pagination, setPagination] = useState({});
  const [loading, setLoading] = useState(true);
  const [page, setPage] = useState(0);
  const [cancellingId, setCancellingId] = useState(null);

  // ── M-04: Modal state instead of window.confirm ──
  const [confirmModal, setConfirmModal] = useState(null); // { orderId, orderNumber }

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
      window.history.replaceState({}, document.title);
      const timer = setTimeout(() => setShowStars(false), 4000);
      return () => clearTimeout(timer);
    }
  }, [showStars]);

  // ── M-04: Open modal instead of window.confirm ──
  const requestCancel = (orderId, orderNumber) => {
    setConfirmModal({ orderId, orderNumber });
  };

  const handleConfirmCancel = async () => {
    if (!confirmModal) return;
    const { orderId } = confirmModal;
    setCancellingId(orderId);
    try {
      await orderApi.cancelOrder(orderId);
      toast.success('Order cancelled');
      setConfirmModal(null);
      fetchOrders();
    } catch (err) {
      toast.error(err.response?.data?.message || 'Cannot cancel this order');
    } finally {
      setCancellingId(null);
    }
  };

  if (loading) return <Loader fullPage />;

  return (
    <div className="page-wrapper">

      {/* ── M-04: Cancel Confirmation Modal ── */}
      {confirmModal && (
        <CancelConfirmModal
          orderNumber={confirmModal.orderNumber}
          onConfirm={handleConfirmCancel}
          onCancel={() => setConfirmModal(null)}
          loading={cancellingId === confirmModal.orderId}
        />
      )}

      {/* ── EPIC SUCCESS OVERLAY ── */}
      {showStars && (
        <div className="epic-success-overlay">
          <div className="stars-container">
            {[...Array(40)].map((_, i) => {
              const size = Math.random() * 2 + 0.5;
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
                    <div style={{ display: 'flex', gap: 8, alignItems: 'center', flexWrap: 'wrap' }}>
                      <span className={`badge ${statusBadge[order.status] || 'badge-blue'}`}>
                        {/* M-05: Show friendlier label for PAYMENT_PENDING */}
                        {order.status === 'PAYMENT_PENDING' ? 'Payment Pending' : order.status}
                      </span>
                      <span className={`badge ${order.paymentStatus === 'PAID' ? 'badge-green' : 'badge-red'}`}>
                        {order.paymentMethod} · {order.paymentStatus}
                      </span>
                    </div>
                  </div>

                  {/* M-05: Informational banner for PAYMENT_PENDING orders */}
                  {order.status === 'PAYMENT_PENDING' && (
                    <div style={{
                      background: 'rgba(244,67,54,0.07)',
                      borderBottom: '1px solid rgba(244,67,54,0.2)',
                      padding: '8px 20px',
                      display: 'flex',
                      alignItems: 'center',
                      gap: 8,
                      fontSize: 'var(--text-sm)',
                      color: 'var(--accent-red)',
                    }}>
                      <MdWarning size={16} />
                      <span>
                        This order's payment was not completed. If you were charged, please contact us at{' '}
                        <a href="mailto:zenenationstore@gmail.com" style={{ color: 'var(--accent-primary)' }}>
                          zenenationstore@gmail.com
                        </a>
                        {' '}with your order number.
                      </span>
                    </div>
                  )}

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

                      {/* Static Delivery Note — hide for terminal / payment-issue states */}
                      {order.status !== 'CANCELLED'
                        && order.status !== 'PAYMENT_FAILED'
                        && order.status !== 'PAYMENT_PENDING' && (
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
                      {/* M-04: Use in-app modal instead of window.confirm */}
                      {(order.status === 'PENDING' || order.status === 'CONFIRMED') && (
                        <button className="btn btn-sm"
                          style={{ background: 'rgba(244,67,54,0.1)', color: 'var(--accent-red)', border: '1px solid rgba(244,67,54,0.2)' }}
                          onClick={() => requestCancel(order.id, order.orderNumber)}
                          disabled={cancellingId === order.id}
                        >
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