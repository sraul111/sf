const fs = require('fs');
const path = require('path');
const { activeNamespace, DEFAULT_NAMESPACE, isNamespace, validRecord } = require('./memory_common');

const STORE_PATH = process.env.MEMORY_STORE_PATH || path.join(__dirname, 'memory_store.json');
const TOKEN_LIMIT = 500;

function usage() {
  process.stdout.write('Usage: node read_memory.js\n');
  process.stdout.write('Reads validated memories for MEMORY_NAMESPACE (default: ServiceForge:feature2-parts:dev).\n');
  process.stdout.write('Store path: MEMORY_STORE_PATH (default: memory_store.json).\n');
  process.stdout.write('Token approximation: 4 characters equal 1 token; oldest matches are dropped FIFO over 500 tokens.\n');
}

function tokenCount(value) {
  return Math.ceil(value.length / 4);
}

function main() {
  if (process.argv.includes('--help') || process.argv.includes('-h')) {
    usage();
    return;
  }
  if (process.argv.length !== 2) {
    usage();
    process.exitCode = 1;
    return;
  }

  const namespace = activeNamespace();
  if (!isNamespace(namespace)) {
    throw new Error('invalid MEMORY_NAMESPACE: expected repo:branch:env');
  }

  const records = JSON.parse(fs.readFileSync(STORE_PATH, 'utf8'));
  if (!Array.isArray(records)) {
    throw new Error('memory store must be a JSON array');
  }
  const matches = records.filter((record) => validRecord(record) && record.payload.namespace === namespace);
  const selected = matches.slice();
  while (selected.length > 0 && tokenCount(JSON.stringify(selected)) > TOKEN_LIMIT) {
    selected.shift();
  }
  const droppedRecords = matches.length - selected.length;
  if (droppedRecords > 0) {
    process.stderr.write(`Memory read warning: ${JSON.stringify({
      code: 'TOKEN_LIMIT_TRIMMED',
      dropped_records: droppedRecords,
      matched_records: matches.length,
      returned_records: selected.length,
      approximate_tokens: tokenCount(JSON.stringify(selected)),
      token_limit: TOKEN_LIMIT
    })}\n`);
  }
  process.stdout.write(`${JSON.stringify(selected)}\n`);
}

try {
  main();
} catch (error) {
  process.stderr.write(`Memory read failed: ${error.message}\n`);
  process.exitCode = 1;
}