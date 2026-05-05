// src/pages/admin/AdminCategories.jsx
import { useState, useEffect, useCallback } from 'react';
import AdminLayout from '../../components/admin/AdminLayout';
import Loader from '../../components/common/Loader';
import { categoryApi } from '../../api/categoryApi';
import toast from 'react-hot-toast';

const AdminCategories = () => {
  const [categories, setCategories] = useState([]);
  const [loading, setLoading] = useState(true);
  const [showModal, setShowModal] = useState(false);
  const [editing, setEditing] = useState(null);
  const [form, setForm] = useState({ name: '', description: '' });
  const [saving, setSaving] = useState(false);
  const [imageFile, setImageFile] = useState(null);
  const [uploadingId, setUploadingId] = useState(null);

  const fetchCategories = useCallback(async () => {
    try {
      const res = await categoryApi.getAllAdmin();
      setCategories(res.data.data || []);
    } catch { toast.error('Failed to load categories'); }
    finally { setLoading(false); }
  }, []);

  useEffect(() => { fetchCategories(); }, [fetchCategories]);

  const openCreate = () => { setEditing(null); setForm({ name: '', description: '' }); setShowModal(true); };
  const openEdit = (cat) => { setEditing(cat); setForm({ name: cat.name, description: cat.description || '' }); setShowModal(true); };
  const closeModal = () => { setShowModal(false); setEditing(null); };

  const handleSave = async (e) => {
    e.preventDefault();
    setSaving(true);
    try {
      if (editing) {
        await categoryApi.update(editing.id, form);
        toast.success('Category updated!');
      } else {
        await categoryApi.create(form);
        toast.success('Category created!');
      }
      closeModal();
      fetchCategories();
    } catch (err) {
      toast.error(err.response?.data?.message || 'Failed to save');
    } finally { setSaving(false); }
  };

  const handleDelete = async (id) => {
    if (!window.confirm('Delete this category?')) return;
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
    } catch { toast.error('Image upload failed'); }
    finally { setUploadingId(null); }
  };

  if (loading) return <AdminLayout><Loader fullPage /></AdminLayout>;

  return (
    <AdminLayout>
      <div className="admin-page-header">
        <div>
          <h1 className="admin-page-title">Categories</h1>
          <p className="admin-page-subtitle">{categories.length} categories total</p>
        </div>
        <button className="btn btn-primary" onClick={openCreate}>+ Add Category</button>
      </div>

      <div className="admin-table-wrapper">
        <table className="admin-table">
          <thead>
            <tr>
              <th>Image</th><th>Name</th><th>Description</th>
              <th>Status</th><th>Upload Image</th><th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {categories.map(cat => (
              <tr key={cat.id}>
                <td>
                  {cat.imageUrl
                    ? <img src={cat.imageUrl} alt={cat.name} style={{ width: 48, height: 48, borderRadius: 8, objectFit: 'cover' }} />
                    : <div style={{ width: 48, height: 48, background: 'var(--bg-hover)', borderRadius: 8, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>🎌</div>
                  }
                </td>
                <td style={{ fontWeight: 600, color: 'var(--text-primary)' }}>{cat.name}</td>
                <td>{cat.description || <span className="text-muted">—</span>}</td>
                <td>
                  <span className={`badge ${cat.isDeleted ? 'badge-red' : 'badge-green'}`}>
                    {cat.isDeleted ? 'Deleted' : 'Active'}
                  </span>
                </td>
                <td>
                  <div style={{ display: 'flex', gap: 8, alignItems: 'center' }}>
                    <input type="file" accept="image/*" style={{ fontSize: 12, maxWidth: 140 }}
                      onChange={(e) => setImageFile(e.target.files[0])} />
                    <button className="btn btn-ghost btn-sm"
                      onClick={() => handleImageUpload(cat.id)}
                      disabled={uploadingId === cat.id || !imageFile}>
                      {uploadingId === cat.id ? '...' : '↑'}
                    </button>
                  </div>
                </td>
                <td>
                  <div style={{ display: 'flex', gap: 8 }}>
                    <button className="btn btn-ghost btn-sm" onClick={() => openEdit(cat)}>Edit</button>
                    {!cat.isDeleted && (
                      <button className="btn btn-sm"
                        style={{ background: 'rgba(244,67,54,0.1)', color: 'var(--accent-red)', border: '1px solid rgba(244,67,54,0.2)' }}
                        onClick={() => handleDelete(cat.id)}>Delete</button>
                    )}
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {showModal && (
        <div className="modal-overlay" onClick={closeModal}>
          <div className="modal" onClick={e => e.stopPropagation()}>
            <div className="modal-header">
              <h3 className="modal-title">{editing ? 'Edit Category' : 'New Category'}</h3>
              <button className="modal-close" onClick={closeModal}>✕</button>
            </div>
            <form onSubmit={handleSave}>
              <div className="modal-body">
                <div className="form-group">
                  <label className="form-label">Category Name *</label>
                  <input className="form-input" value={form.name}
                    onChange={e => setForm({ ...form, name: e.target.value })} required placeholder="e.g. Figures" />
                </div>
                <div className="form-group">
                  <label className="form-label">Description</label>
                  <textarea className="form-input" rows={3} value={form.description}
                    onChange={e => setForm({ ...form, description: e.target.value })}
                    placeholder="Optional description" style={{ resize: 'vertical' }} />
                </div>
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