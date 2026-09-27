import { useCallback, useEffect, useState } from 'react';
import { api } from './api.js';

// Fetches one or more endpoints in parallel and exposes
// { data, loading, error, reload }. `paths` may be a single string, an array
// of strings, or a function returning either (re-evaluated on `deps` change).
// `enabled` lets a page skip fetching until required inputs are ready.
export function useApi(paths, deps = [], { enabled = true } = {}) {
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(enabled);
  const [error, setError] = useState(null);
  const [tick, setTick] = useState(0);

  const resolvedPaths = typeof paths === 'function' ? paths() : paths;
  const isArray = Array.isArray(resolvedPaths);
  const pathList = isArray ? resolvedPaths : [resolvedPaths];

  const load = useCallback(async () => {
    if (!enabled || pathList.some((p) => !p)) {
      setLoading(false);
      return;
    }
    setLoading(true);
    setError(null);
    try {
      const results = await Promise.all(pathList.map((p) => api.get(p)));
      setData(isArray ? results : results[0]);
    } catch (err) {
      setError(err.message || 'Something went wrong.');
    } finally {
      setLoading(false);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [enabled, JSON.stringify(pathList), tick]);

  useEffect(() => {
    load();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, deps.concat([tick, enabled]));

  const reload = () => setTick((t) => t + 1);

  return { data, loading, error, reload };
}
