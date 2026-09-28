const fs = require('fs');
const path = require('path');
const { DEFAULT_NAMESPACE, validateCandidate } = require('./memory_common');

const STORE_PATH = process.env.MEMORY_STORE_PATH || path.join(__dirname, 'memory_store.json');
const LOCK_PATH = `${STORE_PATH}.lock`;
const LOCK_WAIT_MS = 25;
const LOCK_TIMEOUT_MS = 5000;
const STALE_LOCK_MS = 30000;

function fail(message) {
  process.stderr.write(`${message}\n`);
  process.exitCode = 1;
}

function usage() {
  process.stdout.write('Usage: node write_memory.js "<payload-json>" "<source_task>" "<type_tag>"\n');
  process.stdout.write('Payload JSON must contain namespace, validated:true, and non-empty content.\n');
  process.stdout.write('The namespace must be repo:branch:env and match MEMORY_NAMESPACE.\n');
  process.stdout.write('Store path: MEMORY_STORE_PATH (default: memory_store.json).\n');
  process.stdout.write('Type tags: [DECISION], [FACT], [STATE].\n');
}

function readStore() {
  let records;
  try {
    records = JSON.parse(fs.readFileSync(STORE_PATH, 'utf8'));
  } catch (error) {
    throw new Error(`memory store is not valid JSON: ${error.message}`);
  }
  if (!Array.isArray(records)) {
    throw new Error('memory store must be a JSON array');
  }
  return records;
}

function writeStore(records) {
  const temporaryPath = `${STORE_PATH}.${process.pid}.${Date.now()}.tmp`;
  try {
    fs.writeFileSync(temporaryPath, `${JSON.stringify(records, null, 2)}\n`, 'utf8');
    fs.renameSync(temporaryPath, STORE_PATH);
  } finally {
    if (fs.existsSync(temporaryPath)) {
      fs.unlinkSync(temporaryPath);
    }
  }
}

function waitForLock() {
  const startedAt = Date.now();
  const waitBuffer = new Int32Array(new SharedArrayBuffer(4));
  while (true) {
    try {
      const descriptor = fs.openSync(LOCK_PATH, 'wx');
      fs.writeFileSync(descriptor, JSON.stringify({ pid: process.pid, created_at: new Date().toISOString() }), 'utf8');
      fs.closeSync(descriptor);
      return;
    } catch (error) {
      if (error.code !== 'EEXIST') {
        throw error;
      }
      try {
        if (Date.now() - fs.statSync(LOCK_PATH).mtimeMs > STALE_LOCK_MS) {
          fs.unlinkSync(LOCK_PATH);
          continue;
        }
      } catch (lockError) {
        if (lockError.code !== 'ENOENT') {
          throw lockError;
        }
        continue;
      }
      if (Date.now() - startedAt >= LOCK_TIMEOUT_MS) {
        throw new Error('memory store is busy; concurrent write did not complete within 5 seconds');
      }
      Atomics.wait(waitBuffer, 0, 0, LOCK_WAIT_MS);
    }
  }
}

function releaseLock() {
  try {
    fs.unlinkSync(LOCK_PATH);
  } catch (error) {
    if (error.code !== 'ENOENT') {
      throw error;
    }
  }
}

if (process.argv.includes('--help') || process.argv.includes('-h')) {
  usage();
} else if (process.argv.length !== 5) {
  usage();
  process.exitCode = 1;
} else {
  try {
    const payload = JSON.parse(process.argv[2]);
    const sourceTask = process.argv[3];
    const typeTag = process.argv[4];
    validateCandidate(payload, sourceTask, typeTag);
    waitForLock();
    try {
      const records = readStore();
      records.push({
        timestamp: new Date().toISOString(),
        source_task: sourceTask,
        type_tag: typeTag,
        payload
      });
      writeStore(records);
      process.stdout.write('Memory stored.\n');
    } finally {
      releaseLock();
    }
  } catch (error) {
    fail(`Memory rejected: ${error.message}`);
  }
}