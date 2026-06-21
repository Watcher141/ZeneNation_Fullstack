// src/pages/admin/AdminAnnouncements.jsx
import { useState, useEffect, useCallback } from 'react';
import AdminLayout from '../../components/admin/AdminLayout';
import Loader from '../../components/common/Loader';
import { announcementApi } from '../../api/apiCollections';
import { MdCampaign, MdLocalOffer, MdWarning, MdCheckCircle, MdEmail, MdPeople, MdClose, MdCancel } from 'react-icons/md';
import toast from 'react-hot-toast';

const TYPE_OPTIONS = ['INFO', 'DEAL', 'WARNING', 'SUCCESS'];

const typeIcon = { 
  INFO: <MdCampaign size={16} />, 
  DEAL: <MdLocalOffer size={16} />, 
  WARNING: <MdWarning size={16} />, 
  SUCCESS: <MdCheckCircle size={16} /> 
};
const typeBadge = { INFO: 'badge-blue', DEAL: 'badge-gold', WARNING: 'badge-red', SUCCESS: 'badge-green' };

const emptyForm = {
  title: '', message: '', type: 'INFO', isActive: true,
  startsAt: '', endsAt: '', sendEmailBlast: false,
};

const AdminAnnouncements = () => {
  const [announcements, setAnnouncements] = useState([]);
  const [pagination, setPagination] = useState({});
  const [loading, setLoading] = useState(true);
  const [subscriberCount, setSubscriberCount] = useState(0);
  const [showModal, setShowModal] = useState(false);
  const [editing, setEditing] = useState(null);
  const [form, setForm] = useState(emptyForm);
  const [saving, setSaving] = useState(false);
  const [page, setPage] = useState(0);

  const fetchAnnouncements = useCallback(async () => {
    setLoading(true);
    try {
      const [annRes, subRes] = await Promise.all([
        announcementApi.getAll({ page, size: 10 }),
        announcementApi.getSubscriberCount(),
      ]);
      setAnnouncements(annRes.data.data?.content || []);
      setPagination(annRes.data.data || {});
      setSubscriberCount(subRes.data.data || 0);
    } catch { toast.error('Failed to load'); }
    finally { setLoading(false); }
  }, [page]);

  useEffect(() => { fetchAnnouncements(); }, [fetchAnnouncements]);

  const openCreate = () => { setEditing(null); setForm(emptyForm); setShowModal(true); };
  const openEdit = (a) => {
    setEditing(a);
    setForm({
      title: a.title, message: a.message, type: a.type,
      isActive: a.isActive, sendEmailBlast: false,
      startsAt: a.startsAt ? a.startsAt.slice(0, 16) : '',
      endsAt: a.endsAt ? a.endsAt.slice(0, 16) : '',
    });
    setShowModal(true);
  };

  const handleSave = async (e) => {
    e.preventDefault();
    setSaving(true);
    try {
      const payload = {
        ...form,
        startsAt: form.startsAt || null,
        endsAt: form.endsAt || null,
      };
      if (editing) {
        await announcementApi.update(editing.id, payload);
        toast.success('Announcement updated!');
      } else {
        await announcementApi.create(payload);
        toast.success(form.sendEmailBlast
          ? `Announcement created! Email sent to ${subscriberCount} subscribers.`
          : 'Announcement created!');
      }
      setShowModal(false);
      fetchAnnouncements();
    } catch (err) {
      toast.error(err.response?.data?.message || 'Failed to save');
    } finally { setSaving(false); }
  };

  const handleDelete = async (id) => {
    if (!window.confirm('Delete this announcement?')) return;
    try {
      await announcementApi.delete(id);
      toast.success('Deleted');
      fetchAnnouncements();
    } catch { toast.error('Failed to delete'); }
  };

  const handleToggle = async (id) => {
    try {
      await announcementApi.toggle(id);
      toast.success('Updated');
      fetchAnnouncements();
    } catch { toast.error('Failed'); }
  };

  if (loading) return <AdminLayout><Loader fullPage /></AdminLayout>;

  return (
    <AdminLayout>
      <div className="admin-page-header">
        <div>
          <h1 className="admin-page-title">Announcements</h1>
          <p className="admin-page-subtitle">
            {pagination.totalElements || 0} announcements ·
            <span style={{ color: 'var(--accent-secondary)', marginLeft: 8 }}>
              <MdPeople size={14} style={{ verticalAlign: 'middle' }} /> {subscriberCount} subscribers
            </span>
          </p>
        </div>
        <button className="btn btn-primary" onClick={openCreate}>+ New Announcement</button>
      </div>

      {/* Stats */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: 'var(--space-4)', marginBottom: 'var(--space-6)' }}>
        {TYPE_OPTIONS.map(type => {
          const count = announcements.filter(a => a.type === type).length;
          return (
            <div key={type} className={`stat-card stat-card-${type === 'INFO' ? 'accent' : type === 'DEAL' ? 'gold' : type === 'WARNING' ? 'red' : 'green'}`}>
              <div className="stat-card-icon-wrap" style={{ fontSize: '1.5rem' }}>{typeIcon[type]}</div>
              <div className="stat-card-value">{count}</div>
              <div className="stat-card-label">{type}</div>
            </div>
          );
        })}
      </div>

      <div className="admin-table-wrapper">
        <table className="admin-table">
          <thead>
            <tr>
              <th>Type</th><th>Title</th><th>Message</th>
              <th>Email Sent</th><th>Schedule</th><th>Status</th><th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {announcements.length === 0 ? (
              <tr><td colSpan={7} style={{ textAlign: 'center', color: 'var(--text-muted)', padding: '2rem' }}>No announcements yet</td></tr>
            ) : announcements.map(a => (
              <tr key={a.id}>
                <td>
                  <span className={`badge ${typeBadge[a.type]}`}>
                    {typeIcon[a.type]} {a.type}
                  </span>
                </td>
                <td style={{ fontWeight: 600, color: 'var(--text-primary)', maxWidth: 160 }}>{a.title}</td>
                <td style={{ maxWidth: 200, color: 'var(--text-muted)', fontSize: 'var(--text-xs)' }}>
                  {a.message.length > 80 ? a.message.slice(0, 80) + '...' : a.message}
                </td>
                <td>
                  <span className={`badge ${a.emailSent ? 'badge-green' : 'badge-red'}`}
                    style={{ display: 'inline-flex', alignItems: 'center', gap: 4 }}>
                    {a.emailSent ? <><MdCheckCircle size={12} /> Sent</> : <><MdCancel size={12} /> Not sent</>}
                  </span>
                </td>
                <td className="text-xs text-muted">
                  {a.startsAt ? new Date(a.startsAt).toLocaleDateString('en-IN') : '—'}
                  {' → '}
                  {a.endsAt ? new Date(a.endsAt).toLocaleDateString('en-IN') : '∞'}
                </td>
                <td>
                  <span className={`badge ${a.isCurrentlyActive ? 'badge-green' : 'badge-red'}`}>
                    {a.isCurrentlyActive ? 'Live' : 'Hidden'}
                  </span>
                </td>
                <td>
                  <div style={{ display: 'flex', gap: 6 }}>
                    <button className="btn btn-ghost btn-sm" onClick={() => openEdit(a)}>Edit</button>
                    <button className="btn btn-ghost btn-sm" onClick={() => handleToggle(a.id)}>
                      {a.isActive ? 'Hide' : 'Show'}
                    </button>
                    <button className="btn btn-sm"
                      style={{ background: 'rgba(244,67,54,0.1)', color: 'var(--accent-red)', border: '1px solid rgba(244,67,54,0.2)' }}
                      onClick={() => handleDelete(a.id)}>Del</button>
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

      {/* Modal */}
      {showModal && (
        <div className="modal-overlay" onClick={() => setShowModal(false)}>
          <div className="modal" style={{ maxWidth: 580 }} onClick={e => e.stopPropagation()}>
            <div className="modal-header">
              <h3 className="modal-title">{editing ? 'Edit Announcement' : 'New Announcement'}</h3>
              <button className="modal-close" onClick={() => setShowModal(false)}><MdClose size={18} /></button>
            </div>
            <form onSubmit={handleSave}>
              <div className="modal-body">
                <div className="form-group">
                  <label className="form-label">Title *</label>
                  <input className="form-input" value={form.title}
                    onChange={e => setForm({ ...form, title: e.target.value })} required
                    placeholder="e.g. 20% Off All Figures This Weekend!" />
                </div>
                <div className="form-group">
                  <label className="form-label">Message *</label>
                  <textarea className="form-input" rows={4} value={form.message}
                    onChange={e => setForm({ ...form, message: e.target.value })} required
                    placeholder="Full announcement text..." style={{ resize: 'vertical' }} />
                </div>
                <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 'var(--space-4)' }}>
                  <div className="form-group">
                    <label className="form-label">Type</label>
                    <select className="form-select" value={form.type}
                      onChange={e => setForm({ ...form, type: e.target.value })}>
                      {TYPE_OPTIONS.map(t => <option key={t} value={t}>{typeIcon[t]} {t}</option>)}
                    </select>
                  </div>
                  <div className="form-group" style={{ display: 'flex', alignItems: 'center', gap: 8, paddingTop: 24 }}>
                    <input type="checkbox" id="isActiveCheck" checked={form.isActive}
                      onChange={e => setForm({ ...form, isActive: e.target.checked })} />
                    <label htmlFor="isActiveCheck" className="form-label" style={{ margin: 0 }}>Show on website</label>
                  </div>
                  <div className="form-group">
                    <label className="form-label">Start Date (optional)</label>
                    <input className="form-input" type="datetime-local" value={form.startsAt}
                      onChange={e => setForm({ ...form, startsAt: e.target.value })} />
                  </div>
                  <div className="form-group">
                    <label className="form-label">End Date (optional)</label>
                    <input className="form-input" type="datetime-local" value={form.endsAt}
                      onChange={e => setForm({ ...form, endsAt: e.target.value })} />
                  </div>
                </div>

                {/* Email blast option */}
                {!editing?.emailSent && (
                  <div style={{
                    background: 'rgba(245,166,35,0.08)', border: '1px solid rgba(245,166,35,0.3)',
                    borderRadius: 'var(--radius-md)', padding: 'var(--space-4)'
                  }}>
                    <label style={{ display: 'flex', alignItems: 'center', gap: 8, cursor: 'pointer' }}>
                      <input type="checkbox" checked={form.sendEmailBlast}
                        onChange={e => setForm({ ...form, sendEmailBlast: e.target.checked })} />
                      <MdEmail size={18} color="var(--accent-secondary)" />
                      <div>
                        <span style={{ fontWeight: 600, fontSize: 'var(--text-sm)' }}>
                          Send Email Blast to {subscriberCount} Subscribers
                        </span>
                        <p className="text-xs text-muted" style={{ margin: '2px 0 0' }}>
                          Immediately sends this announcement to all active subscribers
                        </p>
                      </div>
                    </label>
                  </div>
                )}
              </div>
              <div className="modal-footer">
                <button type="button" className="btn btn-ghost" onClick={() => setShowModal(false)}>Cancel</button>
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

export default AdminAnnouncements;