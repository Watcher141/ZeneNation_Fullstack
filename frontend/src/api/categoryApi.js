// src/api/categoryApi.js
import api from './axiosInstance';

export const categoryApi = {
  getAll: () => api.get('/api/v1/categories'),
  getById: (id) => api.get(`/api/v1/categories/${id}`),
  getAllAdmin: () => api.get('/api/v1/categories/admin/all'),
  create: (data) => api.post('/api/v1/categories', data),
  update: (id, data) => api.put(`/api/v1/categories/${id}`, data),
  uploadImage: (id, formData) => api.post(`/api/v1/categories/${id}/image`, formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  }),
  delete: (id) => api.delete(`/api/v1/categories/${id}`),
};
