import { useState, useEffect } from 'react';
import { categoryApi, productApi } from '../../api/apiCollections';
import axiosInstance from '../../api/axiosInstance';
import toast from 'react-hot-toast';
import {
  MdAdd, MdClose, MdCheckCircle, MdSearch, MdGridView,
  MdEdit, MdDelete, MdVisibility, MdVisibilityOff, MdPublic
} from 'react-icons/md';
import './FbtAdminManager.css';

// Admin layout wrapper imported here (adjust path if needed based on your project structure)
import AdminLayout from '../../components/admin/AdminLayout'; 

// ── Product Card ──────────────────────────────────────────────────────────────
const ProductCard = ({ product, isSelected, onToggle }) => (
  <div
    className={`fbt-product-card ${isSelected ? 'selected' : ''}`}
    onClick={() => onToggle(product)}
  >
    <div className="fbt-card-img">
      {product.primaryImageUrl
        ? <img src={product.primaryImageUrl} alt={product.name} />
        : <div className="fbt-card-no-img">?</div>
      }
      {isSelected && (
        <div className="fbt-card-overlay"><MdCheckCircle size={28} color="#fff" /></div>
      )}
    </div>
    <div className="fbt-card-info">
      <p className="fbt-card-name">{product.name}</p>
      <p className="fbt-card-price">
        ₹{Number(product.discountedPrice || product.price).toLocaleString('en-IN')}
      </p>
    </div>
  </div>
);

// ── Main Component ────────────────────────────────────────────────────────────
const FbtAdminManager = () => {
  const [categories, setCategories] = useState([]);
  const [bundles, setBundles] = useState([]);
  const [loadingBundles, setLoadingBundles] = useState(true);
  const [loadingCategories, setLoadingCategories] = useState(true);

  // Form state
  const [editingId, setEditingId] = useState(null); // null = create mode
  const [title, setTitle] = useState('');
  const [discountPercent, setDiscountPercent] = useState(0);
  const [showEverywhere, setShowEverywhere] = useState(false);
  const [selectedCategoryIds, setSelectedCategoryIds] = useState([]);
  const [selectedProducts, setSelectedProducts] = useState([]);

  // Right panel
  const [activeTab, setActiveTab] = useState('category');
  const [browseCategory, setBrowseCategory] = useState('');
  const [categoryProducts, setCategoryProducts] = useState([]);
  const [loadingCategoryProducts, setLoadingCategoryProducts] = useState(false);
  const [searchKeyword, setSearchKeyword] = useState('');
  const [searchResults, setSearchResults] = useState([]);
  const [searching, setSearching] = useState(false);
  const [saving, setSaving] = useState(false);

  // ── Load categories + bundles ──
  useEffect(() => {
    categoryApi.getAll()
      .then(res => setCategories(res.data.data || []))
      .catch(() => toast.error('Failed to load categories'))
      .finally(() => setLoadingCategories(false));

    fetchBundles();
  }, []);

  const fetchBundles = () => {
    setLoadingBundles(true);
    axiosInstance.get('/api/v1/fbt-sets/admin')
      .then(res => setBundles(res.data.data || []))
      .catch(() => toast.error('Failed to load bundles'))
      .finally(() => setLoadingBundles(false));
  };

  // ── Load products by category ──
  useEffect(() => {
    if (!browseCategory) { setCategoryProducts([]); return; }
    setLoadingCategoryProducts(true);
    productApi.getByCategory(browseCategory, { page: 0, size: 50 })
      .then(res => setCategoryProducts(res.data.data?.content || []))
      .catch(() => toast.error('Failed to load products'))
      .finally(() => setLoadingCategoryProducts(false));
  }, [browseCategory]);

  // ── Toggle product selection ──
  const toggleProduct = (product) => {
    const isSelected = selectedProducts.some(p => p.id === product.id);
    if (isSelected) {
      setSelectedProducts(prev => prev.filter(p => p.id !== product.id));
    } else {
      if (selectedProducts.length >= 10) {
        toast.error('Maximum 10 products per bundle.');
        return;
      }
      setSelectedProducts(prev => [...prev, product]);
    }
  };

  // ── Toggle category visibility selection ──
  const toggleCategory = (catId) => {
    setSelectedCategoryIds(prev =>
      prev.includes(catId) ? prev.filter(id => id !== catId) : [...prev, catId]
    );
  };

  // ── Search ──
  const handleSearch = async (e) => {
    e.preventDefault();
    if (!searchKeyword.trim()) return;
    setSearching(true);
    try {
      const res = await productApi.search({ keyword: searchKeyword, page: 0, size: 30 });
      const results = res.data.data?.content || [];
      setSearchResults(results);
      if (results.length === 0) toast('No products found.', { icon: '🔍' });
    } catch {
      toast.error('Search failed.');
    } finally {
      setSearching(false);
    }
  };

  // ── Load bundle into form for editing ──
  const handleEdit = (bundle) => {
    setEditingId(bundle.id);
    setTitle(bundle.title);
    setDiscountPercent(bundle.discountPercent || 0);
    setShowEverywhere(bundle.showEverywhere || false);
    setSelectedCategoryIds(bundle.visibleInCategoryIds || []);
    // Reconstruct product objects from bundle
    setSelectedProducts(bundle.products || []);
    window.scrollTo({ top: 0, behavior: 'smooth' });
  };

  // ── Reset form ──
  const resetForm = () => {
    setEditingId(null);
    setTitle('');
    setDiscountPercent(0);
    setShowEverywhere(false);
    setSelectedCategoryIds([]);
    setSelectedProducts([]);
    setSearchKeyword('');
    setSearchResults([]);
    setBrowseCategory('');
    setCategoryProducts([]);
  };

  // ── Save (create or update) ──
  const handleSubmit = async (e) => {
    e.preventDefault();
    if (selectedProducts.length < 2) return toast.error('Add at least 2 products.');
    if (!showEverywhere && selectedCategoryIds.length === 0)
      return toast.error('Select at least one category or enable Show Everywhere.');

    setSaving(true);
    const payload = {
      title,
      discountPercent: Number(discountPercent),
      showEverywhere,
      productIds: selectedProducts.map(p => p.id),
      categoryIds: showEverywhere ? [] : selectedCategoryIds,
    };

    try {
      if (editingId) {
        await axiosInstance.put(`/api/v1/fbt-sets/admin/${editingId}`, payload);
        toast.success('Bundle updated!');
      } else {
        await axiosInstance.post('/api/v1/fbt-sets/admin', payload);
        toast.success('Bundle created!');
      }
      resetForm();
      fetchBundles();
    } catch (err) {
      toast.error(err.response?.data?.message || 'Failed to save bundle.');
    } finally {
      setSaving(false);
    }
  };

  // ── Toggle active ──
  const handleToggle = async (bundle) => {
    try {
      await axiosInstance.patch(`/api/v1/fbt-sets/admin/${bundle.id}/toggle`);
      toast.success(`Bundle ${bundle.isActive ? 'deactivated' : 'activated'}`);
      fetchBundles();
    } catch {
      toast.error('Failed to toggle bundle.');
    }
  };

  // ── Delete ──
  const handleDelete = async (id) => {
    if (!window.confirm('Delete this bundle?')) return;
    try {
      await axiosInstance.delete(`/api/v1/fbt-sets/admin/${id}`);
      toast.success('Bundle deleted.');
      fetchBundles();
      if (editingId === id) resetForm();
    } catch {
      toast.error('Failed to delete bundle.');
    }
  };

  if (loadingCategories) return <div className="fbt-loading">Loading…</div>;

  return (
    <AdminLayout>
      <div className="admin-fbt-container">
        <div className="fbt-page-header">
          <h2>Frequently Bought Together</h2>
          <p className="text-muted">Build product bundles that appear on product pages.</p>
        </div>

        {/* ── TOP: Existing Bundles List ── */}
        <div className="fbt-bundles-list">
          <div className="fbt-panel-title">Existing Bundles</div>
          {loadingBundles ? (
            <p className="fbt-status-msg">Loading bundles…</p>
          ) : bundles.length === 0 ? (
            <p className="fbt-status-msg">No bundles yet. Create one below.</p>
          ) : (
            <div className="fbt-bundle-rows">
              {bundles.map(bundle => (
                <div key={bundle.id} className={`fbt-bundle-row ${!bundle.isActive ? 'inactive' : ''}`}>
                  <div className="fbt-bundle-row-info">
                    <span className="fbt-bundle-row-title">{bundle.title}</span>
                    <div className="fbt-bundle-row-meta">
                      {bundle.showEverywhere
                        ? <span className="fbt-badge fbt-badge-global"><MdPublic size={11} /> All Pages</span>
                        : bundle.visibleInCategoryNames?.map(n => (
                            <span key={n} className="fbt-badge fbt-badge-cat">{n}</span>
                          ))
                      }
                      {bundle.discountPercent > 0 && (
                        <span className="fbt-badge fbt-badge-discount">{bundle.discountPercent}% off</span>
                      )}
                      <span className="fbt-badge fbt-badge-products">
                        {bundle.products?.length} products
                      </span>
                    </div>
                  </div>
                  <div className="fbt-bundle-row-actions">
                    <button className="fbt-icon-btn" title="Edit" onClick={() => handleEdit(bundle)}>
                      <MdEdit size={18} />
                    </button>
                    <button
                      className={`fbt-icon-btn ${bundle.isActive ? 'active' : 'muted'}`}
                      title={bundle.isActive ? 'Deactivate' : 'Activate'}
                      onClick={() => handleToggle(bundle)}
                    >
                      {bundle.isActive ? <MdVisibility size={18} /> : <MdVisibilityOff size={18} />}
                    </button>
                    <button className="fbt-icon-btn danger" title="Delete" onClick={() => handleDelete(bundle.id)}>
                      <MdDelete size={18} />
                    </button>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>

        {/* ── BOTTOM: Create / Edit Form ── */}
        <div className="fbt-grid">

          {/* LEFT: Bundle Settings */}
          <div className="fbt-panel">
            <div className="fbt-panel-title">
              {editingId ? '✏️ Edit Bundle' : '➕ New Bundle'}
              {editingId && (
                <button className="fbt-cancel-edit-btn" onClick={resetForm}>
                  <MdClose size={14} /> Cancel Edit
                </button>
              )}
            </div>

            <form onSubmit={handleSubmit} className="fbt-form">
              <div className="form-group">
                <label className="form-label">Bundle title</label>
                <input className="form-input"
                  placeholder="e.g. The Ultimate Keychain Pack"
                  value={title} onChange={e => setTitle(e.target.value)} required />
              </div>

              <div className="form-group">
                <label className="form-label">Bundle discount (%)</label>
                <input type="number" className="form-input" min="0" max="100"
                  value={discountPercent} onChange={e => setDiscountPercent(e.target.value)} />
                <small className="form-hint">Leave at 0 for no discount.</small>
              </div>

              {/* Visibility */}
              <div className="form-group">
                <label className="form-label">Visibility</label>
                <label className="fbt-toggle-row">
                  <input type="checkbox" checked={showEverywhere}
                    onChange={e => setShowEverywhere(e.target.checked)} />
                  <span><MdPublic size={14} /> Show on ALL product pages</span>
                </label>
              </div>

              {!showEverywhere && (
                <div className="form-group">
                  <label className="form-label">Show on these categories</label>
                  <div className="fbt-category-checkboxes">
                    {categories.map(cat => (
                      <label key={cat.id} className="fbt-cat-checkbox">
                        <input
                          type="checkbox"
                          checked={selectedCategoryIds.includes(cat.id)}
                          onChange={() => toggleCategory(cat.id)}
                        />
                        <span>{cat.name}</span>
                      </label>
                    ))}
                  </div>
                  <small className="form-hint">
                    {selectedCategoryIds.length === 0
                      ? 'Select at least one category.'
                      : `${selectedCategoryIds.length} selected`}
                  </small>
                </div>
              )}

              <div className="fbt-divider" />

              {/* Selected Products */}
              <div className="fbt-selected-header">
                <span className="fbt-panel-title" style={{ marginBottom: 0 }}>Selected Products</span>
                <span className={`fbt-count-badge ${selectedProducts.length >= 2 ? 'ready' : ''}`}>
                  {selectedProducts.length} / 10
                </span>
              </div>

              {selectedProducts.length === 0 ? (
                <div className="fbt-empty-selected">
                  Pick products from the right panel. Min 2, max 10.
                </div>
              ) : (
                <div className="fbt-selected-list">
                  {selectedProducts.map(p => (
                    <div key={p.id} className="fbt-selected-item">
                      {p.primaryImageUrl
                        ? <img src={p.primaryImageUrl} alt={p.name} />
                        : <div className="fbt-selected-no-img">?</div>
                      }
                      <div className="fbt-selected-info">
                        <span className="fbt-selected-name">{p.name}</span>
                        <span className="fbt-selected-price">
                          ₹{Number(p.discountedPrice || p.price).toLocaleString('en-IN')}
                        </span>
                      </div>
                      <button type="button" className="fbt-remove-btn" onClick={() => toggleProduct(p)}>
                        <MdClose size={16} />
                      </button>
                    </div>
                  ))}
                </div>
              )}

              <button type="submit" className="btn btn-primary"
                style={{ width: '100%', marginTop: '1rem' }}
                disabled={saving || selectedProducts.length < 2}>
                {saving ? 'Saving…' : editingId ? 'Update Bundle' : 'Save Bundle'}
              </button>
            </form>
          </div>

          {/* RIGHT: Product Picker */}
          <div className="fbt-panel">
            <div className="fbt-panel-title">Add Products</div>

            <div className="fbt-tabs">
              <button type="button"
                className={`fbt-tab ${activeTab === 'category' ? 'active' : ''}`}
                onClick={() => setActiveTab('category')}>
                <MdGridView size={16} /> By Category
              </button>
              <button type="button"
                className={`fbt-tab ${activeTab === 'search' ? 'active' : ''}`}
                onClick={() => setActiveTab('search')}>
                <MdSearch size={16} /> Search
              </button>
            </div>

            {activeTab === 'category' && (
              <div className="fbt-tab-content">
                <div className="form-group" style={{ marginBottom: '1rem' }}>
                  <select className="form-input" value={browseCategory}
                    onChange={e => setBrowseCategory(e.target.value)}>
                    <option value="">— Pick a category to browse —</option>
                    {categories.map(cat => (
                      <option key={cat.id} value={cat.id}>{cat.name}</option>
                    ))}
                  </select>
                </div>
                {loadingCategoryProducts ? (
                  <p className="fbt-status-msg">Loading products…</p>
                ) : !browseCategory ? (
                  <p className="fbt-status-msg">Select a category above to see its products.</p>
                ) : categoryProducts.length === 0 ? (
                  <p className="fbt-status-msg">No products found in this category.</p>
                ) : (
                  <div className="fbt-product-grid">
                    {categoryProducts.map(p => (
                      <ProductCard key={p.id} product={p}
                        isSelected={selectedProducts.some(s => s.id === p.id)}
                        onToggle={toggleProduct} />
                    ))}
                  </div>
                )}
              </div>
            )}

            {activeTab === 'search' && (
              <div className="fbt-tab-content">
                <form onSubmit={handleSearch} className="fbt-search-bar">
                  <input className="form-input"
                    placeholder="Search any product by name…"
                    value={searchKeyword}
                    onChange={e => setSearchKeyword(e.target.value)} />
                  <button type="submit" className="btn btn-secondary fbt-search-btn"
                    disabled={searching}>
                    <MdSearch size={20} />
                  </button>
                </form>
                {searching ? (
                  <p className="fbt-status-msg">Searching…</p>
                ) : searchResults.length === 0 ? (
                  <p className="fbt-status-msg">Type a product name and hit search.</p>
                ) : (
                  <div className="fbt-product-grid">
                    {searchResults.map(p => (
                      <ProductCard key={p.id} product={p}
                        isSelected={selectedProducts.some(s => s.id === p.id)}
                        onToggle={toggleProduct} />
                    ))}
                  </div>
                )}
              </div>
            )}
          </div>
        </div>
      </div>
    </AdminLayout>
  );
};

export default FbtAdminManager;