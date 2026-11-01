const MAX_DESCRIPTOR_LENGTH = 80;
const MAX_REQUEST_BYTES = 4096;
const MAX_PROVIDER_RESPONSE_BYTES = 65536;
const PROVIDER_DEADLINE_MS = 5000;

class BodyTooLarge extends Error {}

async function boundedJson(message, maxBytes, signal) {
  if (signal?.aborted) {
    void message.body?.cancel().catch(() => {});
    throw new Error("Provider deadline exceeded");
  }
  const length = message.headers.get("content-length");
  if (length && /^\d+$/.test(length) && Number(length) > maxBytes) {
    void message.body?.cancel().catch(() => {});
    throw new BodyTooLarge();
  }
  if (!message.body) throw new SyntaxError("Missing JSON body");
  const reader = message.body.getReader();
  const abort = () => { void reader.cancel().catch(() => {}); };
  signal?.addEventListener("abort", abort, { once: true });
  const chunks = [];
  let size = 0;
  try {
    while (true) {
      if (signal?.aborted) throw new Error("Provider deadline exceeded");
      const { done, value } = await reader.read();
      if (signal?.aborted) throw new Error("Provider deadline exceeded");
      if (done) break;
      size += value.byteLength;
      if (size > maxBytes) {
        void reader.cancel().catch(() => {});
        throw new BodyTooLarge();
      }
      chunks.push(value);
    }
    const bytes = new Uint8Array(size);
    let offset = 0;
    for (const chunk of chunks) { bytes.set(chunk, offset); offset += chunk.byteLength; }
    return JSON.parse(new TextDecoder("utf-8", { fatal: true }).decode(bytes));
  } finally {
    signal?.removeEventListener("abort", abort);
    reader.releaseLock();
  }
}

function json(body, status = 200, extraHeaders = {}) {
  return new Response(JSON.stringify(body), {
    status,
    headers: {
      "content-type": "application/json; charset=utf-8",
      "cache-control": "no-store",
      ...extraHeaders,
    },
  });
}

const CLIENT_ID_PATTERN =
  /^[a-f0-9]{8}-[a-f0-9]{4}-4[a-f0-9]{3}-[89ab][a-f0-9]{3}-[a-f0-9]{12}$/i;

function resolveClientRateKey(request) {
  const raw = request.headers
    .get("x-pesatrack-client-id")
    ?.trim();

  if (!raw || !CLIENT_ID_PATTERN.test(raw)) {
    return "client:anonymous";
  }

  return `client:${raw.toLowerCase()}`;
}

// Supplied by Cloudflare on visitor requests. This is a supplementary abuse
// hint, not a user identity: many mobile users can share one public IP.
function resolveConnectionRateKey(request) {
  const ip = request.headers.get("cf-connecting-ip")?.trim();
  return ip && ip.length <= 45 && /^[0-9a-f:.]+$/i.test(ip)
    ? `ip:${ip.toLowerCase()}`
    : "ip:unknown";
}

async function enforceResolveRateLimit(
  request,
  env,
) {
  const clientLimiter =
    env.ENTITY_RESOLVE_CLIENT_RATE_LIMITER;
  const ipLimiter =
    env.ENTITY_RESOLVE_IP_RATE_LIMITER;
  const edgeLimiter =
    env.ENTITY_RESOLVE_EDGE_RATE_LIMITER;

  if (
    !clientLimiter ||
    typeof clientLimiter.limit !== "function" ||
    !ipLimiter ||
    typeof ipLimiter.limit !== "function" ||
    !edgeLimiter ||
    typeof edgeLimiter.limit !== "function"
  ) {
    return json(
      { error: "Rate limiter is not configured." },
      503,
    );
  }

  try {
    const clientResult =
      await clientLimiter.limit({
        key: resolveClientRateKey(request),
      });

    if (!clientResult.success) {
      return json(
        { error: "Too many enrichment requests." },
        429,
        { "retry-after": "60" },
      );
    }

    const ipResult = await ipLimiter.limit({
      key: resolveConnectionRateKey(request),
    });
    if (!ipResult.success) {
      return json(
        { error: "Too many enrichment requests." },
        429,
        { "retry-after": "60" },
      );
    }

    // Only requests allowed by both caller buckets may spend this shared
    // edge-location allowance; rejections must not starve other callers.
    const edgeResult = await edgeLimiter.limit({
      key: "v1-entity-resolve",
    });
    if (!edgeResult.success) {
      return json(
        { error: "Enrichment is temporarily busy." },
        429,
        { "retry-after": "60" },
      );
    }
  } catch {
    return json(
      { error: "Rate limiter is temporarily unavailable." },
      503,
    );
  }

  return null;
}

function normalizeWhitespace(value) {
  return value.replace(/\s+/g, " ").trim();
}

function looksSensitive(value) {
  const transactionCode = /\b(?=[A-Z0-9]*\d)[A-Z0-9]{10}\b/i;
  const phone = /(?:\+?254|0)[17]\d{8}/;
  const longNumber = /\d{4,}/;
  const email = /[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,}/i;

  return (
    transactionCode.test(value) ||
    phone.test(value) ||
    longNumber.test(value) ||
    email.test(value)
  );
}

// Only known public brand labels may omit a recognizable business-type term.
// Unknown names stay local even when they contain generic words like 'mobile'.
const knownPublicNames = new Set([
  "java house",
  "kplc prepaid",
  "safaricom",
  "quickmart",
  "strathmore university",
  "zuri properties",
]);

const strongBusinessSignals = new Set([
  "bank",
  "sacco",
  "supermarket",
  "mart",
  "shop",
  "store",
  "restaurant",
  "cafe",
  "coffee",
  "hotel",
  "pharmacy",
  "hospital",
  "clinic",
  "hardware",
  "electronics",
  "limited",
  "ltd",
  "company",
  "insurance",
  "school",
  "college",
  "university",
  "logistics",
  "transport",
  "energy",
  "power",
  "water",
  "telecom",
  "internet",
  "mall",
  "fuel",
  "petroleum",
  "airways",
  "airline",
]);

function isClearlyCommercial(value) {
  const normalized = value
    .toLowerCase()
    .replace(/[^a-z&'’\- ]/g, " ")
    .replace(/\s+/g, " ")
    .trim();

  if (knownPublicNames.has(normalized)) return true;

  const words = normalized.split(" ").filter(Boolean);
  if (words.length < 2 || words.length > 6) return false;

  if (words.some((word) => word.length === 10 && !strongBusinessSignals.has(word))) {
    return false;
  }

  return strongBusinessSignals.has(words[words.length - 1]);
}

export function sanitizeDescriptor(raw) {
  if (typeof raw !== "string") return null;

  const cleaned = normalizeWhitespace(raw);
  if (
    cleaned.length < 3 ||
    cleaned.length > MAX_DESCRIPTOR_LENGTH ||
    !/^[A-Za-z&'()./+\- ]+$/.test(cleaned) ||
    looksSensitive(cleaned)
  ) {
    return null;
  }

  const sanitized = cleaned;

  return sanitized.length >= 3 &&
    isClearlyCommercial(sanitized) ? sanitized : null;
}

function mapCategory(raw) {
  if (typeof raw !== "string") return null;

  const value = raw
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, " ")
    .replace(/\s+/g, " ")
    .trim();

  if (
    value.includes("grocery") ||
    value.includes("groceries") ||
    value.includes("supermarket")
  ) {
    return "GROCERIES";
  }

  if (
    value.includes("food") ||
    value.includes("restaurant") ||
    value.includes("dining") ||
    value.includes("cafe") ||
    value.includes("coffee")
  ) {
    return "EATING_OUT";
  }

  if (
    value.includes("transport") ||
    value.includes("transportation") ||
    value.includes("taxi") ||
    value.includes("rideshare") ||
    value.includes("ride share")
  ) {
    return "TRANSPORT";
  }

  if (
    value.includes("entertainment") ||
    value.includes("streaming") ||
    value.includes("cinema")
  ) {
    return "ENTERTAINMENT";
  }

  if (
    value.includes("shopping") ||
    value.includes("retail")
  ) {
    return "SHOPPING";
  }

  if (
    value.includes("health") ||
    value.includes("healthcare") ||
    value.includes("pharmacy")
  ) {
    return "HEALTH";
  }

  if (value.includes("education")) {
    return "EDUCATION";
  }

  if (
    value.includes("personal care") ||
    value.includes("beauty")
  ) {
    return "PERSONAL_CARE";
  }

  if (
    value.includes("telecom") ||
    value.includes("mobile") ||
    value.includes("communications")
  ) {
    return "PHONE_AND_DATA";
  }

  // "Utilities" is intentionally not auto-mapped: it does not
  // distinguish electricity, water and internet safely.
  return null;
}

export function classifyOrganizationRole(
  rawCategory,
  canonicalName,
  domain,
) {
  const value = [
    rawCategory,
    canonicalName,
    domain,
  ]
    .filter((part) => typeof part === "string")
    .join(" ")
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, " ")
    .replace(/\s+/g, " ")
    .trim();

  const paymentSignals = [
    "payment processing",
    "payment processor",
    "payment gateway",
    "merchant acquiring",
    "merchant services",
    "payments platform",
    "mobile money payments",
    "payment service provider",
  ];

  if (
    paymentSignals.some((signal) =>
      value.includes(signal)
    )
  ) {
    return "PAYMENT_PROCESSOR";
  }

  const financeCategory =
    value.includes("financial services") ||
    value.includes("fintech");
  const brandIdentity = [
    canonicalName,
    domain,
  ]
    .filter((part) => typeof part === "string")
    .join(" ")
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, "");

  if (
    financeCategory &&
    (
      brandIdentity.includes("pay") ||
      brandIdentity.includes("wallet") ||
      brandIdentity.includes("gateway")
    )
  ) {
    return "PAYMENT_PROCESSOR";
  }

  if (
    [
      "transport",
      "transportation",
      "taxi",
      "ride share",
      "rideshare",
      "mobility",
    ].some((signal) => value.includes(signal))
  ) {
    return "TRANSPORT";
  }

  if (
    [
      "retail",
      "shopping",
      "supermarket",
      "restaurant",
      "food",
      "hospital",
      "pharmacy",
      "hotel",
      "merchant",
    ].some((signal) => value.includes(signal))
  ) {
    return "MERCHANT";
  }

  if (
    [
      "bank",
      "banking",
      "financial institution",
      "credit union",
      "sacco",
      "insurance",
    ].some((signal) => value.includes(signal))
  ) {
    return "FINANCIAL_INSTITUTION";
  }

  if (
    [
      "employment",
      "employer",
      "staffing",
      "human resources",
      "payroll services",
    ].some((signal) => value.includes(signal))
  ) {
    return "EMPLOYER_LIKE";
  }

  return "UNKNOWN";
}

export function salarySuitabilityForRole(role) {
  if (
    role === "PAYMENT_PROCESSOR" ||
    role === "TRANSPORT" ||
    role === "MERCHANT"
  ) {
    return "REJECT";
  }

  if (role === "EMPLOYER_LIKE") {
    return "ALLOW";
  }

  return "REVIEW";
}

async function resolveMerchant(descriptor, env) {
  const controller = new AbortController();
  let timer;
  const deadline = new Promise((_, reject) => {
    timer = setTimeout(() => {
      controller.abort();
      reject(new Error("Provider deadline exceeded"));
    }, PROVIDER_DEADLINE_MS);
  });
  try {
    return await Promise.race([
      resolveMerchantWithinDeadline(descriptor, env, controller.signal),
      deadline,
    ]);
  } finally {
    clearTimeout(timer);
  }
}

async function resolveMerchantWithinDeadline(descriptor, env, signal) {
  if (!env.LOGO_DEV_SECRET_KEY) {
    return {
      error: "Merchant provider is not configured.",
      status: 503,
    };
  }

  const response = await fetch(
    "https://api.logo.dev/enrich/transaction",
    {
      method: "POST",
      signal,
      headers: {
        authorization: `Bearer ${env.LOGO_DEV_SECRET_KEY}`,
        "content-type": "application/json",
        accept: "application/json",
      },
      body: JSON.stringify({
        description: descriptor,
      }),
    },
  );

  if (!response.ok) {
    return {
      error: "Merchant provider request failed.",
      status: 502,
    };
  }

  const data = await boundedJson(response, MAX_PROVIDER_RESPONSE_BYTES, signal);
  const canonicalName =
    typeof data.merchant === "string"
      ? normalizeWhitespace(data.merchant)
      : "";
  const domain =
    typeof data.domain === "string"
      ? data.domain.trim()
      : null;
  const logoUrl =
    typeof data.logo === "string"
      ? data.logo.trim()
      : null;
  const categoryHint = mapCategory(data.category);
  const organizationRole =
    classifyOrganizationRole(
      data.category,
      canonicalName,
      domain,
    );
  const salaryCandidateSuitability =
    salarySuitabilityForRole(
      organizationRole,
    );
  const ambiguous = data.ambiguous === true;

  if (
    canonicalName.length < 2 ||
    canonicalName.length > 80
  ) {
    return {
      error: "Merchant could not be resolved safely.",
      status: 404,
    };
  }

  const confidence =
    !ambiguous && domain && categoryHint
      ? "HIGH"
      : "LIKELY";

  return {
    value: {
      canonicalName,
      domain,
      logoUrl,
      categoryHint,
      confidence,
      source: "logo_dev",
      organizationRole,
      salaryCandidateSuitability,
    },
    status: 200,
  };
}

export default {
  async fetch(request, env) {
    const url = new URL(request.url);

    if (
      request.method === "GET" &&
      url.pathname === "/v1/entity/health"
    ) {
      return json({
        status: "ok",
        service: "pesatrack-entity-enrichment",
        privacyMode: "sanitized-descriptor-only",
        providerConfigured:
          Boolean(env.LOGO_DEV_SECRET_KEY),
      });
    }

    if (
      request.method !== "POST" ||
      url.pathname !== "/v1/entity/resolve"
    ) {
      return json(
        { error: "Not found." },
        404,
      );
    }

    const limited =
      await enforceResolveRateLimit(
        request,
        env,
      );
    if (limited) {
      return limited;
    }

    let payload;
    try {
      payload = await boundedJson(request, MAX_REQUEST_BYTES);
    } catch (error) {
      if (error instanceof BodyTooLarge) {
        return json({ error: "Request body is too large." }, 413);
      }
      return json(
        { error: "Invalid JSON." },
        400,
      );
    }

    const descriptor =
      sanitizeDescriptor(payload?.descriptor);

    if (!descriptor) {
      return json(
        {
          error:
            "Descriptor rejected by privacy policy.",
        },
        400,
      );
    }

    try {
      const result =
        await resolveMerchant(
          descriptor,
          env,
        );

      if (!result.value) {
        return json(
          { error: result.error },
          result.status,
        );
      }

      return json(result.value);
    } catch {
      return json(
        {
          error:
            "Enrichment is temporarily unavailable.",
        },
        503,
      );
    }
  },
};
