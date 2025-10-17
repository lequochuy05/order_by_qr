// src/main/resources/static/js/config.js
// window.APP_BASE_URL = window.APP_BASE_URL || (location.origin.includes('localhost')
//   ? 'http://localhost:8080'
//   : location.origin.replace(/:\d+$/, '') + ':8080');

window.APP_BASE_URL = window.APP_BASE_URL || (location.origin.includes('orderbyqr-production.up.railway.app')
  ? 'https://orderbyqr-production.up.railway.app'
  : location.origin.replace(/:\d+$/, '') + ':8080');