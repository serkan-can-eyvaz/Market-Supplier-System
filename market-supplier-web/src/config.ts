// API Configuration
export const API_CONFIG = {
  BASE_URL: process.env.REACT_APP_API_URL || 'http://localhost:8080/api',
  TIMEOUT: 10000,
};

// Mapbox Configuration
export const MAPBOX_CONFIG = {
  ACCESS_TOKEN: process.env.REACT_APP_MAPBOX_ACCESS_TOKEN || 'pk.eyJ1IjoibWFwYm94IiwiYSI6ImNpejY4NXVycTA2emYycXBndHRqcmZ3N3gifQ.rJcFIG214AriISLbB6B5aw',
  STYLE: 'mapbox://styles/mapbox/streets-v12',
  NAVIGATION_STYLE: 'mapbox://styles/mapbox/navigation-day-v1',
};

// App Configuration
export const APP_CONFIG = {
  NAME: 'Market Supplier System',
  VERSION: '1.0.0',
  DESCRIPTION: 'Market sahipleri ile tedarikçileri buluşturan sipariş ve teslimat yönetim sistemi',
};
