// src/pages/auth/ForgotPasswordPage.jsx
import { useState } from 'react';
import { Link } from 'react-router-dom';
import { authApi } from '../../api/authApi';
import toast from 'react-hot-toast';
import './AuthPages.css';

const ForgotPasswordPage = () => {
  const [email, setEmail] = useState('');
  const [loading, setLoading] = useState(false);
  const [sent, setSent] = useState(false);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    try {
      await authApi.forgotPassword(email);
      setSent(true);
      toast.success('Reset link sent! Check your email.');
    } catch {
      toast.error('Something went wrong. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="auth-page">
      <div className="auth-card">
        <div className="auth-header">
          <Link to="/" className="auth-logo">
            <span className="logo-zen">ZENE</span><span className="logo-nation">NATION</span>
          </Link>
          <h1>Forgot Password</h1>
          <p>Enter your email to receive a reset link</p>
        </div>

        {sent ? (
          <div className="auth-success">
            <div className="auth-success-icon">📧</div>
            <h3>Check your email!</h3>
            <p>We've sent a password reset link to <strong>{email}</strong></p>
            <p className="text-muted text-sm">The link expires in 15 minutes.</p>
            <Link to="/login" className="btn btn-primary btn-full" style={{ marginTop: '1rem' }}>
              Back to Login
            </Link>
          </div>
        ) : (
          <form onSubmit={handleSubmit} className="auth-form">
            <div className="form-group">
              <label className="form-label">Email Address</label>
              <input
                type="email"
                className="form-input"
                placeholder="your@email.com"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                required
              />
            </div>
            <button type="submit" className="btn btn-primary btn-full" disabled={loading}>
              {loading ? 'Sending...' : 'Send Reset Link'}
            </button>
          </form>
        )}

        <p className="auth-switch">
          Remember it? <Link to="/login">Back to Login</Link>
        </p>
      </div>
    </div>
  );
};

export default ForgotPasswordPage;
