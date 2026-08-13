import { beforeEach, describe, expect, it, vi } from "vitest";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import LoginPage from "./page";

const push = vi.fn();
const setToken = vi.fn();
const api = vi.fn();

vi.mock("next/navigation", () => ({
  useRouter: () => ({ push }),
}));

vi.mock("@/lib/api", () => ({
  api: (...args: unknown[]) => api(...args),
  setToken: (...args: unknown[]) => setToken(...args),
}));

describe("LoginPage", () => {
  beforeEach(() => {
    push.mockClear();
    setToken.mockClear();
    api.mockReset();
  });

  it("renders login form with demo hint", () => {
    render(<LoginPage />);
    expect(screen.getByRole("heading", { name: "ログイン" })).toBeInTheDocument();
    expect(screen.getByLabelText("ユーザー名")).toHaveValue("admin");
    expect(screen.getByLabelText("パスワード")).toBeInTheDocument();
  });

  it("stores token and redirects on success", async () => {
    api.mockResolvedValue({ accessToken: "jwt-xyz" });
    render(<LoginPage />);
    await userEvent.type(screen.getByLabelText("パスワード"), "password123");
    await userEvent.click(screen.getByRole("button", { name: "ログイン" }));
    await waitFor(() => {
      expect(api).toHaveBeenCalledWith(
        "/api/v1/auth/login",
        expect.objectContaining({
          method: "POST",
          auth: false,
          body: { username: "admin", password: "password123" },
        }),
      );
      expect(setToken).toHaveBeenCalledWith("jwt-xyz");
      expect(push).toHaveBeenCalledWith("/");
    });
  });

  it("shows error message on failure", async () => {
    api.mockRejectedValue(new Error("Invalid credentials"));
    render(<LoginPage />);
    await userEvent.type(screen.getByLabelText("パスワード"), "bad");
    await userEvent.click(screen.getByRole("button", { name: "ログイン" }));
    expect(await screen.findByText("Invalid credentials")).toBeInTheDocument();
  });
});
