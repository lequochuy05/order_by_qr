// src/main/resources/static/js/config.js
window.APP_BASE_URL = window.APP_BASE_URL || (location.origin.includes('localhost')
  ? 'http://localhost:8080'
  : location.origin.replace(/:\d+$/, '') + ':8080');

