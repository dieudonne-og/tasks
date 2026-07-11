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
      { href: 'board.html', label: 'Board', show: true },
      { href: 'dashboard.html', label: 'Dashboard', show: isManager(u) },
      { href: 'admin.html', label: 'Admin', show: isAdmin(u) },
    ];
    const nav = document.getElementById('nav');
    if (!nav) return;
    nav.innerHTML = `
      <div class="brand">TaskMS<span class="brand-dot">.</span></div>
      <div class="nav-links">
        ${links.filter(l => l.show).map(l =>
          `<a href="${l.href}" class="${active === l.href ? 'active' : ''}">${l.label}</a>`).join('')}
      </div>
      <div class="nav-user">
        <span class="user-name">${u.fullName}</span>
        <span class="role-badge">${u.role.replace('HR_', '')}</span>
        <button class="btn-ghost" id="logoutBtn">Logout</button>
      </div>`;
    document.getElementById('logoutBtn').addEventListener('click', logout);
  }

  return { requireLogin, logout, isManager, isAdmin, renderNav };
})();
