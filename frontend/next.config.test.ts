import { describe, expect, it } from "vitest";
import nextConfig from "./next.config";

describe("next.config", () => {
  it("rewrites actuator to backend URL", async () => {
    const rewrites =
      typeof nextConfig.rewrites === "function"
        ? await nextConfig.rewrites()
        : nextConfig.rewrites;
    expect(rewrites).toEqual(
      expect.arrayContaining([
        expect.objectContaining({
          source: "/actuator/:path*",
          destination: expect.stringMatching(/\/actuator\/:path\*$/),
        }),
      ]),
    );
  });
});
