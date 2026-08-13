import "@testing-library/jest-dom/vitest";
import { vi } from "vitest";

// CSS Modules / global CSS をテスト時に無視
vi.mock("*.css", () => ({}));
vi.mock("*.module.css", () => new Proxy({}, { get: () => "mock-class" }));
