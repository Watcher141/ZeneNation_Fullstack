// src/components/Navbar.jsx
import { useState, useRef, useEffect } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import { useCart } from '../../context/CartContext';
import { productApi } from '../../api/productApi'; 
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
  const searchContainerRef = useRef(null);
  const mobileSearchContainerRef = useRef(null);
  
  const [debouncedSearch, setDebouncedSearch] = useState('');
  const [searchResults, setSearchResults] = useState([]);
  const [isSearching, setIsSearching] = useState(false);
  const [showSuggestions, setShowSuggestions] = useState(false);
  
  const [activeSearch, setActiveSearch] = useState(null);

  // Close dropdowns when clicking outside
  useEffect(() => {
    const handleClick = (e) => {
      // 1. Handle user profile dropdown
      if (dropdownRef.current && !dropdownRef.current.contains(e.target)) {
        setDropdownOpen(false);
      }
      
      // 2. THE FIX: Check if the click was inside EITHER search bar
      const clickedInsideDesktop = searchContainerRef.current && searchContainerRef.current.contains(e.target);
      const clickedInsideMobile = mobileSearchContainerRef.current && mobileSearchContainerRef.current.contains(e.target);
      
      // Only close suggestions if they clicked totally outside both
      if (!clickedInsideDesktop && !clickedInsideMobile) {
        setShowSuggestions(false);
      }
    };
    
    document.addEventListener('mousedown', handleClick);
    return () => document.removeEventListener('mousedown', handleClick);
  }, []);

  // Close menus on navigation
  useEffect(() => {
    setMobileMenuOpen(false);
    setMobileSearchOpen(false);
    setShowSuggestions(false); 
  }, [navigate]);

  // Debounce the search input
  useEffect(() => {
    const timer = setTimeout(() => {
      setDebouncedSearch(search);
    }, 400); 
    return () => clearTimeout(timer);
  }, [search]);

  // Fetch, filter, and sort search results
  useEffect(() => {
    if (!debouncedSearch.trim()) {
      setSearchResults([]);
      setShowSuggestions(false);
      return;
    }

    const fetchSearchResults = async () => {
      setIsSearching(true);
      setShowSuggestions(true);
      try {
        const res = await productApi.getAll({ 
          page: 0, 
          size: 1000 
        });
        
        const allProducts = res.data?.data?.content || [];
        
        const filteredProducts = allProducts.filter(product => {
          const term = debouncedSearch.toLowerCase();
          return (
            product.name?.toLowerCase().includes(term) ||
            product.category?.name?.toLowerCase().includes(term)
          );
        });

        filteredProducts.sort((a, b) => {
          const term = debouncedSearch.toLowerCase();
          const aName = a.name?.toLowerCase() || '';
          const bName = b.name?.toLowerCase() || '';

          if (aName === term) return -1;
          if (bName === term) return 1;

          return aName.length - bName.length;
        });

        setSearchResults(filteredProducts.slice(0, 5));
      } catch (error) {
        console.error("Failed to fetch search suggestions", error);
      } finally {
        setIsSearching(false);
      }
    };

    fetchSearchResults();
  }, [debouncedSearch]);

  const handleSearch = (e) => {
    e?.preventDefault();
    if (search.trim()) {
      navigate(`/search?keyword=${encodeURIComponent(search.trim())}`);
      setShowSuggestions(false); 
      setMobileSearchOpen(false);
    }
  };

  const handleLogout = () => {
    logout();
    setDropdownOpen(false);
    setMobileMenuOpen(false);
    navigate('/');
  };

  const renderSuggestions = (type) => {
    if (!showSuggestions || !search.trim() || activeSearch !== type) return null;

    return (
      <div className="search-suggestions" style={{
        position: 'absolute',
        top: '100%',
        left: 0,
        right: 0,
        background: 'var(--bg-secondary)', 
        border: '1px solid var(--border-color)',
        borderRadius: '8px',
        marginTop: '8px',
        zIndex: 1000,
        overflow: 'hidden',
        boxShadow: '0 10px 25px rgba(0,0,0,0.5)',
      }}>
        {isSearching ? (
          <div style={{ padding: '16px', textAlign: 'center', color: 'var(--text-muted)', fontSize: '14px' }}>
            Searching...
          </div>
        ) : searchResults.length > 0 ? (
          <>
            <div style={{ display: 'flex', flexDirection: 'column' }}>
              {searchResults.map((product) => {
                const productSlug = product.name?.toLowerCase().replace(/\s+/g, '-');
                
                return (
                  // Reverted back to a clean standard Link tag
                  <Link 
                    key={product.id} 
                    to={`/products/${productSlug}`} 
                    onClick={() => {
                      setShowSuggestions(false);
                      setSearch('');
                      setMobileSearchOpen(false);
                    }}
                    style={{
                      display: 'flex',
                      alignItems: 'center',
                      gap: '12px',
                      padding: '12px',
                      textDecoration: 'none',
                      borderBottom: '1px solid var(--border-color)',
                      transition: 'background 0.2s'
                    }}
                    onMouseOver={(e) => e.currentTarget.style.background = 'var(--bg-tertiary)'}
                    onMouseOut={(e) => e.currentTarget.style.background = 'transparent'}
                  >
                    <img 
                      src={product.primaryImageUrl || '/placeholder.png'} 
                      alt={product.name} 
                      style={{ width: '40px', height: '40px', objectFit: 'cover', borderRadius: '4px', background: 'var(--bg-tertiary)' }} 
                    />
                    <div style={{ display: 'flex', flexDirection: 'column', flex: 1, minWidth: 0 }}>
                      <span style={{ color: 'var(--text-primary)', fontSize: '14px', fontWeight: 500, whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>
                        {product.name}
                      </span>
                      <span style={{ color: 'var(--text-muted)', fontSize: '12px' }}>
                        ₹{product.discountedPrice || product.price}
                      </span>
                    </div>
                  </Link>
                );
              })}
            </div>
            <button 
              onClick={handleSearch} 
              style={{
                width: '100%',
                padding: '12px',
                background: 'var(--bg-tertiary)',
                border: 'none',
                color: 'var(--accent-primary)',
                cursor: 'pointer',
                fontWeight: 600,
                fontSize: '14px',
                textAlign: 'center'
              }}
            >
              View all results for "{search}"
            </button>
          </>
        ) : (
          <div style={{ padding: '16px', textAlign: 'center', color: 'var(--text-muted)', fontSize: '14px' }}>
            No products found
          </div>
        )}
      </div>
    );
  };

  return (
    <>
      <nav className="navbar">
        <div className="container navbar-inner">

          {/* Logo */}
          <Link to="/" className="navbar-logo">
            <img
              src="/button.png"
              alt="Zenenation"
              style={{
                height: '50px',
                width: 'auto',
                objectFit: 'contain',
                display: 'block',
              }}
            />
          </Link>

          {/* Desktop Search */}
          <div className="navbar-search-container" ref={searchContainerRef} style={{ position: 'relative', flex: 1, maxWidth: '500px', margin: '0 20px' }}>
            <form className="navbar-search" onSubmit={handleSearch} style={{ margin: 0 }}>
              <input
                type="text"
                placeholder="Search anime products..."
                value={search}
                onChange={(e) => {
                  setSearch(e.target.value);
                  if (e.target.value.trim()) setShowSuggestions(true);
                }}
                onFocus={() => {
                  setActiveSearch('desktop');
                  if (search.trim()) setShowSuggestions(true);
                }}
                className="navbar-search-input"
              />
              <button type="submit" className="navbar-search-btn">
                <MdSearch size={20} />
              </button>
            </form>
            {renderSuggestions('desktop')}
          </div>

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
            <button className="mobile-icon-btn" onClick={() => {
              setMobileSearchOpen(!mobileSearchOpen);
              if (mobileSearchOpen) {
                 setSearch('');
                 setShowSuggestions(false);
              }
            }}>
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
          <div className="mobile-search-bar" ref={mobileSearchContainerRef} style={{ position: 'relative' }}>
            <form onSubmit={handleSearch} className="mobile-search-form">
              <input
                type="text"
                placeholder="Search anime products..."
                value={search}
                onChange={(e) => {
                  setSearch(e.target.value);
                  if (e.target.value.trim()) setShowSuggestions(true);
                }}
                onFocus={() => {
                  setActiveSearch('mobile');
                  if (search.trim()) setShowSuggestions(true);
                }}
                className="form-input"
                autoFocus
              />
              <button type="submit" className="btn btn-primary btn-sm">
                <MdSearch size={18} />
              </button>
            </form>
            {renderSuggestions('mobile')}
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