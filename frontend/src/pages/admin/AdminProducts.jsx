// src/pages/admin/AdminProducts.jsx
import { useState, useEffect, useCallback } from 'react';
import AdminLayout from '../../components/admin/AdminLayout';
import Loader from '../../components/common/Loader';
import { productApi } from '../../api/productApi';
import { categoryApi } from '../../api/categoryApi';
import toast from 'react-hot-toast';

const emptyForm = {
  name: '', description: '', tagline: '', price: '', discountPercent: '0',
  stockQuantity: '', categoryId: '', isActive: true,
  isPreorder: false, estimatedShipDate: '', preorderNote: '',
};

const AdminProducts = () => {
  const [products, setProducts] = useState([]);
  const [categories, setCategories] = useState([]);
  const [pagination, setPagination] = useState({});
  const [loading, setLoading] = useState(true);
  const [showModal, setShowModal] = useState(false);
  const [editing, setEditing] = useState(null);
  const [form, setForm] = useState(emptyForm);
  const [saving, setSaving] = useState(false);
  const [page, setPage] = useState(0);
  const [imageFiles, setImageFiles] = useState([]);
  const [uploadingId, setUploadingId] = useState(null);

  const fetchProducts = useCallback(async () => {
    setLoading(true);
    try {
      const res = await productApi.getAllAdmin({ page, size: 10 });
      setProducts(res.data.data?.content || []);
      setPagination(res.data.data || {});
    } catch { toast.error('Failed to load products'); }
    finally { setLoading(false); }
  }, [page]);

  useEffect(() => {
    categoryApi.getAll().then(r => setCategories(r.data.data || [])).catch(() => {});
  }, []);

  useEffect(() => { fetchProducts(); }, [fetchProducts]);

  const openCreate = () => { setEditing(null); setForm(emptyForm); setShowModal(true); };
  const openEdit = (p) => {
    setEditing(p);
    setForm({
      name: p.name, description: p.description, tagline: p.tagline || '', price: p.price,
      discountPercent: p.discountPercent || '0', stockQuantity: p.stockQuantity,
      categoryId: p.category?.id || '', isActive: p.isActive,
      isPreorder: p.isPreorder || false,
      estimatedShipDate: p.estimatedShipDate || '',
      preorderNote: p.preorderNote || '',
    });
    setShowModal(true);
  };
  const closeModal = () => { setShowModal(false); setEditing(null); setForm(emptyForm); };

  const handleSave = async (e) => {
    e.preventDefault();
    setSaving(true);
    try {
      const payload = {
        ...form,
        price: Number(form.price),
        discountPercent: Number(form.discountPercent),
        stockQuantity: Number(form.stockQuantity),
        categoryId: Number(form.categoryId),
      };
      if (editing) {
        await productApi.update(editing.id, payload);
        toast.success('Product updated!');
      } else {
        await productApi.create(payload);
        toast.success('Product created!');
      }
      closeModal();
      fetchProducts();
    } catch (err) {
      toast.error(err.response?.data?.message || 'Failed to save');
    } finally { setSaving(false); }
  };

  const handleDelete = async (id) => {
    if (!window.confirm('Delete this product?')) return;
    try {
      await productApi.delete(id);
      toast.success('Product deleted');
      fetchProducts();
    } catch { toast.error('Failed to delete'); }
  };

  const handleToggle = async (id) => {
    try {
      await productApi.toggleVisibility(id);
      toast.success('Visibility updated');
      fetchProducts();
    } catch { toast.error('Failed to update'); }
  };

  const handleImageUpload = async (productId) => {
    if (!imageFiles.length) { toast.error('Select images first'); return; }
    setUploadingId(productId);
    try {
      const formData = new FormData();
      imageFiles.forEach(f => formData.append('images', f));
      await productApi.uploadImages(productId, formData);
      toast.success('Images uploaded!');
      setImageFiles([]);
      fetchProducts();
    } catch { toast.error('Image upload failed'); }
    finally { setUploadingId(null); }
  };

  if (loading) return <AdminLayout><Loader fullPage /></AdminLayout>;

  return (
    <AdminLayout>
      <div className="admin-page-header">
        <div>
          <h1 className="admin-page-title">Products</h1>
          <p className="admin-page-subtitle">{pagination.totalElements || 0} products total</p>
        </div>
        <button className="btn btn-primary" onClick={openCreate}>+ Add Product</button>
      </div>

      <div className="admin-table-wrapper">
        <table className="admin-table">
          <thead>
            <tr>
              <th>Image</th><th>Name</th><th>Category</th><th>Price</th>
              <th>Stock</th><th>Status</th><th>Images</th><th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {products.map(p => (
              <tr key={p.id}>
                <td>
                  {p.primaryImageUrl
                    ? <img src={p.primaryImageUrl} alt={p.name} style={{ width: 48, height: 48, borderRadius: 8, objectFit: 'cover' }} />
                    : <div style={{ width: 48, height: 48, background: 'var(--bg-hover)', borderRadius: 8, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>🎌</div>
                  }
                </td>
                <td style={{ maxWidth: 180 }}>
                  <div style={{ fontWeight: 600, color: 'var(--text-primary)', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>{p.name}</div>
                  <div className="text-xs text-muted">{p.slug}</div>
                </td>
                <td><span className="badge badge-purple">{p.category?.name}</span></td>
                <td>
                  <div className="text-gold">₹{Number(p.discountedPrice || p.price).toLocaleString('en-IN')}</div>
                  {p.discountPercent > 0 && <div className="text-xs text-muted" style={{ textDecoration: 'line-through' }}>₹{Number(p.price).toLocaleString('en-IN')}</div>}
                </td>
                <td>
                  <span className={`badge ${p.stockQuantity === 0 ? 'badge-red' : 'badge-green'}`}>
                    {p.stockQuantity === 0 ? 'Out' : p.stockQuantity}
                  </span>
                </td>
                <td>
                  <span className={`badge ${p.isActive ? 'badge-green' : 'badge-red'}`}>
                    {p.isActive ? 'Active' : 'Hidden'}
                  </span>
                </td>
                <td>
                  <div style={{ display: 'flex', gap: 4, flexDirection: 'column' }}>
                    <input type="file" accept="image/*" multiple style={{ fontSize: 11, maxWidth: 130 }}
                      onChange={e => setImageFiles(Array.from(e.target.files))} />
                    <button className="btn btn-ghost btn-sm" onClick={() => handleImageUpload(p.id)}
                      disabled={uploadingId === p.id}>
                      {uploadingId === p.id ? 'Uploading...' : '↑ Upload'}
                    </button>
                  </div>
                </td>
                <td>
                  <div style={{ display: 'flex', gap: 6, flexWrap: 'wrap' }}>
                    <button className="btn btn-ghost btn-sm" onClick={() => openEdit(p)}>Edit</button>
                    <button className="btn btn-ghost btn-sm" onClick={() => handleToggle(p.id)}>
                      {p.isActive ? 'Hide' : 'Show'}
                    </button>
                    <button className="btn btn-sm"
                      style={{ background: 'rgba(244,67,54,0.1)', color: 'var(--accent-red)', border: '1px solid rgba(244,67,54,0.2)' }}
                      onClick={() => handleDelete(p.id)}>Del</button>
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {pagination.totalPages > 1 && (
        <div className="pagination" style={{ marginTop: 'var(--space-6)' }}>
          <button className="btn btn-ghost btn-sm" disabled={pagination.isFirst} onClick={() => setPage(p => p - 1)}>← Prev</button>
          <span className="text-muted text-sm">Page {page + 1} of {pagination.totalPages}</span>
          <button className="btn btn-ghost btn-sm" disabled={pagination.isLast} onClick={() => setPage(p => p + 1)}>Next →</button>
        </div>
      )}

      {showModal && (
        <div className="modal-overlay" onClick={closeModal}>
          <div className="modal" style={{ maxWidth: 640 }} onClick={e => e.stopPropagation()}>
            <div className="modal-header">
              <h3 className="modal-title">{editing ? 'Edit Product' : 'New Product'}</h3>
              <button className="modal-close" onClick={closeModal}>✕</button>
            </div>
            <form onSubmit={handleSave}>
              <div className="modal-body">
                <div className="form-group">
                  <label className="form-label">Product Name *</label>
                  <input className="form-input" value={form.name}
                    onChange={e => setForm({ ...form, name: e.target.value })} required placeholder="e.g. Naruto Figure 30cm" />
                </div>
                <div className="form-group">
                  <label className="form-label">Description *</label>
                  <textarea className="form-input" rows={3} value={form.description}
                    onChange={e => setForm({ ...form, description: e.target.value })} required
                    placeholder="Product description..." style={{ resize: 'vertical' }} />
                </div>
                <div className="form-group">
                  <label className="form-label">Tagline <span className="text-muted" style={{fontWeight:400}}>(optional — shown below product name)</span></label>
                  <input className="form-input" value={form.tagline}
                    onChange={e => setForm({ ...form, tagline: e.target.value })}
                    placeholder='e.g. "Believe it! — Limited Edition"'
                    maxLength={200} />
                </div>
                <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 'var(--space-4)' }}>
                  <div className="form-group">
                    <label className="form-label">Price (₹) *</label>
                    <input className="form-input" type="number" min="0" step="0.01" value={form.price}
                      onChange={e => setForm({ ...form, price: e.target.value })} required placeholder="999" />
                  </div>
                  <div className="form-group">
                    <label className="form-label">Discount %</label>
                    <input className="form-input" type="number" min="0" max="100" value={form.discountPercent}
                      onChange={e => setForm({ ...form, discountPercent: e.target.value })} placeholder="0" />
                  </div>
                  <div className="form-group">
                    <label className="form-label">Stock Quantity *</label>
                    <input className="form-input" type="number" min="0" value={form.stockQuantity}
                      onChange={e => setForm({ ...form, stockQuantity: e.target.value })} required placeholder="50" />
                  </div>
                  <div className="form-group">
                    <label className="form-label">Category *</label>
                    <select className="form-select" value={form.categoryId}
                      onChange={e => setForm({ ...form, categoryId: e.target.value })} required>
                      <option value="">Select category</option>
                      {categories.map(c => <option key={c.id} value={c.id}>{c.name}</option>)}
                    </select>
                  </div>
                </div>
                <label style={{ display: 'flex', alignItems: 'center', gap: 8, cursor: 'pointer' }}>
                  <input type="checkbox" checked={form.isActive}
                    onChange={e => setForm({ ...form, isActive: e.target.checked })} />
                  <span className="form-label" style={{ margin: 0 }}>Active (visible on website)</span>
                </label>
                <label style={{ display: 'flex', alignItems: 'center', gap: 8, cursor: 'pointer', background: 'rgba(245,166,35,0.08)', border: '1px solid rgba(245,166,35,0.3)', borderRadius: 'var(--radius-md)', padding: 'var(--space-3)' }}>
                  <input type="checkbox" checked={form.isPreorder}
                    onChange={e => setForm({ ...form, isPreorder: e.target.checked })} />
                  <div>
                    <span className="form-label" style={{ margin: 0, color: 'var(--accent-secondary)' }}>🚀 List as Preorder</span>
                    <p className="text-xs text-muted" style={{ margin: '2px 0 0' }}>Hidden from categories — shown in Preorder section on homepage</p>
                  </div>
                </label>
                {form.isPreorder && (
                  <>
                    <div className="form-group">
                      <label className="form-label">Estimated Ship Date</label>
                      <input className="form-input" type="date" value={form.estimatedShipDate}
                        onChange={e => setForm({ ...form, estimatedShipDate: e.target.value })} />
                    </div>
                    <div className="form-group">
                      <label className="form-label">Preorder Note</label>
                      <input className="form-input" value={form.preorderNote}
                        onChange={e => setForm({ ...form, preorderNote: e.target.value })}
                        placeholder='e.g. "Ships Q2 2025 — Limited to 500 units"'
                        maxLength={300} />
                    </div>
                  </>
                )}
              </div>
              <div className="modal-footer">
                <button type="button" className="btn btn-ghost" onClick={closeModal}>Cancel</button>
                <button type="submit" className="btn btn-primary" disabled={saving}>
                  {saving ? 'Saving...' : editing ? 'Update' : 'Create'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </AdminLayout>
  );
};

export default AdminProducts;