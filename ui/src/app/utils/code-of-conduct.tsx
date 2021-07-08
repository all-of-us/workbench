
/**
 * Returns the currently live DUA version. This version should be displayed when
 * completing the DUA and this exact version must be signed in order to receive
 * registered data access.
 *
 * Note: If we instead returned a live DUA version in the server config, we
 * could likely eliminate this helper function. This needs further design
 * thinking though. For now just consolidate DUA logic through this package.
 */
export function getLiveDataUseAgreementVersion(): number {
  return 3;
}
