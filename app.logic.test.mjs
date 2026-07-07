import test from 'node:test';
import assert from 'node:assert/strict';
import {
  daysUntil,
  getNextReminder,
  normalizeTimeline,
  buildCollageLayout,
} from './app.logic.mjs';

test('daysUntil returns inclusive remaining calendar days', () => {
  assert.equal(daysUntil('2026-07-10', new Date('2026-07-07T09:30:00')), 3);
  assert.equal(daysUntil('2026-07-07', new Date('2026-07-07T23:59:00')), 0);
  assert.equal(daysUntil('2026-07-01', new Date('2026-07-07T00:00:00')), -6);
});

test('getNextReminder advances daily, weekly and monthly schedules', () => {
  const now = new Date('2026-07-07T10:00:00');

  assert.equal(
    getNextReminder('2026-07-07T09:00', 'daily', now).toISOString(),
    new Date('2026-07-08T09:00:00').toISOString(),
  );
  assert.equal(
    getNextReminder('2026-07-01T08:30', 'weekly', now).toISOString(),
    new Date('2026-07-08T08:30:00').toISOString(),
  );
  assert.equal(
    getNextReminder('2026-06-30T18:00', 'monthly', now).toISOString(),
    new Date('2026-07-30T18:00:00').toISOString(),
  );
  assert.equal(getNextReminder('', 'none', now), null);
});

test('normalizeTimeline combines generated countdown points with custom nodes', () => {
  const timeline = normalizeTimeline(
    '2026-07-01',
    '2026-07-11',
    [
      { id: 'a', title: '交付日', date: '2026-07-11', note: '最后确认' },
      { id: 'b', title: '纪念', date: '2026-07-05', note: '' },
    ],
  );

  assert.deepEqual(
    timeline.map((item) => item.date),
    ['2026-07-01', '2026-07-05', '2026-07-06', '2026-07-11'],
  );
  assert.equal(timeline.at(-1).title, '交付日');
});

test('buildCollageLayout creates stable cells inside the canvas', () => {
  const cells = buildCollageLayout(5, 1000, 800);

  assert.equal(cells.length, 5);
  assert.ok(cells.every((cell) => cell.w > 0 && cell.h > 0));
  assert.ok(cells.every((cell) => cell.x + cell.w <= 1000));
  assert.ok(cells.every((cell) => cell.y + cell.h <= 800));
});
