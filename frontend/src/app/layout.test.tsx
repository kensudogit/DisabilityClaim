import { describe, expect, it, vi } from "vitest";
import { render, screen } from "@testing-library/react";
import RootLayout, { metadata } from "./layout";

vi.mock("@/components/Header", () => ({
  Header: () => <header data-testid="header">Header</header>,
}));

vi.mock("@/components/UsageGuidePanel", () => ({
  UsageGuidePanel: () => <div data-testid="usage-guide">Guide</div>,
}));

describe("RootLayout", () => {
  it("exposes metadata", () => {
    expect(metadata.title).toBe("障害相談請求 MVP");
    expect(metadata.description).toContain("計画相談支援");
  });

  it("wraps children with header and usage guide", () => {
    render(
      <RootLayout>
        <div>child-content</div>
      </RootLayout>,
    );
    expect(screen.getByTestId("header")).toBeInTheDocument();
    expect(screen.getByTestId("usage-guide")).toBeInTheDocument();
    expect(screen.getByText("child-content")).toBeInTheDocument();
  });
});
