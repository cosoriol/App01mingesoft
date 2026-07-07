import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { getUserBookings, cancelBooking } from '../services/api';

/**
 * PÁGINA: Mis Reservas (Épica 6)
 * ================================
 * Permite al cliente hacer seguimiento de sus reservas: ver su
 * estado (PENDING, CONFIRMED, CANCELLED, EXPIRED) y cancelarlas
 * mientras siga siendo posible.
 */
const STATUS_LABELS = {
  PENDING: { text: 'Pendiente de pago', className: 'status-pending' },
  CONFIRMED: { text: 'Confirmada', className: 'status-confirmed' },
  CANCELLED: { text: 'Cancelada', className: 'status-cancelled' },
  EXPIRED: { text: 'Expirada', className: 'status-expired' },
};

function MyBookings({ currentUser }) {
  const [bookings, setBookings] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    if (currentUser) {
      loadBookings();
    }
  }, [currentUser]);

  const loadBookings = async () => {
    try {
      setLoading(true);
      const response = await getUserBookings(currentUser.id);
      setBookings(response.data);
    } catch (err) {
      console.error('Error loading bookings:', err);
    } finally {
      setLoading(false);
    }
  };

  const handleCancel = async (bookingId) => {
    setError('');
    try {
      await cancelBooking(bookingId, currentUser.id);
      loadBookings(); // Refrescar la lista con el nuevo estado
    } catch (err) {
      setError(err.response?.data?.error || 'No se pudo cancelar la reserva');
    }
  };

  // Sin sesión activa: no hay reservas propias que mostrar
  if (!currentUser) {
    return (
      <div>
        <h1>🧳 Mis Reservas</h1>
        <p>
          Debes <Link to="/login">iniciar sesión</Link> para ver tus reservas.
        </p>
      </div>
    );
  }

  if (loading) return <p>Cargando tus reservas...</p>;

  return (
    <div>
      <h1>🧳 Mis Reservas</h1>

      {error && <p className="login-error">{error}</p>}

      {bookings.length === 0 ? (
        <p>Todavía no tienes reservas.</p>
      ) : (
        <div className="bookings-list">
          {bookings.map((booking) => {
            const statusInfo = STATUS_LABELS[booking.status] || { text: booking.status, className: '' };
            // Solo se puede cancelar mientras no esté ya cancelada o expirada
            const canCancel = booking.status === 'PENDING' || booking.status === 'CONFIRMED';

            return (
              <div className="booking-card" key={booking.id}>
                <div className="booking-card-header">
                  <h3>{booking.packageName}</h3>
                  <span className={`booking-status ${statusInfo.className}`}>
                    {statusInfo.text}
                  </span>
                </div>
                <p className="package-destination">📍 {booking.destination}</p>
                <p>👥 {booking.passengerCount} pasajero(s)</p>
                <p>
                  💰 ${booking.totalAmount}
                  {Number(booking.discountAmount) > 0 && (
                    <span style={{ color: '#888', fontSize: '0.85rem' }}>
                      {' '}(precio original: ${booking.baseAmount}, descuento: ${booking.discountAmount})
                    </span>
                  )}
                </p>
                {booking.discountDetails && booking.discountDetails.length > 0 && (
                  <ul style={{ fontSize: '0.85rem', color: '#666', margin: '0.5rem 0 0 1.2rem' }}>
                    {booking.discountDetails.map((detail, index) => (
                      <li key={index}>
                        🏷️ {detail.description}: {detail.percentage}%
                      </li>
                    ))}
                  </ul>
                )}
                {booking.discountSummary && (
                  <p
                    style={{
                      fontSize: '0.8rem',
                      color: '#999',
                      fontStyle: 'italic',
                      marginTop: '0.4rem',
                    }}
                  >
                    {booking.discountSummary}
                  </p>
                )}

                {canCancel && (
                  <button className="btn-logout" onClick={() => handleCancel(booking.id)}>
                    Cancelar reserva
                  </button>
                )}
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
}

export default MyBookings;
