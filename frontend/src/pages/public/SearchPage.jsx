// src/pages/public/SearchPage.jsx
import { useState, useEffect } from 'react';
import { useSearchParams } from 'react-router-dom';
import { productApi } from '../../api/productApi';
import ProductCard from '../../components/product/ProductCard';
import Loader from '../../components/common/Loader';

const SearchPage = () => {
  const [searchParams] = useSearchParams();
  const keyword = searchParams.get('keyword') || '';
  const [products, setProducts] = useState([]);
  const [loading, setLoading] = useState(true);
  const [pagination, setPagination] = useState({});

useEffect(() => {
  if (!keyword) return;

  let isMounted = true;

  const fetchData = async () => {
    setLoading(true);
    try {
      const r = await productApi.search({ keyword, page: 0, size: 12 });

      if (!isMounted) return;

      setProducts(r.data.data?.content || []);
      setPagination(r.data.data || {});
    } catch {
      if (isMounted) setProducts([]);
    } finally {
      if (isMounted) setLoading(false);
    }
  };

  fetchData();

  return () => {
    isMounted = false;
  };
}, [keyword]);

  return (
    <div className="page-wrapper">
      <div className="container" style={{ paddingTop: '2rem', paddingBottom: '2rem' }}>
        <h2 style={{ marginBottom: '0.5rem' }}>
          Search results for "<span style={{ color: 'var(--accent-primary)' }}>{keyword}</span>"
        </h2>
        <p className="text-muted text-sm" style={{ marginBottom: '2rem' }}>
          {pagination.totalElements || 0} products found
        </p>

        {loading ? <Loader /> : products.length > 0 ? (
          <div className="grid-products">
            {products.map(p => <ProductCard key={p.id} product={p} />)}
          </div>
        ) : (
          <div className="empty-state">
            <div className="empty-state-icon">🔍</div>
            <p className="empty-state-title">No products found</p>
            <p className="empty-state-desc">Try a different search term</p>
          </div>
        )}
      </div>
    </div>
  );
};

export default SearchPage;
