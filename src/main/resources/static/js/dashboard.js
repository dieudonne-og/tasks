// Reporting dashboard: KPIs, accuracy comparison, distributions, workload.
if (!requireAuth()) { throw new Error('redirect'); }
if (!Auth.isManager()) { location.href = '/board'; }
document.getElementById('nav').innerHTML = renderNav('dashboard');

async function boot() {
  try {
    const [d, w] = await Promise.all([api('/dashboard'), api('/workload')]);
    renderStats(d);
    renderAccuracy(d.accuracy);
    renderModel(d.model);
    renderBars('statusChart', d.statusCounts);
    renderBars('typeChart', d.typeCounts);
    renderWorkload(w);
  } catch (ex) {
    document.querySelector('.container').insertAdjacentHTML('beforeend',
      `<div class="card mt">${esc(ex.message)}</div>`);
  }
}

function renderStats(d) {
  const onTime = d.onTimeRate;
  const rate = onTime.completed ? Math.round(onTime.rate * 100) + '%' : '—';
  const model = d.model || {};
  const stats = [
    { label: 'Total tasks', value: d.totalTasks, hint: (d.statusCounts.DONE || 0) + ' completed' },
    { label: 'Open & at risk', value: d.openAtRisk, hint: 'predicted to miss deadline' },
    { label: 'On-time rate', value: rate, hint: onTime.onTime + ' / ' + onTime.completed + ' on time' },
    { label: 'Active model', value: shortModel(model.type), hint: model.trained ? ('RMSE ' + model.rmse + 'd') : 'category-average fallback' }
  ];
  document.getElementById('stats').innerHTML = stats.map(s =>
    `<div class="stat"><div class="label">${s.label}</div><div class="value">${esc(String(s.value))}</div><div class="hint">${esc(s.hint)}</div></div>`
  ).join('');
}

function shortModel(t) {
  return { LINEAR: 'Linear Reg.', RANDOM_FOREST: 'Random Forest', FALLBACK_AVERAGE: 'Average' }[t] || (t || '—');
}

function renderAccuracy(acc) {
  if (!acc || (acc.manualMae == null && acc.modelMae == null)) {
    document.getElementById('accuracy').innerHTML = '<p class="muted">No completed tasks with estimates yet.</p>';
    return;
  }
  const max = Math.max(acc.manualMae || 0, acc.modelMae || 0, 0.1);
  const rows = [
    { label: 'Manual estimate', v: acc.manualMae, warn: true },
    { label: 'AI prediction', v: acc.modelMae, warn: false }
  ];
  document.getElementById('accuracy').innerHTML = rows.map(r => bar(r.label, r.v, max, r.v != null ? r.v + 'd' : '—', r.warn)).join('')
    + `<p class="muted mt" style="font-size:12px">Manual n=${acc.manualSample}, AI n=${acc.modelSample}. A lower AI error demonstrates data-driven prediction beating subjective estimation.</p>`;
}

function renderModel(m) {
  if (!m || !m.trained) {
    document.getElementById('model').innerHTML =
      `<p class="muted">Using the category-average fallback until enough completed tasks accumulate to train a model.</p>`;
    return;
  }
  document.getElementById('model').innerHTML = `
    <table>
      <tr><th>Active model</th><td>${shortModel(m.type)} <span class="badge model">deployed</span></td></tr>
      <tr><th>MAE</th><td>${m.mae} days</td></tr>
      <tr><th>RMSE</th><td>${m.rmse} days</td></tr>
      <tr><th>R²</th><td>${m.r2}</td></tr>
      <tr><th>Trained on</th><td>${m.sampleSize} completed tasks</td></tr>
    </table>
    <p class="muted mt" style="font-size:12px">The model with the lower RMSE (linear regression vs random forest) is deployed automatically.</p>`;
}

function renderBars(elId, counts) {
  const entries = Object.entries(counts || {}).filter(([, v]) => v > 0);
  if (!entries.length) { document.getElementById(elId).innerHTML = '<p class="muted">No data.</p>'; return; }
  const max = Math.max(...entries.map(([, v]) => v));
  document.getElementById(elId).innerHTML = entries.map(([k, v]) => bar(k, v, max, String(v), false)).join('');
}

function bar(label, value, max, valueLabel, warn) {
  const pct = value == null ? 0 : Math.round((value / max) * 100);
  return `<div class="bar-row">
    <span class="bl" title="${esc(label)}">${esc(label)}</span>
    <span class="bar-track"><span class="bar-fill ${warn ? 'warn' : ''}" style="width:${pct}%"></span></span>
    <span class="bv">${esc(valueLabel)}</span>
  </div>`;
}

function renderWorkload(w) {
  const rows = (w.workload || []).map(e => `
    <tr>
      <td>${esc(e.name)} ${e.overloaded ? '<span class="badge risk">overloaded</span>' : ''}</td>
      <td>${e.openTasks}</td>
      <td>${e.predictedLoadDays} d</td>
    </tr>`).join('');
  document.getElementById('workload').innerHTML = `
    <div class="table-wrap"><table>
      <tr><th>Staff member</th><th>Open tasks</th><th>Predicted load</th></tr>
      ${rows || '<tr><td colspan="3" class="muted">No open tasks.</td></tr>'}
    </table></div>
    <p class="muted mt" style="font-size:12px">Team mean load: ${w.teamMeanDays} days.</p>`;

  const sug = w.suggestions || [];
  document.getElementById('suggestions').innerHTML = sug.length
    ? '<h4>Suggested redistribution</h4>' + sug.map(s =>
        `<div class="card" style="padding:12px;margin-bottom:8px">
          Move <strong>${esc(s.taskTitle)}</strong> (${s.predictedDays}d)
          from <strong>${esc(s.fromUser)}</strong> → <strong>${esc(s.toUser)}</strong>
          <div class="muted" style="font-size:12px">${esc(s.reason)}</div>
        </div>`).join('')
    : '<p class="muted">Workload is balanced — no redistribution needed.</p>';
}

boot();
