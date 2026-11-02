import { QueryClientProvider } from "@tanstack/react-query";
import { render, screen, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter, Route, Routes } from "react-router";
import { describe, expect, it, vi } from "vitest";
import { createTestQueryClient } from "../test/renderWithProviders.tsx";
import { AdminLayout } from "./AdminLayout.tsx";

function Boom(): never {
  throw new Error("render failure");
}

describe("AdminLayout", () => {
  it("givenPageThatThrows_whenRendered_thenShowsRefreshMessageAndKeepsShell", () => {
    vi.spyOn(console, "error").mockImplementation(() => undefined);

    render(
      <QueryClientProvider client={createTestQueryClient()}>
        <MemoryRouter initialEntries={["/boom"]}>
          <Routes>
            <Route element={<AdminLayout />}>
              <Route path="/boom" element={<Boom />} />
            </Route>
          </Routes>
        </MemoryRouter>
      </QueryClientProvider>,
    );

    expect(screen.getByRole("alert")).toHaveTextContent(
      "Beklenmeyen bir hata oluştu. Sayfayı yenile.",
    );
    expect(
      screen.getByRole("navigation", { name: "Menü" }),
    ).toBeInTheDocument();
  });

  it("givenCrashedPage_whenNavigatingToHealthyPage_thenErrorIsCleared", async () => {
    vi.spyOn(console, "error").mockImplementation(() => undefined);
    const actor = userEvent.setup();

    render(
      <QueryClientProvider client={createTestQueryClient()}>
        <MemoryRouter initialEntries={["/boom"]}>
          <Routes>
            <Route element={<AdminLayout />}>
              <Route path="/boom" element={<Boom />} />
              <Route path="/onizleme" element={<h1>Sağlam sayfa</h1>} />
            </Route>
          </Routes>
        </MemoryRouter>
      </QueryClientProvider>,
    );
    await actor.click(
      within(screen.getByRole("navigation", { name: "Menü" })).getByRole(
        "link",
        { name: "Önizleme" },
      ),
    );

    expect(screen.queryByRole("alert")).not.toBeInTheDocument();
    expect(
      screen.getByRole("heading", { name: "Sağlam sayfa" }),
    ).toBeInTheDocument();
  });
});
