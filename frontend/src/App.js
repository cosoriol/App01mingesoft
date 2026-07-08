import React, { useState, useEffect } from 'react';
import { BrowserRouter as Router, Routes, Route, Link } from 'react-router-dom';
import PackageList from './pages/PackageList';
import PackageDetail from './pages/PackageDetail';
import Login from './pages/Login';
import MyBookings from './pages/MyBookings';
import Reports from './pages/Reports';
import AdminPackages from './pages/AdminPackages';
import Profile from './pages/Profile';
import BookingPage from './pages/BookingPage';
import PaymentPage from './pages/PaymentPage';
import PaymentConfirmation from './pages/PaymentConfirmation';
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
            {currentUser?.role === 'ADMIN' && <Link to="/admin/packages">Paquetes (Admin)</Link>}
            {currentUser?.role === 'ADMIN' && <Link to="/reports">Reportes</Link>}
            {currentUser && <Link to="/profile">Mi Perfil</Link>}
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
            <Route path="/packages/:id" element={<PackageDetail currentUser={currentUser} />} />
            {/* URL "/booking/5" elige pasajeros y crea la reserva del paquete 5 (Épica 4) */}
            <Route path="/booking/:packageId" element={<BookingPage currentUser={currentUser} />} />
            {/* URL "/payment/12" paga la reserva 12 (Épica 5) */}
            <Route path="/payment/:bookingId" element={<PaymentPage currentUser={currentUser} />} />
            {/* URL "/payment-confirmation/12" muestra el comprobante de la reserva 12 (Épica 5) */}
            <Route path="/payment-confirmation/:bookingId" element={<PaymentConfirmation currentUser={currentUser} />} />
            {/* URL "/login" muestra el formulario de inicio de sesión */}
            <Route path="/login" element={<Login onLogin={setCurrentUser} />} />
            {/* URL "/my-bookings" muestra el seguimiento de reservas (Épica 6) */}
            <Route path="/my-bookings" element={<MyBookings currentUser={currentUser} />} />
            {/* URL "/reports" muestra los reportes administrativos (Épica 7) */}
            <Route path="/reports" element={<Reports currentUser={currentUser} />} />
            {/* URL "/admin/packages" administra paquetes turísticos (Épica 2) */}
            <Route path="/admin/packages" element={<AdminPackages currentUser={currentUser} />} />
            {/* URL "/profile" edita el perfil del usuario logueado (Épica 1) */}
            <Route path="/profile" element={<Profile currentUser={currentUser} onProfileUpdated={setCurrentUser} />} />
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
