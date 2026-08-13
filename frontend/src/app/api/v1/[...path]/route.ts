import type { NextRequest } from "next/server";

export const runtime = "nodejs";
export const dynamic = "force-dynamic";

const BACKEND =
  process.env.BACKEND_INTERNAL_URL ||
  process.env.BACKEND_URL ||
  "http://127.0.0.1:8080";

const HOP_BY_HOP = ["host", "connection", "content-length", "accept-encoding"];

async function proxy(
  request: NextRequest,
  context: { params: Promise<{ path: string[] }> },
): Promise<Response> {
  const { path } = await context.params;
  const target = `${BACKEND}/api/v1/${path.join("/")}${request.nextUrl.search}`;

  const headers = new Headers(request.headers);
  for (const name of HOP_BY_HOP) {
    headers.delete(name);
  }

  const hasBody = request.method !== "GET" && request.method !== "HEAD";

  try {
    const upstream = await fetch(target, {
      method: request.method,
      headers,
      body: hasBody ? request.body : undefined,
      redirect: "manual",
      cache: "no-store",
      // Node の fetch でストリーム body を送るために必要
      duplex: "half",
    } as RequestInit & { duplex: "half" });

    const responseHeaders = new Headers(upstream.headers);
    responseHeaders.delete("content-encoding");
    responseHeaders.delete("content-length");
    responseHeaders.delete("transfer-encoding");

    return new Response(upstream.body, {
      status: upstream.status,
      headers: responseHeaders,
    });
  } catch (error) {
    // バックエンド未起動時に素の 500 を返すと原因が分からないため、理由を明示する
    return Response.json(
      {
        message:
          "バックエンドAPIに接続できません。DB接続設定（DATABASE_URL 等）とバックエンドの起動状態を確認してください。",
        detail: error instanceof Error ? error.message : String(error),
        backendBaseUrl: BACKEND,
        diagnostics: "/api/diag",
      },
      { status: 503 },
    );
  }
}

export const GET = proxy;
export const POST = proxy;
export const PUT = proxy;
export const PATCH = proxy;
export const DELETE = proxy;
export const HEAD = proxy;
export const OPTIONS = proxy;
