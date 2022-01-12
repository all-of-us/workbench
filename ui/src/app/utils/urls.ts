// Canonicalizes a string as a URL. Suitable for use on user inputs. This does
// not have any implications around trusting the URL.
export function canonicalizeUrl(url: string): string {
  url = url.toLowerCase().trim();
  if (/^https?:\/\//.test(url)) {
    return url;
  }
  // Default to http://, as many sites will upgrade to https:// automatically
  // and https:// may yield a bad certificate if not configured.
  return `http://${url}`;
}
