import { describe, expect, it, vi, beforeEach, afterEach } from "vitest";

describe("api/diag route", () => {
  beforeEach(() => {
    vi.resetModules();
    vi.stubGlobal("fetch", vi.fn());
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it("returns frontend ok and backend status", async () => {
    (fetch as unknown as ReturnType<typeof vi.fn>).mockResolvedValue({
      status: 200,
      text: async () => '{"status":"UP"}',
    });
    process.env.BACKEND_INTERNAL_URL = "http://127.0.0.1:18080";
    const { GET } = await import("./route");
    const res = await GET();
    const body = await res.json();
    expect(body.frontend).toBe("ok");
    expect(body.backendStatus).toBe(200);
    expect(body.backendBaseUrl).toBe("http://127.0.0.1:18080");
  });

  it("captures backend connection error", async () => {
    (fetch as unknown as ReturnType<typeof vi.fn>).mockRejectedValue(
      new Error("ECONNREFUSED"),
    );
    process.env.BACKEND_INTERNAL_URL = "http://127.0.0.1:18080";
    const { GET } = await import("./route");
    const res = await GET();
    const body = await res.json();
    expect(body.frontend).toBe("ok");
    expect(body.backendStatus).toBeNull();
    expect(body.backendError).toContain("ECONNREFUSED");
  });
});
