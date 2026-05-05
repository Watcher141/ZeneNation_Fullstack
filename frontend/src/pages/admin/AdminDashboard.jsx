// src/pages/admin/AdminDashboard.jsx
import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import AdminLayout from '../../components/admin/AdminLayout';
import Loader from '../../components/common/Loader';
import { adminApi, orderApi } from '../../api/apiCollections';
import {
  MdShoppingCart,
  MdHourglassEmpty,
  MdLocalShipping,
  MdCheckCircle,
  MdAttachMoney,
  MdCalendarToday,
  MdInventory2,
  MdWarning,
  MdCategory,
  MdPeople,
  MdPersonAdd,
  MdCancel,
} from 'react-icons/md';
import './AdminDashboard.css';

const getStatusBadge = (status) => {
  const map = {
    PENDING:        'badge-red',
    CONFIRMED:      'badge-blue',
    PROCESSING:     'badge-purple',
    SHIPPED:        'badge-gold',
    DELIVERED:      'badge-green',
    CANCELLED:      'badge-red',
    PAYMENT_FAILED: 'badge-red',
  };
  return map[status] || 'badge-blue';
};

const AdminDashboard = () => {
  const [stats, setStats] = useState(null);
  const [recentOrders, setRecentOrders] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    Promise.all([
      adminApi.getDashboard(),
      orderApi.getAllOrders({ page: 0, size: 5 }),
    ]).then(([statsRes, ordersRes]) => {
      setStats(statsRes.data.data);
      setRecentOrders(ordersRes.data.data?.content || []);
    }).catch(() => {}).finally(() => setLoading(false));
  }, []);

  if (loading) return <AdminLayout><Loader fullPage /></AdminLayout>;

  const statCards = [
    { Icon: MdShoppingCart,    label: 'Total Orders',    value: stats?.totalOrders || 0,     accent: 'accent' },
    { Icon: MdHourglassEmpty,  label: 'Pending Orders',  value: stats?.pendingOrders || 0,   accent: 'red'    },
    { Icon: MdLocalShipping,   label: 'Shipped Orders',  value: stats?.shippedOrders || 0,   accent: 'blue'   },
    { Icon: MdCheckCircle,     label: 'Delivered',       value: stats?.deliveredOrders || 0, accent: 'green'  },
    { Icon: MdAttachMoney,     label: 'Total Revenue',   value: `₹${Number(stats?.totalRevenue || 0).toLocaleString('en-IN')}`, accent: 'gold' },
    { Icon: MdCalendarToday,   label: 'Today Revenue',   value: `₹${Number(stats?.todayRevenue || 0).toLocaleString('en-IN')}`, accent: 'gold' },
    { Icon: MdInventory2,      label: 'Total Products',  value: stats?.totalProducts || 0,   accent: 'accent' },
    { Icon: MdWarning,         label: 'Out of Stock',    value: stats?.outOfStockProducts || 0, accent: 'red' },
    { Icon: MdCategory,        label: 'Categories',      value: stats?.totalCategories || 0, accent: 'blue'  },
    { Icon: MdPeople,          label: 'Total Users',     value: stats?.totalUsers || 0,      accent: 'green'  },
    { Icon: MdPersonAdd,       label: 'New Users Today', value: stats?.newUsersToday || 0,   accent: 'accent' },
    { Icon: MdCancel,          label: 'Cancelled',       value: stats?.cancelledOrders || 0, accent: 'red'    },
  ];

  return (
    <AdminLayout>
      <div className="admin-page-header">
        <div>
          <h1 className="admin-page-title">Dashboard</h1>
          <p className="admin-page-subtitle">Welcome back! Here's what's happening.</p>
        </div>
        <Link to="/admin/orders" className="btn btn-primary btn-sm">View All Orders</Link>
      </div>

      {/* Stat Cards */}
      <div className="admin-stats-grid">
        {statCards.map((s, i) => (
          <div key={i} className={`stat-card stat-card-${s.accent}`}>
            <div className="stat-card-icon-wrap">
              <s.Icon size={28} />
            </div>
            <div className="stat-card-value">{s.value}</div>
            <div className="stat-card-label">{s.label}</div>
          </div>
        ))}
      </div>

      {/* Recent Orders */}
      <div>
        <div className="admin-page-header" style={{ marginBottom: 'var(--space-4)' }}>
          <h2 style={{ fontSize: 'var(--text-lg)', fontWeight: 600 }}>Recent Orders</h2>
          <Link to="/admin/orders" className="btn btn-ghost btn-sm">View All →</Link>
        </div>
        <div className="admin-table-wrapper">
          <table className="admin-table">
            <thead>
              <tr>
                <th>Order #</th>
                <th>Customer</th>
                <th>Total</th>
                <th>Payment</th>
                <th>Status</th>
                <th>Date</th>
                <th>Action</th>
              </tr>
            </thead>
            <tbody>
              {recentOrders.length === 0 ? (
                <tr>
                  <td colSpan={7} style={{ textAlign: 'center', color: 'var(--text-muted)', padding: '2rem' }}>
                    No orders yet
                  </td>
                </tr>
              ) : recentOrders.map(order => (
                <tr key={order.id}>
                  <td><span className="text-accent" style={{ fontWeight: 600 }}>{order.orderNumber}</span></td>
                  <td>
                    <div style={{ fontWeight: 500, color: 'var(--text-primary)' }}>{order.userName}</div>
                    <div className="text-xs text-muted">{order.userEmail}</div>
                  </td>
                  <td className="text-gold" style={{ fontWeight: 700 }}>₹{Number(order.totalAmount).toLocaleString('en-IN')}</td>
                  <td><span className="badge badge-blue">{order.paymentMethod}</span></td>
                  <td><span className={`badge ${getStatusBadge(order.status)}`}>{order.status}</span></td>
                  <td className="text-muted">{new Date(order.createdAt).toLocaleDateString('en-IN')}</td>
                  <td><Link to="/admin/orders" className="btn btn-ghost btn-sm">View</Link></td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </AdminLayout>
  );
};

export default AdminDashboard;