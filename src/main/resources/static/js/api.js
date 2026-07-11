// Shared client helpers: auth token storage, fetch wrapper, nav rendering.
const TOKEN_KEY = 'taskms_token';
const USER_KEY = 'taskms_user';

const Auth = {
  get token() { return localStorage.getItem(TOKEN_KEY); },
  get user() {
    const raw = localStorage.getItem(USER_KEY);
    return raw ? JSON.parse(raw) : null;
  },
  save(token, user) {
    localStorage.setItem(TOKEN_KEY, token);
    localStorage.setItem(USER_KEY, JSON.stringify(user));
  },
  clear() {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(USER_KEY);
  },
  isManager() {
    const u = this.user;
    return u && (u.role === 'ADMIN' || u.role === 'HR_MANAGER');
  },
  isAdmin() {
    const u = this.user;
    return u && u.role === 'ADMIN';
  }
};

async function api(path, options = {}) {
  const opts = Object.assign({ headers: {} }, options);
  opts.headers['Content-Type'] = 'application/json';
  if (Auth.token) opts.headers['Authorization'] = 'Bearer ' + Auth.token;
  if (opts.body && typeof opts.body !== 'string') opts.body = JSON.stringify(opts.body);

  const res = await fetch('/api' + path, opts);
  if (res.status === 401) {
    Auth.clear();
    if (!location.pathname.endsWith('/login') && location.pathname !== '/') location.href = '/login';
    throw new Error('Session expired. Please log in again.');
  }
  if (res.status === 204) return null;
  const data = await res.json().catch(() => ({}));
  if (!res.ok) {
    throw new Error(data.message || ('Request failed (' + res.status + ')'));
  }
  return data;
}

// Redirect to login if not authenticated; otherwise render the shared nav bar.
function requireAuth() {
  if (!Auth.token || !Auth.user) {
    location.href = '/login';
    return false;
  }
  return true;
}

function renderNav(active) {
  const u = Auth.user;
  const manager = Auth.isManager();
  const admin = Auth.isAdmin();
  const links = [
    { href: '/board', label: 'Task Board', key: 'board', show: true },
    { href: '/dashboard', label: 'Dashboard', key: 'dashboard', show: manager },
    { href: '/admin', label: 'Administration', key: 'admin', show: admin }
  ];
  const nav = links.filter(l => l.show)
    .map(l => `<a href="${l.href}" class="${l.key === active ? 'active' : ''}">${l.label}</a>`)
    .join('');
  const roleLabel = { ADMIN: 'Administrator', HR_MANAGER: 'HR Manager', HR_OFFICER: 'HR Officer' }[u.role] || u.role;
  return `
    <div class="topbar">
      <div class="brand">HR Task Management<small>AI-Based Deadline Prediction &middot; University of Kigali</small></div>
      <nav>${nav}</nav>
      <div class="spacer"></div>
      <div class="userchip"><strong>${u.fullName}</strong>${roleLabel}</div>
      <button class="ghost small" onclick="logout()">Log out</button>
    </div>`;
}

function logout() {
  Auth.clear();
  location.href = '/login';
}

function esc(s) {
  if (s === null || s === undefined) return '';
  return String(s).replace(/[&<>"']/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
}

function fmtDays(n) {
  if (n === null || n === undefined) return '—';
  return (Math.round(n * 10) / 10) + 'd';
}
