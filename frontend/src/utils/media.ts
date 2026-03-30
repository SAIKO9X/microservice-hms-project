const API_BASE_URL = "http://localhost:9000";

export function resolveImageUrl(
  url: string | null | undefined,
): string | undefined {
  if (!url) return undefined;
  return url.startsWith("http") ? url : `${API_BASE_URL}${url}`;
}
