// src/main/resources/static/js/config.js
window.APP_BASE_URL = window.APP_BASE_URL || (location.origin.includes('192.168.1.8')
  ? 'http://192.168.1.8:8080'
  : location.origin.replace(/:\d+$/, '') + ':8080');
