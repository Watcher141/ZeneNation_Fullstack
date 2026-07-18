// src/pages/admin/AdminProducts.jsx
import { useState, useEffect, useCallback, useRef } from 'react';
import AdminLayout from '../../components/admin/AdminLayout';
import ImageUploader from '../../components/admin/ImageUploader';
import Loader from '../../components/common/Loader';
import { productApi } from '../../api/productApi';
import { categoryApi } from '../../api/categoryApi';
import toast from 'react-hot-toast';
import { MdImage, MdRocketLaunch, MdPhotoLibrary, MdClose } from 'react-icons/md';

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
  const [loading, setLoading] = useState(false);
  const [showModal, setShowModal] = useState(false);
  const [editing, setEditing] = useState(null);
  const [form, setForm] = useState(emptyForm);
  const [saving, setSaving] = useState(false);
  const [page, setPage] = useState(0);
  const [imageModal, setImageModal] = useState(null);

  const [filterCategory, setFilterCategory] = useState('');
  const [filterStatus, setFilterStatus] = useState('');
  const [filterSearch, setFilterSearch] = useState('');
  const [debouncedSearch, setDebouncedSearch] = useState('');

  // NEW: guards against overlapping fetches. The Intersection Observer and the
  // "Anti-Starvation" effect below can both request a page increment around the
  // same render tick — without this lock, two fetches for the same/adjacent
  // page can both resolve and both append, producing duplicate rows.
  const fetchLockRef = useRef(false);
  // NEW: tracks which pages have already been fetched+appended for the current
  // filter/search session, so even if a duplicate fetch slips through, we never
  // append the same page's results twice.
  const fetchedPagesRef = useRef(new Set());
  // NEW: request sequencing. Typing a new search term (or changing a filter)
  // can fire a fetch for the *previous* page number a split second before the
  // "reset to page 0" state update lands (React state updates aren't
  // synchronous). If that stale request's response arrives AFTER the correct
  // page-0 response — which can happen, network responses don't always
  // resolve in the order they were sent — it overwrites the correct results
  // with stale/wrong ones, making a product silently disappear until some
  // unrelated action (like editing it) triggers a clean refetch. Every fetch
  // now gets a ticket number; only the response matching the latest ticket
  // is allowed to update state.
  const requestIdRef = useRef(0);

  // Debounce search input
  useEffect(() => {
    const timer = setTimeout(() => {
      setDebouncedSearch(filterSearch);
    }, 500);
    return () => clearTimeout(timer);
  }, [filterSearch]);

  // Reset back to page 0 whenever filters change
  useEffect(() => {
    setPage(0);
    // NEW: new search/filter session — old fetched-pages record no longer applies
    fetchedPagesRef.current = new Set();
  }, [debouncedSearch, filterStatus, filterCategory]);

  // Fetch Products
  const fetchProducts = useCallback(async (resetPage = false) => {
    const targetPage = resetPage ? 0 : page;

    // NEW: skip if this exact page was already fetched in this session (handles
    // the observer + anti-starvation effect racing to request the same page),
    // and skip if a fetch is already in flight (handles rapid re-triggers).
    if (fetchLockRef.current) return;
    if (targetPage !== 0 && fetchedPagesRef.current.has(targetPage)) return;

    // NEW: claim the next ticket. Only the response matching this exact
    // ticket is allowed to write to state — any response for an older
    // ticket means a newer request has since been made (e.g. search text
    // changed again) and this response is stale, so we throw it away.
    requestIdRef.current += 1;
    const myRequestId = requestIdRef.current;

    fetchLockRef.current = true;
    setLoading(true);
    try {
      const params = { page: targetPage, size: 10 };

      // We pass the search text to the backend to find items deep in the pagination
      if (debouncedSearch) params.search = debouncedSearch;

      const res = await productApi.getAllAdmin(params);

      // NEW: a newer request has been made since this one was fired — discard
      // this response so it can't overwrite more current data.
      if (myRequestId !== requestIdRef.current) return;

      const newProducts = res.data.data?.content || [];

      fetchedPagesRef.current.add(targetPage);

      setProducts(prev => {
        if (targetPage === 0) return newProducts;
        // NEW: de-duplicate by id when appending — a safety net so that even if
        // an overlapping fetch slips past the lock above, the same product can
        // never end up rendered twice in the list.
        const existingIds = new Set(prev.map(p => p.id));
        const deduped = newProducts.filter(p => !existingIds.has(p.id));
        return [...prev, ...deduped];
      });
      setPagination(res.data.data || {});
    } catch {
      toast.error('Failed to load products');
    } finally {
      // NEW: only the most recent request should clear the loading/lock state —
      // otherwise a slow, now-stale request finishing later could incorrectly
      // flip loading back off or release the lock while a newer fetch is
      // still running.
      if (myRequestId === requestIdRef.current) {
        setLoading(false);
        fetchLockRef.current = false;
      }
    }
  }, [page, debouncedSearch]);

  useEffect(() => {
    categoryApi.getAll().then(r => setCategories(r.data.data || [])).catch(() => {});
  }, []);

  useEffect(() => {
    fetchProducts();
  }, [fetchProducts]);

  // Cross-reference categories array to handle Parent -> Subcategory logic accurately
  const filteredProducts = products.filter(p => {
    if (filterCategory) {
      const pCatId = String(p.category?.id);
      const fCatId = String(filterCategory);

      let match = pCatId === fCatId;

      // If it's not a direct match, check if the selected category is a parent
      if (!match) {
        const parentCat = categories.find(c => String(c.id) === fCatId);
        if (parentCat?.subcategories?.some(sub => String(sub.id) === pCatId)) {
          match = true;
        }
      }
      if (!match) return false;
    }

    if (filterStatus === 'active'     && !p.isActive)         return false;
    if (filterStatus === 'hidden'     &&  p.isActive)         return false;
    if (filterStatus === 'preorder'   && !p.isPreorder)       return false;
    if (filterStatus === 'outofstock' &&  p.stockQuantity > 0) return false;
    if (debouncedSearch && !p.name.toLowerCase().includes(debouncedSearch.toLowerCase())) return false;

    return true;
  });

  // 1. Standard Intersection Observer for manual scrolling
  const observer = useRef();
  const lastProductElementRef = useCallback(node => {
    if (loading) return;
    if (observer.current) observer.current.disconnect();

    observer.current = new IntersectionObserver(entries => {
      // NEW: also check the fetch lock here so the observer can't queue a page
      // bump while a fetch triggered by the anti-starvation effect is in flight.
      if (entries[0].isIntersecting && page < (pagination.totalPages - 1) && !fetchLockRef.current) {
        setPage(prev => prev + 1);
      }
    });

    if (node) observer.current.observe(node);
  }, [loading, pagination.totalPages, page]);

  // 2. Anti-Starvation Loop: Rapidly fetch next pages if current filters hide everything loaded
  useEffect(() => {
    if (
      !loading &&
      !fetchLockRef.current && // NEW: don't double-trigger while a fetch is already running
      products.length > 0 &&
      filteredProducts.length < 5 &&
      page < (pagination.totalPages - 1)
    ) {
      setPage(prev => prev + 1);
    }
  }, [loading, products.length, filteredProducts.length, page, pagination.totalPages]);

  const refreshList = () => {
    // NEW: clear fetched-pages record on manual refresh so re-fetched pages
    // aren't skipped by the dedupe guard
    fetchedPagesRef.current = new Set();
    if (page === 0) fetchProducts(true);
    else setPage(0);
  };

  const getParentCategoryName = (productCatId) => {
    const parent = categories.find(c => c.subcategories?.some(s => String(s.id) === String(productCatId)));
    return parent?.name;
  };

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
      const productCatId = full.category?.id;

      let categoryId = String(productCatId || '');
      let subcategoryId = '';
      let subs = [];

      for (const parent of categories) {
        const match = parent.subcategories?.find(s => String(s.id) === String(productCatId));
        if (match) {
          categoryId = String(parent.id);
          subcategoryId = String(productCatId);
          subs = parent.subcategories;
          break;
        }
      }

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
      toast.error('Failed to load product details');
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
      refreshList();
    } catch (err) {
      toast.error(err.response?.data?.message || 'Failed to save');
    } finally { setSaving(false); }
  };

  const handleDelete = async (id) => {
    if (!window.confirm('Delete this product?')) return;
    try {
      await productApi.delete(id);
      toast.success('Product deleted');
      refreshList();
    } catch { toast.error('Failed to delete'); }
  };

  const handleToggle = async (id) => {
    try {
      await productApi.toggleVisibility(id);
      toast.success('Visibility updated');
      refreshList();
    } catch { toast.error('Failed to update'); }
  };

  const openImageModal  = (p) => setImageModal({ id: p.id, images: p.images || [], name: p.name });
  const closeImageModal = () => { setImageModal(null); refreshList(); };

  const isSearching = filterSearch !== debouncedSearch || (loading && page === 0);

  if (loading && products.length === 0 && !filterSearch && !filterStatus && !filterCategory) {
    return <AdminLayout><Loader fullPage /></AdminLayout>;
  }

  return (
    <AdminLayout>
      <div className="admin-page">
        <div className="admin-page-header" style={{ display: 'flex', flexWrap: 'wrap', gap: '16px', justifyContent: 'space-between', alignItems: 'center', marginBottom: '24px' }}>
          <div>
            <h1 className="admin-page-title">Products</h1>
            <p className="admin-page-subtitle">{pagination.totalElements || 0} total · {filteredProducts.length} shown</p>
          </div>
          <button className="btn btn-primary" onClick={openCreate}>+ Add Product</button>
        </div>

        {/* Filters */}
        <div style={{ display: 'flex', gap: 12, flexWrap: 'wrap', marginBottom: 16 }}>
          <input className="form-input" style={{ flex: '1 1 200px', height: 36 }}
            placeholder="Search products..." value={filterSearch}
            onChange={e => setFilterSearch(e.target.value)} />

          <select className="form-select" style={{ flex: '1 1 150px', height: 36 }}
            value={filterCategory} onChange={e => setFilterCategory(e.target.value)}>
            <option value="">All Categories</option>
            {categories.map(cat => (
              <optgroup key={cat.id} label={cat.name}>
                <option value={cat.id}>{cat.name} (All)</option>
                {cat.subcategories?.map(sub => (
                  <option key={sub.id} value={sub.id}>&nbsp;&nbsp;↳ {sub.name}</option>
                ))}
              </optgroup>
            ))}
          </select>

          <select className="form-select" style={{ flex: '1 1 150px', height: 36 }}
            value={filterStatus} onChange={e => setFilterStatus(e.target.value)}>
            <option value="">All Status</option>
            <option value="active">Active</option>
            <option value="hidden">Hidden</option>
            <option value="preorder">Preorder</option>
            <option value="outofstock">Out of Stock</option>
          </select>

          {(filterStatus || filterSearch || filterCategory) && (
            <button className="btn btn-ghost btn-sm" style={{ flex: '0 0 auto', height: 36 }}
              onClick={() => { setFilterStatus(''); setFilterSearch(''); setFilterCategory(''); }}>
              <MdClose size={14} style={{ verticalAlign: 'middle', marginRight: 4 }} /> Clear
            </button>
          )}
        </div>

        {/* Table Wrap */}
        <div className="admin-table-wrap" style={{ maxHeight: '70vh', overflowY: 'auto', overflowX: 'auto', width: '100%', WebkitOverflowScrolling: 'touch' }}>
          <table className="admin-table" style={{ minWidth: '900px', width: '100%' }}>
            <thead style={{ position: 'sticky', top: 0, zIndex: 1, background: 'var(--bg-card)' }}>
              <tr>
                <th>Image</th><th>Name</th><th>Category</th><th>Price</th>
                <th>Stock</th><th>Weight</th><th>Status</th><th>Images</th><th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {isSearching ? (
                <tr>
                  <td colSpan={9} style={{ textAlign: 'center', padding: '64px 0' }}>
                    <Loader />
                  </td>
                </tr>
              ) : (
                <>
                  {filteredProducts.map((p) => (
                    <tr key={p.id}>
                      <td>
                        {p.primaryImageUrl
                          ? <img src={p.primaryImageUrl} alt={p.name} style={{ width: 48, height: 48, borderRadius: 8, objectFit: 'cover' }} />
                          : <div style={{ width: 48, height: 48, background: 'var(--bg-hover)', borderRadius: 8, display: 'flex', alignItems: 'center', justifyContent: 'center' }}><MdImage size={24} color="var(--text-muted)" /></div>
                        }
                      </td>
                      <td style={{ maxWidth: 180 }}>
                        <div style={{ fontWeight: 600, color: 'var(--text-primary)', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>{p.name}</div>
                        <div className="text-xs text-muted">{p.slug}</div>
                        {p.isPreorder && <span className="badge badge-yellow" style={{ fontSize: 10, display: 'inline-flex', alignItems: 'center', gap: 3 }}><MdRocketLaunch size={10} /> Preorder</span>}
                      </td>
                      <td>
                        <span className="badge badge-purple">{p.category?.name}</span>
                        {getParentCategoryName(p.category?.id) && (
                          <div className="text-xs text-muted" style={{ marginTop: 2 }}>
                            ↳ {getParentCategoryName(p.category?.id)}
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
                          <MdPhotoLibrary size={14} style={{ verticalAlign: 'middle', marginRight: 4 }} />{p.images?.length ? `(${p.images.length})` : 'Add'}
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
                    <tr><td colSpan={9} style={{ textAlign: 'center', padding: 32, color: 'var(--text-muted)' }}>No matching products found</td></tr>
                  )}
                </>
              )}
            </tbody>
          </table>

          {!isSearching && (
            <div ref={lastProductElementRef} style={{ height: '10px' }}></div>
          )}

          {loading && page > 0 && (
            <div style={{ padding: '20px', textAlign: 'center', color: 'var(--text-muted)' }}>
              <Loader />
            </div>
          )}
        </div>

        {/* Add/Edit Modal */}
        {showModal && (
          <div className="modal-overlay" style={{ padding: '16px' }} onClick={closeModal}>
            <div className="modal" style={{ maxWidth: 640, width: '100%', maxHeight: '90vh', overflowY: 'auto' }} onClick={e => e.stopPropagation()}>
              <div className="modal-header">
                <h3 className="modal-title">{editing ? 'Edit Product' : 'New Product'}</h3>
                <button className="modal-close" onClick={closeModal}><MdClose size={18} /></button>
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

                  <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(140px, 1fr))', gap: 'var(--space-4)' }}>
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

                    <div className="form-group" style={{ gridColumn: '1 / -1' }}>
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

                  <label style={{ display: 'flex', alignItems: 'center', gap: 8, cursor: 'pointer', marginTop: 16 }}>
                    <input type="checkbox" checked={form.isActive}
                      onChange={e => setForm({ ...form, isActive: e.target.checked })} />
                    <span className="form-label" style={{ margin: 0 }}>Active (visible on website)</span>
                  </label>

                  <label style={{ display: 'flex', alignItems: 'center', gap: 8, cursor: 'pointer', marginTop: 8, background: 'rgba(245,166,35,0.08)', border: '1px solid rgba(245,166,35,0.3)', borderRadius: 'var(--radius-md)', padding: 'var(--space-3)' }}>
                    <input type="checkbox" checked={form.isPreorder}
                      onChange={e => setForm({ ...form, isPreorder: e.target.checked })} />
                    <div>
                      <span className="form-label" style={{ margin: 0, color: 'var(--accent-secondary)', display: 'inline-flex', alignItems: 'center', gap: 4 }}><MdRocketLaunch size={14} /> List as Preorder</span>
                      <p className="text-xs text-muted" style={{ margin: '2px 0 0' }}>Hidden — shown in Preorder section on homepage</p>
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
                <div className="modal-footer" style={{ flexWrap: 'wrap', justifyContent: 'flex-end', gap: '8px' }}>
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
          <div className="modal-overlay" style={{ padding: '16px' }} onClick={closeImageModal}>
            <div className="modal" style={{ maxWidth: 680, width: '100%' }} onClick={e => e.stopPropagation()}>
              <div className="modal-header">
                <h2 style={{ fontSize: '1.2rem', margin: 0, textOverflow: 'ellipsis', overflow: 'hidden', whiteSpace: 'nowrap' }}>Images — {imageModal.name}</h2>
                <button className="modal-close" onClick={closeImageModal}><MdClose size={18} /></button>
              </div>
              <div className="modal-body">
                <ImageUploader
                  productId={imageModal.id}
                  existingImages={imageModal.images}
                  onImagesChange={() => {
                    productApi.getById(imageModal.id).then(res => {
                      setImageModal(prev => ({ ...prev, images: res.data.data?.images || [] }));
                      refreshList();
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