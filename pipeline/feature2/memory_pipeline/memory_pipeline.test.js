const assert = require('node:assert/strict');
const fs = require('node:fs');
const os = require('node:os');
const path = require('node:path');
const { spawn, spawnSync } = require('node:child_process');
const test = require('node:test');

const directory = __dirname;
const committedStorePath = path.join(directory, 'memory_store.json');
const temporaryDirectory = fs.mkdtempSync(path.join(os.tmpdir(), 'serviceforge-memory-test-'));
const storePath = path.join(temporaryDirectory, 'memory_store.json');
const writeScript = path.join(directory, 'write_memory.js');
const readScript = path.join(directory, 'read_memory.js');
const defaultNamespace = 'ServiceForge:feature2-parts:dev';
fs.copyFileSync(committedStorePath, storePath);

function run(script, args = [], environment = {}) {
  const result = spawnSync(process.execPath, [script, ...args], {
    cwd: directory,
    encoding: 'utf8',
    env: { ...process.env, MEMORY_STORE_PATH: storePath, ...environment }
  });
  return {
    status: result.status,
    stdout: result.stdout,
    stderr: result.stderr
  };
}

function write(payload, sourceTask, typeTag, namespace = defaultNamespace) {
  return run(writeScript, [JSON.stringify(payload), sourceTask, typeTag], {
    MEMORY_NAMESPACE: namespace
  });
}

function read(namespace = defaultNamespace) {
  return run(readScript, [], { MEMORY_NAMESPACE: namespace });
}

function writeAsync(payload, sourceTask, typeTag, namespace = defaultNamespace) {
  return new Promise((resolve) => {
    const child = spawn(process.execPath, [writeScript, JSON.stringify(payload), sourceTask, typeTag], {
      cwd: directory,
      env: { ...process.env, MEMORY_NAMESPACE: namespace, MEMORY_STORE_PATH: storePath },
      encoding: 'utf8'
    });
    let stdout = '';
    let stderr = '';
    child.stdout.on('data', (chunk) => { stdout += chunk; });
    child.stderr.on('data', (chunk) => { stderr += chunk; });
    child.on('close', (status) => resolve({ status, stdout, stderr }));
  });
}

function records() {
  return JSON.parse(fs.readFileSync(storePath, 'utf8'));
}

function restoreStore() {
  fs.rmSync(temporaryDirectory, { recursive: true, force: true });
}

test.after(() => restoreStore());

test('approved CLI artifacts, seeded fact, schema, and ISO timestamp are valid', () => {
  const store = records();
  assert.equal(Array.isArray(store), true);
  assert.equal(store.length, 1);
  assert.deepEqual(Object.keys(store[0]).sort(), ['payload', 'source_task', 'timestamp', 'type_tag']);
  assert.equal(store[0].type_tag, '[FACT]');
  assert.equal(store[0].source_task, 'pipeline/decisions/feature-1-decisions.md');
  assert.equal(store[0].payload.namespace, defaultNamespace);
  assert.match(store[0].payload.content, /45 minutes/);
  assert.equal(store[0].payload.validated, true);
  assert.equal(new Date(store[0].timestamp).toISOString(), store[0].timestamp);

  const seededRead = read();
  assert.equal(seededRead.status, 0, seededRead.stderr);
  assert.equal(JSON.parse(seededRead.stdout)[0].payload.content, 'The standard travel buffer per job is 45 minutes.');

  const writeHelp = run(writeScript, ['--help']);
  const readHelp = run(readScript, ['--help']);
  assert.equal(writeHelp.status, 0);
  assert.match(writeHelp.stdout, /node write_memory\.js/);
  assert.equal(readHelp.status, 0);
  assert.match(readHelp.stdout, /node read_memory\.js/);
});

test('valid retained memory has provenance and survives a later process invocation', () => {
  const payload = {
    namespace: defaultNamespace,
    validated: true,
    content: 'Parts reservation reuses the Feature 1 scheduling dependency.'
  };
  const result = write(payload, 'feature-2-test-task', '[DECISION]');
  assert.equal(result.status, 0, result.stderr);

  const laterRead = read();
  assert.equal(laterRead.status, 0, laterRead.stderr);
  const retrieved = JSON.parse(laterRead.stdout);
  const record = retrieved.find((entry) => entry.source_task === 'feature-2-test-task');
  assert.deepEqual(record.payload, payload);
  assert.equal(record.type_tag, '[DECISION]');
  assert.equal(new Date(record.timestamp).toISOString(), record.timestamp);
});

test('retention filter discards logs, reasoning, retries, and unverified context without writes', () => {
  const before = fs.readFileSync(storePath, 'utf8');
  const candidates = [
    'raw stdout output',
    'intermediate reasoning step',
    'successfully retried failure',
    'unverified assumption',
    'out-of-scope domain context'
  ];
  for (const content of candidates) {
    const result = write({ namespace: defaultNamespace, validated: true, content }, 'discard-test', '[STATE]');
    assert.notEqual(result.status, 0, content);
  }
  assert.equal(fs.readFileSync(storePath, 'utf8'), before);
});

test('malformed input, invalid namespace, missing provenance, and invalid type never write', () => {
  const before = fs.readFileSync(storePath, 'utf8');
  const cases = [
    run(writeScript, ['not-json', 'bad-input', '[FACT]'], { MEMORY_NAMESPACE: defaultNamespace }),
    write({ namespace: 'ServiceForge:feature2-parts', validated: true, content: 'bad namespace' }, 'bad-namespace', '[FACT]'),
    write({ namespace: defaultNamespace, content: 'missing validation' }, 'missing-validation', '[FACT]'),
    write({ namespace: defaultNamespace, validated: true, content: 'bad type' }, 'bad-type', '[NOTE]'),
    write({ namespace: defaultNamespace, validated: true, content: 'missing source' }, '', '[FACT]')
  ];
  for (const result of cases) {
    assert.notEqual(result.status, 0);
  }
  assert.equal(fs.readFileSync(storePath, 'utf8'), before);
});

test('exact namespace isolation excludes branch and environment neighbors', () => {
  const isolatedNamespace = 'ServiceForge:feature2-parts:test';
  const branchNamespace = 'ServiceForge:other-branch:dev';
  const environmentNamespace = 'ServiceForge:feature2-parts:prod';
  assert.equal(write({ namespace: isolatedNamespace, validated: true, content: 'isolated test memory' }, 'isolation-test', '[STATE]', isolatedNamespace).status, 0);
  assert.equal(write({ namespace: branchNamespace, validated: true, content: 'wrong branch memory' }, 'isolation-test', '[STATE]', branchNamespace).status, 0);
  assert.equal(write({ namespace: environmentNamespace, validated: true, content: 'wrong environment memory' }, 'isolation-test', '[STATE]', environmentNamespace).status, 0);

  const result = read(isolatedNamespace);
  assert.equal(result.status, 0, result.stderr);
  const retrieved = JSON.parse(result.stdout);
  assert.equal(retrieved.length, 1);
  assert.equal(retrieved[0].payload.namespace, isolatedNamespace);
  assert.equal(read(branchNamespace).status, 0);
  assert.deepEqual(JSON.parse(read(branchNamespace).stdout), [
    records().find((entry) => entry.payload.namespace === branchNamespace)
  ]);
});

test('invalid read namespace is rejected without returning records', () => {
  const result = read('ServiceForge:feature2-parts');
  assert.notEqual(result.status, 0);
  assert.match(result.stderr, /namespace/i);
});

test('read order is deterministic and FIFO removes oldest matches over 500 tokens', () => {
  const namespace = 'ServiceForge:feature2-parts:fifo';
  const contents = [
    'oldest '.repeat(200),
    'middle '.repeat(200),
    'newest '.repeat(200)
  ];
  for (const [index, content] of contents.entries()) {
    assert.equal(write({ namespace, validated: true, content }, `fifo-${index}`, '[FACT]', namespace).status, 0);
  }

  const firstRead = read(namespace);
  const secondRead = read(namespace);
  assert.equal(firstRead.status, 0, firstRead.stderr);
  assert.equal(secondRead.status, 0, secondRead.stderr);
  assert.equal(firstRead.stdout, secondRead.stdout);
  assert.match(firstRead.stderr, /TOKEN_LIMIT_TRIMMED/);
  assert.match(firstRead.stderr, /"dropped_records":2/);
  const retrieved = JSON.parse(firstRead.stdout);
  assert.equal(retrieved.some((entry) => entry.source_task === 'fifo-0'), false);
  assert.deepEqual(retrieved.map((entry) => entry.source_task), ['fifo-2']);
  assert.ok(Math.ceil(firstRead.stdout.length / 4) <= 500);
});

test('token approximation uses four characters per token at the hard ceiling', () => {
  const namespace = 'ServiceForge:feature2-parts:token';
  const content = 'x'.repeat(2200);
  assert.equal(write({ namespace, validated: true, content }, 'token-test', '[STATE]', namespace).status, 0);
  const result = read(namespace);
  assert.equal(result.status, 0, result.stderr);
  const retrieved = JSON.parse(result.stdout);
  assert.equal(retrieved.length, 0);
  assert.ok(Math.ceil(result.stdout.length / 4) <= 500);
  assert.match(result.stderr, /TOKEN_LIMIT_TRIMMED/);
  assert.match(result.stderr, /"dropped_records":1/);
});

test('concurrent writers serialize updates without losing records', async () => {
  const namespace = 'ServiceForge:feature2-parts:concurrent';
  const results = await Promise.all(Array.from({ length: 8 }, (_, index) => writeAsync(
    { namespace, validated: true, content: `concurrent memory ${index}` },
    `concurrent-${index}`,
    '[STATE]',
    namespace
  )));
  assert.deepEqual(results.map((result) => result.status), Array(8).fill(0));
  const retrieved = JSON.parse(read(namespace).stdout);
  assert.deepEqual(retrieved.map((entry) => entry.source_task).sort(), Array.from({ length: 8 }, (_, index) => `concurrent-${index}`));
});