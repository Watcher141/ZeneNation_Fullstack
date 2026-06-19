// src/components/common/ProtectedRoute.jsx
import { Navigate } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import Loader from './Loader';

// Requires user to be logged in
export const ProtectedRoute = ({ children }) => {
  const { user, loading } = useAuth();
  if (loading) return <Loader fullPage />;
  if (!user) return <Navigate to="/login" replace />;
  return children;
};

// Requires user to be admin
export const AdminRoute = ({ children }) => {
  const { user, loading, isAdmin } = useAuth();
  if (loading) return <Loader fullPage />;
  if (!user) return <Navigate to="/login" replace />;
  if (!isAdmin()) return <Navigate to="/" replace />;
  return children;
};

// Redirect logged-in users away from login/register
export const GuestRoute = ({ children }) => {
  const { user, loading } = useAuth();
  if (loading) return <Loader fullPage />;
  if (user) return <Navigate to="/" replace />;
  return children;
};