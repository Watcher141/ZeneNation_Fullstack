// src/components/admin/AdminLayout.jsx
import { useState, useEffect } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import {
  MdDashboard,
  MdCampaign,
  MdHome,
  MdCategory,
  MdInventory2,
  MdShoppingBag,
  MdLocalOffer,
  MdPeople,
  MdLogout,
  MdMenu,
  MdClose,
  MdChevronLeft,
  MdChevronRight,
  MdLocalShipping,
} from 'react-icons/md';
import './AdminLayout.css';

const navItems = [
  { path: '/admin',            icon: MdDashboard,   label: 'Dashboard'  },
  { path: '/admin/categories', icon: MdCategory,    label: 'Categories' },
  { path: '/admin/products',   icon: MdInventory2,  label: 'Products'   },
  { path: '/admin/orders',     icon: MdShoppingBag, label: 'Orders'     },
  { path: '/admin/coupons',    icon: MdLocalOffer,  label: 'Coupons'    },
  { path: '/admin/users',      icon: MdPeople,      label: 'Users'      },
  { path: '/admin/shipping',   icon: MdLocalShipping, label: 'Shipping Config' },
  { path: '/admin/announcements', icon: MdCampaign,  label: 'Announcements' },
  { path: '/admin/home-sections',  icon: MdHome,      label: 'Home Sections' },
];

const AdminLayout = ({ children }) => {
  const { user, logout } = useAuth();
  const location = useLocation();
  const navigate = useNavigate();
  const [collapsed, setCollapsed] = useState(false);
  const [mobileOpen, setMobileOpen] = useState(false);

  useEffect(() => { setMobileOpen(false); }, [location.pathname]);

  useEffect(() => {
    const handleResize = () => {
      if (window.innerWidth > 768) setMobileOpen(false);
    };
    window.addEventListener('resize', handleResize);
    return () => window.removeEventListener('resize', handleResize);
  }, []);

  const handleLogout = () => { logout(); navigate('/'); };

  const SidebarContent = () => (
    <>
      <div className="admin-sidebar-header">
        <Link to="/" className="admin-logo" onClick={() => setMobileOpen(false)}>
          {collapsed && window.innerWidth > 768
            ? '⚔️'
            : <><span className="logo-zen">ZENE</span><span className="logo-nation">NATION</span></>
          }
        </Link>
        <button className="collapse-btn desktop-only" onClick={() => setCollapsed(!collapsed)}>
          {collapsed ? <MdChevronRight size={16} /> : <MdChevronLeft size={16} />}
        </button>
        <button className="collapse-btn mobile-only" onClick={() => setMobileOpen(false)}>
          <MdClose size={16} />
        </button>
      </div>

      <nav className="admin-nav">
        {navItems.map(item => {
          const IconComponent = item.icon;
          return (
            <Link
              key={item.path}
              to={item.path}
              className={`admin-nav-item ${location.pathname === item.path ? 'active' : ''}`}
              onClick={() => setMobileOpen(false)}
            >
              <span className="nav-icon">
                <IconComponent size={20} />
              </span>
              <span className="nav-label">{item.label}</span>
            </Link>
          );
        })}
      </nav>

      <div className="admin-sidebar-footer">
        <div className="admin-user-info">
          <div className="admin-avatar">{user?.name?.charAt(0).toUpperCase()}</div>
          <div className="admin-user-details">
            <span className="admin-user-name">{user?.name}</span>
            <span className="admin-user-role">Administrator</span>
          </div>
        </div>
        <button className="admin-logout-btn" onClick={handleLogout} title="Logout">
          <MdLogout size={20} />
        </button>
      </div>
    </>
  );

  return (
    <div className="admin-layout">
      {/* Desktop Sidebar */}
      <aside className={`admin-sidebar desktop-sidebar ${collapsed ? 'collapsed' : ''}`}>
        <SidebarContent />
      </aside>

      {/* Mobile overlay + sidebar */}
      {mobileOpen && (
        <div className="mobile-sidebar-overlay" onClick={() => setMobileOpen(false)}>
          <aside className="admin-sidebar mobile-sidebar" onClick={e => e.stopPropagation()}>
            <SidebarContent />
          </aside>
        </div>
      )}

      {/* Main content */}
      <main className={`admin-main ${collapsed ? 'collapsed' : ''}`}>
        {/* Mobile top bar */}
        <div className="admin-mobile-topbar">
          <button className="admin-hamburger" onClick={() => setMobileOpen(true)}>
            <MdMenu size={24} />
          </button>
          <Link to="/" className="admin-logo">
            <span className="logo-zen">ZENE</span><span className="logo-nation">NATION</span>
          </Link>
          <Link to="/" style={{ color: 'var(--text-primary)', display: 'flex', alignItems: 'center' }}>
            <MdHome size={24} />
          </Link>
        </div>

        <div className="admin-content">
          {children}
        </div>
      </main>
    </div>
  );
};

export default AdminLayout;