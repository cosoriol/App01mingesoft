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
