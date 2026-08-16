"use client";

import { useEffect, useState } from "react";

/**
 * Returns today's ISO date, but only after mount. Returns "" during SSR
 * and the first client render, since the build machine's clock never
 * matches the viewer's — baking "today" into prerendered HTML would break
 * hydration the same way the dashboard greeting and currency formatting
 * did earlier. "" compares less than any real ISO date string, so date
 * comparisons against it (e.g. dueDate < today) safely resolve to false
 * until the real value lands.
 */
export function useToday(): string {
  const [today, setToday] = useState("");
  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    setToday(new Date().toISOString().slice(0, 10));
  }, []);
  return today;
}
