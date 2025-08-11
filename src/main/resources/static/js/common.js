// resource/static/js/common.js
async function readErr(res) {
  const ct = (res.headers.get('content-type') || '').toLowerCase();
  const raw = await res.text();
  if (!raw) return `${res.status} ${res.statusText}`;

  if (ct.includes('application/json') || ct.includes('application/problem+json')) {
    try {
      const j = JSON.parse(raw);
      // Ưu tiên các field chuẩn của Spring ProblemDetail
      return j.detail || j.message || j.error || j.title || raw;
    } catch {
      return raw;
    }
  }
  return raw;
}