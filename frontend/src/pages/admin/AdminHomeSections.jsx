// src/pages/admin/AdminHomeSections.jsx
import { useState, useEffect, useCallback } from 'react';
import AdminLayout from '../../components/admin/AdminLayout';
import Loader from '../../components/common/Loader';
import { homeSectionApi } from '../../api/homeSectionApi';
import { productApi } from '../../api/productApi';
import toast from 'react-hot-toast';
import { MdAdd, MdEdit, MdDelete, MdVisibility, MdVisibilityOff, MdClose, MdSearch } from 'react-icons/md';

const TYPE_OPTIONS = ['NEW_ARRIVAL', 'PREORDER', 'CUSTOM'];
const emptyForm = { title: '', subtitle: '', type: 'CUSTOM', displayOrder: 0, isActive: true, viewAllUrl: '' };

// ── Enhanced Search Ranking Helper ──
const getSearchRank = (product, search) => {
  if (!search) return 0;
  
  const term = search.toLowerCase().trim();
  if (!term) return 0;

  const name = (product.name || '').toLowerCase();
  const category = (product.category?.name || '').toLowerCase();
  const sku = (product.sku || '').toLowerCase();
  const id = String(product.id);

  // 1. Exact Match (Highest Priority)
  if (name === term || id === term || sku === term) return 100;
  
  // 2. Starts With
  if (name.startsWith(term) || sku.startsWith(term)) return 80;
  
  // 3. Contains Exact Phrase
  if (name.includes(term)) return 60;
  if (sku.includes(term)) return 50;

  // 4. Tokenized Match (e.g. "blue shirt" matches "shirt blue mens")
  const tokens = term.split(/\s+/);
  if (tokens.length > 1) {
    const matchesAllTokens = tokens.every(t => name.includes(t) || category.includes(t) || sku.includes(t));
    if (matchesAllTokens) return 40;
  }

  // 5. Category Match
  if (category.includes(term)) return 20;
  
  return 0; // Default
};

const AdminHomeSections = () => {
  const [sections, setSections] = useState([]);
  const [loading, setLoading] = useState(true);
  const [showModal, setShowModal] = useState(false);
  const [editing, setEditing] = useState(null);
  const [form, setForm] = useState(emptyForm);
  const [saving, setSaving] = useState(false);

  // Product picker state
  const [pickerSection, setPickerSection] = useState(null);
  const [allProducts, setAllProducts] = useState([]);
  const [productSearch, setProductSearch] = useState('');
  const [debouncedSearch, setDebouncedSearch] = useState('');
  const [loadingProducts, setLoadingProducts] = useState(false);
  
  // Pagination states
  const [pickerPage, setPickerPage] = useState(0);
  const [hasMoreProducts, setHasMoreProducts] = useState(true);

  const fetchSections = useCallback(async () => {
    setLoading(true);
    try {
      const res = await homeSectionApi.getAll();
      setSections(res.data.data || []);
    } catch { 
      toast.error('Failed to load sections'); 
    } finally { 
      setLoading(false); 
    }
  }, []);

  useEffect(() => { fetchSections(); }, [fetchSections]);

  // Handle Search Debouncing & Page Reset
  useEffect(() => {
    const timer = setTimeout(() => {
      setDebouncedSearch(productSearch.trim()); 
      setPickerPage(0); 
    }, 500);
    return () => clearTimeout(timer);
  }, [productSearch]);

  // Fetch Products (Supports Initial Load, Search, and Pagination)
  useEffect(() => {
    if (!pickerSection?.id) return;

    const fetchProductsForPicker = async () => {
      if (pickerPage === 0) setLoadingProducts(true);
      
      try {
        const res = await productApi.getAllAdmin({ 
          page: pickerPage, 
          size: 50, 
          search: debouncedSearch 
        });
        
        const newProducts = res.data.data?.content || [];
        
        if (pickerPage === 0) {
          setAllProducts(newProducts);
        } else {
          setAllProducts(prev => [...prev, ...newProducts]);
        }

        setHasMoreProducts(newProducts.length === 50); 
      } catch { 
        toast.error('Failed to load products'); 
      } finally { 
        setLoadingProducts(false); 
      }
    };

    fetchProductsForPicker();
  }, [debouncedSearch, pickerSection?.id, pickerPage]);

  // ── Section Handlers ──
  const openCreate = () => { setEditing(null); setForm(emptyForm); setShowModal(true); };
  
  const openEdit = (s) => {
    setEditing(s);
    setForm({ title: s.title, subtitle: s.subtitle || '', type: s.type, displayOrder: s.displayOrder, isActive: s.isActive, viewAllUrl: s.viewAllUrl || '' });
    setShowModal(true);
  };
  
  const closeModal = () => { setShowModal(false); setEditing(null); };

  const handleSave = async (e) => {
    e.preventDefault();
    setSaving(true);
    try {
      if (editing) {
        await homeSectionApi.update(editing.id, form);
        toast.success('Section updated!');
      } else {
        await homeSectionApi.create(form);
        toast.success('Section created!');
      }
      closeModal();
      fetchSections();
    } catch (err) {
      toast.error(err.response?.data?.message || 'Failed to save');
    } finally { setSaving(false); }
  };

  const handleDelete = async (id) => {
    if (!window.confirm('Delete this section? Products will not be deleted.')) return;
    try { await homeSectionApi.delete(id); toast.success('Section deleted'); fetchSections(); }
    catch { toast.error('Failed to delete'); }
  };

  const handleToggle = async (id) => {
    try { await homeSectionApi.toggle(id); fetchSections(); }
    catch { toast.error('Failed to toggle'); }
  };

  // ── Product Picker Handlers ──
  const openPicker = (section) => {
    setPickerSection(section);
    setProductSearch('');
    setDebouncedSearch('');
    setPickerPage(0);
    setAllProducts([]);
    setHasMoreProducts(true);
  };

  const closePicker = () => { 
    setPickerSection(null); 
    setAllProducts([]); 
  };

  const handleAddProduct = async (productId) => {
    try {
      await homeSectionApi.addProduct(pickerSection.id, productId);
      toast.success('Product added!');
      const res = await homeSectionApi.getAll();
      const updated = (res.data.data || []).find(s => s.id === pickerSection.id);
      setPickerSection(updated);
      setSections(res.data.data || []);
    } catch (err) { toast.error(err.response?.data?.message || 'Failed to add'); }
  };

  const handleRemoveProduct = async (productId) => {
    try {
      await homeSectionApi.removeProduct(pickerSection.id, productId);
      toast.success('Product removed');
      const res = await homeSectionApi.getAll();
      const updated = (res.data.data || []).find(s => s.id === pickerSection.id);
      setPickerSection(updated);
      setSections(res.data.data || []);
    } catch { toast.error('Failed to remove'); }
  };

  const sectionProductIds = new Set(pickerSection?.products?.map(p => p.id) || []);

  if (loading) return <AdminLayout><Loader /></AdminLayout>;

  return (
    <AdminLayout>
      <div className="admin-page">
        <div className="admin-page-header">
          <div>
            <h1 className="admin-page-title">Home Sections</h1>
            <p className="admin-page-subtitle">Manage sections shown on the homepage</p>
          </div>
          <button className="btn btn-primary" onClick={openCreate}><MdAdd size={18} /> Add Section</button>
        </div>

        {/* ── Sections list ── */}
        <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
          {sections.map(section => (
            <div key={section.id} style={{
              background: 'var(--bg-secondary)',
              border: `1px solid ${section.isActive ? 'var(--border-color)' : 'rgba(255,255,255,0.05)'}`,
              borderRadius: 12,
              padding: 20,
              opacity: section.isActive ? 1 : 0.6,
            }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', flexWrap: 'wrap', gap: 12 }}>
                <div>
                  <div style={{ display: 'flex', alignItems: 'center', gap: 10, marginBottom: 4 }}>
                    <span style={{ fontSize: 11, color: 'var(--text-muted)', border: '1px solid var(--border-color)', borderRadius: 4, padding: '2px 8px' }}>
                      #{section.displayOrder}
                    </span>
                    <span style={{ fontSize: 11, background: section.type === 'NEW_ARRIVAL' ? 'rgba(76,175,80,0.15)' : section.type === 'PREORDER' ? 'rgba(245,166,35,0.15)' : 'rgba(100,100,255,0.15)', color: section.type === 'NEW_ARRIVAL' ? '#4caf50' : section.type === 'PREORDER' ? '#f5a623' : '#8888ff', borderRadius: 4, padding: '2px 8px' }}>
                      {section.type}
                    </span>
                    {!section.isActive && <span style={{ fontSize: 11, color: 'var(--text-muted)' }}>Hidden</span>}
                  </div>
                  <h3 style={{ margin: 0, color: 'var(--text-primary)', fontSize: 16 }}>{section.title}</h3>
                  {section.subtitle && <p style={{ margin: '4px 0 0', color: 'var(--text-muted)', fontSize: 13 }}>{section.subtitle}</p>}
                  <p style={{ margin: '8px 0 0', color: 'var(--text-muted)', fontSize: 12 }}>
                    {section.productCount || 0} products
                    {section.viewAllUrl && <span> · <a href={section.viewAllUrl} style={{ color: 'var(--accent-primary)' }}>{section.viewAllUrl}</a></span>}
                  </p>

                  {/* Product thumbnails */}
                  {section.products?.length > 0 && (
                    <div style={{ display: 'flex', gap: 6, marginTop: 10, flexWrap: 'wrap' }}>
                      {section.products.slice(0, 8).map(p => (
                        <div key={p.id} style={{ position: 'relative' }}>
                          {p.primaryImageUrl
                            ? <img src={p.primaryImageUrl} alt={p.name} style={{ width: 44, height: 44, borderRadius: 6, objectFit: 'cover', border: '1px solid var(--border-color)' }} />
                            : <div style={{ width: 44, height: 44, borderRadius: 6, background: 'var(--bg-tertiary)', display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: 20 }}>🎌</div>
                          }
                        </div>
                      ))}
                      {section.products.length > 8 && (
                        <div style={{ width: 44, height: 44, borderRadius: 6, background: 'var(--bg-tertiary)', display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: 12, color: 'var(--text-muted)' }}>
                          +{section.products.length - 8}
                        </div>
                      )}
                    </div>
                  )}
                </div>

                <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap' }}>
                  <button className="btn btn-ghost btn-sm" onClick={() => openPicker(section)}>
                    🎌 Manage Products
                  </button>
                  <button className="btn btn-ghost btn-sm" onClick={() => openEdit(section)}>
                    <MdEdit size={14} /> Edit
                  </button>
                  <button className="btn btn-ghost btn-sm" onClick={() => handleToggle(section.id)}
                    title={section.isActive ? 'Hide section' : 'Show section'}>
                    {section.isActive ? <MdVisibilityOff size={14} /> : <MdVisibility size={14} />}
                  </button>
                  <button className="btn btn-ghost btn-sm" style={{ color: 'var(--accent-red)' }}
                    onClick={() => handleDelete(section.id)}>
                    <MdDelete size={14} />
                  </button>
                </div>
              </div>
            </div>
          ))}
          {sections.length === 0 && (
            <div style={{ textAlign: 'center', padding: 48, color: 'var(--text-muted)' }}>
              No sections yet. Create your first section!
            </div>
          )}
        </div>
      </div>

      {/* ── Create/Edit Modal ── */}
      {showModal && (
        <div className="modal-overlay" onClick={closeModal}>
          <div className="modal" style={{ maxWidth: 520 }} onClick={e => e.stopPropagation()}>
            <div className="modal-header">
              <h3>{editing ? 'Edit Section' : 'New Section'}</h3>
              <button className="modal-close" onClick={closeModal}>✕</button>
            </div>
            <form onSubmit={handleSave}>
              <div className="modal-body">
                <div className="form-group">
                  <label className="form-label">Title *</label>
                  <input className="form-input" value={form.title}
                    onChange={e => setForm({ ...form, title: e.target.value })} required placeholder="e.g. New Arrivals" />
                </div>
                <div className="form-group">
                  <label className="form-label">Subtitle</label>
                  <input className="form-input" value={form.subtitle}
                    onChange={e => setForm({ ...form, subtitle: e.target.value })} placeholder="Optional subtitle" />
                </div>
                <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12 }}>
                  <div className="form-group">
                    <label className="form-label">Type</label>
                    <select className="form-select" value={form.type}
                      onChange={e => setForm({ ...form, type: e.target.value })}>
                      {TYPE_OPTIONS.map(t => <option key={t} value={t}>{t}</option>)}
                    </select>
                  </div>
                  <div className="form-group">
                    <label className="form-label">Display Order</label>
                    <input className="form-input" type="number" min="0" value={form.displayOrder}
                      onChange={e => setForm({ ...form, displayOrder: Number(e.target.value) })} />
                  </div>
                </div>
                <div className="form-group">
                  <label className="form-label">View All URL</label>
                  <input className="form-input" value={form.viewAllUrl}
                    onChange={e => setForm({ ...form, viewAllUrl: e.target.value })} placeholder="/products" />
                </div>
                <label style={{ display: 'flex', alignItems: 'center', gap: 8, cursor: 'pointer' }}>
                  <input type="checkbox" checked={form.isActive}
                    onChange={e => setForm({ ...form, isActive: e.target.checked })} />
                  <span className="form-label" style={{ margin: 0 }}>Active (visible on homepage)</span>
                </label>
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

      {/* ── Product Picker Modal ── */}
      {pickerSection && (
        <div className="modal-overlay" onClick={closePicker}>
          <div className="modal" style={{ maxWidth: 720, width: '95%', maxHeight: '85vh', display: 'flex', flexDirection: 'column' }} onClick={e => e.stopPropagation()}>
            <div className="modal-header" style={{ flexShrink: 0 }}>
              <h3>Manage Products — {pickerSection.title}</h3>
              <button className="modal-close" onClick={closePicker}>✕</button>
            </div>
            
            {/* NEW: Modal Body Flex Structure */}
            <div className="modal-body" style={{ display: 'flex', flexDirection: 'column', flex: 1, overflow: 'hidden', padding: 0 }}>

              {/* Top Section: Selected items & Search Input */}
              <div style={{ padding: '20px 20px 10px', flexShrink: 0, display: 'flex', flexDirection: 'column', gap: 16, borderBottom: '1px solid var(--border-color)' }}>
                {pickerSection.products?.length > 0 && (
                  <div>
                    <p className="form-label" style={{ marginBottom: 10 }}>
                      In this section ({pickerSection.products.length})
                    </p>
                    {/* Fixed Height with Internal Scroll for Selected Items */}
                    <div style={{ display: 'flex', flexWrap: 'wrap', gap: 8, maxHeight: '140px', overflowY: 'auto', paddingRight: 4 }}>
                      {pickerSection.products.map(p => (
                        <div key={p.id} style={{ display: 'flex', alignItems: 'center', gap: 8, background: 'var(--bg-tertiary)', border: '1px solid var(--border-color)', borderRadius: 8, padding: '6px 10px' }}>
                          {p.primaryImageUrl && <img src={p.primaryImageUrl} alt={p.name} style={{ width: 32, height: 32, borderRadius: 4, objectFit: 'cover' }} />}
                          <span style={{ fontSize: 13, color: 'var(--text-primary)', maxWidth: 140, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>{p.name}</span>
                          <button onClick={() => handleRemoveProduct(p.id)}
                            style={{ background: 'none', border: 'none', cursor: 'pointer', color: 'var(--accent-red)', padding: 0, display: 'flex' }}>
                            <MdClose size={16} />
                          </button>
                        </div>
                      ))}
                    </div>
                  </div>
                )}

                <div>
                  <p className="form-label" style={{ marginBottom: 8 }}>Search Directory</p>
                  <div style={{ position: 'relative' }}>
                    <MdSearch size={18} style={{ position: 'absolute', left: 12, top: '50%', transform: 'translateY(-50%)', color: 'var(--text-muted)' }} />
                    <input className="form-input" style={{ paddingLeft: 36 }}
                      placeholder="Search all products by name, SKU, category, or ID..."
                      value={productSearch}
                      onChange={e => setProductSearch(e.target.value)} />
                  </div>
                </div>
              </div>

              {/* Bottom Section: Search Results (Expands to take remaining space) */}
              <div style={{ flex: 1, overflowY: 'auto', padding: '10px 20px 20px', minHeight: 0 }}>
                {loadingProducts && pickerPage === 0 ? <Loader /> : (
                  <div style={{ display: 'flex', flexDirection: 'column', gap: 6 }}>
                    {[...allProducts]
                      .sort((a, b) => {
                        if (!debouncedSearch) return 0;
                        return getSearchRank(b, debouncedSearch) - getSearchRank(a, debouncedSearch);
                      })
                      .map(p => {
                        const inSection = sectionProductIds.has(p.id);
                        return (
                          <div key={p.id} style={{ flexShrink: 0, display: 'flex', alignItems: 'center', gap: 12, padding: '8px 12px', borderRadius: 8, background: inSection ? 'rgba(76,175,80,0.06)' : 'var(--bg-tertiary)', border: `1px solid ${inSection ? 'rgba(76,175,80,0.2)' : 'var(--border-color)'}` }}>
                            {p.primaryImageUrl
                              ? <img src={p.primaryImageUrl} alt={p.name} style={{ width: 40, height: 40, borderRadius: 6, objectFit: 'cover', flexShrink: 0 }} />
                              : <div style={{ width: 40, height: 40, borderRadius: 6, background: 'var(--bg-secondary)', display: 'flex', alignItems: 'center', justifyContent: 'center', flexShrink: 0 }}>🎌</div>
                            }
                            <div style={{ flex: 1, minWidth: 0 }}>
                              <p style={{ margin: 0, fontSize: 14, color: 'var(--text-primary)', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>{p.name}</p>
                              <p style={{ margin: 0, fontSize: 12, color: 'var(--text-muted)' }}>
                                ₹{p.discountedPrice || p.price} {p.sku ? `· SKU: ${p.sku}` : ''} {p.category?.name ? `· ${p.category?.name}` : ''}
                              </p>
                            </div>
                            {inSection ? (
                              <button className="btn btn-ghost btn-sm" style={{ color: 'var(--accent-red)', flexShrink: 0 }}
                                onClick={() => handleRemoveProduct(p.id)}>
                                Remove
                              </button>
                            ) : (
                              <button className="btn btn-ghost btn-sm" style={{ color: 'var(--accent-primary)', flexShrink: 0 }}
                                onClick={() => handleAddProduct(p.id)}>
                                + Add
                              </button>
                            )}
                          </div>
                        );
                      })}
                    
                    {allProducts.length === 0 && (
                      <p style={{ textAlign: 'center', color: 'var(--text-muted)', padding: 24 }}>No products found</p>
                    )}

                    {hasMoreProducts && allProducts.length > 0 && (
                      <button 
                        className="btn btn-ghost" 
                        style={{ flexShrink: 0, marginTop: 12, padding: 12, width: '100%', border: '1px dashed var(--border-color)' }}
                        onClick={() => setPickerPage(prev => prev + 1)}
                        disabled={loadingProducts}
                      >
                        {loadingProducts ? 'Loading...' : 'Load More Products'}
                      </button>
                    )}
                  </div>
                )}
              </div>
            </div>
          </div>
        </div>
      )}
    </AdminLayout>
  );
};

export default AdminHomeSections;