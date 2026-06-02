// src/App.jsx
import { BrowserRouter, Routes, Route } from 'react-router-dom';
import ScrollToTop from './components/common/ScrollToTop';
import { Toaster } from 'react-hot-toast';
import { AuthProvider } from './context/AuthContext';
import { CartProvider } from './context/CartContext';

import Navbar from './components/common/Navbar';
import Footer from './components/common/Footer';
import AnnouncementBanner from './components/common/AnnouncementBanner';
import SubscribePopup from './components/common/SubscribePopup';
import { ProtectedRoute, AdminRoute, GuestRoute } from './components/common/ProtectedRoute';

// Public pages
import HomePage from './pages/public/HomePage';
import ProductsPage from './pages/public/ProductsPage';
import ProductDetailPage from './pages/public/ProductDetailPage';
import SearchPage from './pages/public/SearchPage';
import NotFoundPage from './pages/public/NotFoundPage';
import PreorderPage from './pages/public/PreorderPage';
import LandingPage from './pages/public/LandingPage';

// Auth pages
import LoginPage from './pages/auth/LoginPage';
import RegisterPage from './pages/auth/RegisterPage';
import ForgotPasswordPage from './pages/auth/ForgotPasswordPage';
import ResetPasswordPage from './pages/auth/ResetPasswordPage';
import OAuth2CallbackPage from './pages/auth/OAuth2CallbackPage';

// User pages
import CartPage from './pages/user/CartPage';
import CheckoutPage from './pages/user/CheckoutPage';
import OrdersPage from './pages/user/OrdersPage';
import ProfilePage from './pages/user/ProfilePage';

// Admin pages
import AdminDashboard from './pages/admin/AdminDashboard';
import AdminCategories from './pages/admin/AdminCategories';
import AdminProducts from './pages/admin/AdminProducts';
import AdminOrders from './pages/admin/AdminOrders';
import AdminCoupons from './pages/admin/AdminCoupons';
import AdminUsers from './pages/admin/AdminUsers';
import AdminAnnouncements from './pages/admin/AdminAnnouncements';
import AdminHomeSections from './pages/admin/AdminHomeSections';

const AppLayout = ({ children }) => (
  <>
    <AnnouncementBanner />
    <Navbar />
    {children}
    <Footer />
    <SubscribePopup />
  </>
);

const App = () => {
  return (
    <BrowserRouter>
      <AuthProvider>
        <CartProvider>
          <Toaster
            position="top-right"
            toastOptions={{
              style: {
                background: '#1a1a2e',
                color: '#ffffff',
                border: '1px solid #2a2a45',
                borderRadius: '8px',
              },
              success: { iconTheme: { primary: '#e94560', secondary: '#ffffff' } },
              duration: 3000,
            }}
          />
          <ScrollToTop />
          <Routes>
            {/* ── Public ── */}
            {/*Removed <Route path="/" element={<LandingPage />} />*/}
            {/**Modified Path='/' will now lead to <AppLayout> */}
            <Route path="/" element={<AppLayout><HomePage /></AppLayout>} />
            <Route path="/products" element={<AppLayout><ProductsPage /></AppLayout>} />
            <Route path="/products/:slug" element={<AppLayout><ProductDetailPage /></AppLayout>} />
            <Route path="/search" element={<AppLayout><SearchPage /></AppLayout>} />
            <Route path="/preorder" element={<AppLayout><PreorderPage /></AppLayout>} />

            {/* ── Guest only ── */}
            <Route path="/login" element={<GuestRoute><LoginPage /></GuestRoute>} />
            <Route path="/register" element={<GuestRoute><RegisterPage /></GuestRoute>} />
            <Route path="/forgot-password" element={<GuestRoute><ForgotPasswordPage /></GuestRoute>} />
            <Route path="/reset-password" element={<ResetPasswordPage />} />
            <Route path="/oauth2/callback" element={<OAuth2CallbackPage />} />

            {/* ── Protected user ── */}
            <Route path="/cart" element={<ProtectedRoute><AppLayout><CartPage /></AppLayout></ProtectedRoute>} />
            <Route path="/checkout" element={<ProtectedRoute><AppLayout><CheckoutPage /></AppLayout></ProtectedRoute>} />
            <Route path="/orders" element={<ProtectedRoute><AppLayout><OrdersPage /></AppLayout></ProtectedRoute>} />
            <Route path="/orders/:id" element={<ProtectedRoute><AppLayout><OrdersPage /></AppLayout></ProtectedRoute>} />
            <Route path="/profile" element={<ProtectedRoute><AppLayout><ProfilePage /></AppLayout></ProtectedRoute>} />

            {/* ── Admin ── */}
            <Route path="/admin" element={<AdminRoute><AdminDashboard /></AdminRoute>} />
            <Route path="/admin/categories" element={<AdminRoute><AdminCategories /></AdminRoute>} />
            <Route path="/admin/products" element={<AdminRoute><AdminProducts /></AdminRoute>} />
            <Route path="/admin/orders" element={<AdminRoute><AdminOrders /></AdminRoute>} />
            <Route path="/admin/coupons" element={<AdminRoute><AdminCoupons /></AdminRoute>} />
            <Route path="/admin/users" element={<AdminRoute><AdminUsers /></AdminRoute>} />
            <Route path="/admin/announcements" element={<AdminRoute><AdminAnnouncements /></AdminRoute>} />
            <Route path="/admin/home-sections" element={<AdminRoute><AdminHomeSections /></AdminRoute>} />

            {/* ── 404 ── */}
            <Route path="*" element={<AppLayout><NotFoundPage /></AppLayout>} />
          </Routes>
        </CartProvider>
      </AuthProvider>
    </BrowserRouter>
  );
};

export default App;