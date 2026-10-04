import worker, {
  classifyOrganizationRole,
  salarySuitabilityForRole,
} from "./src/index.js";

function assert(condition, message) {
  if (!condition) {
    throw new Error(message);
  }
}

function allowLimiter(onKey = () => {}) {
  return {
    async limit({ key }) {
      onKey(key);
      return { success: true };
    },
  };
}

const denyLimiter = {
  async limit() {
    return { success: false };
  },
};

const allowedEnv = {
  ENTITY_RESOLVE_CLIENT_RATE_LIMITER:
    allowLimiter(),
  ENTITY_RESOLVE_EDGE_RATE_LIMITER:
    allowLimiter(),
};

const CLIENT_ID =
  "7fbe6f76-c82a-4d44-9f1a-cf36bf9db905";

function resolveRequest(
  descriptor = "ACME HARDWARE LTD",
  clientId = CLIENT_ID,
) {
  const headers = {
    "content-type": "application/json",
  };
  if (clientId !== null) {
    headers["x-pesatrack-client-id"] =
      clientId;
  }

  return new Request(
    "https://example.test/v1/entity/resolve",
    {
      method: "POST",
      headers,
      body: JSON.stringify({ descriptor }),
    },
  );
}

const health = await worker.fetch(
  new Request(
    "https://example.test/v1/entity/health",
  ),
  {},
);
assert(
  health.status === 200,
  "health endpoint should return 200",
);
const healthPayload = await health.json();
assert(
  healthPayload.status === "ok",
  "health endpoint should report ok",
);
assert(
  healthPayload.privacyMode ===
    "sanitized-descriptor-only",
  "health endpoint should expose privacy mode",
);
assert(
  healthPayload.providerConfigured === false,
  "provider should report unconfigured without secret",
);

const sensitive = await worker.fetch(
  resolveRequest(
    "ACME STORE 0712345678",
  ),
  allowedEnv,
);
assert(
  sensitive.status === 400,
  "phone-bearing descriptor must be rejected",
);

const person = await worker.fetch(
  resolveRequest("JOHN KAMAU"),
  allowedEnv,
);
assert(
  person.status === 400,
  "likely personal name must be rejected",
);

const safeWithoutProvider =
  await worker.fetch(
    resolveRequest(),
    allowedEnv,
  );
assert(
  safeWithoutProvider.status === 503,
  "safe descriptor should reach provider boundary",
);

let observedClientKey = null;
let observedEdgeKey = null;
const keyedEnv = {
  ENTITY_RESOLVE_CLIENT_RATE_LIMITER:
    allowLimiter((key) => {
      observedClientKey = key;
    }),
  ENTITY_RESOLVE_EDGE_RATE_LIMITER:
    allowLimiter((key) => {
      observedEdgeKey = key;
    }),
};

await worker.fetch(
  resolveRequest(),
  keyedEnv,
);
assert(
  observedClientKey ===
    `client:${CLIENT_ID}`,
  "client limiter should use the install-scoped client ID",
);
assert(
  observedEdgeKey === "v1-entity-resolve",
  "edge limiter should use the stable resolve-route key",
);

let anonymousKey = null;
await worker.fetch(
  resolveRequest(
    "ACME HARDWARE LTD",
    null,
  ),
  {
    ENTITY_RESOLVE_CLIENT_RATE_LIMITER:
      allowLimiter((key) => {
        anonymousKey = key;
      }),
    ENTITY_RESOLVE_EDGE_RATE_LIMITER:
      allowLimiter(),
  },
);
assert(
  anonymousKey === "client:anonymous",
  "clients without a valid install ID should share the anonymous bucket",
);

const clientLimited = await worker.fetch(
  resolveRequest(),
  {
    ENTITY_RESOLVE_CLIENT_RATE_LIMITER:
      denyLimiter,
    ENTITY_RESOLVE_EDGE_RATE_LIMITER:
      allowLimiter(),
  },
);
assert(
  clientLimited.status === 429,
  "resolve endpoint should return 429 when the client bucket is exhausted",
);
assert(
  clientLimited.headers.get("retry-after") === "60",
  "client-rate-limited responses should tell clients when to retry",
);

const edgeLimited = await worker.fetch(
  resolveRequest(),
  {
    ENTITY_RESOLVE_CLIENT_RATE_LIMITER:
      allowLimiter(),
    ENTITY_RESOLVE_EDGE_RATE_LIMITER:
      denyLimiter,
  },
);
assert(
  edgeLimited.status === 429,
  "resolve endpoint should return 429 when the edge safety cap is exhausted",
);

const missingLimiter = await worker.fetch(
  resolveRequest(),
  {
    ENTITY_RESOLVE_CLIENT_RATE_LIMITER:
      allowLimiter(),
  },
);
assert(
  missingLimiter.status === 503,
  "resolve endpoint should fail closed when either limiter binding is missing",
);

const throwingLimiter = {
  async limit() {
    throw new Error("limiter unavailable");
  },
};
const limiterFailure = await worker.fetch(
  resolveRequest(),
  {
    ENTITY_RESOLVE_CLIENT_RATE_LIMITER:
      allowLimiter(),
    ENTITY_RESOLVE_EDGE_RATE_LIMITER:
      throwingLimiter,
  },
);
assert(
  limiterFailure.status === 503,
  "resolve endpoint should fail closed when Cloudflare limiter evaluation fails",
);

assert(
  classifyOrganizationRole(
    "Payment Processing",
    "Sample Payments Ltd",
    "samplepay.example",
  ) === "PAYMENT_PROCESSOR",
  "payment processors must be identified before salary inference",
);

assert(
  salarySuitabilityForRole(
    "PAYMENT_PROCESSOR",
  ) === "REJECT",
  "payment processors must not be suggested as employers",
);

assert(
  classifyOrganizationRole(
    "Financial Services",
    "AcmePay",
    "acmepay.example",
  ) === "PAYMENT_PROCESSOR",
  "financial-service payment brands should be screened as payment processors",
);

assert(
  classifyOrganizationRole(
    "Financial Services",
    "pawaPay",
    "pawapay.io",
  ) === "PAYMENT_PROCESSOR",
  "payment-gateway brands like pawaPay must be rejected as salary employers",
);

assert(
  salarySuitabilityForRole(
    classifyOrganizationRole(
      "Financial Services",
      "pawaPay",
      "pawapay.io",
    ),
  ) === "REJECT",
  "payment-gateway intermediaries must not surface as possible salary",
);

assert(
  classifyOrganizationRole(
    "Transportation",
    "Sample Mobility Ltd",
    "sample.example",
  ) === "TRANSPORT",
  "transport companies should be recognized as transaction intermediaries",
);

console.log(
  "Entity enrichment Worker contract tests passed.",
);
