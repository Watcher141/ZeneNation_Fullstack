// src/pages/auth/ResetPasswordPage.jsx
import { useState } from 'react';
import { Link, useSearchParams, useNavigate } from 'react-router-dom';
import { authApi } from '../../api/authApi';
import toast from 'react-hot-toast';
import './AuthPages.css';

const ResetPasswordPage = () => {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const token = searchParams.get('token');
  const [form, setForm] = useState({ newPassword: '', confirmPassword: '' });
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (form.newPassword !== form.confirmPassword) {
      toast.error('Passwords do not match');
      return;
    }
    setLoading(true);
    try {
      await authApi.resetPassword({ token, ...form });
      toast.success('Password reset successfully!');
      navigate('/login');
    } catch (err) {
      toast.error(err.response?.data?.message || 'Invalid or expired link');
    } finally {
      setLoading(false);
    }
  };

  if (!token) {
    return (
      <div className="auth-page">
        <div className="auth-card">
          <div className="auth-header">
            <h1>Invalid Link</h1>
            <p>This reset link is invalid or has expired.</p>
          </div>
          <Link to="/forgot-password" className="btn btn-primary btn-full">
            Request New Link
          </Link>
        </div>
      </div>
    );
  }

  return (
    <div className="auth-page">
      <div className="auth-card">
        <div className="auth-header">
          <Link to="/" className="auth-logo">
            <span className="logo-zen">ZENE</span><span className="logo-nation">NATION</span>
          </Link>
          <h1>Reset Password</h1>
          <p>Enter your new password</p>
        </div>

        <form onSubmit={handleSubmit} className="auth-form">
          <div className="form-group">
            <label className="form-label">New Password</label>
            <input
              type="password"
              className="form-input"
              placeholder="Min 8 chars, 1 uppercase, 1 number, 1 special"
              value={form.newPassword}
              onChange={(e) => setForm({ ...form, newPassword: e.target.value })}
              required
            />
          </div>
          <div className="form-group">
            <label className="form-label">Confirm Password</label>
            <input
              type="password"
              className="form-input"
              placeholder="••••••••"
              value={form.confirmPassword}
              onChange={(e) => setForm({ ...form, confirmPassword: e.target.value })}
              required
            />
          </div>
          <button type="submit" className="btn btn-primary btn-full" disabled={loading}>
            {loading ? 'Resetting...' : 'Reset Password'}
          </button>
        </form>
      </div>
    </div>
  );
};

export default ResetPasswordPage;
