// src/pages/public/ProductsPage.jsx
import { useState, useEffect } from 'react';
import { useSearchParams } from 'react-router-dom';
import { productApi } from '../../api/productApi';
import { categoryApi } from '../../api/categoryApi';
import ProductCard from '../../components/product/ProductCard';
import { MdImage } from 'react-icons/md';
import './ProductsPage.css';

// ── NEW: Skeleton Loader Component ──
const ProductSkeleton = () => (
  <div className="product-card skeleton-card">
    <div className="skeleton-image"></div>
    <div className="product-card-info">
      <div className="skeleton-text skeleton-category"></div>
      <div className="skeleton-text skeleton-title"></div>
      <div className="skeleton-text skeleton-price"></div>
      <div className="skeleton-button"></div>
    </div>
  </div>
);

const ProductsPage = () => {
  const [searchParams, setSearchParams] = useSearchParams();
  const [products, setProducts] = useState([]);
  const [categories, setCategories] = useState([]);
  const [pagination, setPagination] = useState({});
  const [loading, setLoading] = useState(true);

  const page = parseInt(searchParams.get('page') || '0');
  const sortBy = searchParams.get('sortBy') || 'createdAt';
  const sortDir = searchParams.get('sortDir') || 'desc';
  const categoryId = searchParams.get('categoryId');

  useEffect(() => {
    categoryApi.getAll().then(r => setCategories(r.data.data || [])).catch(() => {});
  }, []);

  useEffect(() => {
    fetchProducts();
  }, [page, sortBy, sortDir, categoryId]);

  const fetchProducts = async () => {
    setLoading(true);
    try {
      let res;
      if (categoryId) {
        res = await productApi.getByCategory(categoryId, { page, size: 12 });
      } else {
        res = await productApi.getAll({ page, size: 12, sortBy, sortDir });
      }
      setProducts(res.data.data?.content || []);
      setPagination(res.data.data || {});
    } catch {
      setProducts([]);
    } finally {
      setLoading(false);
    }
  };

  const setCategory = (catId) => {
    const p = new URLSearchParams(searchParams);
    if (catId) p.set('categoryId', catId);
    else p.delete('categoryId');
    p.set('page', '0'); 
    setSearchParams(p);
  };

  const setSort = (sb, sd) => {
    const p = new URLSearchParams(searchParams);
    p.set('sortBy', sb);
    p.set('sortDir', sd);
    p.set('page', '0'); 
    setSearchParams(p);
  };

  const goToPage = (newPage) => {
    const p = new URLSearchParams(searchParams);
    p.set('page', String(newPage));
    setSearchParams(p);
  };

  return (
    <div className="page-wrapper">
      <div className="container products-page">
        {/* ── Sidebar ── */}
        <aside className="products-sidebar">
          <h3 className="sidebar-title">Categories</h3>
          <button
            className={`sidebar-cat-item ${!categoryId ? 'active' : ''}`}
            onClick={() => setCategory(null)}
          >
            All Products
          </button>
          {categories.map(cat => (
            <div key={cat.id}>
              <button
                className={`sidebar-cat-item ${categoryId == cat.id ? 'active' : ''}`}
                onClick={() => setCategory(cat.id)}
              >
                {cat.name}
                {cat.subcategories?.length > 0 && (
                  <span style={{ fontSize: 11, color: 'var(--text-muted)', marginLeft: 6 }}>
                    ({cat.subcategories.length})
                  </span>
                )}
              </button>
              {(categoryId == cat.id || cat.subcategories?.some(s => s.id == categoryId)) &&
                cat.subcategories?.map(sub => (
                  <button
                    key={sub.id}
                    className={`sidebar-cat-item ${categoryId == sub.id ? 'active' : ''}`}
                    style={{ paddingLeft: 24, fontSize: 13 }}
                    onClick={() => setCategory(sub.id)}
                  >
                    ↳ {sub.name}
                  </button>
                ))
              }
            </div>
          ))}
        </aside>

        {/* ── Main ── */}
        <main className="products-main">
          <div className="products-toolbar">
            <p className="products-count">
              {pagination.totalElements || 0} products
            </p>
            <select
              className="form-select products-sort"
              value={`${sortBy}-${sortDir}`}
              onChange={(e) => {
                const [sb, sd] = e.target.value.split('-');
                setSort(sb, sd);
              }}
            >
              <option value="createdAt-desc">Newest First</option>
              <option value="createdAt-asc">Oldest First</option>
              <option value="price-asc">Price: Low to High</option>
              <option value="price-desc">Price: High to Low</option>
              <option value="name-asc">Name: A to Z</option>
            </select>
          </div>

          {/* ── Conditional Rendering for Skeleton Loader ── */}
          {loading ? (
            <div className="grid-products">
              {/* Render 12 fake skeleton cards */}
              {[...Array(12)].map((_, index) => (
                <ProductSkeleton key={index} />
              ))}
            </div>
          ) : products.length > 0 ? (
            <>
              <div className="grid-products">
                {products.map(p => <ProductCard key={p.id} product={p} />)}
              </div>

              {/* ── Pagination ── */}
              {pagination.totalPages > 1 && (
                <div className="pagination">
                  <button
                    className="btn btn-ghost btn-sm"
                    disabled={page === 0}
                    onClick={() => goToPage(page - 1)}
                  >
                    ← Prev
                  </button>
                  <span className="pagination-info">
                    Page {page + 1} of {pagination.totalPages}
                  </span>
                  <button
                    className="btn btn-ghost btn-sm"
                    disabled={page >= pagination.totalPages - 1}
                    onClick={() => goToPage(page + 1)}
                  >
                    Next →
                  </button>
                </div>
              )}
            </>
          ) : (
            <div className="empty-state">
              <div className="empty-state-icon"><MdImage size={64} color="var(--text-muted)" /></div>
              <p className="empty-state-title">No products found</p>
            </div>
          )}
        </main>
      </div>
    </div>
  );
};

export default ProductsPage;