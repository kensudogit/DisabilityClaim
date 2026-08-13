export const runtime = "nodejs";
export const dynamic = "force-dynamic";

const BACKEND =
  process.env.BACKEND_INTERNAL_URL ||
  process.env.BACKEND_URL ||
  "http://127.0.0.1:8080";

/** 値そのものは返さず、設定済みかどうかだけを返す（秘密情報を露出しない） */
function isSet(name: string): boolean {
  const value = process.env[name];
  return typeof value === "string" && value.length > 0;
}

export async function GET(): Promise<Response> {
  let backendStatus: number | null = null;
  let backendBody: string | null = null;
  let backendError: string | null = null;

  try {
    const res = await fetch(`${BACKEND}/actuator/health`, {
      cache: "no-store",
      signal: AbortSignal.timeout(5000),
    });
    backendStatus = res.status;
    backendBody = (await res.text()).slice(0, 500);
  } catch (error) {
    backendError = error instanceof Error ? error.message : String(error);
  }

  return Response.json({
    frontend: "ok",
    backendBaseUrl: BACKEND,
    backendStatus,
    backendBody,
    backendError,
    envConfigured: {
      DATABASE_URL: isSet("DATABASE_URL"),
      SPRING_DATASOURCE_URL: isSet("SPRING_DATASOURCE_URL"),
      DATABASE_USERNAME: isSet("DATABASE_USERNAME"),
      DATABASE_PASSWORD: isSet("DATABASE_PASSWORD"),
      JWT_SECRET: isSet("JWT_SECRET"),
      PORT: isSet("PORT"),
    },
    hint:
      backendError !== null
        ? "バックエンドが起動していません。Railway に PostgreSQL を追加し DATABASE_URL / DATABASE_USERNAME / DATABASE_PASSWORD を設定してください。"
        : "バックエンドは応答しています。",
  });
}
