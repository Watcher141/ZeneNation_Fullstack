// src/pages/admin/AdminCategories.jsx
import { useState, useEffect, useCallback } from 'react';
import AdminLayout from '../../components/admin/AdminLayout';
import Loader from '../../components/common/Loader';
import { categoryApi } from '../../api/categoryApi';
import toast from 'react-hot-toast';
import { MdAdd, MdEdit, MdDelete, MdImage, MdExpandMore, MdExpandLess, MdSubdirectoryArrowRight, MdClose } from 'react-icons/md';

const emptyForm = { name: '', description: '', parentId: '' };

const AdminCategories = () => {
  const [categories, setCategories] = useState([]);
  const [loading, setLoading] = useState(true);
  const [showModal, setShowModal] = useState(false);
  const [editing, setEditing] = useState(null);
  const [form, setForm] = useState(emptyForm);
  const [saving, setSaving] = useState(false);
  const [imageFile, setImageFile] = useState(null);
  const [uploadingId, setUploadingId] = useState(null);
  const [expanded, setExpanded] = useState({});

  const fetchCategories = useCallback(async () => {
    try {
      const res = await categoryApi.getAllAdmin();
      setCategories(res.data.data || []);
    } catch { toast.error('Failed to load categories'); }
    finally { setLoading(false); }
  }, []);

  useEffect(() => { fetchCategories(); }, [fetchCategories]);

  const toggleExpand = (id) => setExpanded(prev => ({ ...prev, [id]: !prev[id] }));

  const openCreate = (parentId = null) => {
    setEditing(null);
    setForm({ ...emptyForm, parentId: parentId || '' });
    setShowModal(true);
  };

  const openEdit = (cat) => {
    setEditing(cat);
    setForm({ name: cat.name, description: cat.description || '', parentId: cat.parentId || '' });
    setShowModal(true);
  };

  const closeModal = () => { setShowModal(false); setEditing(null); setForm(emptyForm); };

  const handleSave = async (e) => {
    e.preventDefault();
    setSaving(true);
    try {
      const payload = {
        name: form.name,
        description: form.description,
        parentId: form.parentId ? Number(form.parentId) : null,
      };
      if (editing) {
        await categoryApi.update(editing.id, payload);
        toast.success('Category updated!');
      } else {
        await categoryApi.create(payload);
        toast.success('Category created!');
      }
      closeModal();
      fetchCategories();
    } catch (err) {
      toast.error(err.response?.data?.message || 'Failed to save');
    } finally { setSaving(false); }
  };

  const handleDelete = async (id) => {
    if (!window.confirm('Delete this category and all its subcategories?')) return;
    try {
      await categoryApi.delete(id);
      toast.success('Category deleted');
      fetchCategories();
    } catch { toast.error('Failed to delete'); }
  };

  const handleImageUpload = async (catId) => {
    if (!imageFile) return;
    setUploadingId(catId);
    try {
      const formData = new FormData();
      formData.append('image', imageFile);
      await categoryApi.uploadImage(catId, formData);
      toast.success('Image uploaded!');
      setImageFile(null);
      fetchCategories();
    } catch { toast.error('Failed to upload image'); }
    finally { setUploadingId(null); }
  };

  // All top-level categories for parent selector
  const topLevelCategories = categories.filter(c => !c.parentId);

  if (loading) return <AdminLayout><Loader /></AdminLayout>;

  return (
    <AdminLayout>
      <div className="admin-page">
        <div className="admin-page-header">
          <div>
            <h1 className="admin-page-title">Categories</h1>
            <p className="text-muted text-sm">{categories.length} total · {topLevelCategories.length} top-level</p>
          </div>
          <button className="btn btn-primary" onClick={() => openCreate()}>
            <MdAdd size={18} /> Add Category
          </button>
        </div>

        <div className="admin-table-wrap">
          <table className="admin-table">
            <thead>
              <tr>
                <th>Category</th>
                <th>Description</th>
                <th>Image</th>
                <th>Products</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {topLevelCategories.map(cat => (
                <>
                  {/* Top-level category row */}
                  <tr key={cat.id} style={{ background: 'var(--bg-secondary)' }}>
                    <td>
                      <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                        {cat.subcategories?.length > 0 && (
                          <button
                            onClick={() => toggleExpand(cat.id)}
                            style={{ background: 'none', border: 'none', cursor: 'pointer', color: 'var(--accent-primary)', padding: 0 }}>
                            {expanded[cat.id] ? <MdExpandLess size={18} /> : <MdExpandMore size={18} />}
                          </button>
                        )}
                        <strong style={{ color: 'var(--text-primary)' }}>{cat.name}</strong>
                        {cat.subcategories?.length > 0 && (
                          <span className="badge badge-blue" style={{ fontSize: 10 }}>
                            {cat.subcategories.length} sub
                          </span>
                        )}
                        {cat.isDeleted && <span className="badge badge-red">Deleted</span>}
                      </div>
                    </td>
                    <td className="text-muted text-sm">{cat.description || '—'}</td>
                    <td>
                      <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                        {cat.imageUrl
                          ? <img src={cat.imageUrl} alt={cat.name} style={{ width: 40, height: 40, borderRadius: 4, objectFit: 'cover' }} />
                          : <div style={{ width: 40, height: 40, background: 'var(--bg-tertiary)', borderRadius: 4, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                              <MdImage size={20} color="var(--text-muted)" />
                            </div>
                        }
                        <div style={{ display: 'flex', flexDirection: 'column', gap: 4 }}>
                          <input type="file" accept="image/*" style={{ fontSize: 10, maxWidth: 120 }}
                            onChange={e => setImageFile(e.target.files[0])} />
                          <button className="btn btn-ghost btn-sm" onClick={() => handleImageUpload(cat.id)}
                            disabled={uploadingId === cat.id}>
                            {uploadingId === cat.id ? 'Uploading...' : 'Upload'}
                          </button>
                        </div>
                      </div>
                    </td>
                    <td className="text-muted text-sm">{cat.productCount || 0}</td>
                    <td>
                      <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap' }}>
                        <button className="btn btn-ghost btn-sm" onClick={() => openEdit(cat)}>
                          <MdEdit size={14} /> Edit
                        </button>
                        <button className="btn btn-ghost btn-sm" style={{ color: 'var(--accent-primary)' }}
                          onClick={() => openCreate(cat.id)}>
                          <MdAdd size={14} /> Sub
                        </button>
                        {!cat.isDeleted && (
                          <button className="btn btn-ghost btn-sm" style={{ color: 'var(--accent-red)' }}
                            onClick={() => handleDelete(cat.id)}>
                            <MdDelete size={14} />
                          </button>
                        )}
                      </div>
                    </td>
                  </tr>

                  {/* Subcategory rows */}
                  {expanded[cat.id] && cat.subcategories?.map(sub => (
                    <tr key={sub.id} style={{ background: 'rgba(0,0,0,0.15)' }}>
                      <td>
                        <div style={{ display: 'flex', alignItems: 'center', gap: 8, paddingLeft: 32 }}>
                          <MdSubdirectoryArrowRight size={16} color="var(--text-muted)" />
                          <span style={{ color: 'var(--text-secondary)' }}>{sub.name}</span>
                          {sub.isDeleted && <span className="badge badge-red">Deleted</span>}
                        </div>
                      </td>
                      <td className="text-muted text-sm">{sub.description || '—'}</td>
                      <td>
                        <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                          {sub.imageUrl
                            ? <img src={sub.imageUrl} alt={sub.name} style={{ width: 40, height: 40, borderRadius: 4, objectFit: 'cover' }} />
                            : <div style={{ width: 40, height: 40, background: 'var(--bg-tertiary)', borderRadius: 4, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                                <MdImage size={20} color="var(--text-muted)" />
                              </div>
                          }
                          <div style={{ display: 'flex', flexDirection: 'column', gap: 4 }}>
                            <input type="file" accept="image/*" style={{ fontSize: 10, maxWidth: 120 }}
                              onChange={e => setImageFile(e.target.files[0])} />
                            <button className="btn btn-ghost btn-sm" onClick={() => handleImageUpload(sub.id)}
                              disabled={uploadingId === sub.id}>
                              {uploadingId === sub.id ? 'Uploading...' : 'Upload'}
                            </button>
                          </div>
                        </div>
                      </td>
                      <td className="text-muted text-sm">{sub.productCount || 0}</td>
                      <td>
                        <div style={{ display: 'flex', gap: 8 }}>
                          <button className="btn btn-ghost btn-sm" onClick={() => openEdit(sub)}>
                            <MdEdit size={14} /> Edit
                          </button>
                          {!sub.isDeleted && (
                            <button className="btn btn-ghost btn-sm" style={{ color: 'var(--accent-red)' }}
                              onClick={() => handleDelete(sub.id)}>
                              <MdDelete size={14} />
                            </button>
                          )}
                        </div>
                      </td>
                    </tr>
                  ))}
                </>
              ))}
            </tbody>
          </table>
        </div>
      </div>

      {/* Modal */}
      {showModal && (
        <div className="modal-overlay" onClick={closeModal}>
          <div className="modal" onClick={e => e.stopPropagation()}>
            <div className="modal-header">
              <h2>{editing ? 'Edit Category' : 'Add Category'}</h2>
              <button className="modal-close" onClick={closeModal}><MdClose size={18} /></button>
            </div>
            <form onSubmit={handleSave} className="modal-body">
              <div className="form-group">
                <label className="form-label">Category Name *</label>
                <input className="form-input" value={form.name}
                  onChange={e => setForm({ ...form, name: e.target.value })}
                  placeholder="e.g. Naruto Figures" required />
              </div>

              <div className="form-group">
                <label className="form-label">
                  Parent Category
                  <span className="text-muted" style={{ fontWeight: 400, fontSize: 12, marginLeft: 8 }}>
                    (leave empty for top-level)
                  </span>
                </label>
                <select className="form-input" value={form.parentId}
                  onChange={e => setForm({ ...form, parentId: e.target.value })}>
                  <option value="">— Top-level category —</option>
                  {topLevelCategories
                    .filter(c => !editing || c.id !== editing.id)
                    .map(c => (
                      <option key={c.id} value={c.id}>{c.name}</option>
                    ))}
                </select>
                {form.parentId && (
                  <p className="text-xs text-muted" style={{ marginTop: 4 }}>
                    This will be a subcategory under <strong>{topLevelCategories.find(c => c.id == form.parentId)?.name}</strong>
                  </p>
                )}
              </div>

              <div className="form-group">
                <label className="form-label">Description</label>
                <textarea className="form-input" rows={3} value={form.description}
                  onChange={e => setForm({ ...form, description: e.target.value })}
                  placeholder="Optional description" />
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

export default AdminCategories;