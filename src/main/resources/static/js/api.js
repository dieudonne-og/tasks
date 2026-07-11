// Thin fetch wrapper that attaches the JWT and handles auth errors.
const API = (() => {
  const TOKEN_KEY = 'taskms_token';
  const USER_KEY = 'taskms_user';

  function token() { return localStorage.getItem(TOKEN_KEY); }
  function setSession(token, user) {
    localStorage.setItem(TOKEN_KEY, token);
    localStorage.setItem(USER_KEY, JSON.stringify(user));
  }
  function user() {
    const raw = localStorage.getItem(USER_KEY);
    return raw ? JSON.parse(raw) : null;
  }
  function clear() {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(USER_KEY);
  }

  async function request(method, path, body) {
    const headers = { 'Content-Type': 'application/json' };
    const t = token();
    if (t) headers['Authorization'] = 'Bearer ' + t;
    const res = await fetch(path, {
      method,
      headers,
      body: body ? JSON.stringify(body) : undefined,
    });
    if (res.status === 401) {
      clear();
      if (!location.pathname.endsWith('index.html') && location.pathname !== '/') {
        location.href = 'index.html';
      }
      throw new Error('Unauthorized');
    }
    if (!res.ok) {
      let message = res.statusText;
      try { const err = await res.json(); message = err.message || message; } catch (_) {}
      throw new Error(message);
    }
    if (res.status === 204) return null;
    return res.json();
  }

  return {
    token, user, setSession, clear,
    get: (p) => request('GET', p),
    post: (p, b) => request('POST', p, b),
    put: (p, b) => request('PUT', p, b),
    patch: (p, b) => request('PATCH', p, b),
    del: (p) => request('DELETE', p),
  };
})();
