import React, { useState, useEffect, useCallback } from 'react';
import { getAllPackages, createPackage, updatePackage, changePackageStatus, getErrorMessage } from '../services/api';

/**
 * PÁGINA: Administración de Paquetes (Épica 2)
 * ================================================
 * Panel básico para que un ADMIN cree y edite paquetes turísticos, y
 * cancele/reactive los existentes. Solo visible para rol ADMIN
 * (el backend también lo valida — ver TravelPackageService.requireAdmin).
 *
 * El mismo formulario sirve para crear y editar: si `editingId` tiene
 * un valor, el submit llama a updatePackage en vez de createPackage.
 */
const STATUS_LABELS = {
  AVAILABLE: { text: 'Disponible', className: 'status-confirmed' },
  SOLD_OUT: { text: 'Agotado', className: 'status-pending' },
  EXPIRED: { text: 'Expirado', className: 'status-expired' },
  CANCELLED: { text: 'Cancelado', className: 'status-cancelled' },
};

const EMPTY_FORM = {
  name: '',
  destination: '',
  description: '',
  startDate: '',
  endDate: '',
  price: '',
  totalSlots: '',
  includedServices: '',
  restrictions: '',
  travelType: '',
  season: '',
};

function AdminPackages({ currentUser }) {
  const [packages, setPackages] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [formError, setFormError] = useState('');
  const [form, setForm] = useState(EMPTY_FORM);
  const [saving, setSaving] = useState(false);
  const [editingId, setEditingId] = useState(null);

  const loadPackages = useCallback(async () => {
    try {
      setLoading(true);
      const response = await getAllPackages(currentUser.id);
      setPackages(response.data);
    } catch (err) {
      setError('No se pudieron cargar los paquetes');
    } finally {
      setLoading(false);
    }
  }, [currentUser]);

  useEffect(() => {
    if (currentUser?.role === 'ADMIN') {
      loadPackages();
    }
  }, [currentUser, loadPackages]);

  const handleFieldChange = (field) => (e) => {
    setForm({ ...form, [field]: e.target.value });
  };

  const handleEditClick = (pkg) => {
    setEditingId(pkg.id);
    setFormError('');
    setForm({
      name: pkg.name,
      destination: pkg.destination,
      description: pkg.description,
      startDate: pkg.startDate,
      endDate: pkg.endDate,
      price: pkg.price,
      totalSlots: pkg.totalSlots,
      includedServices: pkg.includedServices || '',
      restrictions: pkg.restrictions || '',
      travelType: pkg.travelType || '',
      season: pkg.season || '',
    });
    window.scrollTo({ top: 0, behavior: 'smooth' });
  };

  const handleCancelEdit = () => {
    setEditingId(null);
    setForm(EMPTY_FORM);
    setFormError('');
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setFormError('');
    setSaving(true);
    const payload = {
      ...form,
      price: Number(form.price),
      totalSlots: Number(form.totalSlots),
    };
    try {
      if (editingId) {
        await updatePackage(editingId, currentUser.id, payload);
      } else {
        await createPackage(currentUser.id, payload);
      }
      setForm(EMPTY_FORM);
      setEditingId(null);
      loadPackages();
    } catch (err) {
      setFormError(getErrorMessage(err, 'No se pudo guardar el paquete'));
    } finally {
      setSaving(false);
    }
  };

  const handleToggleStatus = async (pkg) => {
    const newStatus = pkg.status === 'CANCELLED' ? 'AVAILABLE' : 'CANCELLED';
    try {
      await changePackageStatus(pkg.id, newStatus, currentUser.id);
      loadPackages();
    } catch (err) {
      setError(getErrorMessage(err, 'No se pudo cambiar el estado del paquete'));
    }
  };

  if (!currentUser || currentUser.role !== 'ADMIN') {
    return (
      <div>
        <h1>🧭 Administrar Paquetes</h1>
        <p>Esta sección es solo para administradores.</p>
      </div>
    );
  }

  return (
    <div>
      <h1>🧭 Administrar Paquetes</h1>

      {error && <p className="login-error">{error}</p>}

      {/* --- Formulario de creación / edición --- */}
      <section className="report-section">
        <h2>{editingId ? `Editar paquete #${editingId}` : 'Crear nuevo paquete'}</h2>
        {formError && <p className="login-error">{formError}</p>}
        <form onSubmit={handleSubmit} className="admin-form">
          <div className="admin-form-grid">
            <div className="filter-group">
              <label>Nombre</label>
              <input value={form.name} onChange={handleFieldChange('name')} required />
            </div>
            <div className="filter-group">
              <label>Destino</label>
              <input value={form.destination} onChange={handleFieldChange('destination')} required />
            </div>
            <div className="filter-group">
              <label>Fecha inicio</label>
              <input type="date" value={form.startDate} onChange={handleFieldChange('startDate')} required />
            </div>
            <div className="filter-group">
              <label>Fecha término</label>
              <input type="date" value={form.endDate} onChange={handleFieldChange('endDate')} required />
            </div>
            <div className="filter-group">
              <label>Precio (por persona)</label>
              <input type="number" min="0" step="0.01" value={form.price} onChange={handleFieldChange('price')} required />
            </div>
            <div className="filter-group">
              <label>Cupos totales</label>
              <input type="number" min="1" value={form.totalSlots} onChange={handleFieldChange('totalSlots')} required />
            </div>
            <div className="filter-group">
              <label>Tipo de viaje</label>
              <input placeholder="Aventura, Playa, Cultural..." value={form.travelType} onChange={handleFieldChange('travelType')} />
            </div>
            <div className="filter-group">
              <label>Temporada</label>
              <input placeholder="Alta, Media, Baja" value={form.season} onChange={handleFieldChange('season')} />
            </div>
          </div>
          <div className="filter-group" style={{ marginTop: '1rem' }}>
            <label>Descripción</label>
            <textarea value={form.description} onChange={handleFieldChange('description')} rows={3} required />
          </div>
          <div className="admin-form-grid" style={{ marginTop: '1rem' }}>
            <div className="filter-group">
              <label>Servicios incluidos (opcional)</label>
              <input value={form.includedServices} onChange={handleFieldChange('includedServices')} />
            </div>
            <div className="filter-group">
              <label>Restricciones (opcional)</label>
              <input value={form.restrictions} onChange={handleFieldChange('restrictions')} />
            </div>
          </div>
          <div style={{ display: 'flex', gap: '0.75rem', marginTop: '1rem' }}>
            <button className="btn-search" type="submit" disabled={saving}>
              {saving ? 'Guardando...' : editingId ? '💾 Guardar cambios' : '➕ Crear paquete'}
            </button>
            {editingId && (
              <button type="button" className="btn-logout" onClick={handleCancelEdit}>
                Cancelar edición
              </button>
            )}
          </div>
        </form>
      </section>

      {/* --- Listado de paquetes --- */}
      <section className="report-section">
        <h2>Todos los paquetes ({packages.length})</h2>
        {loading ? (
          <p>Cargando paquetes...</p>
        ) : (
          <div className="bookings-list">
            {packages.map((pkg) => {
              const statusInfo = STATUS_LABELS[pkg.status] || { text: pkg.status, className: '' };
              return (
                <div className="booking-card" key={pkg.id}>
                  <div className="booking-card-header">
                    <h3>{pkg.name}</h3>
                    <span className={`booking-status ${statusInfo.className}`}>{statusInfo.text}</span>
                  </div>
                  <p className="package-destination">📍 {pkg.destination}</p>
                  <p>💰 ${pkg.price} por persona · 🎟️ {pkg.availableSlots}/{pkg.totalSlots} cupos disponibles</p>
                  {(pkg.travelType || pkg.season) && (
                    <p style={{ fontSize: '0.85rem', color: '#666' }}>
                      {pkg.travelType && `Tipo: ${pkg.travelType}`}
                      {pkg.travelType && pkg.season && ' · '}
                      {pkg.season && `Temporada: ${pkg.season}`}
                    </p>
                  )}
                  <div style={{ display: 'flex', gap: '0.5rem', marginTop: '0.75rem' }}>
                    <button className="btn-search" onClick={() => handleEditClick(pkg)}>
                      ✏️ Editar
                    </button>
                    <button
                      className={pkg.status === 'CANCELLED' ? 'btn-search' : 'btn-logout'}
                      onClick={() => handleToggleStatus(pkg)}
                    >
                      {pkg.status === 'CANCELLED' ? '✅ Reactivar' : '🚫 Cancelar paquete'}
                    </button>
                  </div>
                </div>
              );
            })}
          </div>
        )}
      </section>
    </div>
  );
}

export default AdminPackages;
