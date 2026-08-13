import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import {
  ApiError,
  api,
  clearToken,
  getToken,
  masterOrUnset,
  setToken,
} from "./api";

describe("masterOrUnset", () => {
  it("returns 制度マスタ未設定 for nullish and empty", () => {
    expect(masterOrUnset(null)).toBe("制度マスタ未設定");
    expect(masterOrUnset(undefined)).toBe("制度マスタ未設定");
    expect(masterOrUnset("")).toBe("制度マスタ未設定");
  });

  it("stringifies numbers and strings", () => {
    expect(masterOrUnset(0)).toBe("0");
    expect(masterOrUnset(1234)).toBe("1234");
    expect(masterOrUnset("500")).toBe("500");
  });
});

describe("token helpers", () => {
  beforeEach(() => {
    localStorage.clear();
  });

  it("set/get/clear round trip", () => {
    expect(getToken()).toBeNull();
    setToken("abc");
    expect(getToken()).toBe("abc");
    clearToken();
    expect(getToken()).toBeNull();
  });
});

describe("api()", () => {
  beforeEach(() => {
    localStorage.clear();
    vi.stubGlobal("fetch", vi.fn());
    // jsdom location is read-only-ish; redefine pathname safely
    Object.defineProperty(window, "location", {
      configurable: true,
      value: { pathname: "/beneficiaries", href: "http://localhost/beneficiaries" },
    });
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it("throws 401 when auth required but token missing", async () => {
    await expect(api("/api/v1/beneficiaries")).rejects.toMatchObject({
      status: 401,
      message: "ログインが必要です",
    });
  });

  it("sends Authorization bearer and returns JSON", async () => {
    setToken("tok");
    (fetch as unknown as ReturnType<typeof vi.fn>).mockResolvedValue({
      ok: true,
      status: 200,
      text: async () => JSON.stringify({ items: [] }),
    });

    const data = await api<{ items: unknown[] }>("/api/v1/beneficiaries");
    expect(data.items).toEqual([]);
    expect(fetch).toHaveBeenCalledWith(
      "/api/v1/beneficiaries",
      expect.objectContaining({
        headers: expect.any(Headers),
      }),
    );
    const headers = (fetch as unknown as ReturnType<typeof vi.fn>).mock.calls[0][1]
      .headers as Headers;
    expect(headers.get("Authorization")).toBe("Bearer tok");
  });

  it("throws ApiError with server message on failure", async () => {
    setToken("tok");
    (fetch as unknown as ReturnType<typeof vi.fn>).mockResolvedValue({
      ok: false,
      status: 500,
      text: async () => JSON.stringify({ message: "DB error" }),
    });

    await expect(api("/api/v1/x")).rejects.toBeInstanceOf(ApiError);
    await expect(api("/api/v1/x")).rejects.toMatchObject({
      status: 500,
      message: "DB error",
    });
  });

  it("skips auth header when auth=false", async () => {
    (fetch as unknown as ReturnType<typeof vi.fn>).mockResolvedValue({
      ok: true,
      status: 200,
      text: async () => JSON.stringify({ accessToken: "t" }),
    });
    await api("/api/v1/auth/login", {
      auth: false,
      method: "POST",
      body: { username: "admin", password: "x" },
    });
    const headers = (fetch as unknown as ReturnType<typeof vi.fn>).mock.calls[0][1]
      .headers as Headers;
    expect(headers.get("Authorization")).toBeNull();
    expect(headers.get("Content-Type")).toBe("application/json");
  });
});
