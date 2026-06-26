// src/api/orderApi.js
import api from './axiosInstance';

// ── Category API ──
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

// ── Product API ──
export const productApi = {
  getPreorders: () => api.get('/api/v1/products/preorder'),
  getAll: (params) => api.get('/api/v1/products', { params }),
  getById: (id) => api.get(`/api/v1/products/${id}`),
  getBySlug: (slug) => api.get(`/api/v1/products/slug/${slug}`),
  getByCategory: (categoryId, params) => api.get(`/api/v1/products/category/${categoryId}`, { params }),
  search: (params) => api.get('/api/v1/products/search', { params }),
  getAllAdmin: (params) => api.get('/api/v1/products/admin/all', { params }),
  create: (data) => api.post('/api/v1/products', data),
  update: (id, data) => api.put(`/api/v1/products/${id}`, data),
  delete: (id) => api.delete(`/api/v1/products/${id}`),
  toggleVisibility: (id) => api.patch(`/api/v1/products/${id}/visibility`),
  uploadImages: (id, formData) => api.post(`/api/v1/products/${id}/images`, formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  }),
  deleteImage: (productId, imageId) => api.delete(`/api/v1/products/${productId}/images/${imageId}`),
  replaceImage: (productId, imageId, formData) => api.put(`/api/v1/products/${productId}/images/${imageId}/replace`, formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  }),
  setPrimaryImage: (productId, imageId) => api.patch(`/api/v1/products/${productId}/images/${imageId}/primary`),
};

export const orderApi = {

  
  placeOrder: (data) => api.post('/api/v1/orders', data),
  getMyOrders: (params) => api.get('/api/v1/orders', { params }),
  getOrderById: (id) => api.get(`/api/v1/orders/${id}`),
  cancelOrder: (id) => api.patch(`/api/v1/orders/${id}/cancel`),

  // Admin
  getAllOrders: (params) => api.get('/api/v1/admin/orders', { params }),
  getOrderByIdAdmin: (id) => api.get(`/api/v1/admin/orders/${id}`),
  updateOrderStatus: (id, data) => api.patch(`/api/v1/admin/orders/${id}/status`, data),
};

// src/api/paymentApi.js
export const paymentApi = {
  verifyPayment: (data) => api.post('/api/v1/payments/verify', data),
  getPaymentByOrder: (orderId) => api.get(`/api/v1/payments/order/${orderId}`),
};

// src/api/couponApi.js
export const couponApi = {
  getMyWelcomeCoupon: () => api.get('/api/v1/coupons/my-welcome'),
  validate: (data, cartTotal) => api.post(`/api/v1/coupons/validate?cartTotal=${cartTotal}`, data),
  getAllAdmin: (params) => api.get('/api/v1/coupons/admin', { params }),
  getCouponById: (id) => api.get(`/api/v1/coupons/admin/${id}`),
  createCoupon: (data) => api.post('/api/v1/coupons/admin', data),
  updateCoupon: (id, data) => api.put(`/api/v1/coupons/admin/${id}`, data),
  toggleCoupon: (id) => api.patch(`/api/v1/coupons/admin/${id}/toggle`),
  deleteCoupon: (id) => api.delete(`/api/v1/coupons/admin/${id}`),
};

// src/api/addressApi.js
export const addressApi = {
  getAll: () => api.get('/api/v1/address'),
  getById: (id) => api.get(`/api/v1/address/${id}`),
  add: (data) => api.post('/api/v1/address', data),
  update: (id, data) => api.put(`/api/v1/address/${id}`, data),
  setDefault: (id) => api.patch(`/api/v1/address/${id}/default`),
  delete: (id) => api.delete(`/api/v1/address/${id}`),
};

// src/api/userApi.js
export const userApi = {
  getProfile: () => api.get('/api/v1/user/profile'),
  updateProfile: (data) => api.put('/api/v1/user/profile', data),
  changePassword: (data) => api.put('/api/v1/user/password', data),
  uploadProfileImage: (formData) => api.post('/api/v1/user/profile/image', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  }),
};

// src/api/adminApi.js
export const adminApi = {
  getDashboard: () => api.get('/api/v1/admin/dashboard'),
  getAllUsers: (params) => api.get('/api/v1/admin/users', { params }),
  getUserById: (id) => api.get(`/api/v1/admin/users/${id}`),
  deactivateUser: (id) => api.patch(`/api/v1/admin/users/${id}/deactivate`),
  activateUser: (id) => api.patch(`/api/v1/admin/users/${id}/activate`),
};

// ── Review API ──
export const reviewApi = {
  getSummary:      (productId)          => api.get(`/api/v1/reviews/product/${productId}/summary`),
  getReviews:      (productId, params)  => api.get(`/api/v1/reviews/product/${productId}`, { params }),
  submitReview:    (productId, data)    => api.post(`/api/v1/reviews/product/${productId}`, data),
  updateReview:    (productId, data)    => api.put(`/api/v1/reviews/product/${productId}`, data),
  deleteReview:    (productId)          => api.delete(`/api/v1/reviews/product/${productId}`),
  adminDelete:     (reviewId)           => api.delete(`/api/v1/reviews/${reviewId}/admin`),
};

// ── Rewards API ──
export const rewardApi = {
  getWallet:          ()             => api.get('/api/v1/rewards/wallet'),
  getHistory:         (params)       => api.get('/api/v1/rewards/history', { params }),
  getMaxRedeemable:   (orderTotal)   => api.get('/api/v1/rewards/max-redeemable', { params: { orderTotal } }),
};

// ── Announcement + Subscriber API ──
export const announcementApi = {
  getActive:    ()       => api.get('/api/v1/announcements/active'),
  subscribe:    (data)   => api.post('/api/v1/subscribers/subscribe', data),
  unsubscribe:  (email)  => api.post(`/api/v1/subscribers/unsubscribe?email=${email}`),

  // Admin
  getAll:       (params) => api.get('/api/v1/admin/announcements', { params }),
  create:       (data)   => api.post('/api/v1/admin/announcements', data),
  update:       (id, data) => api.put(`/api/v1/admin/announcements/${id}`, data),
  delete:       (id)     => api.delete(`/api/v1/admin/announcements/${id}`),
  toggle:       (id)     => api.patch(`/api/v1/admin/announcements/${id}/toggle`),
  getSubscriberCount: () => api.get('/api/v1/admin/subscribers/count'),
};

// ── Shipping API ──
export const shippingApi = {
  getConfig:           ()       => api.get('/api/v1/shipping/config'),
  updateDeliverySlabs: (data)   => api.put('/api/v1/shipping/admin/delivery-slabs', data),
  updateCodSlabs:      (data)   => api.put('/api/v1/shipping/admin/cod-slabs', data),
};