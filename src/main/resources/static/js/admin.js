// Administration: users, task types, and model retraining.
if (!requireAuth()) { throw new Error('redirect'); }
if (!Auth.isAdmin()) { location.href = '/board'; }
document.getElementById('nav').innerHTML = renderNav('admin');

const ROLE_LABEL = { ADMIN: 'Administrator', HR_MANAGER: 'HR Manager', HR_OFFICER: 'HR Officer' };

async function boot() {
  await Promise.all([loadModel(), loadUsers(), loadTypes()]);
}

// ---- Model ----
async function loadModel() {
  try {
    const m = await api('/models/metrics');
    const latest = m.latest;
    const history = (m.history || []).map(h =>
      `<tr><td>${modelName(h.modelType)} ${h.active ? '<span class="badge model">active</span>' : ''}</td>
       <td>${h.mae}</td><td>${h.rmse}</td><td>${h.r2}</td><td>${h.sampleSize}</td></tr>`).join('');
    document.getElementById('modelBox').innerHTML = `
      <p>Active model: <strong>${modelName(m.activeModel)}</strong>
        ${m.trained ? '' : '<span class="muted">(category-average fallback — needs more completed tasks)</span>'}</p>
      ${history ? `<div class="table-wrap"><table>
        <tr><th>Model</th><th>MAE (d)</th><th>RMSE (d)</th><th>R²</th><th>Samples</th></tr>${history}</table></div>` : ''}`;
  } catch (ex) {
    document.getElementById('modelBox').innerHTML = `<span class="error-text">${esc(ex.message)}</span>`;
  }
}

document.getElementById('retrainBtn').addEventListener('click', async (e) => {
  e.target.disabled = true;
  e.target.textContent = 'Retraining…';
  try {
    await api('/models/retrain', { method: 'POST' });
    await loadModel();
  } catch (ex) { alert(ex.message); }
  e.target.disabled = false;
  e.target.textContent = 'Retrain now';
});

function modelName(t) {
  return { LINEAR: 'Linear Regression', RANDOM_FOREST: 'Random Forest', FALLBACK_AVERAGE: 'Category Average' }[t] || t;
}

// ---- Users ----
async function loadUsers() {
  const users = await api('/users');
  const rows = users.map(u => `
    <tr>
      <td>${esc(u.fullName)}</td>
      <td>${esc(u.email)}</td>
      <td>${ROLE_LABEL[u.role] || u.role}</td>
      <td>${u.active ? '<span class="badge ok">active</span>' : '<span class="badge low">inactive</span>'}</td>
      <td class="right">
        <button class="ghost small" onclick='editUser(${JSON.stringify(u)})'>Edit</button>
        ${u.active ? `<button class="ghost small" onclick="deactivateUser(${u.id})">Deactivate</button>` : ''}
      </td>
    </tr>`).join('');
  document.getElementById('usersTable').innerHTML =
    `<tr><th>Name</th><th>Email</th><th>Role</th><th>Status</th><th></th></tr>${rows}`;
}

const userModal = document.getElementById('userModal');
document.getElementById('newUserBtn').addEventListener('click', () => {
  document.getElementById('userForm').reset();
  document.getElementById('userId').value = '';
  document.getElementById('userModalTitle').textContent = 'Add user';
  document.getElementById('upassword').required = true;
  document.getElementById('userErr').textContent = '';
  userModal.classList.add('open');
});

function editUser(u) {
  document.getElementById('userId').value = u.id;
  document.getElementById('fullName').value = u.fullName;
  document.getElementById('uemail').value = u.email;
  document.getElementById('role').value = u.role;
  document.getElementById('upassword').value = '';
  document.getElementById('upassword').required = false;
  document.getElementById('userModalTitle').textContent = 'Edit user';
  document.getElementById('userErr').textContent = '';
  userModal.classList.add('open');
}

document.getElementById('userForm').addEventListener('submit', async (e) => {
  e.preventDefault();
  const id = document.getElementById('userId').value;
  const body = {
    fullName: document.getElementById('fullName').value.trim(),
    email: document.getElementById('uemail').value.trim(),
    role: document.getElementById('role').value
  };
  const pw = document.getElementById('upassword').value;
  if (pw) body.password = pw;
  try {
    if (id) await api('/users/' + id, { method: 'PUT', body });
    else await api('/users', { method: 'POST', body });
    userModal.classList.remove('open');
    await loadUsers();
  } catch (ex) {
    document.getElementById('userErr').textContent = ex.message;
  }
});

async function deactivateUser(id) {
  if (!confirm('Deactivate this user?')) return;
  try { await api('/users/' + id, { method: 'DELETE' }); await loadUsers(); }
  catch (ex) { alert(ex.message); }
}

// ---- Task types ----
async function loadTypes() {
  const types = await api('/task-types');
  const rows = types.map(t => `
    <tr>
      <td>${esc(t.name)}</td>
      <td class="muted">${esc(t.description || '')}</td>
      <td class="right">
        <button class="ghost small" onclick='editType(${JSON.stringify(t)})'>Edit</button>
        <button class="ghost small" onclick="deleteType(${t.id})">Delete</button>
      </td>
    </tr>`).join('');
  document.getElementById('typesTable').innerHTML =
    `<tr><th>Name</th><th>Description</th><th></th></tr>${rows}`;
}

const typeModal = document.getElementById('typeModal');
document.getElementById('newTypeBtn').addEventListener('click', () => {
  document.getElementById('typeForm').reset();
  document.getElementById('typeId').value = '';
  document.getElementById('typeModalTitle').textContent = 'Add task type';
  document.getElementById('typeErr').textContent = '';
  typeModal.classList.add('open');
});

function editType(t) {
  document.getElementById('typeId').value = t.id;
  document.getElementById('typeName').value = t.name;
  document.getElementById('typeDesc').value = t.description || '';
  document.getElementById('typeModalTitle').textContent = 'Edit task type';
  document.getElementById('typeErr').textContent = '';
  typeModal.classList.add('open');
}

document.getElementById('typeForm').addEventListener('submit', async (e) => {
  e.preventDefault();
  const id = document.getElementById('typeId').value;
  const body = {
    name: document.getElementById('typeName').value.trim(),
    description: document.getElementById('typeDesc').value.trim()
  };
  try {
    if (id) await api('/task-types/' + id, { method: 'PUT', body });
    else await api('/task-types', { method: 'POST', body });
    typeModal.classList.remove('open');
    await loadTypes();
  } catch (ex) {
    document.getElementById('typeErr').textContent = ex.message;
  }
});

async function deleteType(id) {
  if (!confirm('Delete this task type?')) return;
  try { await api('/task-types/' + id, { method: 'DELETE' }); await loadTypes(); }
  catch (ex) { alert(ex.message); }
}

boot();
