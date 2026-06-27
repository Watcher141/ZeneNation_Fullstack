// src/context/CartContext.jsx
import { createContext, useContext, useState, useEffect } from 'react';
import { cartApi } from '../api/cartApi';
import { useAuth } from './AuthContext';

const CartContext = createContext(null);

export const CartProvider = ({ children }) => {
  const { isAuthenticated } = useAuth();
  const [cart, setCart] = useState(null);
  const [cartCount, setCartCount] = useState(0);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    if (isAuthenticated()) {
      fetchCart();
    } else {
      setCart(null);
      setCartCount(0);
    }
  }, [isAuthenticated()]);

  const fetchCart = async () => {
    try {
      setLoading(true);
      const res = await cartApi.getCart();
      setCart(res.data.data);
      setCartCount(res.data.data?.totalQuantity || 0);
    } catch {
      // silent fail
    } finally {
      setLoading(false);
    }
  };

  const addToCart = async (productId, quantity = 1, bundlePrice = null, bundleGroupId = null) => {
    const payload = { productId, quantity };
    if (bundlePrice !== null) payload.bundlePrice = bundlePrice;
    if (bundleGroupId !== null) payload.bundleGroupId = bundleGroupId;
    const res = await cartApi.addToCart(payload);
    setCart(res.data.data);
    setCartCount(res.data.data?.totalQuantity || 0);
    return res.data.data;
  };

  const updateItem = async (cartItemId, quantity) => {
    const res = await cartApi.updateItem(cartItemId, { productId: 0, quantity });
    setCart(res.data.data);
    setCartCount(res.data.data?.totalQuantity || 0);
  };

  const removeItem = async (cartItemId) => {
    const res = await cartApi.removeItem(cartItemId);
    setCart(res.data.data);
    setCartCount(res.data.data?.totalQuantity || 0);
  };

  const removeBundleGroup = async (bundleGroupId) => {
    const res = await cartApi.removeBundleGroup(bundleGroupId);
    setCart(res.data.data);
    setCartCount(res.data.data?.totalQuantity || 0);
  };

  const clearCart = async () => {
    const res = await cartApi.clearCart();
    setCart(res.data.data);
    setCartCount(0);
  };

  return (
    <CartContext.Provider value={{
      cart, cartCount, loading, fetchCart,
      addToCart, updateItem, removeItem, removeBundleGroup, clearCart
    }}>
      {children}
    </CartContext.Provider>
  );
};

export const useCart = () => {
  const ctx = useContext(CartContext);
  if (!ctx) throw new Error('useCart must be used within CartProvider');
  return ctx;
};