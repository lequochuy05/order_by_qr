// src/main/resources/static/admin/js/config.js
// window.APP_BASE_URL = window.APP_BASE_URL || (location.origin.includes('10.50.252.193')
//   ? 'http://10.50.252.193:8080'
//   : location.origin.replace(/:\d+$/, '') + ':8080');
  
window.APP_BASE_URL = window.APP_BASE_URL || (location.origin.includes('orderbyqr-production.up.railway.app')
  ? 'https://orderbyqr-production.up.railway.app'
  : location.origin.replace(/:\d+$/, '') + ':8080');