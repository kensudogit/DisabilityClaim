import { beforeEach, describe, expect, it, vi } from "vitest";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { UsageGuidePanel } from "./UsageGuidePanel";

describe("UsageGuidePanel", () => {
  beforeEach(() => {
    localStorage.clear();
    Object.defineProperty(window, "innerWidth", { configurable: true, value: 1280 });
    Object.defineProperty(window, "innerHeight", { configurable: true, value: 800 });
  });

  it("renders usage guide dialog with hero content", async () => {
    render(<UsageGuidePanel />);
    expect(await screen.findByRole("dialog", { name: "利用手順" })).toBeInTheDocument();
    expect(screen.getByText("障害福祉サービス請求 MVP")).toBeInTheDocument();
    expect(screen.getByText("本パッケージの位置づけ")).toBeInTheDocument();
    expect(screen.getByText(/Java 21 · Spring Boot 3.5/)).toBeInTheDocument();
  });

  it("can collapse and expand", async () => {
    render(<UsageGuidePanel />);
    await screen.findByRole("dialog", { name: "利用手順" });
    const toggle = screen.getByRole("button", { name: "閉じる" });
    await userEvent.click(toggle);
    expect(screen.queryByText("障害福祉サービス請求 MVP")).not.toBeInTheDocument();
    await userEvent.click(screen.getByRole("button", { name: "開く" }));
    expect(await screen.findByText("障害福祉サービス請求 MVP")).toBeInTheDocument();
  });

  it("persists expanded state to localStorage", async () => {
    render(<UsageGuidePanel />);
    await screen.findByRole("dialog", { name: "利用手順" });
    await userEvent.click(screen.getByRole("button", { name: "閉じる" }));
    await vi.waitFor(() => {
      const raw = localStorage.getItem("disability-claim-usage-guide-v1");
      expect(raw).toBeTruthy();
      expect(JSON.parse(raw!).expanded).toBe(false);
    });
  });
});
