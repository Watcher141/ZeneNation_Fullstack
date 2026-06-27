// src/api/cartApi.js
import api from './axiosInstance';

export const cartApi = {
  getCart: () => api.get('/api/v1/cart'),
  addToCart: (data) => api.post('/api/v1/cart/items', data),
  updateItem: (cartItemId, data) => api.put(`/api/v1/cart/items/${cartItemId}`, data),
  removeItem: (cartItemId) => api.delete(`/api/v1/cart/items/${cartItemId}`),
  removeBundleGroup: (bundleGroupId) => api.delete(`/api/v1/cart/bundle/${bundleGroupId}`),
  clearCart: () => api.delete('/api/v1/cart'),
};