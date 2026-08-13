import { beforeEach, describe, expect, it, vi } from "vitest";
import { render, screen } from "@testing-library/react";
import BeneficiaryDetailPage from "./page";

const api = vi.fn();

vi.mock("next/navigation", () => ({
  useParams: () => ({ id: "b1" }),
}));

vi.mock("next/link", () => ({
  default: ({ href, children }: { href: string; children: React.ReactNode }) => (
    <a href={href}>{children}</a>
  ),
}));

vi.mock("@/lib/api", () => ({
  api: (...args: unknown[]) => api(...args),
}));

describe("BeneficiaryDetailPage", () => {
  beforeEach(() => {
    api.mockReset();
  });

  it("shows loading then beneficiary and certificates", async () => {
    api.mockImplementation(async (path: string) => {
      if (String(path).includes("/certificates")) {
        return [
          {
            id: "c1",
            certificateNumber: "CERT-1",
            municipalityCode: "999001",
            validFrom: "2026-01-01",
            validTo: "2026-12-31",
            serviceCategory: "PLAN_CONSULTATION",
          },
        ];
      }
      return {
        id: "b1",
        anonymizedCode: "U-0001",
        category: "ADULT",
        municipalityCode: "999001",
        status: "ACTIVE",
      };
    });
    render(<BeneficiaryDetailPage />);
    expect(screen.getByText("読み込み中…")).toBeInTheDocument();
    expect(await screen.findByText("CERT-1")).toBeInTheDocument();
    expect(screen.getByText(/U-0001/)).toBeInTheDocument();
  });

  it("shows error on failure", async () => {
    api.mockRejectedValue(new Error("not found"));
    render(<BeneficiaryDetailPage />);
    expect(await screen.findByText("not found")).toBeInTheDocument();
  });
});
