import { describe, expect, it } from "vitest";
import type { Beneficiary, BillingBatchStatus } from "./types";

describe("types compile-time contracts", () => {
  it("accepts valid beneficiary shape", () => {
    const b: Beneficiary = {
      id: "1",
      anonymizedCode: "U-0001",
      category: "ADULT",
      municipalityCode: "999001",
      status: "ACTIVE",
    };
    expect(b.category).toBe("ADULT");
  });

  it("billing batch statuses are known", () => {
    const statuses: BillingBatchStatus[] = [
      "DRAFT",
      "CALCULATED",
      "VALIDATED",
      "CONFIRMED",
      "EXPORTED",
    ];
    expect(statuses).toHaveLength(5);
  });
});
