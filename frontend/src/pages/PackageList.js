import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { getAvailablePackages, searchPackages } from '../services/api';

/**
 * PÁGINA: Lista de Paquetes Turísticos (Épica 3)
 * =================================================
 * Muestra todos los paquetes disponibles con filtros de búsqueda.
 *
 * useState = guarda datos que pueden cambiar (estado del componente)
 * useEffect = ejecuta código al cargar la página
 */
function PackageList() {
  // Estado: lista de paquetes cargados desde el backend
  const [packages, setPackages] = useState([]);
  // Estado: indica si está cargando datos
  const [loading, setLoading] = useState(true);
  // Estado: filtros de búsqueda
  const [filters, setFilters] = useState({
    destination: '',
    minPrice: '',
    maxPrice: '',
  });

  /**
   * useEffect con [] vacío = se ejecuta UNA vez al cargar la página.
   * Equivale a "cuando la página se abre, carga los paquetes".
   */
  useEffect(() => {
    loadPackages();
  }, []);

  /**
   * Cargar paquetes disponibles desde el backend.
   */
  const loadPackages = async () => {
    try {
      setLoading(true);
      const response = await getAvailablePackages();
      setPackages(response.data); // Guardar los paquetes en el estado
    } catch (error) {
      console.error('Error loading packages:', error);
    } finally {
      setLoading(false);
    }
  };

  /**
   * Buscar paquetes con los filtros aplicados.
   * Se ejecuta cuando el usuario presiona "Buscar".
   */
  const handleSearch = async () => {
    try {
      setLoading(true);
      // Enviar solo los filtros que tienen valor
      const activeFilters = {};
      if (filters.destination) activeFilters.destination = filters.destination;
      if (filters.minPrice) activeFilters.minPrice = filters.minPrice;
      if (filters.maxPrice) activeFilters.maxPrice = filters.maxPrice;

      const response = await searchPackages(activeFilters);
      setPackages(response.data);
    } catch (error) {
      console.error('Error searching packages:', error);
    } finally {
      setLoading(false);
    }
  };

  // Si está cargando, mostrar mensaje
  if (loading) return <p>Cargando paquetes...</p>;

  return (
    <div>
      <h1>🌍 Paquetes Turísticos Disponibles</h1>

      {/* Filtros de búsqueda */}
      <div className="search-filters">
        <div className="filter-group">
          <label>Destino</label>
          <input
            type="text"
            placeholder="Ej: Machu Picchu"
            value={filters.destination}
            onChange={(e) => setFilters({...filters, destination: e.target.value})}
          />
        </div>
        <div className="filter-group">
          <label>Precio mínimo</label>
          <input
            type="number"
            placeholder="500"
            value={filters.minPrice}
            onChange={(e) => setFilters({...filters, minPrice: e.target.value})}
          />
        </div>
        <div className="filter-group">
          <label>Precio máximo</label>
          <input
            type="number"
            placeholder="2000"
            value={filters.maxPrice}
            onChange={(e) => setFilters({...filters, maxPrice: e.target.value})}
          />
        </div>
        <button className="btn-search" onClick={handleSearch}>
          🔍 Buscar
        </button>
      </div>

      {/* Grid de tarjetas de paquetes */}
      <div className="packages-grid">
        {packages.length === 0 ? (
          <p>No se encontraron paquetes con esos filtros.</p>
        ) : (
          packages.map((pkg) => (
            <Link to={`/packages/${pkg.id}`} key={pkg.id} style={{textDecoration: 'none'}}>
              <div className="package-card">
                <div className="package-card-body">
                  <h3>{pkg.name}</h3>
                  <p className="package-destination">📍 {pkg.destination}</p>
                  <p className="package-price">
                    ${pkg.price} <span>por persona</span>
                  </p>
                  <p className="package-dates">
                    📅 {pkg.startDate} → {pkg.endDate}
                  </p>
                  <span className="package-slots">
                    🎟️ {pkg.availableSlots} cupos disponibles
                  </span>
                </div>
              </div>
            </Link>
          ))
        )}
      </div>
    </div>
  );
}

export default PackageList;
