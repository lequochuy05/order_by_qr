// src/main/resources/static/js/config.js
window.APP_BASE_URL = window.APP_BASE_URL || (location.origin.includes('localhost')
  ? 'http://localhost:8080'
  : location.origin.replace(/:\d+$/, '') + ':8080');


// window.APP_BASE_URL = window.APP_BASE_URL || (location.origin.includes('10.160.39.96')
//   ? 'http://10.160.39.96:8080'
//   : location.origin.replace(/:\d+$/, '') + ':8080');

