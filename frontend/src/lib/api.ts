const TOKEN_KEY = "dc_access_token";

export function getToken(): string | null {
  if (typeof window === "undefined") return null;
  return localStorage.getItem(TOKEN_KEY);
}

export function setToken(token: string): void {
  localStorage.setItem(TOKEN_KEY, token);
}

export function clearToken(): void {
  localStorage.removeItem(TOKEN_KEY);
}

export class ApiError extends Error {
  status: number;
  body: unknown;

  constructor(status: number, message: string, body?: unknown) {
    super(message);
    this.name = "ApiError";
    this.status = status;
    this.body = body;
  }
}

type RequestOptions = Omit<RequestInit, "body"> & {
  body?: unknown;
  auth?: boolean;
};

export async function api<T = unknown>(
  path: string,
  options: RequestOptions = {},
): Promise<T> {
  const { body, auth = true, headers, ...rest } = options;
  const reqHeaders = new Headers(headers);

  if (body !== undefined && !reqHeaders.has("Content-Type")) {
    reqHeaders.set("Content-Type", "application/json");
  }

  if (auth) {
    const token = getToken();
    if (token) {
      reqHeaders.set("Authorization", `Bearer ${token}`);
    }
  }

  const res = await fetch(path.startsWith("/") ? path : `/${path}`, {
    ...rest,
    headers: reqHeaders,
    body: body === undefined ? undefined : JSON.stringify(body),
  });

  if (res.status === 401 && auth) {
    clearToken();
    if (typeof window !== "undefined" && !window.location.pathname.startsWith("/login")) {
      window.location.href = "/login";
    }
  }

  const text = await res.text();
  let data: unknown = null;
  if (text) {
    try {
      data = JSON.parse(text);
    } catch {
      data = text;
    }
  }

  if (!res.ok) {
    const message =
      typeof data === "object" &&
      data !== null &&
      "message" in data &&
      typeof (data as { message: unknown }).message === "string"
        ? (data as { message: string }).message
        : `APIエラー (${res.status})`;
    throw new ApiError(res.status, message, data);
  }

  return data as T;
}

/** 単位・金額が未設定のとき表示する固定文言（偽の報酬額を出さない） */
export function masterOrUnset(value: number | string | null | undefined): string {
  if (value === null || value === undefined || value === "") {
    return "制度マスタ未設定";
  }
  return String(value);
}
