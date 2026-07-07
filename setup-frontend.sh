#!/bin/bash
# ============================================================
# SETUP FRONTEND - Proyecto TravelAgency
# ============================================================
# Crea la estructura y el código del Frontend (ReactJS).
#
# Puede ejecutarse solo, o ser llamado por setup-project.sh
#
# USO: Ejecutar desde ~/App01Mingesoft/
#   chmod +x setup-frontend.sh
#   ./setup-frontend.sh
# ============================================================

# ============================================================
# 2. ESTRUCTURA DEL FRONTEND (ReactJS)
# ============================================================
echo ""
echo "📦 Creando estructura del Frontend..."

mkdir -p frontend/src/components
mkdir -p frontend/src/pages
mkdir -p frontend/src/services
mkdir -p frontend/public

# ---- package.json ----
cat > frontend/package.json << 'FPKGEOF'
{
  "name": "travelagency-frontend",
  "version": "1.0.0",
  "private": true,
  "dependencies": {
    "react": "^18.2.0",
    "react-dom": "^18.2.0",
    "react-router-dom": "^6.20.0",
    "react-scripts": "5.0.1",
    "axios": "^1.6.0"
  },
  "scripts": {
    "start": "react-scripts start",
    "build": "react-scripts build",
    "test": "react-scripts test"
  },
  "browserslist": {
    "production": [">0.2%", "not dead", "not op_mini all"],
    "development": ["last 1 chrome version", "last 1 firefox version", "last 1 safari version"]
  }
}
FPKGEOF

# ---- index.html ----
cat > frontend/public/index.html << 'HTMLEOF'
<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="utf-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1" />
    <title>TravelAgency - Paquetes Turísticos</title>
</head>
<body>
    <div id="root"></div>
</body>
</html>
HTMLEOF

# ---- index.js ----
cat > frontend/src/index.js << 'INDEXEOF'
import React from 'react';
import ReactDOM from 'react-dom/client';
import App from './App';

// Punto de entrada de la aplicación React
// Renderiza el componente App dentro del div con id="root"
const root = ReactDOM.createRoot(document.getElementById('root'));
root.render(
  <React.StrictMode>
    <App />
  </React.StrictMode>
);
INDEXEOF

# ---- App.js ----
cat > frontend/src/App.js << 'APPJSEOF'
import React from 'react';
import { BrowserRouter as Router, Routes, Route, Link } from 'react-router-dom';
import PackageList from './pages/PackageList';
import PackageDetail from './pages/PackageDetail';
import './App.css';

/**
 * COMPONENTE PRINCIPAL DE LA APLICACIÓN
 * =======================================
 * Define la navegación (rutas) y la estructura general.
 *
 * BrowserRouter: permite la navegación entre páginas sin recargar
 * Routes/Route: define qué componente se muestra en cada URL
 */
function App() {
  return (
    <Router>
      <div className="app">
        {/* Barra de navegación superior */}
        <nav className="navbar">
          <Link to="/" className="logo">
            ✈️ TravelAgency
          </Link>
          <div className="nav-links">
            <Link to="/">Paquetes</Link>
            <Link to="/my-bookings">Mis Reservas</Link>
            <Link to="/login">Iniciar Sesión</Link>
          </div>
        </nav>

        {/* Contenido principal: cambia según la URL */}
        <main className="main-content">
          <Routes>
            {/* URL "/" muestra la lista de paquetes */}
            <Route path="/" element={<PackageList />} />
            {/* URL "/packages/5" muestra el detalle del paquete 5 */}
            <Route path="/packages/:id" element={<PackageDetail />} />
          </Routes>
        </main>

        {/* Pie de página */}
        <footer className="footer">
          <p>© 2026 TravelAgency - Todos los derechos reservados</p>
        </footer>
      </div>
    </Router>
  );
}

export default App;
APPJSEOF

# ---- App.css ----
cat > frontend/src/App.css << 'CSSEOF'
/* ============================================================
   ESTILOS GLOBALES - TravelAgency
   ============================================================ */

* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

body {
  font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
  background-color: #f5f7fa;
  color: #333;
}

/* --- Barra de navegación --- */
.navbar {
  background: linear-gradient(135deg, #1a73e8, #0d47a1);
  padding: 1rem 2rem;
  display: flex;
  justify-content: space-between;
  align-items: center;
  box-shadow: 0 2px 4px rgba(0,0,0,0.1);
}

.logo {
  color: white;
  font-size: 1.5rem;
  font-weight: bold;
  text-decoration: none;
}

.nav-links {
  display: flex;
  gap: 1.5rem;
}

.nav-links a {
  color: white;
  text-decoration: none;
  font-size: 0.95rem;
  opacity: 0.9;
  transition: opacity 0.2s;
}

.nav-links a:hover {
  opacity: 1;
}

/* --- Contenido principal --- */
.main-content {
  max-width: 1200px;
  margin: 0 auto;
  padding: 2rem;
  min-height: calc(100vh - 140px);
}

/* --- Tarjetas de paquetes --- */
.packages-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 1.5rem;
  margin-top: 1.5rem;
}

.package-card {
  background: white;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0,0,0,0.08);
  transition: transform 0.2s, box-shadow 0.2s;
  cursor: pointer;
}

.package-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 4px 16px rgba(0,0,0,0.12);
}

.package-card-body {
  padding: 1.25rem;
}

.package-card-body h3 {
  font-size: 1.1rem;
  margin-bottom: 0.5rem;
  color: #1a73e8;
}

.package-destination {
  color: #666;
  font-size: 0.9rem;
  margin-bottom: 0.5rem;
}

.package-price {
  font-size: 1.3rem;
  font-weight: bold;
  color: #2e7d32;
}

.package-price span {
  font-size: 0.8rem;
  color: #888;
  font-weight: normal;
}

.package-dates {
  font-size: 0.85rem;
  color: #888;
  margin-top: 0.5rem;
}

.package-slots {
  display: inline-block;
  background: #e3f2fd;
  color: #1565c0;
  padding: 0.25rem 0.75rem;
  border-radius: 20px;
  font-size: 0.8rem;
  margin-top: 0.75rem;
}

/* --- Filtros de búsqueda --- */
.search-filters {
  background: white;
  padding: 1.5rem;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.08);
  display: flex;
  gap: 1rem;
  flex-wrap: wrap;
  align-items: flex-end;
}

.filter-group {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.filter-group label {
  font-size: 0.85rem;
  color: #666;
  font-weight: 500;
}

.filter-group input {
  padding: 0.5rem 0.75rem;
  border: 1px solid #ddd;
  border-radius: 8px;
  font-size: 0.9rem;
}

.btn-search {
  background: #1a73e8;
  color: white;
  border: none;
  padding: 0.5rem 1.5rem;
  border-radius: 8px;
  cursor: pointer;
  font-size: 0.9rem;
  transition: background 0.2s;
}

.btn-search:hover {
  background: #1557b0;
}

/* --- Pie de página --- */
.footer {
  background: #333;
  color: #aaa;
  text-align: center;
  padding: 1rem;
  font-size: 0.85rem;
}
CSSEOF

echo "   ✅ App.js y estilos creados"

# ---- Servicio API (conexión frontend → backend) ----
cat > frontend/src/services/api.js << 'APIEOF'
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
APIEOF

echo "   ✅ Servicio API creado"

# ---- Página PackageList ----
cat > frontend/src/pages/PackageList.js << 'PLISTEOF'
import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { getAvailablePackages, searchPackages } from '../services/api';

/**
 * PÁGINA: Lista de Paquetes Turísticos (Épica 3)
 * =================================================
 * Muestra todos los paquetes disponibles con filtros de búsqueda.
 *
 * useState = guarda datos que pueden cambiar (estado del componente)
 * useEffect = ejecuta código al cargar la página
 */
function PackageList() {
  // Estado: lista de paquetes cargados desde el backend
  const [packages, setPackages] = useState([]);
  // Estado: indica si está cargando datos
  const [loading, setLoading] = useState(true);
  // Estado: filtros de búsqueda
  const [filters, setFilters] = useState({
    destination: '',
    minPrice: '',
    maxPrice: '',
  });

  /**
   * useEffect con [] vacío = se ejecuta UNA vez al cargar la página.
   * Equivale a "cuando la página se abre, carga los paquetes".
   */
  useEffect(() => {
    loadPackages();
  }, []);

  /**
   * Cargar paquetes disponibles desde el backend.
   */
  const loadPackages = async () => {
    try {
      setLoading(true);
      const response = await getAvailablePackages();
      setPackages(response.data); // Guardar los paquetes en el estado
    } catch (error) {
      console.error('Error loading packages:', error);
    } finally {
      setLoading(false);
    }
  };

  /**
   * Buscar paquetes con los filtros aplicados.
   * Se ejecuta cuando el usuario presiona "Buscar".
   */
  const handleSearch = async () => {
    try {
      setLoading(true);
      // Enviar solo los filtros que tienen valor
      const activeFilters = {};
      if (filters.destination) activeFilters.destination = filters.destination;
      if (filters.minPrice) activeFilters.minPrice = filters.minPrice;
      if (filters.maxPrice) activeFilters.maxPrice = filters.maxPrice;

      const response = await searchPackages(activeFilters);
      setPackages(response.data);
    } catch (error) {
      console.error('Error searching packages:', error);
    } finally {
      setLoading(false);
    }
  };

  // Si está cargando, mostrar mensaje
  if (loading) return <p>Cargando paquetes...</p>;

  return (
    <div>
      <h1>🌍 Paquetes Turísticos Disponibles</h1>

      {/* Filtros de búsqueda */}
      <div className="search-filters">
        <div className="filter-group">
          <label>Destino</label>
          <input
            type="text"
            placeholder="Ej: Machu Picchu"
            value={filters.destination}
            onChange={(e) => setFilters({...filters, destination: e.target.value})}
          />
        </div>
        <div className="filter-group">
          <label>Precio mínimo</label>
          <input
            type="number"
            placeholder="500"
            value={filters.minPrice}
            onChange={(e) => setFilters({...filters, minPrice: e.target.value})}
          />
        </div>
        <div className="filter-group">
          <label>Precio máximo</label>
          <input
            type="number"
            placeholder="2000"
            value={filters.maxPrice}
            onChange={(e) => setFilters({...filters, maxPrice: e.target.value})}
          />
        </div>
        <button className="btn-search" onClick={handleSearch}>
          🔍 Buscar
        </button>
      </div>

      {/* Grid de tarjetas de paquetes */}
      <div className="packages-grid">
        {packages.length === 0 ? (
          <p>No se encontraron paquetes con esos filtros.</p>
        ) : (
          packages.map((pkg) => (
            <Link to={`/packages/${pkg.id}`} key={pkg.id} style={{textDecoration: 'none'}}>
              <div className="package-card">
                <div className="package-card-body">
                  <h3>{pkg.name}</h3>
                  <p className="package-destination">📍 {pkg.destination}</p>
                  <p className="package-price">
                    ${pkg.price} <span>por persona</span>
                  </p>
                  <p className="package-dates">
                    📅 {pkg.startDate} → {pkg.endDate}
                  </p>
                  <span className="package-slots">
                    🎟️ {pkg.availableSlots} cupos disponibles
                  </span>
                </div>
              </div>
            </Link>
          ))
        )}
      </div>
    </div>
  );
}

export default PackageList;
PLISTEOF

echo "   ✅ PackageList.js creado"

# ---- Página PackageDetail ----
cat > frontend/src/pages/PackageDetail.js << 'PDETEOF'
import React, { useState, useEffect } from 'react';
import { useParams } from 'react-router-dom';
import { getPackageById } from '../services/api';

/**
 * PÁGINA: Detalle de un Paquete Turístico (Épica 3)
 * ====================================================
 * Muestra toda la información de un paquete específico.
 * El usuario llega aquí al hacer clic en una tarjeta de paquete.
 *
 * useParams = obtiene el ID de la URL (Ej: /packages/5 → id = 5)
 */
function PackageDetail() {
  const { id } = useParams(); // Obtener ID de la URL
  const [pkg, setPkg] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const loadPackage = async () => {
      try {
        const response = await getPackageById(id);
        setPkg(response.data);
      } catch (error) {
        console.error('Error loading package:', error);
      } finally {
        setLoading(false);
      }
    };
    loadPackage();
  }, [id]);

  if (loading) return <p>Cargando detalle del paquete...</p>;
  if (!pkg) return <p>Paquete no encontrado.</p>;

  return (
    <div style={{ maxWidth: '800px', margin: '0 auto' }}>
      <h1>{pkg.name}</h1>
      <p style={{ color: '#666', fontSize: '1.1rem' }}>📍 {pkg.destination}</p>

      <div style={{
        background: 'white',
        borderRadius: '12px',
        padding: '2rem',
        marginTop: '1.5rem',
        boxShadow: '0 2px 8px rgba(0,0,0,0.08)'
      }}>
        <h2 style={{ color: '#2e7d32', marginBottom: '1rem' }}>
          ${pkg.price} <span style={{ fontSize: '0.9rem', color: '#888' }}>por persona</span>
        </h2>

        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1rem', marginBottom: '1.5rem' }}>
          <div>
            <strong>📅 Fecha inicio:</strong> {pkg.startDate}
          </div>
          <div>
            <strong>📅 Fecha término:</strong> {pkg.endDate}
          </div>
          <div>
            <strong>⏱️ Duración:</strong> {pkg.durationDays} días
          </div>
          <div>
            <strong>🎟️ Cupos disponibles:</strong> {pkg.availableSlots}
          </div>
        </div>

        <h3>📝 Descripción</h3>
        <p style={{ lineHeight: '1.6', marginBottom: '1rem' }}>{pkg.description}</p>

        {pkg.includedServices && (
          <>
            <h3>✅ Servicios incluidos</h3>
            <p style={{ lineHeight: '1.6', marginBottom: '1rem' }}>{pkg.includedServices}</p>
          </>
        )}

        {pkg.restrictions && (
          <>
            <h3>⚠️ Restricciones</h3>
            <p style={{ lineHeight: '1.6', marginBottom: '1rem' }}>{pkg.restrictions}</p>
          </>
        )}

        <button style={{
          background: '#1a73e8',
          color: 'white',
          border: 'none',
          padding: '0.75rem 2rem',
          borderRadius: '8px',
          fontSize: '1rem',
          cursor: 'pointer',
          marginTop: '1rem'
        }}>
          🛒 Reservar ahora
        </button>
      </div>
    </div>
  );
}

export default PackageDetail;
PDETEOF

echo "   ✅ PackageDetail.js creado"

# ---- Frontend .gitignore ----
cat > frontend/.gitignore << 'FGITEOF'
node_modules/
build/
.env
.env.local
FGITEOF

