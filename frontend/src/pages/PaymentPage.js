import React, { useState, useEffect } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import { getPaymentSummary, processPayment, getErrorMessage } from '../services/api';

/**
 * PÁGINA: Pagar una Reserva (Épica 5)
 * ======================================
 * Tercer paso del flujo de compra. Muestra el resumen REAL de la
 * reserva (con los descuentos ya calculados por el backend al
 * crearla) y un formulario de tarjeta de crédito simulada.
 *
 * El pago es simulado: no se valida contra ningún banco real, solo
 * el FORMATO de los datos (número de 16 dígitos, CVV de 3, fecha de
 * expiración vigente en formato MM/YY).
 */

/** Formatea el número de tarjeta agregando un espacio cada 4 dígitos. */
function formatCardNumber(rawValue) {
  const digits = rawValue.replace(/\D/g, '').slice(0, 16);
  return digits.match(/.{1,4}/g)?.join(' ') || digits;
}

/** Formatea la fecha de expiración como MM/YY, agregando "/" automáticamente. */
function formatExpirationDate(rawValue) {
  const digits = rawValue.replace(/\D/g, '').slice(0, 4);
  if (digits.length >= 3) {
    return `${digits.slice(0, 2)}/${digits.slice(2)}`;
  }
  return digits;
}

function PaymentPage({ currentUser }) {
  const { bookingId } = useParams();
  const navigate = useNavigate();

  const [summary, setSummary] = useState(null);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState('');
  const [paymentError, setPaymentError] = useState('');
  const [paying, setPaying] = useState(false);

  const [cardHolderName, setCardHolderName] = useState('');
  const [cardNumber, setCardNumber] = useState('');
  const [expirationDate, setExpirationDate] = useState('');
  const [cvv, setCvv] = useState('');

  useEffect(() => {
    if (!currentUser) return;
    const loadSummary = async () => {
      try {
        const response = await getPaymentSummary(bookingId, currentUser.id);
        setSummary(response.data);
      } catch (err) {
        setLoadError(getErrorMessage(err, 'No se pudo cargar el resumen de la reserva'));
      } finally {
        setLoading(false);
      }
    };
    loadSummary();
  }, [bookingId, currentUser]);

  /**
   * CONFIRMAR Y PAGAR.
   * El número de tarjeta se envía SIN espacios (solo dígitos); el
   * resto de los campos van tal cual se muestran en el formulario.
   */
  const handlePay = async (e) => {
    e.preventDefault();
    setPaymentError('');
    setPaying(true);
    try {
      await processPayment(currentUser.id, {
        bookingId: Number(bookingId),
        amount: summary.totalAmount,
        paymentMethod: 'CREDIT_CARD',
        cardNumber: cardNumber.replace(/\s/g, ''),
        expirationDate,
        cvv,
        cardHolderName,
      });
      navigate(`/payment-confirmation/${bookingId}`);
    } catch (err) {
      setPaymentError(getErrorMessage(err, 'No se pudo procesar el pago'));
    } finally {
      setPaying(false);
    }
  };

  if (!currentUser) {
    return (
      <div>
        <h1>💳 Pagar</h1>
        <p>
          Debes <Link to="/login">iniciar sesión</Link> para pagar una reserva.
        </p>
      </div>
    );
  }

  if (loading) return <p>Cargando resumen de la reserva...</p>;
  if (loadError) return <p className="login-error">{loadError}</p>;
  if (!summary) return null;

  // Si la reserva ya no está PENDING, no tiene sentido mostrar el
  // formulario de pago (ya fue pagada, cancelada o expiró).
  if (summary.bookingStatus !== 'PENDING') {
    return (
      <div style={{ maxWidth: '600px', margin: '0 auto' }}>
        <h1>💳 Pagar</h1>
        <p className="login-error">
          Esta reserva ya no está pendiente de pago (estado actual: {summary.bookingStatus}).
        </p>
        <Link to="/my-bookings" className="btn-secondary btn-large" style={{ display: 'inline-block', textDecoration: 'none', marginTop: '1rem' }}>
          Ver mis reservas
        </Link>
      </div>
    );
  }

  return (
    <div style={{ maxWidth: '700px', margin: '0 auto' }}>
      <h1>💳 Confirmar Pago</h1>

      {/* Resumen de la reserva, con los descuentos YA calculados */}
      <div className="summary-card">
        <h2>{summary.packageName}</h2>
        <p className="package-destination">📍 {summary.destination}</p>
        <p>👥 {summary.passengerCount} pasajero(s)</p>

        <div className="payment-summary">
          <div className="payment-summary-row">
            <span>Precio original</span>
            <span>${summary.baseAmount}</span>
          </div>
          {Number(summary.discountAmount) > 0 && (
            <div className="payment-summary-row payment-summary-discount">
              <span>Descuentos ({summary.discountPercentage}%)</span>
              <span>-${summary.discountAmount}</span>
            </div>
          )}
          {summary.discountDetails?.map((detail, index) => (
            <p key={index} className="price-note">
              🏷️ {detail.description}: {detail.percentage}%
            </p>
          ))}
          <div className="payment-summary-divider" />
          <div className="payment-summary-row payment-summary-total">
            <span>TOTAL A PAGAR</span>
            <span>${summary.totalAmount}</span>
          </div>
        </div>
      </div>

      {/* Formulario de tarjeta de crédito simulada */}
      <form className="credit-card-form" onSubmit={handlePay}>
        <div className="credit-card-icons">💳 VISA &nbsp; MASTERCARD</div>

        <div className="filter-group">
          <label>Nombre del titular</label>
          <input
            type="text"
            value={cardHolderName}
            onChange={(e) => setCardHolderName(e.target.value)}
            placeholder="Como aparece en la tarjeta"
            required
          />
        </div>

        <div className="filter-group">
          <label>Número de tarjeta</label>
          <input
            type="text"
            className="card-number-input"
            value={cardNumber}
            onChange={(e) => setCardNumber(formatCardNumber(e.target.value))}
            placeholder="0000 0000 0000 0000"
            inputMode="numeric"
            required
          />
        </div>

        <div style={{ display: 'flex', gap: '1rem' }}>
          <div className="filter-group" style={{ flex: 1 }}>
            <label>Fecha de expiración</label>
            <input
              type="text"
              value={expirationDate}
              onChange={(e) => setExpirationDate(formatExpirationDate(e.target.value))}
              placeholder="MM/YY"
              inputMode="numeric"
              required
            />
          </div>
          <div className="filter-group" style={{ width: '100px' }}>
            <label>CVV</label>
            <input
              type="password"
              value={cvv}
              onChange={(e) => setCvv(e.target.value.replace(/\D/g, '').slice(0, 3))}
              placeholder="123"
              inputMode="numeric"
              required
            />
          </div>
        </div>

        {paymentError && <p className="login-error">{paymentError}</p>}

        <button className="btn-pay" type="submit" disabled={paying}>
          {paying ? 'Procesando pago...' : `Confirmar y Pagar $${summary.totalAmount}`}
        </button>
      </form>
    </div>
  );
}

export default PaymentPage;
