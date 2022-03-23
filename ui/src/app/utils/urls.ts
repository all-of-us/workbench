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

function isValidUrl(potentialUrl: string): boolean {
  try {
    new URL(potentialUrl);
    return true;
  } catch (e) {
    return false;
  }
}

export function getCustomOrDefaultUrl(customUrl: string, defaultUrl: string) {
  let url = defaultUrl;
  if (customUrl) {
    const adjustedUrl = canonicalizeUrl(customUrl);
    if (isValidUrl(adjustedUrl)) {
      url = adjustedUrl;
    }
  }
  return url;
}
