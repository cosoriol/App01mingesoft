import React, { useState, useEffect, useCallback } from 'react';
import {
  getSalesReport,
  getMostBookedPackages,
  getBookingsSummary,
  getDiscountEffectiveness,
  getErrorMessage,
} from '../services/api';

/**
 * PÁGINA: Reportes (Épica 7)
 * ============================
 * Panel de administración con los 4 reportes del backend: ventas por
 * período, paquetes más reservados, resumen de reservas por estado,
 * y efectividad de los descuentos aplicados.
 *
 * Solo visible para usuarios con rol ADMIN.
 */
const STATUS_COLORS = {
  PENDING: { bg: '#fff3e0', text: '#e65100', label: 'Pendiente' },
  CONFIRMED: { bg: '#e8f5e9', text: '#2e7d32', label: 'Confirmada' },
  CANCELLED: { bg: '#f5f5f5', text: '#757575', label: 'Cancelada' },
  EXPIRED: { bg: '#fdecea', text: '#c62828', label: 'Expirada' },
};

const DISCOUNT_TYPE_LABELS = {
  GROUP: 'Descuento por grupo',
  FREQUENT_CLIENT: 'Cliente frecuente',
  MULTI_PACKAGE: 'Multi-paquete',
  PROMOTION: 'Promoción',
};

const money = (value) =>
  new Intl.NumberFormat('es-CL', { style: 'currency', currency: 'USD', maximumFractionDigits: 2 })
    .format(Number(value) || 0);

/** Fila de barra horizontal: etiqueta a la izquierda, barra proporcional al máximo, valor al final. */
function BarRow({ label, value, max, color, sublabel }) {
  const widthPercent = max > 0 ? Math.max((value / max) * 100, 4) : 0;
  return (
    <div className="bar-row">
      <div className="bar-row-label">
        <span>{label}</span>
        {sublabel && <span className="bar-row-sublabel">{sublabel}</span>}
      </div>
      <div className="bar-track">
        <div className="bar-fill" style={{ width: `${widthPercent}%`, background: color }} />
      </div>
      <span className="bar-value">{value}</span>
    </div>
  );
}

function Reports({ currentUser }) {
  const today = new Date().toISOString().slice(0, 10);
  const [startDate, setStartDate] = useState(`${new Date().getFullYear()}-01-01`);
  const [endDate, setEndDate] = useState(today);

  const [sales, setSales] = useState(null);
  const [packages, setPackages] = useState([]);
  const [summary, setSummary] = useState(null);
  const [discounts, setDiscounts] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const loadReports = useCallback(async () => {
    setError('');
    setLoading(true);
    try {
      const [salesRes, packagesRes, summaryRes, discountsRes] = await Promise.all([
        getSalesReport(startDate, endDate, currentUser.id),
        getMostBookedPackages(5, currentUser.id),
        getBookingsSummary(currentUser.id),
        getDiscountEffectiveness(currentUser.id),
      ]);
      setSales(salesRes.data);
      setPackages(packagesRes.data);
      setSummary(summaryRes.data);
      setDiscounts(discountsRes.data);
    } catch (err) {
      setError(getErrorMessage(err, 'No se pudieron cargar los reportes'));
    } finally {
      setLoading(false);
    }
  }, [startDate, endDate, currentUser]);

  useEffect(() => {
    if (currentUser?.role === 'ADMIN') {
      loadReports();
    }
    // eslint-disable-next-line
  }, [currentUser]);

  if (!currentUser || currentUser.role !== 'ADMIN') {
    return (
      <div>
        <h1>📊 Reportes</h1>
        <p>Esta sección es solo para administradores.</p>
      </div>
    );
  }

  const maxPackageBookings = Math.max(0, ...packages.map((p) => p.bookingCount));
  const maxStatusCount = summary ? Math.max(0, ...summary.byStatus.map((s) => s.count)) : 0;
  const maxDiscountUses = discounts ? Math.max(0, ...discounts.byType.map((d) => d.timesApplied)) : 0;

  return (
    <div>
      <h1>📊 Reportes</h1>

      {error && <p className="login-error">{error}</p>}

      {/* --- Filtro de fechas para el reporte de ventas --- */}
      <div className="search-filters">
        <div className="filter-group">
          <label>Desde</label>
          <input type="date" value={startDate} onChange={(e) => setStartDate(e.target.value)} />
        </div>
        <div className="filter-group">
          <label>Hasta</label>
          <input type="date" value={endDate} onChange={(e) => setEndDate(e.target.value)} />
        </div>
        <button className="btn-search" onClick={loadReports}>
          🔍 Actualizar
        </button>
      </div>

      {loading ? (
        <p>Cargando reportes...</p>
      ) : (
        <>
          {/* --- Reporte de ventas: stat tiles --- */}
          {sales && (
            <section className="report-section">
              <h2>Ventas ({sales.startDate} a {sales.endDate})</h2>
              <div className="stat-grid">
                <div className="stat-tile">
                  <span className="stat-label">Ingresos totales</span>
                  <span className="stat-value">{money(sales.totalRevenue)}</span>
                </div>
                <div className="stat-tile">
                  <span className="stat-label">Reservas confirmadas</span>
                  <span className="stat-value">{sales.totalConfirmedBookings}</span>
                </div>
                <div className="stat-tile">
                  <span className="stat-label">Valor promedio</span>
                  <span className="stat-value">{money(sales.averageBookingValue)}</span>
                </div>
                <div className="stat-tile">
                  <span className="stat-label">Descuentos otorgados</span>
                  <span className="stat-value">{money(sales.totalDiscountGiven)}</span>
                </div>
              </div>
            </section>
          )}

          {/* --- Paquetes más reservados --- */}
          <section className="report-section">
            <h2>Paquetes más reservados</h2>
            {packages.length === 0 ? (
              <p>Todavía no hay reservas para armar este ranking.</p>
            ) : (
              <div className="bar-list">
                {packages.map((pkg) => (
                  <BarRow
                    key={pkg.packageId}
                    label={pkg.packageName}
                    sublabel={pkg.destination}
                    value={pkg.bookingCount}
                    max={maxPackageBookings}
                    color="#1a73e8"
                  />
                ))}
              </div>
            )}
          </section>

          {/* --- Resumen de reservas por estado --- */}
          {summary && (
            <section className="report-section">
              <h2>Reservas por estado ({summary.totalBookings} en total)</h2>
              <div className="bar-list">
                {summary.byStatus.map((item) => {
                  const colors = STATUS_COLORS[item.status] || { text: '#666', label: item.status };
                  return (
                    <BarRow
                      key={item.status}
                      label={colors.label}
                      value={item.count}
                      max={maxStatusCount}
                      color={colors.text}
                    />
                  );
                })}
              </div>
            </section>
          )}

          {/* --- Efectividad de descuentos --- */}
          {discounts && (
            <section className="report-section">
              <h2>Efectividad de descuentos</h2>
              <div className="stat-grid">
                <div className="stat-tile">
                  <span className="stat-label">Reservas con descuento</span>
                  <span className="stat-value">
                    {discounts.bookingsWithDiscount} / {discounts.totalBookingsAnalyzed}
                  </span>
                </div>
                <div className="stat-tile">
                  <span className="stat-label">Total descontado</span>
                  <span className="stat-value">{money(discounts.totalDiscountGiven)}</span>
                </div>
              </div>

              {discounts.byType.length === 0 ? (
                <p>Todavía no se ha aplicado ningún descuento.</p>
              ) : (
                <div className="bar-list" style={{ marginTop: '1rem' }}>
                  {discounts.byType.map((item) => (
                    <BarRow
                      key={item.type}
                      label={DISCOUNT_TYPE_LABELS[item.type] || item.type}
                      sublabel={`${item.averagePercentage}% promedio`}
                      value={item.timesApplied}
                      max={maxDiscountUses}
                      color="#1a73e8"
                    />
                  ))}
                </div>
              )}
            </section>
          )}
        </>
      )}
    </div>
  );
}

export default Reports;
