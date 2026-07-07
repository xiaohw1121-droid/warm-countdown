const DAY_MS = 24 * 60 * 60 * 1000;

export function toDateOnly(value) {
  if (!value) return null;
  const date = value instanceof Date ? value : new Date(value);
  if (Number.isNaN(date.getTime())) return null;
  return new Date(date.getFullYear(), date.getMonth(), date.getDate());
}

export function formatDateInput(date) {
  if (!(date instanceof Date) || Number.isNaN(date.getTime())) return '';
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
}

export function daysUntil(targetDate, fromDate = new Date()) {
  const target = toDateOnly(targetDate);
  const from = toDateOnly(fromDate);
  if (!target || !from) return 0;
  return Math.round((target.getTime() - from.getTime()) / DAY_MS);
}

export function addByRepeat(date, repeat) {
  const next = new Date(date);
  if (repeat === 'daily') next.setDate(next.getDate() + 1);
  if (repeat === 'weekly') next.setDate(next.getDate() + 7);
  if (repeat === 'monthly') next.setMonth(next.getMonth() + 1);
  return next;
}

export function getNextReminder(reminderAt, repeat = 'none', now = new Date()) {
  if (!reminderAt || repeat === 'none') return null;
  let next = new Date(reminderAt);
  if (Number.isNaN(next.getTime())) return null;
  while (next <= now) {
    next = addByRepeat(next, repeat);
  }
  return next;
}

export function getReminderOccurrence(reminderAt, repeat = 'none', now = new Date()) {
  if (!reminderAt) return null;
  const first = new Date(reminderAt);
  if (Number.isNaN(first.getTime()) || first > now) return null;
  if (repeat === 'none') return first;

  let occurrence = new Date(first);
  let next = addByRepeat(occurrence, repeat);
  while (next <= now) {
    occurrence = next;
    next = addByRepeat(next, repeat);
  }
  return occurrence;
}

export function normalizeTimeline(startDate, endDate, nodes = [], today = new Date()) {
  const start = toDateOnly(startDate) || toDateOnly(today);
  const end = toDateOnly(endDate) || start;
  const totalDays = Math.max(0, Math.round((end.getTime() - start.getTime()) / DAY_MS));
  const generated = [
    {
      id: 'auto-start',
      date: formatDateInput(start),
      title: '倒计时开始',
      note: '从这一天开始，把重要的事一点点安放好。',
      kind: 'auto',
    },
  ];

  if (totalDays >= 10) {
    generated.push({
      id: 'auto-half',
      date: formatDateInput(new Date(start.getTime() + Math.round(totalDays / 2) * DAY_MS)),
      title: '倒计时过半',
      note: '回头看一眼已经完成的记录，再继续向前。',
      kind: 'auto',
    });
  }

  if (formatDateInput(start) !== formatDateInput(end)) {
    generated.push({
      id: 'auto-end',
      date: formatDateInput(end),
      title: '最后一天',
      note: '把想留下的画面和文字都收好。',
      kind: 'auto',
    });
  }

  const customNodes = nodes.map((node) => ({ ...node, kind: node.kind || 'custom' }));
  const customDates = new Set(customNodes.map((node) => node.date));

  return [...generated.filter((node) => !customDates.has(node.date)), ...customNodes]
    .filter((item) => item.date && item.title)
    .sort((a, b) => a.date.localeCompare(b.date) || a.title.localeCompare(b.title));
}

export function buildCollageLayout(count, width, height) {
  if (count <= 0) return [];
  const gap = Math.max(12, Math.round(width * 0.018));
  const columns = Math.ceil(Math.sqrt(count));
  const rows = Math.ceil(count / columns);
  const cellW = (width - gap * (columns + 1)) / columns;
  const cellH = (height - gap * (rows + 1)) / rows;

  return Array.from({ length: count }, (_, index) => {
    const column = index % columns;
    const row = Math.floor(index / columns);
    return {
      x: Math.round(gap + column * (cellW + gap)),
      y: Math.round(gap + row * (cellH + gap)),
      w: Math.floor(cellW),
      h: Math.floor(cellH),
    };
  });
}

export function createId(prefix = 'id') {
  return `${prefix}-${Date.now().toString(36)}-${Math.random().toString(36).slice(2, 8)}`;
}
