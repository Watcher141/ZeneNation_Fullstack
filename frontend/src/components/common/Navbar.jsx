// src/components/common/Navbar.jsx
import { useState, useRef, useEffect } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import { useCart } from '../../context/CartContext';
import {
  MdSearch,
  MdShoppingCart,
  MdMenu,
  MdClose,
  MdPerson,
  MdLogout,
  MdInventory2,
  MdDashboard,
  MdShoppingBag,
  MdRocketLaunch,
} from 'react-icons/md';
import './Navbar.css';

const Navbar = () => {
  const { user, logout, isAdmin } = useAuth();
  const { cartCount } = useCart();
  const navigate = useNavigate();
  const [search, setSearch] = useState('');
  const [dropdownOpen, setDropdownOpen] = useState(false);
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);
  const [mobileSearchOpen, setMobileSearchOpen] = useState(false);
  const dropdownRef = useRef(null);

  useEffect(() => {
    const handleClick = (e) => {
      if (dropdownRef.current && !dropdownRef.current.contains(e.target)) {
        setDropdownOpen(false);
      }
    };
    document.addEventListener('mousedown', handleClick);
    return () => document.removeEventListener('mousedown', handleClick);
  }, []);

  useEffect(() => {
    setMobileMenuOpen(false);
    setMobileSearchOpen(false);
  }, [navigate]);

  const handleSearch = (e) => {
    e.preventDefault();
    if (search.trim()) {
      navigate(`/search?keyword=${encodeURIComponent(search.trim())}`);
      setSearch('');
      setMobileSearchOpen(false);
    }
  };

  const handleLogout = () => {
    logout();
    setDropdownOpen(false);
    setMobileMenuOpen(false);
    navigate('/');
  };

  return (
    <>
      <nav className="navbar">
        <div className="container navbar-inner">
          {/* Logo */}
          <Link to="/" className="navbar-logo">
            <span className="logo-zen">ZENE</span>
            <span className="logo-nation">NATION</span>
          </Link>

          {/* Desktop Search */}
          <form className="navbar-search" onSubmit={handleSearch}>
            <input
              type="text"
              placeholder="Search anime products..."
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              className="navbar-search-input"
            />
            <button type="submit" className="navbar-search-btn">
              <MdSearch size={20} />
            </button>
          </form>

          {/* Desktop Actions */}
          <div className="navbar-actions">
            {user ? (
              <>
                {isAdmin() && (
                  <Link to="/admin" className="btn btn-sm btn-secondary">
                    <MdDashboard size={16} /> Admin
                  </Link>
                )}
                <Link to="/cart" className="navbar-cart">
                  <MdShoppingCart size={24} />
                  {cartCount > 0 && <span className="cart-badge">{cartCount}</span>}
                </Link>
                <div className="navbar-user" ref={dropdownRef}>
                  <button className="user-avatar" onClick={() => setDropdownOpen(!dropdownOpen)}>
                    {user.name?.charAt(0).toUpperCase()}
                  </button>
                  {dropdownOpen && (
                    <div className="user-dropdown">
                      <div className="dropdown-header">
                        <span className="dropdown-name">{user.name}</span>
                        <span className="dropdown-email">{user.email}</span>
                      </div>
                      <div className="dropdown-divider" />
                      <Link to="/profile" className="dropdown-item" onClick={() => setDropdownOpen(false)}>
                        <MdPerson size={16} /> My Profile
                      </Link>
                      <Link to="/orders" className="dropdown-item" onClick={() => setDropdownOpen(false)}>
                        <MdShoppingBag size={16} /> My Orders
                      </Link>
                      <div className="dropdown-divider" />
                      <button className="dropdown-item dropdown-logout" onClick={handleLogout}>
                        <MdLogout size={16} /> Logout
                      </button>
                    </div>
                  )}
                </div>
              </>
            ) : (
              <>
                <Link to="/login" className="btn btn-sm btn-ghost">Login</Link>
                <Link to="/register" className="btn btn-sm btn-primary">Register</Link>
              </>
            )}
          </div>

          {/* Mobile right icons */}
          <div className="mobile-actions">
            <button className="mobile-icon-btn" onClick={() => setMobileSearchOpen(!mobileSearchOpen)}>
              {mobileSearchOpen ? <MdClose size={22} /> : <MdSearch size={22} />}
            </button>
            {user && (
              <Link to="/cart" className="navbar-cart mobile-icon-btn">
                <MdShoppingCart size={22} />
                {cartCount > 0 && <span className="cart-badge">{cartCount}</span>}
              </Link>
            )}
            <button className="mobile-icon-btn" onClick={() => setMobileMenuOpen(!mobileMenuOpen)}>
              {mobileMenuOpen ? <MdClose size={22} /> : <MdMenu size={22} />}
            </button>
          </div>
        </div>

        {/* Mobile Search Bar */}
        {mobileSearchOpen && (
          <div className="mobile-search-bar">
            <form onSubmit={handleSearch} className="mobile-search-form">
              <input
                type="text"
                placeholder="Search anime products..."
                value={search}
                onChange={(e) => setSearch(e.target.value)}
                className="form-input"
                autoFocus
              />
              <button type="submit" className="btn btn-primary btn-sm">
                <MdSearch size={18} />
              </button>
            </form>
          </div>
        )}
      </nav>

      {/* Mobile Menu Overlay */}
      {mobileMenuOpen && (
        <div className="mobile-menu-overlay" onClick={() => setMobileMenuOpen(false)}>
          <div className="mobile-menu" onClick={e => e.stopPropagation()}>
            {user ? (
              <>
                <div className="mobile-menu-user">
                  <div className="user-avatar">{user.name?.charAt(0).toUpperCase()}</div>
                  <div>
                    <div style={{ fontWeight: 600 }}>{user.name}</div>
                    <div className="text-xs text-muted">{user.email}</div>
                  </div>
                </div>
                <div className="mobile-menu-divider" />
                {isAdmin() && (
                  <Link to="/admin" className="mobile-menu-item" onClick={() => setMobileMenuOpen(false)}>
                    <MdDashboard size={18} /> Admin Dashboard
                  </Link>
                )}
                <Link to="/profile" className="mobile-menu-item" onClick={() => setMobileMenuOpen(false)}>
                  <MdPerson size={18} /> My Profile
                </Link>
                <Link to="/orders" className="mobile-menu-item" onClick={() => setMobileMenuOpen(false)}>
                  <MdShoppingBag size={18} /> My Orders
                </Link>
                <Link to="/cart" className="mobile-menu-item" onClick={() => setMobileMenuOpen(false)}>
                  <MdShoppingCart size={18} />
                  Cart {cartCount > 0 && <span className="badge badge-red" style={{ marginLeft: 8 }}>{cartCount}</span>}
                </Link>
                <div className="mobile-menu-divider" />
                <Link to="/products" className="mobile-menu-item" onClick={() => setMobileMenuOpen(false)}>
                  <MdInventory2 size={18} /> All Products
                </Link>
                <Link to="/preorder" className="mobile-menu-item" onClick={() => setMobileMenuOpen(false)}>
                  <MdRocketLaunch size={18} color="var(--accent-primary)" /> Preorders
                </Link>
                <div className="mobile-menu-divider" />
                <button className="mobile-menu-item" style={{ color: 'var(--accent-red)' }} onClick={handleLogout}>
                  <MdLogout size={18} /> Logout
                </button>
              </>
            ) : (
              <>
                <Link to="/products" className="mobile-menu-item" onClick={() => setMobileMenuOpen(false)}>
                  <MdInventory2 size={18} /> All Products
                </Link>
                <Link to="/preorder" className="mobile-menu-item" onClick={() => setMobileMenuOpen(false)}>
                  <MdRocketLaunch size={18} color="var(--accent-primary)" /> Preorders
                </Link>
                <div className="mobile-menu-divider" />
                <Link to="/login" className="mobile-menu-item" onClick={() => setMobileMenuOpen(false)}>
                  <MdPerson size={18} /> Login
                </Link>
                <Link to="/register" className="mobile-menu-item" onClick={() => setMobileMenuOpen(false)}>
                  ✨ Register
                </Link>
              </>
            )}
          </div>
        </div>
      )}
    </>
  );
};

export default Navbar;