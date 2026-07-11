const user = Auth.requireLogin();
Auth.renderNav('admin.html');
if (!Auth.isAdmin(user)) location.href = 'board.html';

const userModal = document.getElementById('userModal');
const typeModal = document.getElementById('typeModal');

async function load() {
  renderUsers(await API.get('/api/users'));
  renderTypes(await API.get('/api/task-types'));
}

function renderUsers(users) {
  document.getElementById('users').innerHTML = `<table>
    <tr><th>Name</th><th>Email</th><th>Role</th><th>Status</th><th></th></tr>
    ${users.map(u => `<tr>
      <td>${esc(u.fullName)}</td><td>${esc(u.email)}</td>
      <td>${u.role.replace('HR_', '')}</td>
      <td>${u.active ? '<span class="tag-active">active</span>' : '<span class="tag-inactive">inactive</span>'}</td>
      <td><button class="btn-ghost btn-sm" data-uid="${u.id}">Edit</button></td>
    </tr>`).join('')}</table>`;
  document.querySelectorAll('[data-uid]').forEach(b =>
    b.addEventListener('click', () => editUser(users.find(u => u.id == b.dataset.uid))));
}

function renderTypes(types) {
  document.getElementById('types').innerHTML = `<table>
    <tr><th>Name</th><th>Description</th><th></th></tr>
    ${types.map(t => `<tr>
      <td>${esc(t.name)}</td><td class="muted">${esc(t.description || '')}</td>
      <td><button class="btn-ghost btn-sm" data-tid="${t.id}">Edit</button></td>
    </tr>`).join('')}</table>`;
  document.querySelectorAll('[data-tid]').forEach(b =>
    b.addEventListener('click', () => editType(types.find(t => t.id == b.dataset.tid))));
}

// ----- Users -----
function newUser() {
  document.getElementById('userForm').reset();
  document.getElementById('u_id').value = '';
  document.getElementById('userModalTitle').textContent = 'New user';
  document.getElementById('u_pwHint').textContent = '(required)';
  document.getElementById('userError').textContent = '';
  userModal.classList.add('open');
}

function editUser(u) {
  document.getElementById('u_id').value = u.id;
  document.getElementById('u_fullName').value = u.fullName;
  document.getElementById('u_email').value = u.email;
  document.getElementById('u_email').disabled = true;
  document.getElementById('u_password').value = '';
  document.getElementById('u_role').value = u.role;
  document.getElementById('u_active').value = String(u.active);
  document.getElementById('u_pwHint').textContent = '(leave blank to keep)';
  document.getElementById('userModalTitle').textContent = 'Edit user';
  document.getElementById('userError').textContent = '';
  userModal.classList.add('open');
}

document.getElementById('userForm').addEventListener('submit', async (e) => {
  e.preventDefault();
  const id = document.getElementById('u_id').value;
  const body = {
    fullName: document.getElementById('u_fullName').value,
    email: document.getElementById('u_email').value,
    password: document.getElementById('u_password').value || null,
    role: document.getElementById('u_role').value,
    active: document.getElementById('u_active').value === 'true',
  };
  try {
    if (id) await API.put('/api/users/' + id, body);
    else await API.post('/api/users', body);
    document.getElementById('u_email').disabled = false;
    userModal.classList.remove('open');
    await load();
  } catch (err) { document.getElementById('userError').textContent = err.message; }
});

// ----- Task types -----
function newType() {
  document.getElementById('typeForm').reset();
  document.getElementById('t_id').value = '';
  document.getElementById('typeModalTitle').textContent = 'New task type';
  document.getElementById('typeError').textContent = '';
  typeModal.classList.add('open');
}

function editType(t) {
  document.getElementById('t_id').value = t.id;
  document.getElementById('t_name').value = t.name;
  document.getElementById('t_description').value = t.description || '';
  document.getElementById('typeModalTitle').textContent = 'Edit task type';
  document.getElementById('typeError').textContent = '';
  typeModal.classList.add('open');
}

document.getElementById('typeForm').addEventListener('submit', async (e) => {
  e.preventDefault();
  const id = document.getElementById('t_id').value;
  const body = {
    name: document.getElementById('t_name').value,
    description: document.getElementById('t_description').value || null,
  };
  try {
    if (id) await API.put('/api/task-types/' + id, body);
    else await API.post('/api/task-types', body);
    typeModal.classList.remove('open');
    await load();
  } catch (err) { document.getElementById('typeError').textContent = err.message; }
});

document.getElementById('newUserBtn').addEventListener('click', newUser);
document.getElementById('newTypeBtn').addEventListener('click', newType);

function esc(s) {
  return (s || '').replace(/[&<>"']/g, c =>
    ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
}

load();
