// resource/static/admin/js/auth-fetch.js

window.authFetch = async function(url, options = {}) {
  const token = localStorage.getItem('accessToken');
  const headers = new Headers(options.headers || {});
  if (token) headers.set('Authorization', 'Bearer ' + token);

  // CHỈ set application/json khi KHÔNG phải FormData
  const isForm = (options.body && typeof FormData !== 'undefined' && options.body instanceof FormData);
  if (!headers.has('Content-Type') && options.body && !isForm) {
    headers.set('Content-Type', 'application/json');
  }
  return fetch(url, { ...options, headers });
};
