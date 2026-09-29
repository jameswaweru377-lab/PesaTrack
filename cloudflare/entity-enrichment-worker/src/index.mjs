const JSON_HEADERS = {
  "content-type": "application/json; charset=utf-8",
};

export default {
  async fetch(request, env) {
    const url = new URL(request.url);

    if (
      request.method !== "POST" ||
      url.pathname !== "/v1/entities/resolve"
    ) {
      return json(
        { status: "not_found" },
        404
      );
    }

    let input;
    try {
      input = await request.json();
    } catch {
      return json(
        {
          status: "invalid_request",
          reason: "invalid_json",
        },
        400
      );
    }

    const descriptor =
      sanitizeDescriptor(input?.descriptor);

    if (!descriptor) {
      return json(
        {
          status: "invalid_request",
          reason: "unsafe_descriptor",
        },
        400
      );
    }

    if (!env.LOGO_DEV_API_KEY) {
      return json(
        {
          status: "unavailable",
          reason: "provider_not_configured",
        },
        503
      );
    }

    let upstream;
    try {
      upstream = await fetch(
        "https://api.logo.dev/enrich/transaction",
        {
          method: "POST",
          headers: {
            authorization:
              "Bearer " + env.LOGO_DEV_API_KEY,
            "content-type": "application/json",
            accept: "application/json",
          },
          body: JSON.stringify({
            description: descriptor,
          }),
        }
      );
    } catch {
      return json(
        {
          status: "unavailable",
          reason: "provider_network_error",
        },
        503
      );
    }

    if (
      upstream.status === 404 ||
      upstream.status === 204
    ) {
      return json(
        { status: "not_found" },
        404
      );
    }

    if (
      upstream.status === 408 ||
      upstream.status === 429 ||
      upstream.status >= 500
    ) {
      return json(
        {
          status: "unavailable",
          reason:
            "provider_http_" +
            upstream.status,
        },
        503
      );
    }

    if (!upstream.ok) {
      return json(
        {
          status: "not_found",
          reason:
            "provider_http_" +
            upstream.status,
        },
        404
      );
    }

    let data;
    try {
      data = await upstream.json();
    } catch {
      return json(
        {
          status: "unavailable",
          reason: "provider_invalid_json",
        },
        503
      );
    }

    if (data?.ambiguous === true) {
      return json(
        {
          status: "ambiguous",
          source: "logo_dev",
        },
        200
      );
    }

    const merchant =
      cleanText(data?.merchant, 96);
    const domain =
      cleanDomain(data?.domain);
    const category =
      cleanText(data?.category, 64);
    const logo =
      cleanHttpsUrl(data?.logo);

    if (!merchant) {
      return json(
        {
          status: "not_found",
          source: "logo_dev",
        },
        404
      );
    }

    const confidence =
      domain && category
        ? "high"
        : "likely";

    return json({
      status: "resolved",
      merchant,
      domain,
      logo,
      category,
      confidence,
      source: "logo_dev",
    });
  },
};

function sanitizeDescriptor(value) {
  if (typeof value !== "string") {
    return null;
  }

  const raw = value.trim();

  if (
    raw.length < 3 ||
    raw.length > 96 ||
    /[\r\n\t]/.test(raw) ||
    /(?:\+?254|0)[17]\d{8}/.test(raw) ||
    /\b(?:KES|KSH)\s*[0-9,]+(?:\.\d{1,2})?\b/i
      .test(raw) ||
    /\b[A-Z0-9]{10}\b/.test(raw)
  ) {
    return null;
  }

  const cleaned = raw
    .replace(/\s+/g, " ")
    .replace(
      /^[\s.,:;_-]+|[\s.,:;_-]+$/g,
      ""
    );

  if (
    cleaned.length < 3 ||
    (cleaned.match(/[A-Za-z]/g) || [])
      .length < 2
  ) {
    return null;
  }

  return cleaned;
}

function cleanText(value, maxLength) {
  if (typeof value !== "string") {
    return null;
  }

  const cleaned =
    value.replace(/\s+/g, " ").trim();

  if (!cleaned) {
    return null;
  }

  return cleaned.slice(0, maxLength);
}

function cleanDomain(value) {
  const cleaned = cleanText(value, 253);
  if (
    !cleaned ||
    !/^[A-Za-z0-9.-]+\.[A-Za-z]{2,}$/
      .test(cleaned)
  ) {
    return null;
  }
  return cleaned.toLowerCase();
}

function cleanHttpsUrl(value) {
  if (typeof value !== "string") {
    return null;
  }

  try {
    const url = new URL(value);
    return url.protocol === "https:"
      ? url.toString()
      : null;
  } catch {
    return null;
  }
}

function json(body, status = 200) {
  return new Response(
    JSON.stringify(body),
    {
      status,
      headers: JSON_HEADERS,
    }
  );
}
