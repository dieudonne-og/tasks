// Shared auth/nav helpers used by every authenticated page.
const Auth = (() => {
  function requireLogin() {
    if (!API.token()) {
      location.href = 'index.html';
      return null;
    }
    return API.user();
  }

  function logout() {
    API.clear();
    location.href = 'index.html';
  }

  function isManager(u) { return u && (u.role === 'ADMIN' || u.role === 'HR_MANAGER'); }
  function isAdmin(u) { return u && u.role === 'ADMIN'; }

  // Renders the top navigation, hiding links the role cannot use.
  function renderNav(active) {
    const u = API.user();
    if (!u) return;
    const links = [
      { href: 'board.html', label: 'Board', icon: '🗂️', show: true },
      { href: 'dashboard.html', label: 'Dashboard', icon: '📊', show: true },
      { href: 'admin.html', label: 'Admin', icon: '⚙️', show: isAdmin(u) },
    ];
    const sidebar = document.getElementById('sidebar');
    if (sidebar) {
      sidebar.innerHTML = links.filter(l => l.show).map(l =>
        `<a href="${l.href}" class="side-link ${active === l.href ? 'active' : ''}">
           <span class="side-ic">${l.icon}</span><span>${l.label}</span></a>`).join('');
    }
    const nav = document.getElementById('nav');
    if (!nav) return;
    nav.innerHTML = `
      <div class="brand">TaskMS<span class="brand-dot">.</span></div>
      <div class="nav-user">
        <div class="notif" id="notif">
          <button class="notif-bell" id="notifBell" title="Notifications">🔔<span class="notif-badge" id="notifBadge" style="display:none;">0</span></button>
          <div class="notif-panel" id="notifPanel" style="display:none;">
            <div class="notif-head">
              <b>Notifications</b>
              <button class="btn-ghost" id="notifReadAll">Mark all read</button>
            </div>
            <div class="notif-list" id="notifList"></div>
          </div>
        </div>
        <span class="user-name">${u.fullName}</span>
        <span class="role-badge">${u.role.replace('HR_', '')}</span>
        <button class="btn-ghost" id="logoutBtn">Logout</button>
      </div>`;
    document.getElementById('logoutBtn').addEventListener('click', logout);
    Notif.mount();
  }

  // In-app notifications: bell badge + panel, polled every 30s.
  const Notif = (() => {
    let timer = null;

    function icon(type) {
      return ({ PREDICTION: '🤖', AT_RISK: '⚠️', DEADLINE_APPROACHING: '⏰',
                DEADLINE_TODAY: '🔴' })[type] || '🔔';
    }

    function esc(s) {
      return (s || '').replace(/[&<>"']/g, c =>
        ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
    }

    async function refresh() {
      const badge = document.getElementById('notifBadge');
      if (!badge) return;
      try {
        const { count } = await API.get('/api/notifications/unread-count');
        badge.textContent = count;
        badge.style.display = count > 0 ? 'inline-flex' : 'none';
      } catch (_) {}
    }

    async function openPanel() {
      const panel = document.getElementById('notifPanel');
      const list = document.getElementById('notifList');
      const items = await API.get('/api/notifications');
      list.innerHTML = items.length
        ? items.map(n => `<div class="notif-item ${n.read ? '' : 'unread'}">
             <span class="notif-ic">${icon(n.type)}</span>
             <span class="notif-msg">${esc(n.message)}</span>
           </div>`).join('')
        : '<div class="notif-empty">No notifications</div>';
      panel.style.display = 'block';
    }

    function mount() {
      const bell = document.getElementById('notifBell');
      if (!bell) return;
      const panel = document.getElementById('notifPanel');
      bell.addEventListener('click', async (e) => {
        e.stopPropagation();
        if (panel.style.display === 'block') { panel.style.display = 'none'; return; }
        await openPanel();
      });
      document.getElementById('notifReadAll').addEventListener('click', async (e) => {
        e.stopPropagation();
        await API.post('/api/notifications/read-all');
        await openPanel();
        await refresh();
      });
      document.addEventListener('click', () => { panel.style.display = 'none'; });
      panel.addEventListener('click', (e) => e.stopPropagation());
      refresh();
      if (timer) clearInterval(timer);
      timer = setInterval(refresh, 30000);
    }

    return { mount, refresh };
  })();

  return { requireLogin, logout, isManager, isAdmin, renderNav };
})();
