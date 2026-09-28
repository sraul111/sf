const DEFAULT_NAMESPACE = 'ServiceForge:feature2-parts:dev';
const TYPE_TAGS = new Set(['[DECISION]', '[FACT]', '[STATE]']);
const FORBIDDEN_CONTENT = /stdout|stderr|stack\s*trace|stacktrace|intermediate\s+reasoning|conversational\s+filler|successfully\s+retried|unverified|out[- ]of[- ]scope/i;

function activeNamespace() {
  return process.env.MEMORY_NAMESPACE || DEFAULT_NAMESPACE;
}

function isNamespace(value) {
  return typeof value === 'string' && /^[^:\s]+:[^:\s]+:[^:\s]+$/.test(value);
}

function validRecord(record) {
  return record && typeof record === 'object' && !Array.isArray(record)
    && typeof record.timestamp === 'string' && !Number.isNaN(Date.parse(record.timestamp))
    && typeof record.source_task === 'string' && record.source_task.trim() !== ''
    && TYPE_TAGS.has(record.type_tag)
    && record.payload && typeof record.payload === 'object' && !Array.isArray(record.payload)
    && isNamespace(record.payload.namespace)
    && record.payload.validated === true
    && record.payload.content !== undefined && record.payload.content !== null;
}

function validateCandidate(payload, sourceTask, typeTag) {
  if (!payload || typeof payload !== 'object' || Array.isArray(payload)) {
    throw new Error('payload must be a JSON object');
  }
  if (!isNamespace(payload.namespace) || payload.namespace !== activeNamespace()) {
    throw new Error('payload namespace must be an exact repo:branch:env match for the active namespace');
  }
  if (payload.validated !== true) {
    throw new Error('payload must be validated:true');
  }
  if (payload.content === undefined || payload.content === null || String(payload.content).trim() === '') {
    throw new Error('payload content is required');
  }
  if (FORBIDDEN_CONTENT.test(JSON.stringify(payload.content))) {
    throw new Error('transient, unverified, or out-of-scope content is not retained');
  }
  if (typeof sourceTask !== 'string' || sourceTask.trim() === '' || /[\r\n]/.test(sourceTask)) {
    throw new Error('source_task is required');
  }
  if (!TYPE_TAGS.has(typeTag)) {
    throw new Error('type_tag must be [DECISION], [FACT], or [STATE]');
  }
}

module.exports = {
  DEFAULT_NAMESPACE,
  TYPE_TAGS,
  activeNamespace,
  isNamespace,
  validRecord,
  validateCandidate
};