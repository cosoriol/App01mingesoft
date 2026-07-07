import React, { useState, useEffect } from 'react';
import { BrowserRouter as Router, Routes, Route, Link } from 'react-router-dom';
import PackageList from './pages/PackageList';
import PackageDetail from './pages/PackageDetail';
import Login from './pages/Login';
import './App.css';

/**
 * COMPONENTE PRINCIPAL DE LA APLICACIÓN
 * =======================================
 * Define la navegación (rutas) y la estructura general.
 *
 * BrowserRouter: permite la navegación entre páginas sin recargar
 * Routes/Route: define qué componente se muestra en cada URL
 *
 * currentUser: guarda la sesión activa (o null si no hay nadie
 * logueado). Se lee de localStorage al cargar la página, así la
 * sesión sobrevive a un refresh del navegador.
 */
function App() {
  const [currentUser, setCurrentUser] = useState(null);

  useEffect(() => {
    const stored = localStorage.getItem('currentUser');
    if (stored) setCurrentUser(JSON.parse(stored));
  }, []);

  const handleLogout = () => {
    localStorage.removeItem('currentUser');
    setCurrentUser(null);
  };

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
            {currentUser ? (
              <>
                <span className="nav-user">👤 {currentUser.fullName}</span>
                <button className="btn-logout" onClick={handleLogout}>
                  Cerrar Sesión
                </button>
              </>
            ) : (
              <Link to="/login">Iniciar Sesión</Link>
            )}
          </div>
        </nav>

        {/* Contenido principal: cambia según la URL */}
        <main className="main-content">
          <Routes>
            {/* URL "/" muestra la lista de paquetes */}
            <Route path="/" element={<PackageList />} />
            {/* URL "/packages/5" muestra el detalle del paquete 5 */}
            <Route path="/packages/:id" element={<PackageDetail />} />
            {/* URL "/login" muestra el formulario de inicio de sesión */}
            <Route path="/login" element={<Login onLogin={setCurrentUser} />} />
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
