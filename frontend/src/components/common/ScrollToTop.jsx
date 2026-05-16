// src/components/common/ScrollToTop.jsx
import { useEffect } from 'react';
import { useLocation } from 'react-router-dom';

const ScrollToTop = () => {
  const { pathname } = useLocation();

  useEffect(() => {
    // Try all possible scroll containers
    window.scrollTo({ top: 0, behavior: 'instant' });
    document.documentElement.scrollTo({ top: 0, behavior: 'instant' });
    document.body.scrollTo({ top: 0, behavior: 'instant' });

    // Also reset any scrollable div (e.g. #root or main)
    const root = document.getElementById('root');
    if (root) root.scrollTop = 0;
  }, [pathname]);

  return null;
};

export default ScrollToTop;