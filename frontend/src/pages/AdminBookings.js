import React, { useState, useEffect, useCallback } from 'react';
import { getAllBookings, cancelBooking } from '../services/api';

/**
 * PÁGINA: Administración de Reservas (Épica 6)
 * ================================================
 * Igual que MyBookings, pero para un ADMIN: lista las reservas de
 * TODOS los clientes (no solo las propias) y permite cancelarlas.
 * Solo visible para rol ADMIN (el backend también lo valida — ver
 * BookingService.getAllBookings/AccessControlService.requireAdmin).
 */
const STATUS_LABELS = {
  PENDING: { text: 'Pendiente de pago', className: 'status-pending' },
  CONFIRMED: { text: 'Confirmada', className: 'status-confirmed' },
  CANCELLED: { text: 'Cancelada', className: 'status-cancelled' },
  EXPIRED: { text: 'Expirada', className: 'status-expired' },
};

function AdminBookings({ currentUser }) {
  const [bookings, setBookings] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const loadBookings = useCallback(async () => {
    try {
      setLoading(true);
      const response = await getAllBookings(currentUser.id);
      setBookings(response.data);
    } catch (err) {
      setError('No se pudieron cargar las reservas');
    } finally {
      setLoading(false);
    }
  }, [currentUser]);

  useEffect(() => {
    if (currentUser?.role === 'ADMIN') {
      loadBookings();
    }
  }, [currentUser, loadBookings]);

  const handleCancel = async (bookingId) => {
    if (!window.confirm('¿Cancelar esta reserva?')) return;
    setError('');
    try {
      await cancelBooking(bookingId, currentUser.id);
      loadBookings();
    } catch (err) {
      setError(err.response?.data?.error || 'No se pudo cancelar la reserva');
    }
  };

  if (!currentUser || currentUser.role !== 'ADMIN') {
    return (
      <div>
        <h1>🧳 Administrar Reservas</h1>
        <p>Esta sección es solo para administradores.</p>
      </div>
    );
  }

  if (loading) return <p>Cargando reservas...</p>;

  return (
    <div>
      <h1>🧳 Administrar Reservas</h1>

      {error && <p className="login-error">{error}</p>}

      {bookings.length === 0 ? (
        <p>No hay reservas registradas.</p>
      ) : (
        <div className="bookings-list">
          {bookings.map((booking) => {
            const statusInfo = STATUS_LABELS[booking.status] || { text: booking.status, className: '' };
            const canCancel = booking.status === 'PENDING' || booking.status === 'CONFIRMED';

            return (
              <div className="booking-card" key={booking.id}>
                <div className="booking-card-header">
                  <h3>{booking.packageName}</h3>
                  <span className={`booking-status ${statusInfo.className}`}>{statusInfo.text}</span>
                </div>
                <p className="package-destination">📍 {booking.destination}</p>
                <p>
                  🙋 {booking.userFullName} <span style={{ color: '#888' }}>({booking.userEmail})</span>
                </p>
                <p>👥 {booking.passengerCount} pasajero(s)</p>
                <p>
                  💰 ${booking.totalAmount}
                  {Number(booking.discountAmount) > 0 && (
                    <span style={{ color: '#888', fontSize: '0.85rem' }}>
                      {' '}(precio original: ${booking.baseAmount}, descuento: ${booking.discountAmount})
                    </span>
                  )}
                </p>

                <div className="booking-card-actions">
                  {canCancel && (
                    <button className="btn-danger" onClick={() => handleCancel(booking.id)}>
                      Cancelar reserva
                    </button>
                  )}
                </div>
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
}

export default AdminBookings;
