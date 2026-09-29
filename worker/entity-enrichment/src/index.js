const MAX_DESCRIPTOR_LENGTH = 80;

function json(body, status = 200) {
  return new Response(JSON.stringify(body), {
    status,
    headers: {
      "content-type": "application/json; charset=utf-8",
      "cache-control": "no-store",
    },
  });
}

function normalizeWhitespace(value) {
  return value.replace(/\s+/g, " ").trim();
}

function looksSensitive(value) {
  const transactionCode = /\b[A-Z0-9]{10}\b/i;
  const phone = /(?:\+?254|0)[17]\d{8}/;
  const longNumber = /\d{5,}/;
  const email = /[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,}/i;

  return (
    transactionCode.test(value) ||
    phone.test(value) ||
    longNumber.test(value) ||
    email.test(value)
  );
}

const businessSignals = new Set([
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
  "services",
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
  "mobile",
  "internet",
  "mall",
  "fuel",
  "petroleum",
  "airways",
  "airline",
]);

function looksLikePersonalName(value) {
  if (/\d/.test(value)) return false;

  const words = value
    .toLowerCase()
    .replace(/[^a-z&'’\- ]/g, " ")
    .replace(/\s+/g, " ")
    .trim()
    .split(" ")
    .filter(Boolean);

  if (words.length < 2 || words.length > 4) return false;
  if (words.some((word) => businessSignals.has(word))) return false;

  return words.every(
    (word) =>
      word.length >= 2 &&
      /^[a-z'’\-]+$/i.test(word),
  );
}

function sanitizeDescriptor(raw) {
  if (typeof raw !== "string") return null;

  const cleaned = normalizeWhitespace(raw);
  if (
    cleaned.length < 3 ||
    cleaned.length > MAX_DESCRIPTOR_LENGTH ||
    looksSensitive(cleaned) ||
    looksLikePersonalName(cleaned)
  ) {
    return null;
  }

  const sanitized = normalizeWhitespace(
    cleaned.replace(/[^A-Za-z0-9&'’()./+\- ]/g, " "),
  );

  return sanitized.length >= 3 ? sanitized : null;
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

async function resolveMerchant(descriptor, env) {
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

  const data = await response.json();
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
    },
    status: 200,
  };
}

export default {
  async fetch(request, env) {
    const url = new URL(request.url);

    if (
      request.method !== "POST" ||
      url.pathname !== "/v1/entity/resolve"
    ) {
      return json(
        { error: "Not found." },
        404,
      );
    }

    let payload;
    try {
      payload = await request.json();
    } catch {
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
