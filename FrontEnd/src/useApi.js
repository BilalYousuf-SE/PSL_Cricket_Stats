import { useCallback, useEffect, useState } from 'react';
import { api } from './api.js';

// Fetch one endpoint or a group in parallel. Explicit deps can also trigger a
// refresh; changed paths always reload, even when callers omit those deps.
export function useApi(paths, deps = [], { enabled = true } = {}) {
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(enabled);
  const [error, setError] = useState(null);
  const [tick, setTick] = useState(0);

  const resolvedPaths = typeof paths === 'function' ? paths() : paths;
  const isArray = Array.isArray(resolvedPaths);
  const pathKey = JSON.stringify(isArray ? resolvedPaths : [resolvedPaths]);

  useEffect(() => {
    const pathList = JSON.parse(pathKey);
    if (!enabled || pathList.some((path) => !path)) {
      setLoading(false);
      setData(null);
      setError(null);
      return;
    }

    const controller = new AbortController();
    let active = true;
    setLoading(true);
    setData(null);
    setError(null);

    async function load() {
      try {
        const results = await Promise.all(
          pathList.map((path) => api.get(path, { signal: controller.signal }))
        );
        if (active) setData(isArray ? results : results[0]);
      } catch (err) {
        if (active && err.name !== 'AbortError') {
          setError(err.message || 'Something went wrong.');
        }
        controller.abort();
      } finally {
        if (active) setLoading(false);
      }
    }

    load();
    return () => {
      active = false;
      controller.abort();
    };
    // Keep the existing explicit-dependency API for callers with other inputs.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [enabled, pathKey, isArray, tick, ...deps]);

  const reload = useCallback(() => setTick((value) => value + 1), []);
  return { data, loading, error, reload };
}
