import React from "react";
import { vi } from "vitest";

export const mockPush = vi.fn();
export const mockParams: Record<string, string> = {};

export function mockNextNavigation(options?: {
  pathname?: string;
  params?: Record<string, string>;
}) {
  const pathname = options?.pathname ?? "/";
  Object.assign(mockParams, options?.params ?? {});
  vi.mock("next/navigation", () => ({
    usePathname: () => pathname,
    useRouter: () => ({ push: mockPush, replace: vi.fn(), back: vi.fn() }),
    useParams: () => mockParams,
  }));
}

export function mockNextLink() {
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
}
