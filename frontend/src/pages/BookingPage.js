import React, { useState, useEffect } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import { getPackageById, createBooking } from '../services/api';

/**
 * PÁGINA: Reservar un Paquete (Épica 4)
 * ========================================
 * Segundo paso del flujo de compra (después del detalle del paquete).
 * Muestra el resumen del paquete elegido, permite ingresar la
 * cantidad de pasajeros, y crea la reserva al continuar.
 *
 * El precio que se ve aquí es el precio BASE (precio × pasajeros):
 * los descuentos reales (por grupo, cliente frecuente, etc.) los
 * calcula el backend recién al crear la reserva, así que se muestran
 * en la siguiente pantalla (pago), no en esta.
 */
function BookingPage({ currentUser }) {
  const { packageId } = useParams();
  const navigate = useNavigate();

  const [pkg, setPkg] = useState(null);
  const [loading, setLoading] = useState(true);
  const [passengerCount, setPassengerCount] = useState(1);
  const [error, setError] = useState('');
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    const loadPackage = async () => {
      try {
        const response = await getPackageById(packageId);
        setPkg(response.data);
      } catch (err) {
        setError('No se pudo cargar el paquete seleccionado');
      } finally {
        setLoading(false);
      }
    };
    loadPackage();
  }, [packageId]);

  /**
   * CREAR LA RESERVA y avanzar al paso de pago.
   * userId viene de la sesión activa (currentUser), nunca hardcodeado,
   * para que la reserva quede a nombre de quien realmente la hace.
   */
  const handleContinue = async () => {
    setError('');
    setSubmitting(true);
    try {
      const response = await createBooking(currentUser.id, {
        packageId: pkg.id,
        passengerCount: Number(passengerCount),
      });
      navigate(`/payment/${response.data.id}`);
    } catch (err) {
      setError(err.response?.data?.error || 'No se pudo crear la reserva');
    } finally {
      setSubmitting(false);
    }
  };

  if (!currentUser) {
    return (
      <div>
        <h1>🧾 Reservar</h1>
        <p>
          Debes <Link to="/login">iniciar sesión</Link> para reservar un paquete.
        </p>
      </div>
    );
  }

  if (loading) return <p>Cargando...</p>;
  if (!pkg) return <p className="login-error">{error || 'Paquete no encontrado.'}</p>;

  const baseAmount = (Number(pkg.price) * Number(passengerCount || 0)).toFixed(2);
  const maxPassengers = pkg.availableSlots;
  const validCount = passengerCount >= 1 && passengerCount <= maxPassengers;

  return (
    <div style={{ maxWidth: '700px', margin: '0 auto' }}>
      <h1>🧾 Reservar Paquete</h1>

      {/* Resumen del paquete elegido */}
      <div className="summary-card">
        <h2>{pkg.name}</h2>
        <p className="package-destination">📍 {pkg.destination}</p>
        <div className="summary-grid">
          <div><strong>📅 Inicio:</strong> {pkg.startDate}</div>
          <div><strong>📅 Término:</strong> {pkg.endDate}</div>
          <div><strong>💵 Precio por persona:</strong> ${pkg.price}</div>
          <div><strong>🎟️ Cupos disponibles:</strong> {maxPassengers}</div>
        </div>
      </div>

      {/* Formulario de cantidad de pasajeros */}
      <div className="summary-card">
        <div className="filter-group">
          <label>Cantidad de pasajeros</label>
          <input
            type="number"
            min="1"
            max={maxPassengers}
            value={passengerCount}
            onChange={(e) => setPassengerCount(e.target.value)}
          />
        </div>

        {!validCount && (
          <p className="login-error">
            La cantidad de pasajeros debe ser entre 1 y {maxPassengers}.
          </p>
        )}

        {/* Precio en vivo: solo el monto BASE (precio x pasajeros).
            Los descuentos reales se calculan al crear la reserva y se
            muestran en la pantalla de pago que sigue. */}
        <div className="price-breakdown">
          <div className="price-row">
            <span>${pkg.price} × {passengerCount || 0} pasajero(s)</span>
            <span>${baseAmount}</span>
          </div>
          <p className="price-note">
            Los descuentos aplicables (grupo, cliente frecuente, promociones) se
            calculan y muestran en el siguiente paso, antes de pagar.
          </p>
        </div>

        {error && <p className="login-error">{error}</p>}

        <button
          className="btn-primary btn-large"
          onClick={handleContinue}
          disabled={submitting || !validCount}
        >
          {submitting ? 'Creando reserva...' : 'Continuar al pago'}
        </button>
      </div>
    </div>
  );
}

export default BookingPage;
