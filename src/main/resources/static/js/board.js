const user = Auth.requireLogin();
Auth.renderNav('board.html');

const COLUMNS = [
  { key: 'TODO', label: 'To Do' },
  { key: 'IN_PROGRESS', label: 'In Progress' },
  { key: 'DONE', label: 'Done' },
  { key: 'CANCELLED', label: 'Cancelled' },
];

const modal = document.getElementById('modal');
let taskTypes = [];
let users = [];
let tasks = [];

async function load() {
  taskTypes = await API.get('/api/task-types');
  users = Auth.isManager(user) ? await loadUsers() : [{ id: user.userId, fullName: user.fullName }];
  tasks = await API.get('/api/tasks');
  render();
  fillSelects();
}

async function loadUsers() {
  // Managers can assign to any HR officer; admin endpoint is admin-only, so managers
  // fall back to the assignees already present plus themselves.
  if (Auth.isAdmin(user)) {
    try { return await API.get('/api/users'); } catch (_) {}
  }
  const map = new Map();
  map.set(user.userId, { id: user.userId, fullName: user.fullName });
  tasks.forEach(t => { if (t.assigneeId) map.set(t.assigneeId, { id: t.assigneeId, fullName: t.assigneeName }); });
  return [...map.values()];
}

function render() {
  const board = document.getElementById('board');
  board.innerHTML = COLUMNS.map(col => {
    const items = tasks.filter(t => t.status === col.key);
    return `<div class="column">
      <h3>${col.label} <span class="count">${items.length}</span></h3>
      ${items.map(cardHtml).join('')}
    </div>`;
  }).join('');
  board.querySelectorAll('.card').forEach(c =>
    c.addEventListener('click', () => openEdit(Number(c.dataset.id))));
}

function cardHtml(t) {
  const cx = t.complexity.toLowerCase();
  const predict = t.predictedDurationDays != null
    ? `<div class="predict">
         <b>Predicted:</b> ${t.predictedDurationDays}d
         <span class="muted">(CI ${t.predictedLowerDays}–${t.predictedUpperDays}d · ${modelLabel(t.predictionModel)})</span>
         ${t.atRisk ? '<div class="risk-flag">⚠ At risk of missing deadline</div>' : ''}
       </div>` : '';
  return `<div class="card ${t.atRisk ? 'risk' : ''}" data-id="${t.id}">
    <div class="card-title">${escapeHtml(t.title)}</div>
    <div class="card-meta">
      <span class="pill pill-type">${escapeHtml(t.taskTypeName)}</span>
      <span class="pill pill-${cx}">${t.complexity}</span>
      <span>👤 ${escapeHtml(t.assigneeName || '')}</span>
      ${t.dueDate ? `<span>📅 ${t.dueDate}</span>` : ''}
      ${t.actualDurationDays != null ? `<span>✔ ${t.actualDurationDays}d actual</span>` : ''}
    </div>
    ${predict}
  </div>`;
}

function modelLabel(m) {
  return ({ LINEAR_REGRESSION: 'Linear Regression', RANDOM_FOREST: 'Random Forest',
            CATEGORY_AVERAGE: 'Category average' })[m] || m;
}

function fillSelects() {
  document.getElementById('f_taskTypeId').innerHTML =
    taskTypes.map(t => `<option value="${t.id}">${escapeHtml(t.name)}</option>`).join('');
  document.getElementById('f_assigneeId').innerHTML =
    users.map(u => `<option value="${u.id}">${escapeHtml(u.fullName)}</option>`).join('');
}

function openNew() {
  document.getElementById('taskForm').reset();
  document.getElementById('taskId').value = '';
  document.getElementById('modalTitle').textContent = 'New task';
  document.getElementById('completeSection').style.display = 'none';
  document.getElementById('predictBox').style.display = 'none';
  document.getElementById('formError').textContent = '';
  modal.classList.add('open');
}

function openEdit(id) {
  const t = tasks.find(x => x.id === id);
  if (!t) return;
  const f = document.getElementById('taskForm');
  f.reset();
  document.getElementById('taskId').value = t.id;
  document.getElementById('modalTitle').textContent = 'Edit task';
  document.getElementById('f_title').value = t.title;
  document.getElementById('f_description').value = t.description || '';
  document.getElementById('f_taskTypeId').value = t.taskTypeId;
  document.getElementById('f_assigneeId').value = t.assigneeId;
  document.getElementById('f_complexity').value = t.complexity;
  document.getElementById('f_estimatedDurationDays').value = t.estimatedDurationDays ?? '';
  document.getElementById('f_startDate').value = t.startDate || '';
  document.getElementById('f_dueDate').value = t.dueDate || '';
  const pb = document.getElementById('predictBox');
  if (t.predictedDurationDays != null) {
    pb.style.display = 'block';
    pb.innerHTML = `<b>AI prediction:</b> ${t.predictedDurationDays} days
      (95% CI ${t.predictedLowerDays}–${t.predictedUpperDays}d, ${modelLabel(t.predictionModel)})
      ${t.atRisk ? '<div class="risk-flag">⚠ At risk of missing deadline</div>' : ''}`;
  } else pb.style.display = 'none';
  document.getElementById('completeSection').style.display =
    (t.status === 'DONE' || t.status === 'CANCELLED') ? 'none' : 'block';
  document.getElementById('formError').textContent = '';
  modal.classList.add('open');
}

function formBody() {
  const num = (id) => { const v = document.getElementById(id).value; return v === '' ? null : Number(v); };
  const str = (id) => { const v = document.getElementById(id).value; return v === '' ? null : v; };
  return {
    title: document.getElementById('f_title').value,
    description: str('f_description'),
    taskTypeId: Number(document.getElementById('f_taskTypeId').value),
    assigneeId: Number(document.getElementById('f_assigneeId').value),
    complexity: document.getElementById('f_complexity').value,
    estimatedDurationDays: num('f_estimatedDurationDays'),
    startDate: str('f_startDate'),
    dueDate: str('f_dueDate'),
  };
}

document.getElementById('taskForm').addEventListener('submit', async (e) => {
  e.preventDefault();
  const id = document.getElementById('taskId').value;
  try {
    if (id) await API.put('/api/tasks/' + id, formBody());
    else await API.post('/api/tasks', formBody());
    modal.classList.remove('open');
    await load();
  } catch (err) {
    document.getElementById('formError').textContent = err.message;
  }
});

document.getElementById('completeBtn').addEventListener('click', async () => {
  const id = document.getElementById('taskId').value;
  const actual = document.getElementById('f_actual').value;
  if (!actual) { document.getElementById('formError').textContent = 'Enter actual effort'; return; }
  try {
    await API.patch('/api/tasks/' + id + '/complete', {
      actualDurationDays: Number(actual),
      completedDate: document.getElementById('f_completedDate').value || null,
    });
    modal.classList.remove('open');
    await load();
  } catch (err) { document.getElementById('formError').textContent = err.message; }
});

document.getElementById('startBtn').addEventListener('click', async () => {
  const id = document.getElementById('taskId').value;
  try {
    await API.patch('/api/tasks/' + id + '/status', { status: 'IN_PROGRESS' });
    modal.classList.remove('open');
    await load();
  } catch (err) { document.getElementById('formError').textContent = err.message; }
});

document.getElementById('newTaskBtn').addEventListener('click', openNew);
document.getElementById('cancelBtn').addEventListener('click', () => modal.classList.remove('open'));
modal.addEventListener('click', (e) => { if (e.target === modal) modal.classList.remove('open'); });

function escapeHtml(s) {
  return (s || '').replace(/[&<>"']/g, c =>
    ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
}

load();
