import React, { useState, useEffect } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import { getPaymentByBooking, getErrorMessage } from '../services/api';

/**
 * PÁGINA: Confirmación de Pago (Épica 5)
 * ==========================================
 * Último paso del flujo de compra. Se llega aquí justo después de un
 * pago exitoso, y también sirve como "comprobante" reutilizable desde
 * Mis Reservas (botón "Ver comprobante" en una reserva CONFIRMED),
 * ya que solo hace una consulta GET del pago ya registrado.
 */
function PaymentConfirmation({ currentUser }) {
  const { bookingId } = useParams();
  const navigate = useNavigate();

  const [payment, setPayment] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    if (!currentUser) return;
    const loadPayment = async () => {
      try {
        const response = await getPaymentByBooking(bookingId, currentUser.id);
        setPayment(response.data);
      } catch (err) {
        setError(getErrorMessage(err, 'No se pudo cargar el comprobante de pago'));
      } finally {
        setLoading(false);
      }
    };
    loadPayment();
  }, [bookingId, currentUser]);

  if (!currentUser) {
    return (
      <div>
        <h1>✅ Confirmación</h1>
        <p>
          Debes <Link to="/login">iniciar sesión</Link> para ver este comprobante.
        </p>
      </div>
    );
  }

  if (loading) return <p>Cargando comprobante...</p>;
  if (error) return <p className="login-error">{error}</p>;
  if (!payment) return null;

  return (
    <div className="confirmation-page">
      <div className="confirmation-icon">✅</div>
      <h1>¡Pago realizado exitosamente!</h1>

      <div className="summary-card confirmation-summary">
        <div className="summary-grid">
          <div><strong>Número de reserva:</strong> #{payment.bookingId}</div>
          <div><strong>Paquete:</strong> {payment.packageName}</div>
          <div><strong>Destino:</strong> {payment.destination}</div>
          <div><strong>Fechas del viaje:</strong> {payment.startDate} → {payment.endDate}</div>
          <div><strong>Pasajeros:</strong> {payment.passengerCount}</div>
          <div><strong>Monto pagado:</strong> ${payment.amount}</div>
          <div><strong>Método de pago:</strong> Tarjeta terminada en ****{payment.cardLastFour}</div>
          <div>
            <strong>Estado:</strong>{' '}
            <span className="booking-status status-confirmed">Confirmada</span>
          </div>
        </div>
      </div>

      <div className="confirmation-actions">
        <button className="btn-primary btn-large" onClick={() => navigate('/my-bookings')}>
          Ver mis reservas
        </button>
        <button className="btn-secondary btn-large" onClick={() => navigate('/')}>
          Buscar más paquetes
        </button>
      </div>
    </div>
  );
}

export default PaymentConfirmation;
