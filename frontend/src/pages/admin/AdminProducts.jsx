// src/pages/admin/AdminProducts.jsx
import { useState, useEffect, useCallback } from 'react';
import AdminLayout from '../../components/admin/AdminLayout';
import ImageUploader from '../../components/admin/ImageUploader';
import Loader from '../../components/common/Loader';
import { productApi } from '../../api/productApi';
import { categoryApi } from '../../api/categoryApi';
import toast from 'react-hot-toast';

const emptyForm = {
  name: '', description: '', tagline: '', price: '', discountPercent: '0',
  stockQuantity: '', weightGrams: '0', categoryId: '', subcategoryId: '', isActive: true,
  isPreorder: false, estimatedShipDate: '', preorderNote: '',
};

const AdminProducts = () => {
  const [products, setProducts] = useState([]);
  const [categories, setCategories] = useState([]);
  const [subcategories, setSubcategories] = useState([]);
  const [pagination, setPagination] = useState({});
  const [loading, setLoading] = useState(true);
  const [showModal, setShowModal] = useState(false);
  const [editing, setEditing] = useState(null);
  const [form, setForm] = useState(emptyForm);
  const [saving, setSaving] = useState(false);
  const [page, setPage] = useState(0);
  const [imageModal, setImageModal] = useState(null);

  const [filterCategory, setFilterCategory] = useState('');
  const [filterStatus, setFilterStatus] = useState('');
  const [filterSearch, setFilterSearch] = useState('');

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

  const openCreate = () => {
    setEditing(null);
    setForm(emptyForm);
    setSubcategories([]);
    setShowModal(true);
  };

const openEdit = async (p) => {
  setEditing(p);
  setShowModal(true);

  try {
    const res = await productApi.getById(p.id);
    const full = res.data.data;

    console.log('CATEGORY:', JSON.stringify(full.category, null, 2));

    const productCatId = full.category?.id;

    // Check if this category id exists as a subcategory inside any parent
    let categoryId = String(productCatId || '');
    let subcategoryId = '';
    let subs = [];

    for (const parent of categories) {
      const match = parent.subcategories?.find(s => String(s.id) === String(productCatId));
      if (match) {
        // It's a subcategory — set parent as category, this as subcategory
        categoryId = String(parent.id);
        subcategoryId = String(productCatId);
        subs = parent.subcategories;
        break;
      }
    }

    // If not found as a subcategory, it's a top-level category — load its subs
    if (!subcategoryId) {
      const parent = categories.find(c => String(c.id) === String(productCatId));
      subs = parent?.subcategories || [];
    }

    setSubcategories(subs);

    setForm({
      name:              full.name              || '',
      description:       full.description       || '',
      tagline:           full.tagline           || '',
      price:             String(full.price      || ''),
      discountPercent:   String(full.discountPercent || 0),
      stockQuantity:     String(full.stockQuantity   || ''),
      weightGrams:       String(full.weightGrams     || 0),
      categoryId,
      subcategoryId,
      isActive:          full.isActive          ?? true,
      isPreorder:        full.isPreorder        || false,
      estimatedShipDate: full.estimatedShipDate || '',
      preorderNote:      full.preorderNote      || '',
    });
  } catch (err) {
    toast.error('Failed to load product');
    console.error(err);
  }
};

  const closeModal = () => {
    setShowModal(false);
    setEditing(null);
    setForm(emptyForm);
    setSubcategories([]);
  };

  const handleSave = async (e) => {
    e.preventDefault();
    setSaving(true);
    try {
      const payload = {
        ...form,
        price:           Number(form.price),
        discountPercent: Number(form.discountPercent),
        stockQuantity:   Number(form.stockQuantity),
        weightGrams:     Number(form.weightGrams || 0),
        categoryId:      Number(form.subcategoryId || form.categoryId),
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
    try { await productApi.delete(id); toast.success('Product deleted'); fetchProducts(); }
    catch { toast.error('Failed to delete'); }
  };

  const handleToggle = async (id) => {
    try { await productApi.toggleVisibility(id); toast.success('Visibility updated'); fetchProducts(); }
    catch { toast.error('Failed to update'); }
  };

  const openImageModal  = (p) => setImageModal({ id: p.id, images: p.images || [], name: p.name });
  const closeImageModal = () => { setImageModal(null); fetchProducts(); };

  const filteredProducts = products.filter(p => {
    if (filterCategory && p.category?.id != filterCategory && p.category?.parentId != filterCategory) return false;
    if (filterStatus === 'active'     && !p.isActive)         return false;
    if (filterStatus === 'hidden'     &&  p.isActive)         return false;
    if (filterStatus === 'preorder'   && !p.isPreorder)       return false;
    if (filterStatus === 'outofstock' &&  p.stockQuantity > 0) return false;
    if (filterSearch && !p.name.toLowerCase().includes(filterSearch.toLowerCase())) return false;
    return true;
  });

  const allCategoriesFlat = categories.reduce((acc, cat) => {
    acc.push(cat);
    if (cat.subcategories) acc.push(...cat.subcategories);
    return acc;
  }, []);

  if (loading && products.length === 0) return <AdminLayout><Loader fullPage /></AdminLayout>;

  return (
    <AdminLayout>
      <div className="admin-page">
        <div className="admin-page-header">
          <div>
            <h1 className="admin-page-title">Products</h1>
            <p className="admin-page-subtitle">{pagination.totalElements || 0} total · {filteredProducts.length} shown</p>
          </div>
          <button className="btn btn-primary" onClick={openCreate}>+ Add Product</button>
        </div>

        {/* Filters */}
        <div style={{ display: 'flex', gap: 12, flexWrap: 'wrap', marginBottom: 16 }}>
          <input className="form-input" style={{ maxWidth: 220, height: 36 }}
            placeholder="🔍 Search products..." value={filterSearch}
            onChange={e => setFilterSearch(e.target.value)} />
          <select className="form-select" style={{ maxWidth: 200, height: 36 }}
            value={filterCategory} onChange={e => setFilterCategory(e.target.value)}>
            <option value="">All Categories</option>
            {categories.map(cat => (
              <>
                <option key={cat.id} value={cat.id}>{cat.name}</option>
                {cat.subcategories?.map(sub => (
                  <option key={sub.id} value={sub.id}>&nbsp;&nbsp;↳ {sub.name}</option>
                ))}
              </>
            ))}
          </select>
          <select className="form-select" style={{ maxWidth: 160, height: 36 }}
            value={filterStatus} onChange={e => setFilterStatus(e.target.value)}>
            <option value="">All Status</option>
            <option value="active">Active</option>
            <option value="hidden">Hidden</option>
            <option value="preorder">Preorder</option>
            <option value="outofstock">Out of Stock</option>
          </select>
          {(filterCategory || filterStatus || filterSearch) && (
            <button className="btn btn-ghost btn-sm"
              onClick={() => { setFilterCategory(''); setFilterStatus(''); setFilterSearch(''); }}>
              ✕ Clear
            </button>
          )}
        </div>

        {/* Table */}
        <div className="admin-table-wrap">
          <table className="admin-table">
            <thead>
              <tr>
                <th>Image</th><th>Name</th><th>Category</th><th>Price</th>
                <th>Stock</th><th>Weight</th><th>Status</th><th>Images</th><th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {filteredProducts.map(p => (
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
                    {p.isPreorder && <span className="badge badge-yellow" style={{ fontSize: 10 }}>🚀 Preorder</span>}
                  </td>
                  <td>
                    <span className="badge badge-purple">{p.category?.name}</span>
                    {p.category?.parentId && (
                      <div className="text-xs text-muted" style={{ marginTop: 2 }}>
                        ↳ {allCategoriesFlat.find(c => c.id === p.category?.parentId)?.name}
                      </div>
                    )}
                  </td>
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
                    <span className="text-muted" style={{ fontSize: 12 }}>
                      {p.weightGrams ? `${p.weightGrams}g` : '—'}
                    </span>
                  </td>
                  <td>
                    <span className={`badge ${p.isActive ? 'badge-green' : 'badge-red'}`}>
                      {p.isActive ? 'Active' : 'Hidden'}
                    </span>
                  </td>
                  <td>
                    <button className="btn btn-ghost btn-sm" onClick={() => openImageModal(p)}>
                      🖼 {p.images?.length ? `(${p.images.length})` : 'Add'}
                    </button>
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
              {filteredProducts.length === 0 && (
                <tr><td colSpan={9} style={{ textAlign: 'center', padding: 32, color: 'var(--text-muted)' }}>No products found</td></tr>
              )}
            </tbody>
          </table>
        </div>

        {/* Pagination */}
        {pagination.totalPages > 1 && (
          <div className="pagination" style={{ marginTop: 'var(--space-6)' }}>
            <button className="btn btn-ghost btn-sm" disabled={page === 0}
              onClick={() => setPage(p => Math.max(0, p - 1))}>← Prev</button>
            <span className="text-muted text-sm">Page {page + 1} of {pagination.totalPages}</span>
            <button className="btn btn-ghost btn-sm" disabled={page >= pagination.totalPages - 1}
              onClick={() => setPage(p => Math.min(pagination.totalPages - 1, p + 1))}>Next →</button>
          </div>
        )}

        {/* Add/Edit Modal */}
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
                    <input className="form-input" value={form.name} required
                      onChange={e => setForm({ ...form, name: e.target.value })}
                      placeholder="e.g. Naruto Figure 30cm" />
                  </div>
                  <div className="form-group">
                    <label className="form-label">Description *</label>
                    <textarea className="form-input" rows={3} value={form.description} required
                      onChange={e => setForm({ ...form, description: e.target.value })}
                      placeholder="Product description..." style={{ resize: 'vertical' }} />
                  </div>
                  <div className="form-group">
                    <label className="form-label">
                      Tagline <span className="text-muted" style={{ fontWeight: 400 }}>(optional)</span>
                    </label>
                    <input className="form-input" value={form.tagline} maxLength={200}
                      onChange={e => setForm({ ...form, tagline: e.target.value })}
                      placeholder='e.g. "Believe it! — Limited Edition"' />
                  </div>

                  <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 'var(--space-4)' }}>
                    <div className="form-group">
                      <label className="form-label">Price (₹) *</label>
                      <input className="form-input" type="number" min="0" step="0.01"
                        value={form.price} required placeholder="999"
                        onChange={e => setForm({ ...form, price: e.target.value })} />
                    </div>
                    <div className="form-group">
                      <label className="form-label">Discount %</label>
                      <input className="form-input" type="number" min="0" max="100"
                        value={form.discountPercent} placeholder="0"
                        onChange={e => setForm({ ...form, discountPercent: e.target.value })} />
                    </div>
                    <div className="form-group">
                      <label className="form-label">Stock Quantity *</label>
                      <input className="form-input" type="number" min="0"
                        value={form.stockQuantity} required placeholder="50"
                        onChange={e => setForm({ ...form, stockQuantity: e.target.value })} />
                    </div>
                    <div className="form-group">
                      <label className="form-label">Weight (grams)</label>
                      <input className="form-input" type="number" min="0"
                        value={form.weightGrams} placeholder="250"
                        onChange={e => setForm({ ...form, weightGrams: e.target.value })} />
                    </div>

                    {/* Category */}
                    <div className="form-group">
                      <label className="form-label">Category *</label>
                      <select
                        className="form-select"
                        value={String(form.categoryId || '')}
                        onChange={e => {
                          const categoryId = e.target.value;
                          const cat = categories.find(c => String(c.id) === categoryId);
                          setSubcategories(cat?.subcategories || []);
                          setForm(prev => ({ ...prev, categoryId, subcategoryId: '' }));
                        }}
                        required
                      >
                        <option value="">Select category</option>
                        {categories.map(c => (
                          <option key={c.id} value={String(c.id)}>{c.name}</option>
                        ))}
                      </select>
                    </div>

                    {/* Subcategory */}
                    {subcategories.length > 0 && (
                      <div className="form-group" style={{ gridColumn: '1 / -1' }}>
                        <label className="form-label">
                          Subcategory
                          <span className="text-muted" style={{ fontWeight: 400, fontSize: 12, marginLeft: 8 }}>
                            (optional — assign to a more specific category)
                          </span>
                        </label>
                        <select
                          className="form-select"
                          value={String(form.subcategoryId || '')}
                          onChange={e => setForm(prev => ({ ...prev, subcategoryId: e.target.value }))}
                        >
                          <option value="">— Keep in parent category —</option>
                          {subcategories.map(s => (
                            <option key={s.id} value={String(s.id)}>{s.name}</option>
                          ))}
                        </select>
                      </div>
                    )}
                  </div>

                  {/* Active */}
                  <label style={{ display: 'flex', alignItems: 'center', gap: 8, cursor: 'pointer', marginTop: 8 }}>
                    <input type="checkbox" checked={form.isActive}
                      onChange={e => setForm({ ...form, isActive: e.target.checked })} />
                    <span className="form-label" style={{ margin: 0 }}>Active (visible on website)</span>
                  </label>

                  {/* Preorder */}
                  <label style={{ display: 'flex', alignItems: 'center', gap: 8, cursor: 'pointer', marginTop: 8, background: 'rgba(245,166,35,0.08)', border: '1px solid rgba(245,166,35,0.3)', borderRadius: 'var(--radius-md)', padding: 'var(--space-3)' }}>
                    <input type="checkbox" checked={form.isPreorder}
                      onChange={e => setForm({ ...form, isPreorder: e.target.checked })} />
                    <div>
                      <span className="form-label" style={{ margin: 0, color: 'var(--accent-secondary)' }}>🚀 List as Preorder</span>
                      <p className="text-xs text-muted" style={{ margin: '2px 0 0' }}>Hidden from categories — shown in Preorder section on homepage</p>
                    </div>
                  </label>

                  {form.isPreorder && (
                    <>
                      <div className="form-group" style={{ marginTop: 12 }}>
                        <label className="form-label">Estimated Ship Date</label>
                        <input className="form-input" type="date" value={form.estimatedShipDate}
                          onChange={e => setForm({ ...form, estimatedShipDate: e.target.value })} />
                      </div>
                      <div className="form-group">
                        <label className="form-label">Preorder Note</label>
                        <input className="form-input" value={form.preorderNote} maxLength={300}
                          onChange={e => setForm({ ...form, preorderNote: e.target.value })}
                          placeholder='e.g. "Ships Q2 2025 — Limited to 500 units"' />
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

        {/* Image Modal */}
        {imageModal && (
          <div className="modal-overlay" onClick={closeImageModal}>
            <div className="modal" style={{ maxWidth: 680, width: '95%' }} onClick={e => e.stopPropagation()}>
              <div className="modal-header">
                <h2>Images — {imageModal.name}</h2>
                <button className="modal-close" onClick={closeImageModal}>✕</button>
              </div>
              <div className="modal-body">
                <ImageUploader
                  productId={imageModal.id}
                  existingImages={imageModal.images}
                  onImagesChange={() => {
                    productApi.getById(imageModal.id).then(res => {
                      const updated = res.data.data;
                      setImageModal(prev => ({ ...prev, images: updated?.images || [] }));
                      fetchProducts();
                    }).catch(() => {});
                  }}
                />
              </div>
            </div>
          </div>
        )}
      </div>
    </AdminLayout>
  );
};

export default AdminProducts;