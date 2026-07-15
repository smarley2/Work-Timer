import test from 'node:test';
import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';

const statePath = new URL('../android/app/src/main/java/com/smarley/worktimer/WorkTimerState.java', import.meta.url);
const pluginPath = new URL('../android/app/src/main/java/com/smarley/worktimer/WorkTimerPlugin.java', import.meta.url);
const manifestPath = new URL('../android/app/src/main/AndroidManifest.xml', import.meta.url);

test('daily goal uses an exact alarm when Android allows it', async () => {
  const source = await readFile(statePath, 'utf8');
  assert.match(source, /canScheduleExactAlarms\(\)/);
  assert.match(source, /setExactAndAllowWhileIdle\(/);
  assert.match(source, /setAndAllowWhileIdle\(/, 'must keep a fallback when exact alarms are unavailable');
});

test('app declares and exposes exact alarm authorization', async () => {
  const [manifest, plugin] = await Promise.all([
    readFile(manifestPath, 'utf8'),
    readFile(pluginPath, 'utf8'),
  ]);
  assert.match(manifest, /android\.permission\.SCHEDULE_EXACT_ALARM/);
  assert.match(plugin, /ACTION_REQUEST_SCHEDULE_EXACT_ALARM/);
  assert.match(plugin, /canScheduleExactAlarms\(\)/);
});
