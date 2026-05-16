// src/api/homeSectionApi.js
import api from './axiosInstance';

export const homeSectionApi = {
  // Public
  getActive: () => api.get('/api/v1/home-sections/active'),

  // Admin
  getAll: () => api.get('/api/v1/home-sections/admin/all'),
  create: (data) => api.post('/api/v1/home-sections/admin', data),
  update: (id, data) => api.put(`/api/v1/home-sections/admin/${id}`, data),
  delete: (id) => api.delete(`/api/v1/home-sections/admin/${id}`),
  toggle: (id) => api.patch(`/api/v1/home-sections/admin/${id}/toggle`),
  addProduct: (sectionId, productId) => api.post(`/api/v1/home-sections/admin/${sectionId}/products/${productId}`),
  removeProduct: (sectionId, productId) => api.delete(`/api/v1/home-sections/admin/${sectionId}/products/${productId}`),
};