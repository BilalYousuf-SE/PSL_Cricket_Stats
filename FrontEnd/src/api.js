// Centralized fetch wrapper for the Spring Boot backend.
// Change BASE_URL here (or via VITE_API_BASE_URL in a .env file) if the
// backend ever runs somewhere other than localhost:8080.

const BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';

class ApiError extends Error {
  constructor(message, status) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
  }
}

async function request(path, options = {}) {
  const url = `${BASE_URL}${path}`;
  let res;
  try {
    res = await fetch(url, {
      headers: { Accept: 'application/json', ...options.headers },
      ...options,
    });
  } catch (err) {
    throw new ApiError(
      `Could not reach the backend at ${BASE_URL}. Is the Spring Boot app running?`,
      0
    );
  }

  if (!res.ok) {
    let detail = '';
    try {
      const body = await res.json();
      detail = body?.message || body?.error || '';
    } catch {
      /* body wasn't JSON, ignore */
    }
    throw new ApiError(
      detail || `Request to ${path} failed with status ${res.status}`,
      res.status
    );
  }

  if (res.status === 204) return null;

  const text = await res.text();
  return text ? JSON.parse(text) : null;
}

export const api = {
  get: (path) => request(path),
};

export { ApiError, BASE_URL };
