// src/components/product/FrequentlyBoughtTogether.jsx
import React, { useState, useEffect, useRef } from 'react';
import axiosInstance from '../../api/axiosInstance';
import { useCart } from '../../context/CartContext';
import { useAuth } from '../../context/AuthContext';
import { MdAdd, MdShoppingCart, MdChevronLeft, MdChevronRight } from 'react-icons/md';
import { useNavigate } from 'react-router-dom';
import toast from 'react-hot-toast';
import './FrequentlyBoughtTogether.css';

const calcDiscounted = (price, discountPercent) => {
  const p = Number(price || 0);
  const d = Number(discountPercent || 0);
  return d > 0 ? p * (1 - d / 100) : p;
};

const fmt = (amount) =>
  '₹' + Number(amount).toLocaleString('en-IN', {
    minimumFractionDigits: 0,
    maximumFractionDigits: 2
  });

const generateGroupId = () =>
  'bundle-' + Date.now() + '-' + Math.random().toString(36).slice(2, 7);

const BundleCard = ({ bundle, onAddToCart, addingToCart }) => {
  const allProducts = bundle.products || [];
  const [checkedIds, setCheckedIds] = useState(() => new Set(allProducts.map(p => p.id)));

  const checkedProducts = allProducts.filter(p => checkedIds.has(p.id));
  const checkedCount = checkedProducts.length;
  const totalCount = allProducts.length;
  const deselectedCount = totalCount - checkedCount;
  const baseDiscount = Number(bundle.discountPercent || 0);
  const effectiveDiscount = Math.max(0, baseDiscount - deselectedCount * 20);

  const checkedSubtotal = checkedProducts.reduce(
    (sum, p) => sum + calcDiscounted(p.price, p.discountPercent), 0
  );
  const finalTotal = checkedSubtotal * (1 - effectiveDiscount / 100);
  const savedAmount = checkedSubtotal - finalTotal;

  const toggleProduct = (id) => {
    if (checkedIds.has(id) && checkedCount === 1) return;
    setCheckedIds(prev => {
      const next = new Set(prev);
      next.has(id) ? next.delete(id) : next.add(id);
      return next;
    });
  };

  const handleAdd = () => {
    if (checkedProducts.length === 0) return;
    const bundleGroupId = generateGroupId();
    const proportionalPrices = checkedProducts.map(p => {
      const productDiscounted = calcDiscounted(p.price, p.discountPercent);
      const share = checkedSubtotal > 0
        ? productDiscounted / checkedSubtotal
        : 1 / checkedProducts.length;
      return {
        id: p.id,
        bundlePrice: Number((finalTotal * share).toFixed(2)),
        bundleGroupId,
      };
    });
    onAddToCart(bundle, proportionalPrices);
  };

  return (
    <div className="fbt-bundle-card">
      {effectiveDiscount > 0 && (
        <div className="fbt-discount-badge">SAVE {effectiveDiscount}%</div>
      )}

      <h4 className="fbt-bundle-title">{bundle.title}</h4>

      <div className="fbt-visual-row">
        {allProducts.map((product, index) => (
          <React.Fragment key={product.id}>
            <div className={`fbt-product-thumb ${!checkedIds.has(product.id) ? 'unchecked' : ''}`}>
              {product.primaryImageUrl
                ? <img src={product.primaryImageUrl} alt={product.name} />
                : <div className="fbt-thumb-placeholder">?</div>
              }
              <p className="fbt-thumb-name">{product.name}</p>
            </div>
            {index < allProducts.length - 1 && (
              <div className="fbt-plus"><MdAdd size={14} /></div>
            )}
          </React.Fragment>
        ))}
      </div>

      <div className="fbt-checklist">
        {allProducts.map(product => {
          const isChecked = checkedIds.has(product.id);
          const productDiscounted = calcDiscounted(product.price, product.discountPercent);
          return (
            <label key={product.id} className={`fbt-check-row ${!isChecked ? 'unchecked' : ''}`}>
              <input
                type="checkbox"
                checked={isChecked}
                onChange={() => toggleProduct(product.id)}
              />
              <span className="fbt-check-name">{product.name}</span>
              <span className="fbt-check-prices">
                {Number(product.price) !== productDiscounted && (
                  <span className="fbt-check-original">{fmt(product.price)}</span>
                )}
                <span className="fbt-check-price">{fmt(productDiscounted)}</span>
              </span>
            </label>
          );
        })}
      </div>

      {deselectedCount > 0 && effectiveDiscount > 0 && (
        <div className="fbt-discount-note">
          {deselectedCount} item{deselectedCount > 1 ? 's' : ''} removed —
          discount reduced to {effectiveDiscount}%
        </div>
      )}

      {deselectedCount > 0 && effectiveDiscount === 0 && (
        <div className="fbt-discount-note" style={{ color: 'var(--text-muted)' }}>
          Select all items to unlock the bundle discount
        </div>
      )}

      <div className="fbt-checkout-block">
        <div className="fbt-price-calc">
          <span className="fbt-total-label">Selected total</span>
          <span className="fbt-final-price">{fmt(finalTotal)}</span>
          {savedAmount > 0.5 && (
            <span className="fbt-original-price">{fmt(checkedSubtotal)}</span>
          )}
        </div>
        {savedAmount > 0.5 && (
          <div className="fbt-saving-line">
            You save {fmt(savedAmount)}
            {effectiveDiscount > 0 && ` (${effectiveDiscount}% bundle discount)`}
          </div>
        )}
        <button
          className="btn btn-primary fbt-add-btn"
          onClick={handleAdd}
          disabled={addingToCart === bundle.id || checkedCount === 0}
        >
          <MdShoppingCart size={16} />
          {addingToCart === bundle.id
            ? 'Adding...'
            : `Add ${checkedCount} Item${checkedCount > 1 ? 's' : ''} to Cart`}
        </button>
      </div>
    </div>
  );
};

const FrequentlyBoughtTogether = ({ categoryId, parentCategoryId }) => {
  const [bundles, setBundles] = useState([]);
  const [loading, setLoading] = useState(true);
  const [addingToCart, setAddingToCart] = useState(null);
  const [canScrollLeft, setCanScrollLeft] = useState(false);
  const [canScrollRight, setCanScrollRight] = useState(false);
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
        setBundles(Array.isArray(data) ? data : []);
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

  const handleAddToCart = async (bundle, proportionalPrices) => {
    if (!isAuthenticated()) {
      toast.error('Please login to add items to cart');
      navigate('/login');
      return;
    }
    setAddingToCart(bundle.id);
    try {
      for (const { id, bundlePrice, bundleGroupId } of proportionalPrices) {
        await addToCart(id, 1, bundlePrice, bundleGroupId);
      }
      toast.success(`${bundle.title} added to cart!`);
    } catch {
      toast.error('Failed to add bundle to cart.');
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
        {bundles.map(bundle => (
          <BundleCard
            key={bundle.id}
            bundle={bundle}
            onAddToCart={handleAddToCart}
            addingToCart={addingToCart}
          />
        ))}
      </div>
    </div>
  );
};

export default FrequentlyBoughtTogether;