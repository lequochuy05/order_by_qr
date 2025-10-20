// src/main/resources/static/admin/js/config.js  
window.APP_BASE_URL = window.APP_BASE_URL || (
  location.hostname.includes('vercel.app')
  ? 'https://order-by-qr.onrender.com'
  : 'http://localhost:8080'
);