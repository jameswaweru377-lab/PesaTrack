import assert from "node:assert/strict";
import worker from "./src/index.js";

const env = {
  ENTITY_RESOLVE_CLIENT_RATE_LIMITER: { limit: async () => ({ success: true }) },
  ENTITY_RESOLVE_EDGE_RATE_LIMITER: { limit: async () => ({ success: true }) },
  LOGO_DEV_SECRET_KEY: "invented-test-key",
};
const request = (body, headers = {}) => new Request("https://example.test/v1/entity/resolve", {
  method: "POST", headers, body,
});
const validBody = JSON.stringify({ descriptor: "ACME HARDWARE LTD" });
const providerBody = JSON.stringify({ merchant: "Acme Hardware", domain: "example.test", category: "retail" });
const originalFetch = globalThis.fetch;
const originalSetTimeout = globalThis.setTimeout;
let calls = 0;
let failures = 0;
async function check(name, run) {
  try { await run(); console.log(`PASS ${name}`); }
  catch (error) { failures++; console.error(`FAIL ${name}: ${error.message}`); }
}
try {
  globalThis.fetch = async () => { calls++; return new Response(providerBody); };
  await check("oversized body without content-length is rejected before provider", async () => {
    const response = await worker.fetch(request(JSON.stringify({ descriptor: "ACME HARDWARE LTD", padding: "x".repeat(4096) })), env);
    assert.equal(response.status, 413);
    assert.equal(calls, 0);
  });
  await check("oversized declared body is rejected", async () => {
    assert.equal((await worker.fetch(request(validBody, { "content-length": "4097" }), env)).status, 413);
  });
  await check("normal provider response resolves", async () => {
    assert.equal((await worker.fetch(request(validBody), env)).status, 200);
  });
  await check("oversized streamed provider response fails safely", async () => {
    globalThis.fetch = async () => new Response(JSON.stringify({ merchant: "Acme Hardware", padding: "x".repeat(65536) }));
    assert.equal((await worker.fetch(request(validBody), env)).status, 503);
  });
  await check("provider content-length is bounded before reading", async () => {
    globalThis.fetch = async () => new Response(providerBody, { headers: { "content-length": "65537" } });
    assert.equal((await worker.fetch(request(validBody), env)).status, 503);
  });
  await check("UTF8 request byte count is enforced", async () => {
    assert.equal((await worker.fetch(request(JSON.stringify({ descriptor: "ACME HARDWARE LTD", padding: "é".repeat(2048) })), env)).status, 413);
  });
  await check("deadline aborts stalled provider fetch", async () => {
    let signal;
    globalThis.fetch = async (_, options) => { signal = options.signal; return new Promise(() => {}); };
    globalThis.setTimeout = (fn, ms, ...args) => originalSetTimeout(fn, Math.min(ms, 20), ...args);
    const response = await Promise.race([
      worker.fetch(request(validBody), env),
      new Promise((_, reject) => originalSetTimeout(() => reject(new Error("application deadline missing")), 100)),
    ]);
    assert.equal(response.status, 503);
    assert.equal(signal.aborted, true);
  });
  await check("deadline includes stalled response body and cancels reader", async () => {
    let canceled = false;
    globalThis.fetch = async () => new Response(new ReadableStream({ cancel() { canceled = true; } }));
    // Accelerate the production timer; the body itself never resolves.
    globalThis.setTimeout = (fn, ms, ...args) => originalSetTimeout(fn, Math.min(ms, 20), ...args);
    const response = await Promise.race([
      worker.fetch(request(validBody), env),
      new Promise((_, reject) => originalSetTimeout(() => reject(new Error("application deadline missing")), 100)),
    ]);
    assert.equal(response.status, 503);
    assert.equal(canceled, true);
  });
} finally {
  globalThis.fetch = originalFetch;
  globalThis.setTimeout = originalSetTimeout;
}
if (failures) process.exitCode = 1;
