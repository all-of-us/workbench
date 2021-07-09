/**
 * Returns the currently live DUCC version. This version should be displayed when
 * completing the DUCC and this exact version must be signed in order to receive
 * registered data access.
 *
 * Note: If we instead returned a live DUCC version in the server config, we
 * could likely eliminate this helper function. This needs further design
 * thinking though. For now just consolidate DUCC logic through this package.
 */
export function getLiveDUCCVersion(): number {
  return 3;
}
