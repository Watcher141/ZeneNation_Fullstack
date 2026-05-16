// src/api/productApi.js
import api from './axiosInstance';

export const productApi = {
  getPreorders: ()      => api.get('/api/v1/products/preorder'),
  getAll: (params) => api.get('/api/v1/products', { params }),
  getById: (id) => api.get(`/api/v1/products/${id}`),
  getBySlug: (slug) => api.get(`/api/v1/products/slug/${slug}`),
  getByCategory: (categoryId, params) => api.get(`/api/v1/products/category/${categoryId}`, { params }),
  search: (params) => api.get('/api/v1/products/search', { params }),

  // Admin
  getAllAdmin: (params) => api.get('/api/v1/products/admin/all', { params }),
  create: (data) => api.post('/api/v1/products', data),
  update: (id, data) => api.put(`/api/v1/products/${id}`, data),
  delete: (id) => api.delete(`/api/v1/products/${id}`),
  toggleVisibility: (id) => api.patch(`/api/v1/products/${id}/visibility`),

  // Images
  uploadImages: (id, formData) => api.post(`/api/v1/products/${id}/images`, formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  }),
  deleteImage: (productId, imageId) => api.delete(`/api/v1/products/${productId}/images/${imageId}`),
  replaceImage: (productId, imageId, formData) => api.put(`/api/v1/products/${productId}/images/${imageId}/replace`, formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  }),
  setPrimaryImage: (productId, imageId) => api.patch(`/api/v1/products/${productId}/images/${imageId}/primary`),
};