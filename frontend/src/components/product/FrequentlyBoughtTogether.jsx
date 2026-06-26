// src/components/product/FrequentlyBoughtTogether.jsx
import React, { useState, useEffect, useRef } from 'react';
import axiosInstance from '../../api/axiosInstance';
import { useCart } from '../../context/CartContext';
import { useAuth } from '../../context/AuthContext';
import { MdAdd, MdShoppingCart, MdChevronLeft, MdChevronRight } from 'react-icons/md';
import { useNavigate } from 'react-router-dom';
import toast from 'react-hot-toast';
import './FrequentlyBoughtTogether.css';

const FrequentlyBoughtTogether = ({ categoryId, parentCategoryId }) => {
  const [bundles, setBundles] = useState([]);
  const [loading, setLoading] = useState(true);
  const [addingToCart, setAddingToCart] = useState(null);
  const [canScrollLeft, setCanScrollLeft] = useState(false);
  const [canScrollRight, setCanScrollRight] = useState(false);
  
  // NEW: State to track selected product IDs per bundle
  const [selectedProducts, setSelectedProducts] = useState({});
  
  const scrollRef = useRef(null);
  const { addToCart } = useCart();
  const { isAuthenticated } = useAuth();
  const navigate = useNavigate();

  useEffect(() => {
    if (!categoryId) return;
    setLoading(true);
    const params = new URLSearchParams();
    params.append('categoryId', categoryId);
    params.append('parentCategoryId', parentCategoryId || categoryId);
    
    axiosInstance.get(`/api/v1/fbt-sets/for-product?${params.toString()}`)
      .then(res => {
        const data = res.data?.data ?? res.data;
        const fetchedBundles = Array.isArray(data) ? data : [];
        setBundles(fetchedBundles);

        // NEW: Initialize all products as checked by default
        const initialSelection = {};
        fetchedBundles.forEach(bundle => {
          initialSelection[bundle.id] = bundle.products.map(p => p.id);
        });
        setSelectedProducts(initialSelection);
      })
      .catch(err => console.error('Failed to load FBT bundles', err))
      .finally(() => setLoading(false));
  }, [categoryId, parentCategoryId]);

  const updateScrollButtons = () => {
    const el = scrollRef.current;
    if (!el) return;
    setCanScrollLeft(el.scrollLeft > 10);
    setCanScrollRight(el.scrollLeft + el.clientWidth < el.scrollWidth - 10);
  };

  useEffect(() => {
    const el = scrollRef.current;
    if (!el) return;
    updateScrollButtons();
    el.addEventListener('scroll', updateScrollButtons);
    window.addEventListener('resize', updateScrollButtons);
    return () => {
      el.removeEventListener('scroll', updateScrollButtons);
      window.removeEventListener('resize', updateScrollButtons);
    };
  }, [bundles]);

  const scroll = (dir) => {
    const el = scrollRef.current;
    if (!el) return;
    el.scrollBy({ left: dir === 'left' ? -320 : 320, behavior: 'smooth' });
  };

  // NEW: Handle checkbox toggle logic
  const handleToggleProduct = (bundleId, productId) => {
    setSelectedProducts(prev => {
      const currentSelected = prev[bundleId] || [];
      if (currentSelected.includes(productId)) {
        // Remove item if unchecking
        return { ...prev, [bundleId]: currentSelected.filter(id => id !== productId) };
      } else {
        // Add item if checking
        return { ...prev, [bundleId]: [...currentSelected, productId] };
      }
    });
  };

  const handleAddBundleToCart = async (bundle) => {
    if (!isAuthenticated()) {
      toast.error('Please login to add items to cart');
      navigate('/login');
      return;
    }

    // NEW: Only get products that are currently checked
    const selectedIds = selectedProducts[bundle.id] || [];
    const productsToAdd = bundle.products.filter(p => selectedIds.includes(p.id));

    if (productsToAdd.length === 0) {
      toast.error('Please select at least one item to add to cart.');
      return;
    }

    setAddingToCart(bundle.id);
    try {
      for (const product of productsToAdd) {
        await addToCart(product.id, 1);
      }
      toast.success(
        productsToAdd.length === bundle.products.length 
        ? `${bundle.title} added to cart!` 
        : `${productsToAdd.length} items added to cart!`
      );
    } catch {
      toast.error('Failed to add items to cart.');
    } finally {
      setAddingToCart(null);
    }
  };

  if (loading || bundles.length === 0) return null;

  return (
    <div className="fbt-container">
      <div className="fbt-header-row">
        <h3 className="fbt-section-title">Frequently Bought Together</h3>
        <div className="fbt-nav-btns">
          <button
            className={`fbt-nav-btn ${!canScrollLeft ? 'disabled' : ''}`}
            onClick={() => scroll('left')}
            disabled={!canScrollLeft}
          >
            <MdChevronLeft size={22} />
          </button>
          <button
            className={`fbt-nav-btn ${!canScrollRight ? 'disabled' : ''}`}
            onClick={() => scroll('right')}
            disabled={!canScrollRight}
          >
            <MdChevronRight size={22} />
          </button>
        </div>
      </div>

      <div className="fbt-scroll-wrapper" ref={scrollRef}>
        {bundles.map(bundle => {
          const products = bundle.products || [];
          
          // NEW: Dynamic Calculations based on selected items only
          const selectedIds = selectedProducts[bundle.id] || [];
          const activeProducts = products.filter(p => selectedIds.includes(p.id));
          
          const originalTotal = activeProducts.reduce((sum, p) => sum + Number(p.price || 0), 0);
          // Assuming the discount applies to whatever is selected. If it requires all items to be selected, 
          // you could wrap this in a check: `selectedIds.length === products.length ? ... : 0`
          const discountAmount = originalTotal * (Number(bundle.discountPercent || 0) / 100);
          const finalTotal = originalTotal - discountAmount;

          return (
            <div key={bundle.id} className="fbt-bundle-card">
              {bundle.discountPercent > 0 && selectedIds.length > 0 && (
                <div className="fbt-discount-badge">Save {bundle.discountPercent}%</div>
              )}

              <h4 className="fbt-bundle-title">{bundle.title}</h4>

              {/* Keep the visual row for aesthetics, optionally fade out unselected ones */}
              <div className="fbt-visual-row">
                {products.map((product, index) => (
                  <React.Fragment key={product.id}>
                    <div 
                      className="fbt-product-thumb" 
                      style={{ opacity: selectedIds.includes(product.id) ? 1 : 0.4 }}
                    >
                      {product.primaryImageUrl
                        ? <img src={product.primaryImageUrl} alt={product.name} />
                        : <div className="fbt-thumb-placeholder">?</div>
                      }
                      <p className="fbt-thumb-name">{product.name}</p>
                    </div>
                    {index < products.length - 1 && (
                      <div className="fbt-plus"><MdAdd size={16} /></div>
                    )}
                  </React.Fragment>
                ))}
              </div>

              {/* NEW: Checkbox List Implementation */}
              <ul className="fbt-item-list">
                {products.map(product => (
                  <li key={product.id} className="fbt-list-item">
                    <label className="fbt-checkbox-label" style={{ display: 'flex', alignItems: 'center', gap: '8px', cursor: 'pointer' }}>
                      <input
                        type="checkbox"
                        checked={selectedIds.includes(product.id)}
                        onChange={() => handleToggleProduct(bundle.id, product.id)}
                        style={{ cursor: 'pointer', width: '16px', height: '16px' }}
                      />
                      <span className="fbt-item-name" style={{ textDecoration: selectedIds.includes(product.id) ? 'none' : 'line-through', opacity: selectedIds.includes(product.id) ? 1 : 0.6 }}>
                        {product.name}
                      </span>
                    </label>
                    <span className="fbt-item-price" style={{ opacity: selectedIds.includes(product.id) ? 1 : 0.6 }}>
                      ₹{Number(product.price).toLocaleString('en-IN')}
                    </span>
                  </li>
                ))}
              </ul>

              <div className="fbt-checkout-block">
                <div className="fbt-price-calc">
                  <span className="fbt-total-label">Selected total</span>
                  <span className="fbt-final-price">
                    ₹{finalTotal.toLocaleString('en-IN')}
                  </span>
                  {bundle.discountPercent > 0 && originalTotal > 0 && (
                    <span className="fbt-original-price">
                      ₹{originalTotal.toLocaleString('en-IN')}
                    </span>
                  )}
                </div>
                <button
                  className="btn btn-primary fbt-add-btn"
                  onClick={() => handleAddBundleToCart(bundle)}
                  disabled={addingToCart === bundle.id || selectedIds.length === 0}
                >
                  <MdShoppingCart size={16} />
                  {addingToCart === bundle.id ? 'Adding...' : 'Add Selected to Cart'}
                </button>
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
};

export default FrequentlyBoughtTogether;