import worker from "./src/index.js";

function assert(condition, message) {
  if (!condition) {
    throw new Error(message);
  }
}

const health = await worker.fetch(
  new Request(
    "https://example.test/v1/entity/health"
  ),
  {}
);
assert(
  health.status === 200,
  "health endpoint should return 200"
);
const healthPayload = await health.json();
assert(
  healthPayload.status === "ok",
  "health endpoint should report ok"
);
assert(
  healthPayload.privacyMode ===
    "sanitized-descriptor-only",
  "health endpoint should expose privacy mode"
);
assert(
  healthPayload.providerConfigured === false,
  "provider should report unconfigured without secret"
);

const sensitive = await worker.fetch(
  new Request(
    "https://example.test/v1/entity/resolve",
    {
      method: "POST",
      headers: {
        "content-type": "application/json",
      },
      body: JSON.stringify({
        descriptor:
          "ACME STORE 0712345678",
      }),
    }
  ),
  {}
);
assert(
  sensitive.status === 400,
  "phone-bearing descriptor must be rejected"
);

const person = await worker.fetch(
  new Request(
    "https://example.test/v1/entity/resolve",
    {
      method: "POST",
      headers: {
        "content-type": "application/json",
      },
      body: JSON.stringify({
        descriptor: "JOHN KAMAU",
      }),
    }
  ),
  {}
);
assert(
  person.status === 400,
  "likely personal name must be rejected"
);

const safeWithoutProvider = await worker.fetch(
  new Request(
    "https://example.test/v1/entity/resolve",
    {
      method: "POST",
      headers: {
        "content-type": "application/json",
      },
      body: JSON.stringify({
        descriptor: "ACME HARDWARE LTD",
      }),
    }
  ),
  {}
);
assert(
  safeWithoutProvider.status === 503,
  "safe descriptor should reach provider boundary"
);

console.log(
  "Entity enrichment Worker contract tests passed."
);
