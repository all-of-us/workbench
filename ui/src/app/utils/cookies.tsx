// Local storage key to override the API base path.
export const LOCAL_STORAGE_API_OVERRIDE_KEY = 'allOfUsApiUrlOverride';

// Local storage key to override authn with a Puppeteer test token.
export const LOCAL_STORAGE_KEY_TEST_ACCESS_TOKEN = 'test-access-token-override';

/**
 * See feature-detects/cookies.js in https://github.com/Modernizr
 *
 * navigator.cookieEnabled cannot detect custom or nuanced cookie blocking
 * configurations. For example, when blocking cookies via the Advanced
 * Privacy Settings in IE9, it always returns true. And there have been
 * issues in the past with site-specific exceptions.
 * Don't rely on it.
 *
 * try..catch because some in situations `document.cookie` is exposed but throws a
 * SecurityError if you try to access it; e.g. documents created from data URIs
 * or in sandboxed iframes (depending on flags/context)
 */
export function cookiesEnabled(): boolean {
  try {
    // Create cookie
    document.cookie = 'cookietest=1';
    const ret = document.cookie.indexOf('cookietest=') !== -1;
    // Delete cookie
    document.cookie = 'cookietest=1; expires=Thu, 01-Jan-1970 00:00:01 GMT';
    return ret;
  } catch (e) {
    return false;
  }
}
