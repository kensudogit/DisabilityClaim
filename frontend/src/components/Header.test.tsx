import { describe, expect, it, vi, beforeEach } from "vitest";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { Header } from "./Header";

const push = vi.fn();

vi.mock("next/navigation", () => ({
  usePathname: () => "/",
  useRouter: () => ({ push }),
}));

vi.mock("next/link", () => ({
  default: ({
    href,
    children,
    className,
  }: {
    href: string;
    children: React.ReactNode;
    className?: string;
  }) => (
    <a href={href} className={className}>
      {children}
    </a>
  ),
}));

describe("Header", () => {
  beforeEach(() => {
    localStorage.clear();
    push.mockClear();
  });

  it("renders navigation links and login", () => {
    render(<Header />);
    expect(screen.getByText("障害相談請求")).toBeInTheDocument();
    expect(screen.getByText("ダッシュボード")).toBeInTheDocument();
    expect(screen.getByText("利用者")).toBeInTheDocument();
    expect(screen.getByText("Excel移行")).toBeInTheDocument();
    expect(screen.getByText("月次請求")).toBeInTheDocument();
    expect(screen.getByText("ログイン")).toBeInTheDocument();
  });

  it("shows logout when token exists and clears on click", async () => {
    localStorage.setItem("dc_access_token", "tok");
    render(<Header />);
    const logout = await screen.findByRole("button", { name: "ログアウト" });
    await userEvent.click(logout);
    expect(localStorage.getItem("dc_access_token")).toBeNull();
    expect(push).toHaveBeenCalledWith("/login");
  });
});
