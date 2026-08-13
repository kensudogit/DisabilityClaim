import { beforeEach, describe, expect, it, vi } from "vitest";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import ImportsPage from "./page";

vi.mock("@/lib/api", () => ({
  getToken: () => "tok",
}));

describe("ImportsPage", () => {
  beforeEach(() => {
    vi.stubGlobal("fetch", vi.fn());
  });

  it("disables upload until a file is selected", () => {
    render(<ImportsPage />);
    expect(screen.getByRole("button", { name: /アップロードして検証/ })).toBeDisabled();
  });

  it("uploads file and shows result", async () => {
    (fetch as unknown as ReturnType<typeof vi.fn>).mockResolvedValue({
      ok: true,
      status: 200,
      text: async () =>
        JSON.stringify({
          jobId: "j1",
          totalRows: 2,
          successRows: 1,
          errorRows: 1,
          errors: [{ row: 2, column: "市町村コード", reason: "必須" }],
        }),
    });
    render(<ImportsPage />);
    const input = screen.getByLabelText(/Excelファイル/);
    const file = new File(["xlsx"], "demo.xlsx", {
      type: "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
    });
    await userEvent.upload(input, file);
    await userEvent.click(screen.getByRole("button", { name: /アップロード/ }));
    await waitFor(() => {
      expect(fetch).toHaveBeenCalledWith(
        "/api/v1/imports/excel",
        expect.objectContaining({ method: "POST" }),
      );
    });
    expect(await screen.findByText("市町村コード")).toBeInTheDocument();
    expect(screen.getByText("必須")).toBeInTheDocument();
  });

  it("shows upload error message", async () => {
    (fetch as unknown as ReturnType<typeof vi.fn>).mockResolvedValue({
      ok: false,
      status: 400,
      text: async () => JSON.stringify({ message: "officeId required" }),
    });
    render(<ImportsPage />);
    const input = screen.getByLabelText(/Excelファイル/);
    const file = new File(["xlsx"], "demo.xlsx", {
      type: "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
    });
    await userEvent.upload(input, file);
    await userEvent.click(screen.getByRole("button", { name: /アップロード/ }));
    expect(await screen.findByText("officeId required")).toBeInTheDocument();
  });
});
