// src/pages/admin/AdminCoupons.jsx
import { useState, useEffect, useCallback } from 'react';
import AdminLayout from '../../components/admin/AdminLayout';
import Loader from '../../components/common/Loader';
import { couponApi } from '../../api/apiCollections';
import toast from 'react-hot-toast';

const emptyForm = {
  code:'', description:'', discountType:'PERCENTAGE', discountValue:'',
  minimumOrderAmount:'0', maximumDiscount:'', usageLimit:'', perUserLimit:'1',
  isActive:true, validFrom:'', validUntil:'',
};

const AdminCoupons = () => {
  const [coupons, setCoupons] = useState([]);
  const [pagination, setPagination] = useState({});
  const [loading, setLoading] = useState(true);
  const [showModal, setShowModal] = useState(false);
  const [editing, setEditing] = useState(null);
  const [form, setForm] = useState(emptyForm);
  const [saving, setSaving] = useState(false);
  const [page, setPage] = useState(0);

  const fetchCoupons = useCallback(async () => {
    setLoading(true);
    try {
      const res = await couponApi.getAllAdmin({ page, size: 10 });
      setCoupons(res.data.data?.content || []);
      setPagination(res.data.data || {});
    } catch { toast.error('Failed to load coupons'); }
    finally { setLoading(false); }
  }, [page]);

  useEffect(() => { fetchCoupons(); }, [fetchCoupons]);

  const openCreate = () => { setEditing(null); setForm(emptyForm); setShowModal(true); };
  const openEdit = (c) => {
    setEditing(c);
    setForm({
      code: c.code, description: c.description || '',
      discountType: c.discountType, discountValue: c.discountValue,
      minimumOrderAmount: c.minimumOrderAmount || '0',
      maximumDiscount: c.maximumDiscount || '', usageLimit: c.usageLimit || '',
      perUserLimit: c.perUserLimit || '1', isActive: c.isActive,
      validFrom: c.validFrom ? c.validFrom.slice(0,16) : '',
      validUntil: c.validUntil ? c.validUntil.slice(0,16) : '',
    });
    setShowModal(true);
  };
  const closeModal = () => { setShowModal(false); setEditing(null); };

  const handleSave = async (e) => {
    e.preventDefault();
    setSaving(true);
    try {
      const payload = {
        ...form,
        discountValue: Number(form.discountValue),
        minimumOrderAmount: Number(form.minimumOrderAmount),
        maximumDiscount: form.maximumDiscount ? Number(form.maximumDiscount) : null,
        usageLimit: form.usageLimit ? Number(form.usageLimit) : null,
        perUserLimit: Number(form.perUserLimit),
        validFrom: form.validFrom || null,
        validUntil: form.validUntil || null,
      };
      if (editing) { await couponApi.updateCoupon(editing.id, payload); toast.success('Coupon updated!'); }
      else { await couponApi.createCoupon(payload); toast.success('Coupon created!'); }
      closeModal(); fetchCoupons();
    } catch (err) { toast.error(err.response?.data?.message || 'Failed to save'); }
    finally { setSaving(false); }
  };

  const handleToggle = async (id) => {
    try { await couponApi.toggleCoupon(id); toast.success('Updated'); fetchCoupons(); }
    catch { toast.error('Failed'); }
  };

  const handleDelete = async (id) => {
    if (!window.confirm('Delete this coupon?')) return;
    try { await couponApi.deleteCoupon(id); toast.success('Deleted'); fetchCoupons(); }
    catch { toast.error('Failed to delete'); }
  };

  if (loading) return <AdminLayout><Loader fullPage /></AdminLayout>;

  return (
    <AdminLayout>
      <div className="admin-page-header">
        <div>
          <h1 className="admin-page-title">Coupons</h1>
          <p className="admin-page-subtitle">{pagination.totalElements || 0} coupons total</p>
        </div>
        <button className="btn btn-primary" onClick={openCreate}>+ Create Coupon</button>
      </div>

      <div className="admin-table-wrapper">
        <table className="admin-table">
          <thead>
            <tr><th>Code</th><th>Type</th><th>Value</th><th>Min Order</th><th>Usage</th><th>Valid Until</th><th>Status</th><th>Actions</th></tr>
          </thead>
          <tbody>
            {coupons.length === 0 ? (
              <tr><td colSpan={8} style={{ textAlign:'center', color:'var(--text-muted)', padding:'2rem' }}>No coupons yet</td></tr>
            ) : coupons.map(c => (
              <tr key={c.id}>
                <td>
                  <span style={{ fontWeight:700, color:'var(--accent-secondary)', letterSpacing:1 }}>{c.code}</span>
                  {c.description && <div className="text-xs text-muted">{c.description}</div>}
                </td>
                <td><span className="badge badge-blue">{c.discountType}</span></td>
                <td className="text-gold" style={{ fontWeight:700 }}>
                  {c.discountType === 'PERCENTAGE' ? `${c.discountValue}%` : `₹${c.discountValue}`}
                  {c.maximumDiscount && <div className="text-xs text-muted">Max ₹{c.maximumDiscount}</div>}
                </td>
                <td>₹{c.minimumOrderAmount}</td>
                <td>
                  <div>{c.usedCount} / {c.usageLimit || '∞'}</div>
                  <div className="text-xs text-muted">Per user: {c.perUserLimit}</div>
                </td>
                <td className="text-muted text-xs">{c.validUntil ? new Date(c.validUntil).toLocaleDateString('en-IN') : '∞'}</td>
                <td>
                  <span className={`badge ${c.isActive && c.isCurrentlyValid ? 'badge-green' : 'badge-red'}`}>
                    {c.isActive ? (c.isCurrentlyValid ? 'Active' : 'Expired') : 'Inactive'}
                  </span>
                </td>
                <td>
                  <div style={{ display:'flex', gap:6 }}>
                    <button className="btn btn-ghost btn-sm" onClick={() => openEdit(c)}>Edit</button>
                    <button className="btn btn-ghost btn-sm" onClick={() => handleToggle(c.id)}>{c.isActive ? 'Disable' : 'Enable'}</button>
                    <button className="btn btn-sm" style={{ background:'rgba(244,67,54,0.1)', color:'var(--accent-red)', border:'1px solid rgba(244,67,54,0.2)' }}
                      onClick={() => handleDelete(c.id)}>Del</button>
                  </div>
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

      {showModal && (
        <div className="modal-overlay" onClick={closeModal}>
          <div className="modal" style={{ maxWidth:600 }} onClick={e => e.stopPropagation()}>
            <div className="modal-header">
              <h3 className="modal-title">{editing ? 'Edit Coupon' : 'Create Coupon'}</h3>
              <button className="modal-close" onClick={closeModal}>✕</button>
            </div>
            <form onSubmit={handleSave}>
              <div className="modal-body">
                <div style={{ display:'grid', gridTemplateColumns:'1fr 1fr', gap:'var(--space-4)' }}>
                  <div className="form-group">
                    <label className="form-label">Coupon Code *</label>
                    <input className="form-input" value={form.code}
                      onChange={e => setForm({ ...form, code: e.target.value.toUpperCase() })}
                      required placeholder="SAVE20" style={{ textTransform:'uppercase' }} />
                  </div>
                  <div className="form-group">
                    <label className="form-label">Discount Type *</label>
                    <select className="form-select" value={form.discountType} onChange={e => setForm({ ...form, discountType: e.target.value })}>
                      <option value="PERCENTAGE">Percentage (%)</option>
                      <option value="FLAT">Flat (₹)</option>
                    </select>
                  </div>
                  <div className="form-group">
                    <label className="form-label">Discount Value *</label>
                    <input className="form-input" type="number" min="0.01" step="0.01" value={form.discountValue}
                      onChange={e => setForm({ ...form, discountValue: e.target.value })} required
                      placeholder={form.discountType === 'PERCENTAGE' ? '20' : '100'} />
                  </div>
                  <div className="form-group">
                    <label className="form-label">Min Order Amount (₹)</label>
                    <input className="form-input" type="number" min="0" value={form.minimumOrderAmount}
                      onChange={e => setForm({ ...form, minimumOrderAmount: e.target.value })} placeholder="500" />
                  </div>
                  {form.discountType === 'PERCENTAGE' && (
                    <div className="form-group">
                      <label className="form-label">Max Discount (₹)</label>
                      <input className="form-input" type="number" min="0" value={form.maximumDiscount}
                        onChange={e => setForm({ ...form, maximumDiscount: e.target.value })} placeholder="300" />
                    </div>
                  )}
                  <div className="form-group">
                    <label className="form-label">Total Usage Limit</label>
                    <input className="form-input" type="number" min="1" value={form.usageLimit}
                      onChange={e => setForm({ ...form, usageLimit: e.target.value })} placeholder="Empty = unlimited" />
                  </div>
                  <div className="form-group">
                    <label className="form-label">Per User Limit</label>
                    <input className="form-input" type="number" min="1" value={form.perUserLimit}
                      onChange={e => setForm({ ...form, perUserLimit: e.target.value })} placeholder="1" />
                  </div>
                  <div className="form-group">
                    <label className="form-label">Valid From</label>
                    <input className="form-input" type="datetime-local" value={form.validFrom}
                      onChange={e => setForm({ ...form, validFrom: e.target.value })} />
                  </div>
                  <div className="form-group">
                    <label className="form-label">Valid Until</label>
                    <input className="form-input" type="datetime-local" value={form.validUntil}
                      onChange={e => setForm({ ...form, validUntil: e.target.value })} />
                  </div>
                </div>
                <div className="form-group">
                  <label className="form-label">Description</label>
                  <input className="form-input" value={form.description}
                    onChange={e => setForm({ ...form, description: e.target.value })} placeholder="e.g. 20% off for new users" />
                </div>
                <label style={{ display:'flex', alignItems:'center', gap:8, cursor:'pointer' }}>
                  <input type="checkbox" checked={form.isActive} onChange={e => setForm({ ...form, isActive: e.target.checked })} />
                  <span className="form-label" style={{ margin:0 }}>Active</span>
                </label>
              </div>
              <div className="modal-footer">
                <button type="button" className="btn btn-ghost" onClick={closeModal}>Cancel</button>
                <button type="submit" className="btn btn-primary" disabled={saving}>{saving ? 'Saving...' : editing ? 'Update' : 'Create'}</button>
              </div>
            </form>
          </div>
        </div>
      )}
    </AdminLayout>
  );
};

export default AdminCoupons;