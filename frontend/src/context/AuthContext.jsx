// src/context/AuthContext.jsx
import { createContext, useContext, useState, useEffect } from 'react';
import { authApi } from '../api/authApi';
import toast from 'react-hot-toast';

const AuthContext = createContext(null);

export const AuthProvider = ({ children }) => {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);

  // Load user from localStorage on app start
  useEffect(() => {
    const stored = localStorage.getItem('user');
    const token = localStorage.getItem('accessToken');
    if (stored && token) {
      setUser(JSON.parse(stored));
    }
    setLoading(false);
  }, []);

  const login = async (email, password) => {
    const res = await authApi.login({ email, password });
    const data = res.data.data;
    localStorage.setItem('accessToken', data.accessToken);
    localStorage.setItem('refreshToken', data.refreshToken);
    localStorage.setItem('user', JSON.stringify(data));
    setUser(data);
    return data;
  };

  const register = async (formData) => {
    const res = await authApi.register(formData);
    const data = res.data.data;
    localStorage.setItem('accessToken', data.accessToken);
    localStorage.setItem('refreshToken', data.refreshToken);
    localStorage.setItem('user', JSON.stringify(data));
    setUser(data);
    return data;
  };

  const logout = () => {
    localStorage.removeItem('accessToken');
    localStorage.removeItem('refreshToken');
    localStorage.removeItem('user');
    setUser(null);
    toast.success('Logged out successfully');
  };

  const isAdmin = () => user?.role === 'ROLE_ADMIN';
  const isAuthenticated = () => !!user;

  return (
    <AuthContext.Provider value={{ user, loading, login, register, logout, isAdmin, isAuthenticated, setUser }}>
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used within AuthProvider');
  return ctx;
};
