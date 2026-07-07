import axios from 'axios';

/**
 * SERVICIO API - Conexión con el Backend
 * ========================================
 * Centraliza TODAS las llamadas HTTP al backend.
 *
 * axios es una librería que facilita hacer peticiones HTTP.
 * baseURL apunta al backend (puerto 8080).
 */

// Crear instancia de axios con configuración base
const api = axios.create({
  baseURL: 'http://localhost:8080/api',  // URL del backend
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
 * Obtener reservas de un usuario.
 * Llama a: GET /api/bookings/user/1
 */
export const getUserBookings = (userId) => api.get(`/bookings/user/${userId}`);

// ============================================================
// FUNCIONES PARA PAGOS (Épica 5)
// ============================================================

/**
 * Procesar un pago.
 * Llama a: POST /api/payments
 */
export const processPayment = (paymentData) => api.post('/payments', paymentData);

export default api;
