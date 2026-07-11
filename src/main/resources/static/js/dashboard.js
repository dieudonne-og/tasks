const user = Auth.requireLogin();
Auth.renderNav('dashboard.html');
if (!Auth.isManager(user)) location.href = 'board.html';

const MODEL_LABEL = { LINEAR_REGRESSION: 'Linear Regression', RANDOM_FOREST: 'Random Forest',
  CATEGORY_AVERAGE: 'Category average' };

async function load() {
  const d = await API.get('/api/dashboard');
  renderStats(d);
  renderAccuracy(d.accuracy);
  renderBars('byType', d.countByType, false);
  renderWorkload(d.workload);
  renderModels(d.latestMetrics, d.activeModel);
  const s = await API.get('/api/workload/suggestions');
  renderSuggestions(s);
}

function renderStats(d) {
  const done = d.countByStatus.DONE || 0;
  const cards = [
    { v: d.totalTasks, l: 'Total tasks' },
    { v: (d.onTimeCompletionRate * 100).toFixed(0) + '%', l: 'On-time completion' },
    { v: done, l: 'Completed' },
    { v: d.activeModel ? MODEL_LABEL[d.activeModel.modelType] : 'Fallback', l: 'Active model' },
  ];
  document.getElementById('stats').innerHTML = cards.map(c =>
    `<div class="panel"><div class="stat-value">${c.v}</div><div class="stat-label">${c.l}</div></div>`).join('');
}

function renderAccuracy(a) {
  if (!a || a.sampleSize === 0) {
    document.getElementById('accuracy').innerHTML = '<p class="muted">No completed tasks yet.</p>';
    return;
  }
  const max = Math.max(a.manualEstimateMae, a.modelPredictionMae, 0.1);
  document.getElementById('accuracy').innerHTML =
    bar('Manual estimate', a.manualEstimateMae, max, 'alt') +
    bar('AI prediction', a.modelPredictionMae, max, '');
  const better = a.modelPredictionMae <= a.manualEstimateMae;
  document.getElementById('accuracyNote').textContent =
    `Based on ${a.sampleSize} completed tasks. ` +
    (better ? 'The AI model is more accurate than manual estimates.'
            : 'Manual estimates currently edge out the model — more history will help.');
}

function renderWorkload(loads) {
  if (!loads || loads.length === 0) {
    document.getElementById('workload').innerHTML = '<p class="muted">No open tasks.</p>';
    return;
  }
  const max = Math.max(...loads.map(l => l.predictedLoadDays), 0.1);
  document.getElementById('workload').innerHTML = loads.map(l =>
    `<div class="bar-row">
       <div class="bar-label">${l.name}${l.overloaded ? ' ⚠' : ''}</div>
       <div class="bar-track"><div class="bar-fill ${l.overloaded ? 'alt' : ''}" style="width:${(l.predictedLoadDays / max * 100)}%"></div></div>
       <div class="bar-val">${l.predictedLoadDays}</div>
     </div>`).join('');
}

function renderBars(elId, obj, _) {
  const entries = Object.entries(obj).filter(([, v]) => v > 0);
  if (entries.length === 0) { document.getElementById(elId).innerHTML = '<p class="muted">No data.</p>'; return; }
  const max = Math.max(...entries.map(([, v]) => v), 1);
  document.getElementById(elId).innerHTML = entries.map(([k, v]) =>
    `<div class="bar-row"><div class="bar-label">${k}</div>
       <div class="bar-track"><div class="bar-fill" style="width:${(v / max * 100)}%"></div></div>
       <div class="bar-val">${v}</div></div>`).join('');
}

function bar(label, val, max, cls) {
  return `<div class="bar-row"><div class="bar-label">${label}</div>
    <div class="bar-track"><div class="bar-fill ${cls}" style="width:${(val / max * 100)}%"></div></div>
    <div class="bar-val">${val}</div></div>`;
}

function renderModels(metrics, active) {
  if (!metrics || metrics.length === 0) {
    document.getElementById('models').innerHTML =
      '<p class="muted">Models not trained yet (using category-average fallback until enough history exists).</p>';
    return;
  }
  document.getElementById('models').innerHTML = `<table>
    <tr><th>Model</th><th>MAE</th><th>RMSE</th><th>R²</th><th></th></tr>
    ${metrics.map(m => `<tr>
      <td>${MODEL_LABEL[m.modelType]}</td>
      <td>${m.mae.toFixed(2)}</td><td>${m.rmse.toFixed(2)}</td><td>${m.r2.toFixed(2)}</td>
      <td>${m.active ? '<span class="tag-active">active</span>' : ''}</td>
    </tr>`).join('')}
  </table>`;
}

function renderSuggestions(list) {
  const el = document.getElementById('suggestions');
  if (!list || list.length === 0) {
    el.innerHTML = '<p class="muted">Workload is balanced — no reassignments suggested.</p>';
    return;
  }
  el.innerHTML = list.map(s =>
    `<div class="suggestion">Move <b>${s.taskTitle}</b> (${s.predictedDays}d) from
       <b>${s.fromUser}</b> → <b>${s.toUser}</b>. <span class="muted">${s.reason}</span></div>`).join('');
}

if (Auth.isAdmin(user)) {
  const btn = document.getElementById('retrainBtn');
  btn.style.display = 'inline-block';
  btn.addEventListener('click', async () => {
    btn.disabled = true; btn.textContent = 'Retraining...';
    try { await API.post('/api/models/retrain'); await load(); }
    finally { btn.disabled = false; btn.textContent = 'Retrain models'; }
  });
}

load();
