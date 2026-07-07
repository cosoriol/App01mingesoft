import React, { useState, useEffect } from 'react';
import { updateProfile } from '../services/api';

/**
 * PÁGINA: Mi Perfil (Épica 1)
 * =============================
 * Permite a cualquier usuario logueado editar sus propios datos de
 * contacto (nombre, teléfono, documento, nacionalidad). No permite
 * cambiar email, contraseña ni rol — eso necesita un flujo aparte
 * (ver UpdateProfileRequest en el backend para el porqué).
 */
function Profile({ currentUser, onProfileUpdated }) {
  const [form, setForm] = useState({
    fullName: '',
    phone: '',
    identityDocument: '',
    nationality: '',
  });
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState(false);

  /**
   * currentUser llega null en el primer render (App.js todavía no
   * termina de leerlo de localStorage) y recién después se actualiza.
   * Si el formulario se inicializara solo con useState(currentUser...),
   * quedaría con TODOS los campos vacíos para siempre — y al guardar,
   * borraría los datos reales del usuario. Por eso se sincroniza acá,
   * cada vez que currentUser cambia.
   */
  useEffect(() => {
    if (currentUser) {
      setForm({
        fullName: currentUser.fullName || '',
        phone: currentUser.phone || '',
        identityDocument: currentUser.identityDocument || '',
        nationality: currentUser.nationality || '',
      });
    }
  }, [currentUser]);

  if (!currentUser) {
    return (
      <div>
        <h1>👤 Mi Perfil</h1>
        <p>Debes iniciar sesión para ver tu perfil.</p>
      </div>
    );
  }

  const handleChange = (field) => (e) => {
    setForm({ ...form, [field]: e.target.value });
    setSuccess(false);
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setSuccess(false);
    setSaving(true);
    try {
      const response = await updateProfile(currentUser.id, currentUser.id, form);
      const updatedUser = response.data;
      // Actualizar la sesión (localStorage + estado de App) para que
      // la navbar y el resto de la app reflejen el nombre nuevo al toque.
      localStorage.setItem('currentUser', JSON.stringify(updatedUser));
      onProfileUpdated(updatedUser);
      setSuccess(true);
    } catch (err) {
      setError(err.response?.data?.error || 'No se pudo actualizar el perfil');
    } finally {
      setSaving(false);
    }
  };

  return (
    <div>
      <h1>👤 Mi Perfil</h1>

      <section className="report-section" style={{ maxWidth: '500px' }}>
        {error && <p className="login-error">{error}</p>}
        {success && (
          <p style={{ background: '#e8f5e9', color: '#2e7d32', padding: '0.6rem 0.75rem', borderRadius: '8px', fontSize: '0.85rem', marginBottom: '1rem' }}>
            Perfil actualizado correctamente.
          </p>
        )}

        <form onSubmit={handleSubmit}>
          <div className="filter-group" style={{ marginBottom: '1rem' }}>
            <label>Email (no editable)</label>
            <input value={currentUser.email} disabled />
          </div>
          <div className="filter-group" style={{ marginBottom: '1rem' }}>
            <label>Nombre completo</label>
            <input value={form.fullName} onChange={handleChange('fullName')} required />
          </div>
          <div className="filter-group" style={{ marginBottom: '1rem' }}>
            <label>Teléfono</label>
            <input value={form.phone} onChange={handleChange('phone')} />
          </div>
          <div className="filter-group" style={{ marginBottom: '1rem' }}>
            <label>Documento de identidad</label>
            <input value={form.identityDocument} onChange={handleChange('identityDocument')} />
          </div>
          <div className="filter-group" style={{ marginBottom: '1rem' }}>
            <label>Nacionalidad</label>
            <input value={form.nationality} onChange={handleChange('nationality')} />
          </div>
          <button className="btn-search" type="submit" disabled={saving}>
            {saving ? 'Guardando...' : '💾 Guardar cambios'}
          </button>
        </form>
      </section>
    </div>
  );
}

export default Profile;
