import {
  buildCollageLayout,
  createId,
  daysUntil,
  formatDateInput,
  getNextReminder,
  getReminderOccurrence,
  normalizeTimeline,
} from './app.logic.mjs';

const STORAGE_KEY = 'warm-countdown-app:v1';
const today = new Date();
const defaultEnd = new Date(today);
defaultEnd.setDate(today.getDate() + 100);

const defaultState = {
  title: '倒计时 100 天',
  startDate: formatDateInput(today),
  endDate: formatDateInput(defaultEnd),
  todos: [],
  records: [],
  nodes: [],
  notified: {},
};

let state = loadState();
let recordDraft = createEmptyRecordDraft();
let toastTimer = null;

const $ = (selector) => document.querySelector(selector);
const dom = {
  title: $('#countdown-title'),
  startDate: $('#start-date'),
  endDate: $('#end-date'),
  daysLeft: $('#days-left'),
  countdownSubtitle: $('#countdown-subtitle'),
  workspaceTitle: $('#workspace-title'),
  notifyButton: $('#notify-button'),
  tabButtons: document.querySelectorAll('.tab-button'),
  tabPanels: document.querySelectorAll('.tab-panel'),
  todoForm: $('#todo-form'),
  todoId: $('#todo-id'),
  todoTitle: $('#todo-title'),
  todoDeadline: $('#todo-deadline'),
  todoReminder: $('#todo-reminder'),
  todoRepeat: $('#todo-repeat'),
  todoNote: $('#todo-note'),
  todoReset: $('#todo-reset'),
  todoList: $('#todo-list'),
  todoCount: $('#todo-count'),
  recordForm: $('#record-form'),
  recordId: $('#record-id'),
  recordDate: $('#record-date'),
  recordFields: $('#record-fields'),
  addRecordField: $('#add-record-field'),
  recordReset: $('#record-reset'),
  recordList: $('#record-list'),
  recordCount: $('#record-count'),
  summaryField: $('#summary-field'),
  summaryMode: $('#summary-mode'),
  buildSummary: $('#build-summary'),
  downloadSummary: $('#download-summary'),
  summaryCanvas: $('#summary-canvas'),
  nodeForm: $('#node-form'),
  nodeId: $('#node-id'),
  nodeDate: $('#node-date'),
  nodeTitle: $('#node-title'),
  nodeNote: $('#node-note'),
  nodeReset: $('#node-reset'),
  timelineList: $('#timeline-list'),
  nodeCount: $('#node-count'),
  toast: $('#toast'),
};

init();

function init() {
  dom.title.value = state.title;
  dom.startDate.value = state.startDate;
  dom.endDate.value = state.endDate;
  dom.recordDate.value = formatDateInput(today);
  dom.todoDeadline.value = state.endDate;
  dom.nodeDate.value = formatDateInput(today);

  bindEvents();
  renderAll();
  drawEmptySummary();
  checkReminders();
  window.setInterval(checkReminders, 60 * 1000);
}

function bindEvents() {
  dom.tabButtons.forEach((button) => {
    button.addEventListener('click', () => activateTab(button.dataset.tab));
  });

  dom.title.addEventListener('input', () => {
    state.title = dom.title.value.trim() || defaultState.title;
    saveAndRender();
  });

  dom.startDate.addEventListener('change', () => {
    state.startDate = dom.startDate.value || defaultState.startDate;
    saveAndRender();
  });

  dom.endDate.addEventListener('change', () => {
    state.endDate = dom.endDate.value || defaultState.endDate;
    saveAndRender();
  });

  dom.notifyButton.addEventListener('click', requestNotifications);
  dom.todoForm.addEventListener('submit', saveTodo);
  dom.todoReset.addEventListener('click', resetTodoForm);
  dom.addRecordField.addEventListener('click', () => {
    recordDraft.items.push(createEmptyRecordItem());
    renderRecordFields();
  });
  dom.recordForm.addEventListener('submit', saveRecord);
  dom.recordReset.addEventListener('click', resetRecordForm);
  dom.buildSummary.addEventListener('click', buildSummary);
  dom.downloadSummary.addEventListener('click', downloadSummary);
  dom.nodeForm.addEventListener('submit', saveNode);
  dom.nodeReset.addEventListener('click', resetNodeForm);
}

function activateTab(tabId) {
  const labels = {
    todos: '待办事项',
    records: '每日记录',
    summary: '进度汇总',
    timeline: '关键节点',
  };

  dom.tabButtons.forEach((button) => button.classList.toggle('active', button.dataset.tab === tabId));
  dom.tabPanels.forEach((panel) => panel.classList.toggle('active', panel.id === tabId));
  dom.workspaceTitle.textContent = labels[tabId] || labels.todos;
}

function loadState() {
  try {
    const saved = JSON.parse(localStorage.getItem(STORAGE_KEY));
    return { ...defaultState, ...saved };
  } catch {
    return { ...defaultState };
  }
}

function saveState() {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(state));
}

function saveAndRender() {
  saveState();
  renderAll();
}

function renderAll() {
  renderCountdown();
  renderTodos();
  renderRecordFields();
  renderRecords();
  renderSummaryOptions();
  renderTimeline();
}

function renderCountdown() {
  const left = daysUntil(state.endDate);
  dom.daysLeft.textContent = String(left);
  const total = Math.max(1, daysUntil(state.endDate, state.startDate));
  const passed = Math.min(total, Math.max(0, daysUntil(new Date(), state.startDate)));
  dom.countdownSubtitle.textContent =
    left > 0 ? `已经走过 ${passed} 天，还剩 ${left} 天` : left === 0 ? '今天就是最后一天' : `已经过去 ${Math.abs(left)} 天`;
}

function saveTodo(event) {
  event.preventDefault();
  const id = dom.todoId.value || createId('todo');
  const existing = state.todos.find((todo) => todo.id === id);
  const todo = {
    id,
    title: dom.todoTitle.value.trim(),
    deadline: dom.todoDeadline.value,
    reminderAt: dom.todoReminder.value,
    repeat: dom.todoRepeat.value,
    note: dom.todoNote.value.trim(),
    done: existing?.done || false,
    createdAt: existing?.createdAt || new Date().toISOString(),
  };

  if (!todo.title || !todo.deadline) return;

  state.todos = existing
    ? state.todos.map((item) => (item.id === id ? todo : item))
    : [...state.todos, todo];
  resetTodoForm();
  showToast('事项已保存');
  saveAndRender();
}

function resetTodoForm() {
  dom.todoForm.reset();
  dom.todoId.value = '';
  dom.todoDeadline.value = state.endDate;
  dom.todoRepeat.value = 'none';
}

function renderTodos() {
  const sorted = [...state.todos].sort((a, b) => Number(a.done) - Number(b.done) || a.deadline.localeCompare(b.deadline));
  dom.todoCount.textContent = `${state.todos.length} 项`;
  dom.todoList.replaceChildren();

  if (!sorted.length) {
    dom.todoList.append(emptyState('还没有待办事项'));
    return;
  }

  sorted.forEach((todo) => {
    const item = document.createElement('article');
    item.className = `todo-item ${todo.done ? 'done' : ''}`;

    const top = document.createElement('div');
    top.className = 'item-top';
    const content = document.createElement('div');
    const title = document.createElement('p');
    title.className = 'item-title';
    title.textContent = todo.title;
    const meta = document.createElement('div');
    meta.className = 'item-meta';
    meta.append(metaChip(`Deadline ${todo.deadline}`));
    meta.append(metaChip(`${daysUntil(todo.deadline)} 天`));
    if (todo.reminderAt) {
      const next = getNextReminder(todo.reminderAt, todo.repeat);
      meta.append(metaChip(todo.repeat === 'none' ? `提醒 ${formatDateTime(todo.reminderAt)}` : `下次 ${formatDateTime(next)}`));
    }
    content.append(title, meta);
    if (todo.note) {
      const note = document.createElement('p');
      note.className = 'item-note';
      note.textContent = todo.note;
      content.append(note);
    }

    const actions = document.createElement('div');
    actions.className = 'item-actions';
    actions.append(
      actionButton(todo.done ? '↺' : '✓', () => toggleTodo(todo.id), todo.done ? '恢复' : '完成', 'check-button'),
      actionButton('改', () => editTodo(todo.id), '编辑'),
      actionButton('×', () => deleteTodo(todo.id), '删除'),
    );
    top.append(content, actions);
    item.append(top);
    dom.todoList.append(item);
  });
}

function toggleTodo(id) {
  state.todos = state.todos.map((todo) => (todo.id === id ? { ...todo, done: !todo.done } : todo));
  saveAndRender();
}

function editTodo(id) {
  const todo = state.todos.find((item) => item.id === id);
  if (!todo) return;
  dom.todoId.value = todo.id;
  dom.todoTitle.value = todo.title;
  dom.todoDeadline.value = todo.deadline;
  dom.todoReminder.value = todo.reminderAt || '';
  dom.todoRepeat.value = todo.repeat || 'none';
  dom.todoNote.value = todo.note || '';
  activateTab('todos');
}

function deleteTodo(id) {
  state.todos = state.todos.filter((todo) => todo.id !== id);
  saveAndRender();
}

function createEmptyRecordDraft(date = formatDateInput(today)) {
  return {
    id: '',
    date,
    items: [createEmptyRecordItem('照片'), createEmptyRecordItem('想说的话')],
  };
}

function createEmptyRecordItem(label = '') {
  return {
    id: createId('field'),
    label,
    text: '',
    image: '',
  };
}

function renderRecordFields() {
  dom.recordFields.replaceChildren();
  if (!recordDraft.items.length) recordDraft.items.push(createEmptyRecordItem());

  recordDraft.items.forEach((field, index) => {
    const wrap = document.createElement('section');
    wrap.className = 'record-field';

    const head = document.createElement('div');
    head.className = 'field-head';
    const labelInput = document.createElement('input');
    labelInput.type = 'text';
    labelInput.placeholder = '记录项名称';
    labelInput.value = field.label;
    labelInput.maxLength = 28;
    labelInput.addEventListener('input', () => {
      field.label = labelInput.value;
    });
    head.append(labelInput, actionButton('×', () => removeRecordField(index), '删除'));

    const imageLabel = document.createElement('label');
    imageLabel.className = 'image-input';
    const fileInput = document.createElement('input');
    fileInput.type = 'file';
    fileInput.accept = 'image/*';
    fileInput.addEventListener('change', () => readImage(fileInput.files?.[0], field));
    const imageText = document.createElement('span');
    imageText.textContent = field.image ? '更换图片' : '上传图片';
    imageLabel.append(fileInput);
    if (field.image) {
      const preview = document.createElement('img');
      preview.className = 'image-preview';
      preview.src = field.image;
      preview.alt = field.label || '记录图片';
      imageLabel.append(preview);
    } else {
      imageLabel.append(imageText);
    }

    const text = document.createElement('textarea');
    text.rows = 4;
    text.maxLength = 360;
    text.placeholder = '写下今天';
    text.value = field.text;
    text.addEventListener('input', () => {
      field.text = text.value;
    });

    wrap.append(head, imageLabel, text);
    dom.recordFields.append(wrap);
  });
}

function removeRecordField(index) {
  recordDraft.items.splice(index, 1);
  renderRecordFields();
}

function readImage(file, field) {
  if (!file) return;
  const reader = new FileReader();
  reader.addEventListener('load', () => {
    compressImage(String(reader.result || ''))
      .then((image) => {
        field.image = image;
        renderRecordFields();
      })
      .catch(() => {
        field.image = String(reader.result || '');
        renderRecordFields();
      });
  });
  reader.readAsDataURL(file);
}

function compressImage(src) {
  return new Promise((resolve, reject) => {
    const image = new Image();
    image.addEventListener('load', () => {
      const maxSize = 1280;
      const scale = Math.min(1, maxSize / Math.max(image.width, image.height));
      if (scale >= 1 && src.length < 700000) {
        resolve(src);
        return;
      }

      const canvas = document.createElement('canvas');
      canvas.width = Math.max(1, Math.round(image.width * scale));
      canvas.height = Math.max(1, Math.round(image.height * scale));
      const ctx = canvas.getContext('2d');
      ctx.drawImage(image, 0, 0, canvas.width, canvas.height);
      resolve(canvas.toDataURL('image/jpeg', 0.82));
    });
    image.addEventListener('error', reject);
    image.src = src;
  });
}

function saveRecord(event) {
  event.preventDefault();
  recordDraft.date = dom.recordDate.value;
  recordDraft.id = dom.recordId.value || recordDraft.id || createId('record');
  const cleaned = {
    id: recordDraft.id,
    date: recordDraft.date,
    items: recordDraft.items
      .map((item) => ({
        ...item,
        label: item.label.trim() || '未命名',
        text: item.text.trim(),
      }))
      .filter((item) => item.text || item.image || item.label),
  };

  if (!cleaned.date || !cleaned.items.length) return;

  const existing = state.records.find((record) => record.id === cleaned.id);
  state.records = existing
    ? state.records.map((record) => (record.id === cleaned.id ? cleaned : record))
    : [...state.records, cleaned];
  resetRecordForm();
  showToast('记录已保存');
  saveAndRender();
}

function resetRecordForm() {
  recordDraft = createEmptyRecordDraft(dom.recordDate.value || formatDateInput(today));
  dom.recordId.value = '';
  dom.recordDate.value = recordDraft.date;
  renderRecordFields();
}

function renderRecords() {
  const sorted = [...state.records].sort((a, b) => b.date.localeCompare(a.date));
  dom.recordCount.textContent = `${state.records.length} 天`;
  dom.recordList.replaceChildren();

  if (!sorted.length) {
    dom.recordList.append(emptyState('还没有每日记录'));
    return;
  }

  sorted.forEach((record) => {
    const item = document.createElement('article');
    item.className = 'record-item';
    const top = document.createElement('div');
    top.className = 'item-top';
    const title = document.createElement('p');
    title.className = 'item-title';
    title.textContent = record.date;
    const actions = document.createElement('div');
    actions.className = 'item-actions';
    actions.append(actionButton('改', () => editRecord(record.id), '编辑'), actionButton('×', () => deleteRecord(record.id), '删除'));
    top.append(title, actions);

    const grid = document.createElement('div');
    grid.className = 'record-item-grid';
    record.items.forEach((field) => {
      const chip = document.createElement('div');
      chip.className = 'record-chip';
      const strong = document.createElement('strong');
      strong.textContent = field.label;
      chip.append(strong);
      if (field.image) {
        const img = document.createElement('img');
        img.src = field.image;
        img.alt = field.label;
        img.loading = 'lazy';
        img.decoding = 'async';
        chip.append(img);
      }
      if (field.text) {
        const text = document.createElement('p');
        text.className = 'item-note';
        text.textContent = field.text;
        chip.append(text);
      }
      grid.append(chip);
    });

    item.append(top, grid);
    dom.recordList.append(item);
  });
}

function editRecord(id) {
  const record = state.records.find((item) => item.id === id);
  if (!record) return;
  recordDraft = JSON.parse(JSON.stringify(record));
  dom.recordId.value = record.id;
  dom.recordDate.value = record.date;
  renderRecordFields();
  activateTab('records');
}

function deleteRecord(id) {
  state.records = state.records.filter((record) => record.id !== id);
  saveAndRender();
}

function renderSummaryOptions() {
  const labels = [...new Set(state.records.flatMap((record) => record.items.map((item) => item.label)))];
  const current = dom.summaryField.value;
  dom.summaryField.replaceChildren();
  if (!labels.length) {
    const option = new Option('暂无记录项', '');
    dom.summaryField.append(option);
    return;
  }
  labels.forEach((label) => dom.summaryField.append(new Option(label, label)));
  if (labels.includes(current)) dom.summaryField.value = current;
}

async function buildSummary() {
  const label = dom.summaryField.value;
  if (!label) {
    drawEmptySummary();
    return;
  }

  const entries = state.records
    .slice()
    .sort((a, b) => a.date.localeCompare(b.date))
    .map((record) => ({
      date: record.date,
      item: record.items.find((field) => field.label === label),
    }))
    .filter((entry) => entry.item);

  if (dom.summaryMode.value === 'photos') {
    await drawPhotoSummary(label, entries.filter((entry) => entry.item.image));
  } else {
    drawTextSummary(label, entries.filter((entry) => entry.item.text));
  }
}

function drawEmptySummary() {
  const ctx = dom.summaryCanvas.getContext('2d');
  paintCanvasBackground(ctx);
  ctx.fillStyle = '#7f6c60';
  ctx.font = '700 34px Microsoft YaHei';
  ctx.textAlign = 'center';
  ctx.fillText('进度汇总', 600, 420);
  ctx.font = '22px Microsoft YaHei';
  ctx.fillText('保存每日记录后生成图片或文字排版', 600, 468);
}

async function drawPhotoSummary(label, entries) {
  const ctx = dom.summaryCanvas.getContext('2d');
  paintCanvasBackground(ctx);
  drawCanvasTitle(ctx, label, '图片拼图');

  if (!entries.length) {
    drawCanvasEmpty(ctx, '这个记录项还没有图片');
    return;
  }

  const cells = buildCollageLayout(entries.length, 1080, 650).map((cell) => ({
    ...cell,
    x: cell.x + 60,
    y: cell.y + 170,
  }));
  const images = await Promise.all(entries.map((entry) => loadImage(entry.item.image)));
  images.forEach((image, index) => {
    const cell = cells[index];
    drawCoverImage(ctx, image, cell.x, cell.y, cell.w, cell.h);
    ctx.fillStyle = 'rgba(63, 53, 44, 0.72)';
    ctx.fillRect(cell.x, cell.y + cell.h - 34, cell.w, 34);
    ctx.fillStyle = '#fffaf0';
    ctx.font = '700 20px Microsoft YaHei';
    ctx.textAlign = 'left';
    ctx.fillText(entries[index].date, cell.x + 12, cell.y + cell.h - 11);
  });
}

function drawTextSummary(label, entries) {
  const ctx = dom.summaryCanvas.getContext('2d');
  paintCanvasBackground(ctx);
  drawCanvasTitle(ctx, label, '文字排版');

  if (!entries.length) {
    drawCanvasEmpty(ctx, '这个记录项还没有文字');
    return;
  }

  ctx.textAlign = 'left';
  let y = 176;
  entries.slice(0, 9).forEach((entry, index) => {
    const x = index % 2 === 0 ? 78 : 620;
    if (index > 0 && index % 2 === 0) y += 154;
    drawTextCard(ctx, x, y, 500, 126, entry.date, entry.item.text);
  });
}

function paintCanvasBackground(ctx) {
  ctx.clearRect(0, 0, 1200, 900);
  const gradient = ctx.createLinearGradient(0, 0, 1200, 900);
  gradient.addColorStop(0, '#fff4df');
  gradient.addColorStop(0.52, '#fffaf0');
  gradient.addColorStop(1, '#eaf7ee');
  ctx.fillStyle = gradient;
  ctx.fillRect(0, 0, 1200, 900);
  ctx.fillStyle = 'rgba(244, 143, 139, 0.18)';
  ctx.beginPath();
  ctx.arc(1040, 90, 96, 0, Math.PI * 2);
  ctx.fill();
  ctx.fillStyle = 'rgba(159, 214, 191, 0.24)';
  ctx.beginPath();
  ctx.arc(112, 780, 140, 0, Math.PI * 2);
  ctx.fill();
}

function drawCanvasTitle(ctx, label, mode) {
  ctx.fillStyle = '#3f352c';
  ctx.textAlign = 'left';
  ctx.font = '800 46px Georgia, Microsoft YaHei';
  ctx.fillText(state.title, 70, 82);
  ctx.font = '700 24px Microsoft YaHei';
  ctx.fillStyle = '#cc625d';
  ctx.fillText(`${label} · ${mode}`, 72, 124);
}

function drawCanvasEmpty(ctx, text) {
  ctx.fillStyle = '#7f6c60';
  ctx.font = '700 30px Microsoft YaHei';
  ctx.textAlign = 'center';
  ctx.fillText(text, 600, 430);
}

function drawTextCard(ctx, x, y, w, h, date, text) {
  roundedRect(ctx, x, y, w, h, 18, 'rgba(255, 255, 255, 0.74)');
  ctx.fillStyle = '#cc625d';
  ctx.font = '800 20px Microsoft YaHei';
  ctx.fillText(date, x + 22, y + 36);
  ctx.fillStyle = '#4f433a';
  ctx.font = '20px Microsoft YaHei';
  wrapCanvasText(ctx, text, x + 22, y + 70, w - 44, 28, 2);
}

function roundedRect(ctx, x, y, w, h, r, fill) {
  ctx.beginPath();
  ctx.moveTo(x + r, y);
  ctx.arcTo(x + w, y, x + w, y + h, r);
  ctx.arcTo(x + w, y + h, x, y + h, r);
  ctx.arcTo(x, y + h, x, y, r);
  ctx.arcTo(x, y, x + w, y, r);
  ctx.closePath();
  ctx.fillStyle = fill;
  ctx.fill();
}

function wrapCanvasText(ctx, text, x, y, maxWidth, lineHeight, maxLines) {
  const chars = [...text];
  let line = '';
  let lines = 0;
  chars.forEach((char, index) => {
    const testLine = line + char;
    if (ctx.measureText(testLine).width > maxWidth && line) {
      ctx.fillText(line, x, y + lines * lineHeight);
      line = char;
      lines += 1;
    } else {
      line = testLine;
    }
    if (index === chars.length - 1 && lines < maxLines) {
      ctx.fillText(line, x, y + lines * lineHeight);
    }
  });
}

function loadImage(src) {
  return new Promise((resolve, reject) => {
    const image = new Image();
    image.onload = () => resolve(image);
    image.onerror = reject;
    image.src = src;
  });
}

function drawCoverImage(ctx, image, x, y, w, h) {
  const sourceRatio = image.width / image.height;
  const targetRatio = w / h;
  let sw = image.width;
  let sh = image.height;
  let sx = 0;
  let sy = 0;
  if (sourceRatio > targetRatio) {
    sw = image.height * targetRatio;
    sx = (image.width - sw) / 2;
  } else {
    sh = image.width / targetRatio;
    sy = (image.height - sh) / 2;
  }
  ctx.save();
  roundedRect(ctx, x, y, w, h, 18, '#fff');
  ctx.clip();
  ctx.drawImage(image, sx, sy, sw, sh, x, y, w, h);
  ctx.restore();
}

function downloadSummary() {
  const link = document.createElement('a');
  link.download = `${state.title || '倒计时'}-汇总.png`;
  link.href = dom.summaryCanvas.toDataURL('image/png');
  link.click();
}

function saveNode(event) {
  event.preventDefault();
  const id = dom.nodeId.value || createId('node');
  const node = {
    id,
    date: dom.nodeDate.value,
    title: dom.nodeTitle.value.trim(),
    note: dom.nodeNote.value.trim(),
    kind: 'custom',
  };
  if (!node.date || !node.title) return;
  const existing = state.nodes.find((item) => item.id === id);
  state.nodes = existing ? state.nodes.map((item) => (item.id === id ? node : item)) : [...state.nodes, node];
  resetNodeForm();
  showToast('节点已保存');
  saveAndRender();
}

function resetNodeForm() {
  dom.nodeForm.reset();
  dom.nodeId.value = '';
  dom.nodeDate.value = formatDateInput(today);
}

function renderTimeline() {
  const timeline = normalizeTimeline(state.startDate, state.endDate, state.nodes);
  dom.nodeCount.textContent = `${state.nodes.length} 个`;
  dom.timelineList.replaceChildren();
  timeline.forEach((node) => {
    const item = document.createElement('article');
    item.className = `timeline-item ${node.kind === 'auto' ? 'auto' : ''}`;
    const top = document.createElement('div');
    top.className = 'item-top';
    const content = document.createElement('div');
    const title = document.createElement('p');
    title.className = 'item-title';
    title.textContent = node.title;
    const meta = document.createElement('div');
    meta.className = 'item-meta';
    meta.append(metaChip(node.date), metaChip(`${daysUntil(node.date)} 天`));
    content.append(title, meta);
    if (node.note) {
      const note = document.createElement('p');
      note.className = 'item-note';
      note.textContent = node.note;
      content.append(note);
    }
    top.append(content);
    if (node.kind !== 'auto') {
      const actions = document.createElement('div');
      actions.className = 'item-actions';
      actions.append(actionButton('改', () => editNode(node.id), '编辑'), actionButton('×', () => deleteNode(node.id), '删除'));
      top.append(actions);
    }
    item.append(top);
    dom.timelineList.append(item);
  });
}

function editNode(id) {
  const node = state.nodes.find((item) => item.id === id);
  if (!node) return;
  dom.nodeId.value = node.id;
  dom.nodeDate.value = node.date;
  dom.nodeTitle.value = node.title;
  dom.nodeNote.value = node.note || '';
  activateTab('timeline');
}

function deleteNode(id) {
  state.nodes = state.nodes.filter((node) => node.id !== id);
  saveAndRender();
}

function requestNotifications() {
  if (!('Notification' in window)) {
    showToast('当前浏览器不支持系统提醒');
    return;
  }
  Notification.requestPermission().then((permission) => {
    showToast(permission === 'granted' ? '提醒已开启' : '提醒未开启');
  });
}

function checkReminders() {
  const now = new Date();
  let changed = false;
  state.todos.forEach((todo) => {
    if (todo.done || !todo.reminderAt) return;
    const occurrence = getReminderOccurrence(todo.reminderAt, todo.repeat, now);
    if (!occurrence) return;
    const key = `${todo.id}:${occurrence.toISOString()}`;
    if (state.notified[key]) return;
    state.notified[key] = true;
    changed = true;
    notify(todo.title, `Deadline ${todo.deadline}`);
  });
  if (changed) saveState();
}

function notify(title, body) {
  showToast(`${title} · ${body}`);
  if ('Notification' in window && Notification.permission === 'granted') {
    new Notification(title, { body });
  }
}

function showToast(message) {
  dom.toast.textContent = message;
  dom.toast.classList.add('show');
  window.clearTimeout(toastTimer);
  toastTimer = window.setTimeout(() => dom.toast.classList.remove('show'), 2600);
}

function emptyState(text) {
  const empty = document.createElement('div');
  empty.className = 'empty';
  empty.textContent = text;
  return empty;
}

function metaChip(text) {
  const chip = document.createElement('span');
  chip.className = 'pill';
  chip.textContent = text;
  return chip;
}

function actionButton(text, onClick, label, extraClass = 'icon-button') {
  const button = document.createElement('button');
  button.type = 'button';
  button.className = extraClass;
  button.textContent = text;
  button.title = label;
  button.setAttribute('aria-label', label);
  button.addEventListener('click', onClick);
  return button;
}

function formatDateTime(value) {
  if (!value) return '';
  const date = value instanceof Date ? value : new Date(value);
  if (Number.isNaN(date.getTime())) return '';
  return `${formatDateInput(date)} ${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`;
}
