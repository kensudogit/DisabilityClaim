import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

function makeRequest(method: string, url: string, body?: BodyInit | null) {
  return new Request(url, { method, body: body ?? undefined });
}

describe("api/v1 proxy route", () => {
  beforeEach(() => {
    vi.resetModules();
    vi.stubGlobal("fetch", vi.fn());
    process.env.BACKEND_INTERNAL_URL = "http://127.0.0.1:18080";
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it("proxies GET to backend and returns upstream status", async () => {
    (fetch as unknown as ReturnType<typeof vi.fn>).mockResolvedValue(
      new Response(JSON.stringify([{ id: "1" }]), {
        status: 200,
        headers: { "content-type": "application/json" },
      }),
    );
    const { GET } = await import("./route");
    const req = makeRequest(
      "GET",
      "http://localhost:3000/api/v1/beneficiaries?status=ACTIVE",
    ) as unknown as import("next/server").NextRequest;
    Object.defineProperty(req, "nextUrl", {
      value: new URL("http://localhost:3000/api/v1/beneficiaries?status=ACTIVE"),
    });
    Object.defineProperty(req, "headers", {
      value: new Headers({ host: "localhost:3000", authorization: "Bearer t" }),
    });

    const res = await GET(req, {
      params: Promise.resolve({ path: ["beneficiaries"] }),
    });
    expect(res.status).toBe(200);
    expect(fetch).toHaveBeenCalledWith(
      "http://127.0.0.1:18080/api/v1/beneficiaries?status=ACTIVE",
      expect.objectContaining({ method: "GET", cache: "no-store" }),
    );
  });

  it("returns 503 JSON when backend is unreachable", async () => {
    (fetch as unknown as ReturnType<typeof vi.fn>).mockRejectedValue(
      new Error("ECONNREFUSED"),
    );
    const { GET } = await import("./route");
    const req = makeRequest("GET", "http://localhost:3000/api/v1/beneficiaries") as unknown as import("next/server").NextRequest;
    Object.defineProperty(req, "nextUrl", {
      value: new URL("http://localhost:3000/api/v1/beneficiaries"),
    });
    Object.defineProperty(req, "headers", { value: new Headers() });

    const res = await GET(req, {
      params: Promise.resolve({ path: ["beneficiaries"] }),
    });
    expect(res.status).toBe(503);
    const body = await res.json();
    expect(body.message).toContain("バックエンドAPIに接続できません");
    expect(body.diagnostics).toBe("/api/diag");
    expect(body.detail).toContain("ECONNREFUSED");
  });

  it("exports HTTP methods", async () => {
    const mod = await import("./route");
    expect(mod.GET).toBeTypeOf("function");
    expect(mod.POST).toBeTypeOf("function");
    expect(mod.PUT).toBeTypeOf("function");
    expect(mod.PATCH).toBeTypeOf("function");
    expect(mod.DELETE).toBeTypeOf("function");
    expect(mod.HEAD).toBeTypeOf("function");
    expect(mod.OPTIONS).toBeTypeOf("function");
  });
});
