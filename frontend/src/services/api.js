import axios from 'axios';

/**
 * SERVICIO API - Conexión con el Backend
 * ========================================
 * Centraliza TODAS las llamadas HTTP al backend.
 *
 * axios es una librería que facilita hacer peticiones HTTP.
 * baseURL apunta al backend (puerto 8080).
 */

// baseURL relativo: las peticiones van al mismo origen que sirvio la SPA
// (nginx del frontend o el balanceador), que las reenvia al backend.
// Usar una URL absoluta tipo "http://localhost:8080" rompe produccion: el
// navegador del usuario final la ejecutaria literal contra su propia maquina.
// En "npm start" (desarrollo), el campo "proxy" de package.json reenvia estas
// mismas rutas relativas hacia el backend local.
const api = axios.create({
  baseURL: '/api',
  headers: {
    'Content-Type': 'application/json',
  },
});

// ============================================================
// FUNCIONES DE AUTENTICACIÓN (Épica 1)
// ============================================================

/**
 * Iniciar sesión con email y contraseña.
 * Llama a: POST /api/auth/login
 */
export const login = (email, password) => api.post('/auth/login', { email, password });

/**
 * Registrar un nuevo usuario.
 * Llama a: POST /api/auth/register
 */
export const register = (userData) => api.post('/auth/register', userData);

// ============================================================
// FUNCIONES PARA PAQUETES TURÍSTICOS (Épicas 2 y 3)
// ============================================================

/**
 * Obtener todos los paquetes disponibles para clientes.
 * Llama a: GET /api/packages/available
 */
export const getAvailablePackages = () => api.get('/packages/available');

/**
 * Buscar paquetes con filtros.
 * Llama a: GET /api/packages/search?destination=...&minPrice=...
 */
export const searchPackages = (filters) => api.get('/packages/search', { params: filters });

/**
 * Obtener detalle de un paquete por ID.
 * Llama a: GET /api/packages/5
 */
export const getPackageById = (id) => api.get(`/packages/${id}`);

/**
 * Obtener TODOS los paquetes, incluidos los no disponibles. userId debe ser un ADMIN.
 * Llama a: GET /api/packages?userId=1
 */
export const getAllPackages = (userId) => api.get('/packages', { params: { userId } });

/**
 * Crear un paquete nuevo. userId debe ser un ADMIN (lo valida el backend).
 * Llama a: POST /api/packages?userId=1
 */
export const createPackage = (userId, packageData) =>
  api.post('/packages', packageData, { params: { userId } });

/**
 * Cambiar el estado de un paquete (ej. cancelarlo o reactivarlo).
 * Llama a: PATCH /api/packages/5/status?status=CANCELLED&userId=1
 */
export const changePackageStatus = (packageId, status, userId) =>
  api.patch(`/packages/${packageId}/status`, null, { params: { status, userId } });

/**
 * Editar un paquete existente. userId debe ser un ADMIN.
 * Llama a: PUT /api/packages/5?userId=1
 */
export const updatePackage = (packageId, userId, packageData) =>
  api.put(`/packages/${packageId}`, packageData, { params: { userId } });

// ============================================================
// FUNCIONES PARA RESERVAS (Épica 4)
// ============================================================

/**
 * Crear una nueva reserva.
 * Llama a: POST /api/bookings?userId=1
 */
export const createBooking = (userId, bookingData) =>
  api.post(`/bookings?userId=${userId}`, bookingData);

/**
 * Obtener reservas de un usuario (opcionalmente filtradas por estado).
 * requesterId identifica a quien pregunta, para que el backend valide
 * que sea ese mismo usuario o un ADMIN; en todos los usos actuales del
 * frontend coincide con userId (cada quien ve solo sus reservas).
 * Llama a: GET /api/bookings/user/1?requesterId=1
 * Llama a: GET /api/bookings/user/1?requesterId=1&status=CONFIRMED
 */
export const getUserBookings = (userId, status) =>
  api.get(`/bookings/user/${userId}`, { params: { requesterId: userId, ...(status ? { status } : {}) } });

/**
 * Obtener TODAS las reservas (de cualquier cliente). Solo ADMIN.
 * Llama a: GET /api/bookings?userId=3
 */
export const getAllBookings = (userId) => api.get('/bookings', { params: { userId } });

/**
 * Cancelar una reserva. userId identifica a quien cancela, para que
 * el backend valide que sea el dueño de la reserva (o un ADMIN).
 * Llama a: PATCH /api/bookings/5/cancel?userId=1
 */
export const cancelBooking = (bookingId, userId) =>
  api.patch(`/bookings/${bookingId}/cancel`, null, { params: { userId } });

/**
 * Obtener el detalle de una reserva por ID. userId identifica a
 * quien pregunta, para que el backend valide que sea el dueño de la
 * reserva (o un ADMIN).
 * Llama a: GET /api/bookings/5?userId=1
 */
export const getBookingById = (bookingId, userId) =>
  api.get(`/bookings/${bookingId}`, { params: { userId } });

// ============================================================
// FUNCIONES PARA PAGOS (Épica 5)
// ============================================================

/**
 * Procesar un pago. userId debe ser el dueño de la reserva (o un ADMIN).
 * Llama a: POST /api/payments?userId=1
 */
export const processPayment = (userId, paymentData) =>
  api.post('/payments', paymentData, { params: { userId } });

/**
 * Resumen de la reserva ANTES de pagar (precio, descuentos, total).
 * userId identifica a quien pregunta (dueño de la reserva, o ADMIN).
 * Llama a: GET /api/payments/summary/5?userId=1
 */
export const getPaymentSummary = (bookingId, userId) =>
  api.get(`/payments/summary/${bookingId}`, { params: { userId } });

/**
 * Obtener el pago ya realizado de una reserva (para el comprobante).
 * userId identifica a quien pregunta (dueño de la reserva, o ADMIN).
 * Llama a: GET /api/payments/booking/5?userId=1
 */
export const getPaymentByBooking = (bookingId, userId) =>
  api.get(`/payments/booking/${bookingId}`, { params: { userId } });

// ============================================================
// FUNCIONES PARA REPORTES (Épica 7)
// ============================================================

/**
 * Reporte de ventas/ingresos en un rango de fechas (YYYY-MM-DD).
 * userId debe ser un ADMIN (lo valida el backend).
 * Llama a: GET /api/reports/sales?startDate=...&endDate=...&userId=3
 */
export const getSalesReport = (startDate, endDate, userId) =>
  api.get('/reports/sales', { params: { startDate, endDate, userId } });

/**
 * Ranking de paquetes más reservados.
 * Llama a: GET /api/reports/packages/most-booked?limit=10&userId=3
 */
export const getMostBookedPackages = (limit = 10, userId) =>
  api.get('/reports/packages/most-booked', { params: { limit, userId } });

/**
 * Resumen de reservas agrupadas por estado.
 * Llama a: GET /api/reports/bookings/summary?userId=3
 */
export const getBookingsSummary = (userId) =>
  api.get('/reports/bookings/summary', { params: { userId } });

/**
 * Efectividad de los descuentos aplicados (por tipo).
 * Llama a: GET /api/reports/discounts/effectiveness?userId=3
 */
export const getDiscountEffectiveness = (userId) =>
  api.get('/reports/discounts/effectiveness', { params: { userId } });

// ============================================================
// FUNCIONES DE ADMINISTRACIÓN DE USUARIOS (Épica 1)
// ============================================================

/**
 * Listar todos los usuarios. userId debe ser un ADMIN.
 * Llama a: GET /api/users?userId=1
 */
export const getAllUsers = (userId) => api.get('/users', { params: { userId } });

/**
 * Activar/desactivar una cuenta. userId (quien pide el cambio) debe ser ADMIN.
 * Llama a: PATCH /api/users/5/status?active=false&userId=1
 */
export const changeUserStatus = (targetUserId, active, userId) =>
  api.patch(`/users/${targetUserId}/status`, null, { params: { active, userId } });

/**
 * Actualizar el perfil propio (o, si eres ADMIN, el de otro usuario).
 * Llama a: PUT /api/users/5?userId=5
 */
export const updateProfile = (targetUserId, userId, profileData) =>
  api.put(`/users/${targetUserId}`, profileData, { params: { userId } });

export default api;
