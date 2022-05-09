/**
 * Returns the currently live DUCC version. This version should be displayed when
 * completing the DUCC and this exact version must be signed in order to receive
 * registered data access.
 *
 * Note: If we instead returned a live DUCC version in the server config, we
 * could likely eliminate this helper function. This needs further design
 * thinking though. For now just consolidate DUCC logic through this package.
 */
export const getLiveDUCCVersion = () => 4;

interface VersionInfo {
  version: number;
  path: string;
  height: string;
}

const versions: VersionInfo[] = [
  {
    version: 4,
    path: '/data-user-code-of-conduct-v4.html',
    height: '90rem',
  },
];

export const getVersionInfo = (versionToFind: number): VersionInfo => {
  return versions.find(({ version }) => version === versionToFind);
};
