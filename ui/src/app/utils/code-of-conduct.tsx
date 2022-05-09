import { serverConfigStore } from './stores';

/**
 * Returns the list of current-for-compliance DUCC version.  One of these versions must be signed in order to receive
 * registered data access.
 */
export const getCurrentDUCCVersions = (): number[] =>
  serverConfigStore.get().config.currentDuccVersions;

/**
 * Returns the latest version of the DUCC, to be displayed when the user requests to sign it.  This may not be the only
 * "current" DUCC version; use getCurrentDUCCVersions() to retrieve the full list.
 */
export const getLiveDUCCVersion = (): number =>
  Math.max(...getCurrentDUCCVersions());

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
