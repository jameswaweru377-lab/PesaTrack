import worker, {
  classifyOrganizationRole,
  salarySuitabilityForRole,
} from "./src/index.js";

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

assert(
  classifyOrganizationRole(
    "Payment Processing",
    "Sample Payments Ltd",
    "samplepay.example"
  ) === "PAYMENT_PROCESSOR",
  "payment processors must be identified before salary inference"
);

assert(
  salarySuitabilityForRole(
    "PAYMENT_PROCESSOR"
  ) === "REJECT",
  "payment processors must not be suggested as employers"
);

assert(
  classifyOrganizationRole(
    "Financial Services",
    "AcmePay",
    "acmepay.example"
  ) === "PAYMENT_PROCESSOR",
  "financial-service payment brands should be screened as payment processors"
);


assert(
  classifyOrganizationRole(
    "Financial Services",
    "pawaPay",
    "pawapay.io"
  ) === "PAYMENT_PROCESSOR",
  "payment-gateway brands like pawaPay must be rejected as salary employers"
);

assert(
  salarySuitabilityForRole(
    classifyOrganizationRole(
      "Financial Services",
      "pawaPay",
      "pawapay.io"
    )
  ) === "REJECT",
  "payment-gateway intermediaries must not surface as possible salary"
);

assert(
  classifyOrganizationRole(
    "Transportation",
    "Sample Mobility Ltd",
    "sample.example"
  ) === "TRANSPORT",
  "transport companies should be recognized as transaction intermediaries"
);

console.log(
  "Entity enrichment Worker contract tests passed."
);
