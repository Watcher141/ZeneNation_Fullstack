// src/components/product/ProductReviews.jsx
import { useState, useEffect, useCallback } from 'react';
import { useAuth } from '../../context/AuthContext';
import { reviewApi } from '../../api/apiCollections';
import StarRating from './StarRating';
import toast from 'react-hot-toast';
import { MdVerified, MdEdit, MdDelete, MdSend } from 'react-icons/md';
import './ProductReviews.css';

const ProductReviews = ({ productId }) => {
  const { user, isAuthenticated } = useAuth();
  const [summary, setSummary] = useState(null);
  const [reviews, setReviews] = useState([]);
  const [pagination, setPagination] = useState({});
  const [page, setPage] = useState(0);
  const [loading, setLoading] = useState(true);
  const [showForm, setShowForm] = useState(false);
  const [editMode, setEditMode] = useState(false);
  const [form, setForm] = useState({ rating: 0, title: '', body: '' });
  const [submitting, setSubmitting] = useState(false);

  const fetchSummary = useCallback(async () => {
    try {
      const res = await reviewApi.getSummary(productId);
      setSummary(res.data.data);
    } catch { /* silent */ }
  }, [productId]);

  const fetchReviews = useCallback(async () => {
    setLoading(true);
    try {
      const res = await reviewApi.getReviews(productId, { page, size: 5 });
      setReviews(res.data.data?.content || []);
      setPagination(res.data.data || {});
    } catch { /* silent */ }
    finally { setLoading(false); }
  }, [productId, page]);

  useEffect(() => { fetchSummary(); }, [fetchSummary]);
  useEffect(() => { fetchReviews(); }, [fetchReviews]);

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!form.rating) { toast.error('Please select a star rating'); return; }
    setSubmitting(true);
    try {
      if (editMode) {
        await reviewApi.updateReview(productId, form);
        toast.success('Review updated!');
      } else {
        await reviewApi.submitReview(productId, form);
        toast.success('Review submitted!');
      }
      setShowForm(false);
      setEditMode(false);
      setForm({ rating: 0, title: '', body: '' });
      await fetchSummary();
      setPage(0);
      await fetchReviews();
    } catch (err) {
      toast.error(err.response?.data?.message || 'Failed to submit review');
    } finally { setSubmitting(false); }
  };

  const handleDelete = async () => {
    if (!window.confirm('Delete your review?')) return;
    try {
      await reviewApi.deleteReview(productId);
      toast.success('Review deleted');
      setShowForm(false);
      setForm({ rating: 0, title: '', body: '' });
      await fetchSummary();
      await fetchReviews();
    } catch { toast.error('Failed to delete review'); }
  };

  const handleEdit = () => {
    // Find user's own review
    const myReview = reviews.find(r => r.userId === user?.id);
    if (myReview) {
      setForm({ rating: myReview.rating, title: myReview.title || '', body: myReview.body || '' });
      setEditMode(true);
      setShowForm(true);
    } else {
      setEditMode(false);
      setShowForm(true);
    }
  };

  const totalReviews = summary?.totalReviews || 0;
  const avgRating = summary?.averageRating || 0;

  return (
    <div className="product-reviews">
      <h2 className="reviews-heading">Customer Reviews</h2>

      {/* ── Summary Bar ── */}
      <div className="reviews-summary">
        <div className="reviews-avg">
          <div className="avg-number">{avgRating.toFixed(1)}</div>
          <StarRating rating={avgRating} size={28} />
          <div className="avg-total">{totalReviews} {totalReviews === 1 ? 'review' : 'reviews'}</div>
        </div>

        {/* Rating distribution bars */}
        <div className="reviews-distribution">
          {[5, 4, 3, 2, 1].map(star => {
            const count = summary?.ratingDistribution?.[star] || 0;
            const pct = totalReviews > 0 ? Math.round((count / totalReviews) * 100) : 0;
            return (
              <div key={star} className="dist-row">
                <span className="dist-label">{star} ★</span>
                <div className="dist-bar-wrap">
                  <div className="dist-bar" style={{ width: `${pct}%` }} />
                </div>
                <span className="dist-count">{count}</span>
              </div>
            );
          })}
        </div>

        {/* Write review CTA */}
        <div className="reviews-cta">
          {!isAuthenticated() ? (
            <p className="text-muted text-sm">Login to write a review</p>
          ) : summary?.userHasReviewed ? (
            <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
              <p className="text-sm text-success">✓ You've reviewed this product</p>
              <div style={{ display: 'flex', gap: 8 }}>
                <button className="btn btn-ghost btn-sm" onClick={handleEdit}>
                  <MdEdit size={16} /> Edit Review
                </button>
                <button className="btn btn-sm" onClick={handleDelete}
                  style={{ background: 'rgba(244,67,54,0.1)', color: 'var(--accent-red)', border: '1px solid rgba(244,67,54,0.2)' }}>
                  <MdDelete size={16} /> Delete
                </button>
              </div>
            </div>
          ) : summary?.userCanReview ? (
            <button className="btn btn-primary btn-sm" onClick={() => { setEditMode(false); setShowForm(!showForm); }}>
              <MdSend size={16} /> Write a Review
            </button>
          ) : (
            <p className="text-muted text-sm">Purchase this product to leave a review</p>
          )}
        </div>
      </div>

      {/* ── Review Form ── */}
      {showForm && (
        <form onSubmit={handleSubmit} className="review-form">
          <h3>{editMode ? 'Edit Your Review' : 'Write a Review'}</h3>

          <div className="form-group">
            <label className="form-label">Your Rating *</label>
            <StarRating
              rating={form.rating}
              size={32}
              interactive
              onChange={(r) => setForm({ ...form, rating: r })}
              color="#f5a623"
            />
          </div>

          <div className="form-group">
            <label className="form-label">Title (optional)</label>
            <input className="form-input" maxLength={150}
              value={form.title}
              onChange={e => setForm({ ...form, title: e.target.value })}
              placeholder="Summarize your experience" />
          </div>

          <div className="form-group">
            <label className="form-label">Review (optional)</label>
            <textarea className="form-input" rows={4} maxLength={2000}
              value={form.body}
              onChange={e => setForm({ ...form, body: e.target.value })}
              placeholder="Tell others about this product..."
              style={{ resize: 'vertical' }} />
            <div className="text-xs text-muted" style={{ marginTop: 4 }}>
              {form.body.length} / 2000
            </div>
          </div>

          <div style={{ display: 'flex', gap: 8 }}>
            <button type="submit" className="btn btn-primary btn-sm" disabled={submitting || !form.rating}>
              {submitting ? 'Submitting...' : editMode ? 'Update Review' : 'Submit Review'}
            </button>
            <button type="button" className="btn btn-ghost btn-sm" onClick={() => setShowForm(false)}>
              Cancel
            </button>
          </div>
        </form>
      )}

      {/* ── Review List ── */}
      <div className="reviews-list">
        {loading ? (
          <p className="text-muted text-sm" style={{ padding: 'var(--space-4)' }}>Loading reviews...</p>
        ) : reviews.length === 0 ? (
          <div className="reviews-empty">
            <p>No reviews yet. Be the first to review this product!</p>
          </div>
        ) : (
          reviews.map(review => (
            <div key={review.id} className={`review-card ${review.userId === user?.id ? 'own-review' : ''}`}>
              <div className="review-card-header">
                <div className="reviewer-avatar">
                  {review.userName?.charAt(0).toUpperCase()}
                </div>
                <div className="reviewer-info">
                  <div className="reviewer-name">
                    {review.userName}
                    {review.isVerified && (
                      <span className="verified-badge">
                        <MdVerified size={14} /> Verified Purchase
                      </span>
                    )}
                    {review.userId === user?.id && (
                      <span className="badge badge-blue" style={{ marginLeft: 8, fontSize: 10 }}>Your Review</span>
                    )}
                  </div>
                  <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                    <StarRating rating={review.rating} size={16} />
                    <span className="review-date text-xs text-muted">
                      {new Date(review.createdAt).toLocaleDateString('en-IN', { day: 'numeric', month: 'short', year: 'numeric' })}
                    </span>
                  </div>
                </div>
              </div>

              {review.title && <h4 className="review-title">{review.title}</h4>}
              {review.body && <p className="review-body">{review.body}</p>}
            </div>
          ))
        )}
      </div>

      {/* Pagination */}
      {pagination.totalPages > 1 && (
        <div className="pagination" style={{ marginTop: 'var(--space-6)' }}>
          <button className="btn btn-ghost btn-sm" disabled={pagination.isFirst} onClick={() => setPage(p => p - 1)}>← Prev</button>
          <span className="text-muted text-sm">Page {page + 1} of {pagination.totalPages}</span>
          <button className="btn btn-ghost btn-sm" disabled={pagination.isLast} onClick={() => setPage(p => p + 1)}>Next →</button>
        </div>
      )}
    </div>
  );
};

export default ProductReviews;