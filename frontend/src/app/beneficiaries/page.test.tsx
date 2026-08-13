import { beforeEach, describe, expect, it, vi } from "vitest";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import BeneficiariesPage from "./page";

const api = vi.fn();

vi.mock("next/link", () => ({
  default: ({ href, children }: { href: string; children: React.ReactNode }) => (
    <a href={href}>{children}</a>
  ),
}));

vi.mock("@/lib/api", () => ({
  api: (...args: unknown[]) => api(...args),
}));

describe("BeneficiariesPage", () => {
  beforeEach(() => {
    api.mockReset();
  });

  it("shows empty list when API returns []", async () => {
    api.mockResolvedValue([]);
    render(<BeneficiariesPage />);
    expect(await screen.findByText("利用者がまだいません。")).toBeInTheDocument();
  });

  it("renders beneficiaries from API", async () => {
    api.mockResolvedValue([
      {
        id: "b1",
        anonymizedCode: "U-0001",
        category: "ADULT",
        municipalityCode: "999001",
        municipalityName: "Demo市",
        status: "ACTIVE",
      },
    ]);
    render(<BeneficiariesPage />);
    expect(await screen.findByText("U-0001")).toBeInTheDocument();
    expect(screen.getAllByText("障害者（成人）").length).toBeGreaterThan(0);
    expect(screen.getAllByText("利用中").length).toBeGreaterThan(0);
    expect(screen.getByRole("link", { name: "詳細" })).toHaveAttribute(
      "href",
      "/beneficiaries/b1",
    );
  });

  it("shows API error", async () => {
    api.mockRejectedValue(new Error("APIエラー (500)"));
    render(<BeneficiariesPage />);
    expect(await screen.findByText("APIエラー (500)")).toBeInTheDocument();
  });

  it("posts new beneficiary and reloads", async () => {
    api
      .mockResolvedValueOnce([])
      .mockResolvedValueOnce({})
      .mockResolvedValueOnce([
        {
          id: "b2",
          anonymizedCode: "U-0002",
          category: "CHILD",
          municipalityCode: "999002",
          status: "ACTIVE",
        },
      ]);
    render(<BeneficiariesPage />);
    await screen.findByText("利用者がまだいません。");
    await userEvent.type(screen.getByLabelText("匿名コード"), "U-0002");
    await userEvent.type(screen.getByLabelText("市町村コード"), "999002");
    await userEvent.selectOptions(screen.getByLabelText("区分"), "CHILD");
    await userEvent.click(screen.getByRole("button", { name: "登録" }));
    await waitFor(() => {
      expect(api).toHaveBeenCalledWith(
        "/api/v1/beneficiaries",
        expect.objectContaining({
          method: "POST",
          body: expect.objectContaining({
            anonymizedCode: "U-0002",
            municipalityCode: "999002",
            category: "CHILD",
          }),
        }),
      );
    });
    expect(await screen.findByText("U-0002")).toBeInTheDocument();
  });
});
