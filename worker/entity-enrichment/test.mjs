import worker, {
  classifyOrganizationRole,
  salarySuitabilityForRole,
  sanitizeDescriptor,
} from "./src/index.js";
import { readFileSync } from "node:fs";

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
  ENTITY_RESOLVE_IP_RATE_LIMITER:
    allowLimiter(),
  ENTITY_RESOLVE_EDGE_RATE_LIMITER:
    allowLimiter(),
};

// GitHub Actions deploys the worker-local config, while Cloudflare Git Builds
// can deploy from the repository root. Both must bind the same limiters.
const localWrangler = JSON.parse(readFileSync(
  new URL("./wrangler.jsonc", import.meta.url),
  "utf8",
));
const rootWrangler = JSON.parse(readFileSync(
  new URL("../../wrangler.jsonc", import.meta.url),
  "utf8",
));
const limiterDefinitions = (config) =>
  config.ratelimits
    .map(({ name, namespace_id, simple }) => ({
      name, namespace_id, simple,
    }))
    .sort((left, right) => left.name.localeCompare(right.name));
assert(
  JSON.stringify(limiterDefinitions(rootWrangler)) ===
    JSON.stringify(limiterDefinitions(localWrangler)),
  "root and worker-local Wrangler rate-limit bindings must match",
);

const privacyCorpus = JSON.parse(
  readFileSync(
    new URL(
      "../../app/src/test/resources/merchant-descriptor-privacy-v1.json",
      import.meta.url,
    ),
    "utf8",
  ),
);
assert(
  privacyCorpus.length >= 60,
  "shared privacy corpus must cover at least 60 descriptors",
);

const CLIENT_ID =
  "7fbe6f76-c82a-4d44-9f1a-cf36bf9db905";

function resolveRequest(
  descriptor = "ACME HARDWARE LTD",
  clientId = CLIENT_ID,
  ip = "198.51.100.42",
) {
  const headers = {
    "content-type": "application/json",
  };
  if (clientId !== null) {
    headers["x-pesatrack-client-id"] =
      clientId;
  }
  if (ip !== null) {
    headers["cf-connecting-ip"] = ip;
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

for (const { input, expected } of privacyCorpus) {
  assert(
    sanitizeDescriptor(input) === expected,
    `Worker sanitizer returned different text for ${JSON.stringify(input)}`,
  );
  const response = await worker.fetch(
    resolveRequest(input),
    allowedEnv,
  );
  assert(
    response.status === (expected === null ? 400 : 503),
    `Worker descriptor filter disagrees for ${JSON.stringify(input)}`,
  );
}

let observedClientKey = null;
let observedEdgeKey = null;
const keyedEnv = {
  ENTITY_RESOLVE_CLIENT_RATE_LIMITER:
    allowLimiter((key) => {
      observedClientKey = key;
    }),
  ENTITY_RESOLVE_IP_RATE_LIMITER:
    allowLimiter(),
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
    ENTITY_RESOLVE_IP_RATE_LIMITER:
      allowLimiter(),
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
    ENTITY_RESOLVE_IP_RATE_LIMITER:
      allowLimiter(),
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
    ENTITY_RESOLVE_IP_RATE_LIMITER:
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
    ENTITY_RESOLVE_IP_RATE_LIMITER:
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
    ENTITY_RESOLVE_IP_RATE_LIMITER:
      allowLimiter(),
    ENTITY_RESOLVE_EDGE_RATE_LIMITER:
      throwingLimiter,
  },
);
assert(
  limiterFailure.status === 503,
  "resolve endpoint should fail closed when Cloudflare limiter evaluation fails",
);

function countingLimiter(max) {
  const counts = new Map();
  const calls = [];
  return {
    calls,
    async limit({ key }) {
      calls.push(key);
      const count = (counts.get(key) ?? 0) + 1;
      counts.set(key, count);
      return { success: count <= max };
    },
  };
}

function inventedUuid(index) {
  return `ab000000-0000-4000-8000-${index.toString(16).padStart(12, "0")}`;
}

// A caller who has already exhausted its client allowance must not consume
// shared edge quota just by sending more rejected requests.
const fixedClient = countingLimiter(60);
const fixedIp = countingLimiter(300);
const fixedEdge = countingLimiter(600);
const fixedEnv = {
  ENTITY_RESOLVE_CLIENT_RATE_LIMITER: fixedClient,
  ENTITY_RESOLVE_IP_RATE_LIMITER: fixedIp,
  ENTITY_RESOLVE_EDGE_RATE_LIMITER: fixedEdge,
};
let fixedFinal;
for (let i = 0; i < 61; i++) {
  fixedFinal = await worker.fetch(resolveRequest(), fixedEnv);
}
assert(fixedFinal.status === 429, "61st fixed-client request must be denied");
assert(fixedEdge.calls.length === 60,
  "denied client request must not use shared edge allowance");

// Rotating untrusted install IDs must hit a separate Cloudflare-connection-IP
// budget. This uses documentation-only IP addresses, not any user device.
const rotatingClient = countingLimiter(60);
const rotatingIp = countingLimiter(300);
const rotatingEdge = countingLimiter(600);
const rotatingEnv = {
  ENTITY_RESOLVE_CLIENT_RATE_LIMITER: rotatingClient,
  ENTITY_RESOLVE_IP_RATE_LIMITER: rotatingIp,
  ENTITY_RESOLVE_EDGE_RATE_LIMITER: rotatingEdge,
};
let rotatingFinal;
for (let i = 0; i < 301; i++) {
  rotatingFinal = await worker.fetch(
    resolveRequest("ACME HARDWARE LTD", inventedUuid(i)),
    rotatingEnv,
  );
}
assert(rotatingFinal.status === 429,
  "301st rotated-ID request from one connection IP must be denied");
assert(rotatingEdge.calls.length === 300,
  "IP-denied requests must not use the shared edge allowance");
assert(new Set(rotatingIp.calls).size === 1,
  "all rotating UUIDs on one connection IP must use one IP bucket");

const twoIpClient = countingLimiter(60);
const twoIp = countingLimiter(300);
const twoIpEdge = countingLimiter(600);
const twoIpEnv = {
  ENTITY_RESOLVE_CLIENT_RATE_LIMITER: twoIpClient,
  ENTITY_RESOLVE_IP_RATE_LIMITER: twoIp,
  ENTITY_RESOLVE_EDGE_RATE_LIMITER: twoIpEdge,
};
for (let i = 0; i < 600; i++) {
  await worker.fetch(
    resolveRequest("ACME HARDWARE LTD", inventedUuid(1000 + i),
      i < 300 ? "198.51.100.42" : "198.51.100.43"),
    twoIpEnv,
  );
}
const edgeFinal = await worker.fetch(
  resolveRequest("ACME HARDWARE LTD", inventedUuid(2000), "198.51.100.44"),
  twoIpEnv,
);
assert(edgeFinal.status === 429,
  "two independent IP buckets must still share the final edge cap");
assert(new Set(twoIp.calls).size === 3,
  "separate Cloudflare connection IPs must not share the same IP bucket");

const noIpClient = countingLimiter(60);
const noIp = countingLimiter(300);
const noIpEdge = countingLimiter(600);
const noIpEnv = {
  ENTITY_RESOLVE_CLIENT_RATE_LIMITER: noIpClient,
  ENTITY_RESOLVE_IP_RATE_LIMITER: noIp,
  ENTITY_RESOLVE_EDGE_RATE_LIMITER: noIpEdge,
};
let unknownIpFinal;
for (let i = 0; i < 301; i++) {
  unknownIpFinal = await worker.fetch(
    resolveRequest("ACME HARDWARE LTD", inventedUuid(3000 + i), null),
    noIpEnv,
  );
}
assert(unknownIpFinal.status === 429,
  "requests without Cloudflare IP must share a bounded fallback bucket");
assert(new Set(noIp.calls).size === 1,
  "missing Cloudflare IP must not create per-UUID IP identities");

const missingIpBinding = await worker.fetch(resolveRequest(), {
  ENTITY_RESOLVE_CLIENT_RATE_LIMITER: allowLimiter(),
  ENTITY_RESOLVE_EDGE_RATE_LIMITER: allowLimiter(),
});
assert((await missingIpBinding.json()).error === "Rate limiter is not configured.",
  "missing IP limiter binding must fail closed before contacting provider");
const throwingIpBinding = await worker.fetch(resolveRequest(), {
  ENTITY_RESOLVE_CLIENT_RATE_LIMITER: allowLimiter(),
  ENTITY_RESOLVE_IP_RATE_LIMITER: throwingLimiter,
  ENTITY_RESOLVE_EDGE_RATE_LIMITER: allowLimiter(),
});
assert((await throwingIpBinding.json()).error === "Rate limiter is temporarily unavailable.",
  "throwing IP limiter must fail closed before contacting provider");

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
