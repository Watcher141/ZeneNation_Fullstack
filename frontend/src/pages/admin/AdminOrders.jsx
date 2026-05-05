// src/pages/admin/AdminOrders.jsx
import { useState, useEffect, useCallback } from 'react';
import AdminLayout from '../../components/admin/AdminLayout';
import Loader from '../../components/common/Loader';
import { orderApi } from '../../api/apiCollections';
import toast from 'react-hot-toast';

const ORDER_STATUSES = ['PENDING','CONFIRMED','PROCESSING','SHIPPED','DELIVERED','CANCELLED'];

const statusBadge = {
  PENDING:'badge-red', CONFIRMED:'badge-blue', PROCESSING:'badge-purple',
  SHIPPED:'badge-gold', DELIVERED:'badge-green', CANCELLED:'badge-red', PAYMENT_FAILED:'badge-red',
};

const AdminOrders = () => {
  const [orders, setOrders] = useState([]);
  const [pagination, setPagination] = useState({});
  const [loading, setLoading] = useState(true);
  const [page, setPage] = useState(0);
  const [filterStatus, setFilterStatus] = useState('');
  const [selectedOrder, setSelectedOrder] = useState(null);
  const [newStatus, setNewStatus] = useState('');
  const [adminNote, setAdminNote] = useState('');
  const [updating, setUpdating] = useState(false);

  const fetchOrders = useCallback(async () => {
    setLoading(true);
    try {
      const params = { page, size: 15 };
      if (filterStatus) params.status = filterStatus;
      const res = await orderApi.getAllOrders(params);
      setOrders(res.data.data?.content || []);
      setPagination(res.data.data || {});
    } catch { toast.error('Failed to load orders'); }
    finally { setLoading(false); }
  }, [page, filterStatus]);

  useEffect(() => { fetchOrders(); }, [fetchOrders]);

  const openOrder = async (order) => {
    try {
      const res = await orderApi.getOrderByIdAdmin(order.id);
      setSelectedOrder(res.data.data);
      setNewStatus(res.data.data.status);
      setAdminNote(res.data.data.adminNote || '');
    } catch { toast.error('Failed to load order details'); }
  };

  const handleUpdateStatus = async () => {
    if (!selectedOrder) return;
    setUpdating(true);
    try {
      await orderApi.updateOrderStatus(selectedOrder.id, { status: newStatus, adminNote });
      toast.success('Order status updated!');
      setSelectedOrder(null);
      fetchOrders();
    } catch (err) {
      toast.error(err.response?.data?.message || 'Failed to update');
    } finally { setUpdating(false); }
  };

  if (loading) return <AdminLayout><Loader fullPage /></AdminLayout>;

  return (
    <AdminLayout>
      <div className="admin-page-header">
        <div>
          <h1 className="admin-page-title">Orders</h1>
          <p className="admin-page-subtitle">{pagination.totalElements || 0} total orders</p>
        </div>
        <select className="form-select" style={{ width: 'auto' }} value={filterStatus}
          onChange={e => { setFilterStatus(e.target.value); setPage(0); }}>
          <option value="">All Statuses</option>
          {ORDER_STATUSES.map(s => <option key={s} value={s}>{s}</option>)}
        </select>
      </div>

      <div className="admin-table-wrapper">
        <table className="admin-table">
          <thead>
            <tr>
              <th>Order #</th><th>Customer</th><th>Phone</th>
              <th>Items</th><th>Total</th><th>Payment</th>
              <th>Status</th><th>Date</th><th>Action</th>
            </tr>
          </thead>
          <tbody>
            {orders.length === 0 ? (
              <tr><td colSpan={9} style={{ textAlign:'center', color:'var(--text-muted)', padding:'2rem' }}>No orders found</td></tr>
            ) : orders.map(order => (
              <tr key={order.id}>
                <td><span className="text-accent" style={{ fontWeight:600 }}>{order.orderNumber}</span></td>
                <td>
                  <div style={{ fontWeight:500, color:'var(--text-primary)' }}>{order.userName}</div>
                  <div className="text-xs text-muted">{order.userEmail}</div>
                </td>
                <td className="text-muted">{order.deliveryPhone}</td>
                <td>{order.orderItems?.length || '—'}</td>
                <td className="text-gold" style={{ fontWeight:700 }}>₹{Number(order.totalAmount).toLocaleString('en-IN')}</td>
                <td>
                  <div><span className="badge badge-blue">{order.paymentMethod}</span></div>
                  <div style={{ marginTop:4 }}>
                    <span className={`badge ${order.paymentStatus === 'PAID' ? 'badge-green' : 'badge-red'}`}>{order.paymentStatus}</span>
                  </div>
                </td>
                <td><span className={`badge ${statusBadge[order.status] || 'badge-blue'}`}>{order.status}</span></td>
                <td className="text-muted text-xs">{new Date(order.createdAt).toLocaleDateString('en-IN')}</td>
                <td><button className="btn btn-ghost btn-sm" onClick={() => openOrder(order)}>View</button></td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {pagination.totalPages > 1 && (
        <div className="pagination" style={{ marginTop:'var(--space-6)' }}>
          <button className="btn btn-ghost btn-sm" disabled={pagination.isFirst} onClick={() => setPage(p => p-1)}>← Prev</button>
          <span className="text-muted text-sm">Page {page+1} of {pagination.totalPages}</span>
          <button className="btn btn-ghost btn-sm" disabled={pagination.isLast} onClick={() => setPage(p => p+1)}>Next →</button>
        </div>
      )}

      {selectedOrder && (
        <div className="modal-overlay" onClick={() => setSelectedOrder(null)}>
          <div className="modal" style={{ maxWidth:680 }} onClick={e => e.stopPropagation()}>
            <div className="modal-header">
              <h3 className="modal-title">Order {selectedOrder.orderNumber}</h3>
              <button className="modal-close" onClick={() => setSelectedOrder(null)}>✕</button>
            </div>
            <div className="modal-body">
              <div style={{ background:'var(--bg-tertiary)', borderRadius:'var(--radius-md)', padding:'var(--space-4)' }}>
                <div style={{ display:'grid', gridTemplateColumns:'1fr 1fr', gap:'var(--space-3)' }}>
                  <div><span className="text-muted text-sm">Customer</span><p style={{ fontWeight:600 }}>{selectedOrder.userName}</p></div>
                  <div><span className="text-muted text-sm">Email</span><p>{selectedOrder.userEmail}</p></div>
                  <div><span className="text-muted text-sm">Phone</span><p>{selectedOrder.deliveryPhone}</p></div>
                  <div><span className="text-muted text-sm">Payment</span><p>{selectedOrder.paymentMethod} — {selectedOrder.paymentStatus}</p></div>
                </div>
                <div style={{ marginTop:'var(--space-3)' }}>
                  <span className="text-muted text-sm">Delivery Address</span>
                  <p>{selectedOrder.deliveryAddressLine1}, {selectedOrder.deliveryAddressLine2 && selectedOrder.deliveryAddressLine2 + ', '}{selectedOrder.deliveryCity}, {selectedOrder.deliveryState} - {selectedOrder.deliveryPincode}</p>
                </div>
              </div>

              <div>
                <h4 style={{ marginBottom:'var(--space-3)' }}>Items Ordered</h4>
                {selectedOrder.orderItems?.map(item => (
                  <div key={item.id} style={{ display:'flex', justifyContent:'space-between', padding:'var(--space-3)', background:'var(--bg-tertiary)', borderRadius:'var(--radius-md)', marginBottom:'var(--space-2)' }}>
                    <div style={{ display:'flex', gap:'var(--space-3)', alignItems:'center' }}>
                      {item.productImageUrl && <img src={item.productImageUrl} alt="" style={{ width:40, height:40, borderRadius:6, objectFit:'cover' }} />}
                      <div>
                        <p style={{ fontWeight:600, fontSize:'var(--text-sm)' }}>{item.productName}</p>
                        <p className="text-xs text-muted">Qty: {item.quantity} × ₹{Number(item.priceAtPurchase).toLocaleString('en-IN')}</p>
                      </div>
                    </div>
                    <span className="text-gold" style={{ fontWeight:700 }}>₹{Number(item.totalPrice).toLocaleString('en-IN')}</span>
                  </div>
                ))}
              </div>

              <div style={{ background:'var(--bg-tertiary)', borderRadius:'var(--radius-md)', padding:'var(--space-4)' }}>
                <div style={{ display:'flex', justifyContent:'space-between', marginBottom:8 }}><span className="text-muted">Subtotal</span><span>₹{Number(selectedOrder.subtotal).toLocaleString('en-IN')}</span></div>
                <div style={{ display:'flex', justifyContent:'space-between', marginBottom:8 }}><span className="text-muted">Delivery</span><span>₹{Number(selectedOrder.deliveryCharge).toLocaleString('en-IN')}</span></div>
                <div style={{ display:'flex', justifyContent:'space-between', fontWeight:700 }}><span>Total</span><span className="text-gold">₹{Number(selectedOrder.totalAmount).toLocaleString('en-IN')}</span></div>
              </div>

              <div>
                <h4 style={{ marginBottom:'var(--space-3)' }}>Update Status</h4>
                <div style={{ display:'grid', gridTemplateColumns:'1fr 1fr', gap:'var(--space-4)' }}>
                  <div className="form-group">
                    <label className="form-label">New Status</label>
                    <select className="form-select" value={newStatus} onChange={e => setNewStatus(e.target.value)}>
                      {ORDER_STATUSES.map(s => <option key={s} value={s}>{s}</option>)}
                    </select>
                  </div>
                  <div className="form-group">
                    <label className="form-label">Admin Note</label>
                    <input className="form-input" value={adminNote} onChange={e => setAdminNote(e.target.value)} placeholder="Optional note..." />
                  </div>
                </div>
              </div>
            </div>
            <div className="modal-footer">
              <button className="btn btn-ghost" onClick={() => setSelectedOrder(null)}>Close</button>
              <button className="btn btn-primary" onClick={handleUpdateStatus} disabled={updating}>
                {updating ? 'Updating...' : 'Update Status'}
              </button>
            </div>
          </div>
        </div>
      )}
    </AdminLayout>
  );
};

export default AdminOrders;