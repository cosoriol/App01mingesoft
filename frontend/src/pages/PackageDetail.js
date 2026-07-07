import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { getPackageById, createBooking } from '../services/api';

/**
 * PÁGINA: Detalle de un Paquete Turístico (Épica 3)
 * ====================================================
 * Muestra toda la información de un paquete específico.
 * El usuario llega aquí al hacer clic en una tarjeta de paquete.
 *
 * useParams = obtiene el ID de la URL (Ej: /packages/5 → id = 5)
 */
function PackageDetail({ currentUser }) {
  const { id } = useParams(); // Obtener ID de la URL
  const navigate = useNavigate();
  const [pkg, setPkg] = useState(null);
  const [loading, setLoading] = useState(true);
  const [passengerCount, setPassengerCount] = useState(1);
  const [bookingError, setBookingError] = useState('');
  const [bookingLoading, setBookingLoading] = useState(false);

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

  /**
   * RESERVAR el paquete actual (Épica 4, conectado desde Épica 6).
   * Si no hay sesión activa, se manda al login primero.
   */
  const handleBooking = async () => {
    if (!currentUser) {
      navigate('/login');
      return;
    }
    setBookingError('');
    setBookingLoading(true);
    try {
      await createBooking(currentUser.id, {
        packageId: pkg.id,
        passengerCount: Number(passengerCount),
      });
      navigate('/my-bookings');
    } catch (error) {
      setBookingError(error.response?.data?.error || 'No se pudo crear la reserva');
    } finally {
      setBookingLoading(false);
    }
  };

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

        {bookingError && <p className="login-error">{bookingError}</p>}

        {pkg.status === 'AVAILABLE' && pkg.availableSlots > 0 ? (
          <div style={{ display: 'flex', gap: '1rem', alignItems: 'flex-end', marginTop: '1rem' }}>
            <div className="filter-group">
              <label>Pasajeros</label>
              <input
                type="number"
                min="1"
                max={pkg.availableSlots}
                value={passengerCount}
                onChange={(e) => setPassengerCount(e.target.value)}
              />
            </div>
            <button
              className="btn-search"
              onClick={handleBooking}
              disabled={bookingLoading}
            >
              {bookingLoading ? 'Reservando...' : '🛒 Reservar ahora'}
            </button>
          </div>
        ) : (
          <p style={{ marginTop: '1rem', color: '#c62828' }}>
            Este paquete no tiene cupos disponibles.
          </p>
        )}
      </div>
    </div>
  );
}

export default PackageDetail;
