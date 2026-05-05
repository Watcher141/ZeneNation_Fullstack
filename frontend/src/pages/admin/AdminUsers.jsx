// src/pages/admin/AdminUsers.jsx
import { useState, useEffect, useCallback } from 'react';
import AdminLayout from '../../components/admin/AdminLayout';
import Loader from '../../components/common/Loader';
import { adminApi } from '../../api/apiCollections';
import toast from 'react-hot-toast';

const AdminUsers = () => {
  const [users, setUsers] = useState([]);
  const [pagination, setPagination] = useState({});
  const [loading, setLoading] = useState(true);
  const [page, setPage] = useState(0);

  const fetchUsers = useCallback(async () => {
    setLoading(true);
    try {
      const res = await adminApi.getAllUsers({ page, size: 15 });
      setUsers(res.data.data?.content || []);
      setPagination(res.data.data || {});
    } catch { toast.error('Failed to load users'); }
    finally { setLoading(false); }
  }, [page]);

  useEffect(() => { fetchUsers(); }, [fetchUsers]);

  const handleToggleStatus = async (user) => {
    const action = user.isActive ? 'deactivate' : 'activate';
    if (!window.confirm(`${action.charAt(0).toUpperCase() + action.slice(1)} this user?`)) return;
    try {
      if (user.isActive) { await adminApi.deactivateUser(user.id); toast.success('User deactivated'); }
      else { await adminApi.activateUser(user.id); toast.success('User activated'); }
      fetchUsers();
    } catch (err) { toast.error(err.response?.data?.message || 'Failed to update user'); }
  };

  if (loading) return <AdminLayout><Loader fullPage /></AdminLayout>;

  return (
    <AdminLayout>
      <div className="admin-page-header">
        <div>
          <h1 className="admin-page-title">Users</h1>
          <p className="admin-page-subtitle">{pagination.totalElements || 0} registered users</p>
        </div>
      </div>

      <div className="admin-table-wrapper">
        <table className="admin-table">
          <thead>
            <tr><th>User</th><th>Email</th><th>Phone</th><th>Role</th><th>Provider</th><th>Joined</th><th>Status</th><th>Action</th></tr>
          </thead>
          <tbody>
            {users.length === 0 ? (
              <tr><td colSpan={8} style={{ textAlign:'center', color:'var(--text-muted)', padding:'2rem' }}>No users found</td></tr>
            ) : users.map(user => (
              <tr key={user.id}>
                <td>
                  <div style={{ display:'flex', alignItems:'center', gap:10 }}>
                    <div style={{ width:36, height:36, borderRadius:'50%', background: user.role === 'ROLE_ADMIN' ? 'var(--accent-primary)' : 'var(--bg-hover)', display:'flex', alignItems:'center', justifyContent:'center', fontWeight:700, fontSize:'var(--text-sm)', color:'white', flexShrink:0 }}>
                      {user.name?.charAt(0).toUpperCase()}
                    </div>
                    <span style={{ fontWeight:600, color:'var(--text-primary)' }}>{user.name}</span>
                  </div>
                </td>
                <td>{user.email}</td>
                <td className="text-muted">{user.phoneNumber || '—'}</td>
                <td><span className={`badge ${user.role === 'ROLE_ADMIN' ? 'badge-red' : 'badge-blue'}`}>{user.role === 'ROLE_ADMIN' ? 'Admin' : 'User'}</span></td>
                <td><span className="badge badge-purple">{user.provider}</span></td>
                <td className="text-muted text-xs">{new Date(user.createdAt).toLocaleDateString('en-IN')}</td>
                <td><span className={`badge ${user.isActive ? 'badge-green' : 'badge-red'}`}>{user.isActive ? 'Active' : 'Inactive'}</span></td>
                <td>
                  {user.role !== 'ROLE_ADMIN' && (
                    <button className="btn btn-sm"
                      style={user.isActive ? { background:'rgba(244,67,54,0.1)', color:'var(--accent-red)', border:'1px solid rgba(244,67,54,0.2)' } : { background:'var(--accent-primary)', color:'white' }}
                      onClick={() => handleToggleStatus(user)}>
                      {user.isActive ? 'Deactivate' : 'Activate'}
                    </button>
                  )}
                </td>
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
    </AdminLayout>
  );
};

export default AdminUsers;