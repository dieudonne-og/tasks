// Task board: renders columns, handles create/edit/complete, shows predictions.
if (!requireAuth()) { throw new Error('redirect'); }
document.getElementById('nav').innerHTML = renderNav('board');

const COLUMNS = [
  { key: 'TODO', label: 'To Do' },
  { key: 'IN_PROGRESS', label: 'In Progress' },
  { key: 'DONE', label: 'Done' },
  { key: 'CANCELLED', label: 'Cancelled' }
];
const COMPLEXITY_CLASS = { LOW: 'low', MEDIUM: 'medium', HIGH: 'high' };

let taskTypes = [];
let staff = [];
let tasks = [];

const canManage = Auth.isManager();
if (canManage) {
  document.getElementById('sub').textContent =
    'Full department board. Log work, record actual effort, and see AI deadline predictions.';
}

async function boot() {
  try {
    [taskTypes, staff] = await Promise.all([api('/task-types'), api('/staff')]);
    populateSelect('taskTypeId', taskTypes.map(t => ({ v: t.id, l: t.name })));
    populateSelect('assigneeId', staff.map(s => ({ v: s.id, l: s.fullName })));
    await loadBoard();
  } catch (ex) {
    document.getElementById('board').innerHTML = `<div class="card">${esc(ex.message)}</div>`;
  }
}

async function loadBoard() {
  tasks = await api('/tasks');
  renderBoard();
}

function renderBoard() {
  const board = document.getElementById('board');
  board.innerHTML = COLUMNS.map(col => {
    const items = tasks.filter(t => t.status === col.key);
    const cards = items.map(taskCard).join('') ||
      '<div class="muted" style="font-size:12px;padding:6px">No tasks</div>';
    return `<div class="column"><h3>${col.label}<span>${items.length}</span></h3>${cards}</div>`;
  }).join('');
}

function taskCard(t) {
  const cx = COMPLEXITY_CLASS[t.complexity] || 'low';
  let pred = '';
  if (t.predictedDurationDays != null) {
    const model = t.predictionModel === 'FALLBACK_AVERAGE' ? 'avg' :
      (t.predictionModel === 'RANDOM_FOREST' ? 'RF' : 'LR');
    pred = `<div class="pred">🔮 Predicted <strong>${fmtDays(t.predictedDurationDays)}</strong>
      (95% CI ${fmtDays(t.predictedLowerDays)}–${fmtDays(t.predictedUpperDays)})
      <span class="badge model">${model}</span></div>`;
  }
  const risk = t.atRisk ? `<span class="badge risk" title="${esc(t.riskReason || '')}">At risk</span>` : '';
  const due = t.dueDate ? `📅 ${t.dueDate}` : '';
  const actual = t.actualDurationDays != null ? `✅ actual ${t.actualDurationDays}d` : '';
  return `<div class="task-card ${t.atRisk ? 'risk' : ''}" onclick="openEdit(${t.id})">
      <div class="t-title">${esc(t.title)} ${risk}</div>
      <div class="t-meta">
        <span>${esc(t.taskTypeName)}</span>
        <span class="badge ${cx}">${t.complexity}</span>
        <span>👤 ${esc(t.assigneeName)}</span>
        ${due ? `<span>${due}</span>` : ''}
        ${actual ? `<span>${actual}</span>` : ''}
      </div>
      ${pred}
    </div>`;
}

function populateSelect(id, options) {
  document.getElementById(id).innerHTML =
    options.map(o => `<option value="${o.v}">${esc(o.l)}</option>`).join('');
}

// ---- Create / edit modal ----
const modal = document.getElementById('modal');
document.getElementById('newBtn').addEventListener('click', () => openCreate());

function openCreate() {
  document.getElementById('modalTitle').textContent = 'New Task';
  document.getElementById('taskForm').reset();
  document.getElementById('taskId').value = '';
  document.getElementById('complexity').value = 'MEDIUM';
  document.getElementById('predBox').style.display = 'none';
  document.getElementById('modalErr').textContent = '';
  document.getElementById('extraActions')?.remove();
  modal.classList.add('open');
}

function openEdit(id) {
  const t = tasks.find(x => x.id === id);
  if (!t) return;
  document.getElementById('modalTitle').textContent = 'Edit Task';
  document.getElementById('taskId').value = t.id;
  document.getElementById('title').value = t.title;
  document.getElementById('description').value = t.description || '';
  document.getElementById('taskTypeId').value = t.taskTypeId;
  document.getElementById('assigneeId').value = t.assigneeId;
  document.getElementById('complexity').value = t.complexity;
  document.getElementById('estimatedDurationDays').value = t.estimatedDurationDays || '';
  document.getElementById('startDate').value = t.startDate || '';
  document.getElementById('dueDate').value = t.dueDate || '';
  document.getElementById('modalErr').textContent = '';

  const pb = document.getElementById('predBox');
  if (t.predictedDurationDays != null) {
    pb.style.display = 'block';
    pb.innerHTML = `🔮 AI prediction: <strong>${fmtDays(t.predictedDurationDays)}</strong>
      (95% CI ${fmtDays(t.predictedLowerDays)}–${fmtDays(t.predictedUpperDays)}) via ${esc(t.predictionModel)}.
      ${t.atRisk ? '<br><span style="color:var(--danger)">⚠ ' + esc(t.riskReason) + '</span>' : ''}`;
  } else {
    pb.style.display = 'none';
  }
  renderStatusActions(t);
  modal.classList.add('open');
}

function renderStatusActions(t) {
  document.getElementById('extraActions')?.remove();
  const wrap = document.createElement('div');
  wrap.id = 'extraActions';
  wrap.style.cssText = 'display:flex;gap:8px;flex-wrap:wrap;border-top:1px solid var(--border);padding-top:14px;margin-top:16px';
  const buttons = [];
  if (t.status === 'TODO') buttons.push(`<button type="button" class="secondary small" onclick="setStatus(${t.id},'IN_PROGRESS')">Start</button>`);
  if (t.status === 'IN_PROGRESS' || t.status === 'TODO') {
    buttons.push(`<button type="button" class="small" onclick="openComplete(${t.id})">Complete…</button>`);
  }
  if (t.status !== 'CANCELLED' && t.status !== 'DONE') {
    buttons.push(`<button type="button" class="ghost small" onclick="setStatus(${t.id},'CANCELLED')">Cancel task</button>`);
  }
  if (canManage) buttons.push(`<button type="button" class="danger small" onclick="deleteTask(${t.id})">Delete</button>`);
  wrap.innerHTML = buttons.join('');
  document.getElementById('taskForm').appendChild(wrap);
}

function closeModal() { modal.classList.remove('open'); }

document.getElementById('taskForm').addEventListener('submit', async (e) => {
  e.preventDefault();
  const id = document.getElementById('taskId').value;
  const body = {
    title: document.getElementById('title').value.trim(),
    description: document.getElementById('description').value.trim(),
    taskTypeId: Number(document.getElementById('taskTypeId').value),
    assigneeId: Number(document.getElementById('assigneeId').value),
    complexity: document.getElementById('complexity').value,
    estimatedDurationDays: intOrNull('estimatedDurationDays'),
    startDate: dateOrNull('startDate'),
    dueDate: dateOrNull('dueDate')
  };
  try {
    if (id) await api('/tasks/' + id, { method: 'PUT', body });
    else await api('/tasks', { method: 'POST', body });
    closeModal();
    await loadBoard();
  } catch (ex) {
    document.getElementById('modalErr').textContent = ex.message;
  }
});

async function setStatus(id, status) {
  try {
    await api('/tasks/' + id + '/status', { method: 'PATCH', body: { status } });
    closeModal();
    await loadBoard();
  } catch (ex) { alert(ex.message); }
}

async function deleteTask(id) {
  if (!confirm('Delete this task permanently?')) return;
  try {
    await api('/tasks/' + id, { method: 'DELETE' });
    closeModal();
    await loadBoard();
  } catch (ex) { alert(ex.message); }
}

// ---- Complete modal ----
const completeModal = document.getElementById('completeModal');
function openComplete(id) {
  const t = tasks.find(x => x.id === id);
  closeModal();
  document.getElementById('completeTaskId').value = id;
  document.getElementById('completeTaskTitle').textContent = t ? t.title : '';
  document.getElementById('actualDurationDays').value = t && t.predictedDurationDays ? Math.round(t.predictedDurationDays) : '';
  document.getElementById('completedDate').value = new Date().toISOString().slice(0, 10);
  document.getElementById('completeErr').textContent = '';
  completeModal.classList.add('open');
}

document.getElementById('completeForm').addEventListener('submit', async (e) => {
  e.preventDefault();
  const id = document.getElementById('completeTaskId').value;
  try {
    await api('/tasks/' + id + '/complete', {
      method: 'PATCH',
      body: {
        actualDurationDays: intOrNull('actualDurationDays'),
        completedDate: dateOrNull('completedDate')
      }
    });
    completeModal.classList.remove('open');
    await loadBoard();
  } catch (ex) {
    document.getElementById('completeErr').textContent = ex.message;
  }
});

function intOrNull(id) { const v = document.getElementById(id).value; return v ? Number(v) : null; }
function dateOrNull(id) { const v = document.getElementById(id).value; return v || null; }

boot();
