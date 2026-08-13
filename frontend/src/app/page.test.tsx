import { describe, expect, it, vi } from "vitest";
import { render, screen } from "@testing-library/react";
import DashboardPage from "./page";

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

describe("DashboardPage", () => {
  it("renders title and navigation tiles", () => {
    render(<DashboardPage />);
    expect(screen.getByRole("heading", { name: "ダッシュボード" })).toBeInTheDocument();
    expect(screen.getByText("利用者")).toBeInTheDocument();
    expect(screen.getByText("Excel移行")).toBeInTheDocument();
    expect(screen.getByText("月次請求")).toBeInTheDocument();
    expect(screen.getByText("テスト結果")).toBeInTheDocument();
    expect(screen.getByRole("link", { name: /テスト結果/ })).toHaveAttribute(
      "href",
      "/test-reports",
    );
  });
});
