// src/pages/public/ProductsPage.jsx
import { useState, useEffect } from 'react';
import { useSearchParams } from 'react-router-dom';
import { productApi } from '../../api/productApi';
import { categoryApi } from '../../api/categoryApi';
import ProductCard from '../../components/product/ProductCard';
import Loader from '../../components/common/Loader';
import './ProductsPage.css';

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

  const setParam = (key, value) => {
    const p = new URLSearchParams(searchParams);
    p.set(key, value);
    p.set('page', '0');
    setSearchParams(p);
  };

  return (
    <div className="page-wrapper">
      <div className="container products-page">
        {/* Filters */}
        <aside className="products-sidebar">
          <h3 className="sidebar-title">Categories</h3>
          <button
            className={`sidebar-cat-item ${!categoryId ? 'active' : ''}`}
            onClick={() => { const p = new URLSearchParams(searchParams); p.delete('categoryId'); p.set('page','0'); setSearchParams(p); }}
          >
            All Products
          </button>
          {categories.map(cat => (
            <button
              key={cat.id}
              className={`sidebar-cat-item ${categoryId == cat.id ? 'active' : ''}`}
              onClick={() => setParam('categoryId', cat.id)}
            >
              {cat.name}
            </button>
          ))}
        </aside>

        {/* Products */}
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
                const p = new URLSearchParams(searchParams);
                p.set('sortBy', sb); p.set('sortDir', sd); p.set('page', '0');
                setSearchParams(p);
              }}
            >
              <option value="createdAt-desc">Newest First</option>
              <option value="createdAt-asc">Oldest First</option>
              <option value="price-asc">Price: Low to High</option>
              <option value="price-desc">Price: High to Low</option>
              <option value="name-asc">Name: A to Z</option>
            </select>
          </div>

          {loading ? <Loader /> : products.length > 0 ? (
            <>
              <div className="grid-products">
                {products.map(p => <ProductCard key={p.id} product={p} />)}
              </div>
              {/* Pagination */}
              {pagination.totalPages > 1 && (
                <div className="pagination">
                  <button
                    className="btn btn-ghost btn-sm"
                    disabled={pagination.isFirst}
                    onClick={() => setParam('page', page - 1)}
                  >
                    ← Prev
                  </button>
                  <span className="pagination-info">
                    Page {page + 1} of {pagination.totalPages}
                  </span>
                  <button
                    className="btn btn-ghost btn-sm"
                    disabled={pagination.isLast}
                    onClick={() => setParam('page', page + 1)}
                  >
                    Next →
                  </button>
                </div>
              )}
            </>
          ) : (
            <div className="empty-state">
              <div className="empty-state-icon">🎌</div>
              <p className="empty-state-title">No products found</p>
            </div>
          )}
        </main>
      </div>
    </div>
  );
};

export default ProductsPage;
