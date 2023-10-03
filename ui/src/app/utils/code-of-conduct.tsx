import { serverConfigStore } from './stores';

/**
 * Returns the list of current-for-compliance DUCC versions.
 * One of these versions must be signed in order to receive Registered Tier and Controlled Tier data access.
 */
const getCurrentDUCCVersions = (): number[] =>
  serverConfigStore.get().config.currentDuccVersions;

/**
 * Returns the latest version of the DUCC, to be displayed when the user requests to sign it.
 * This may not be the only DUCC version considered to be current for access tier membership requirements;
 * use getCurrentDUCCVersions() to retrieve the full list.
 */
export const getLiveDUCCVersion = (): number =>
  Math.max(...getCurrentDUCCVersions());

export const isCurrentDUCCVersion = (
  duccSignedVersion: number | undefined
): boolean => getCurrentDUCCVersions().includes(duccSignedVersion);

interface VersionInfo {
  version: number;
  path: string;
  height: string;
}

const VERSIONS: VersionInfo[] = [
  {
    version: 3,
    path: '/data-user-code-of-conduct-v3.html',
    height: '135rem', // no inner vertical scrollbars at min width, via trial and error
  },
  {
    version: 4,
    path: '/data-user-code-of-conduct-v4.html',
    height: '135rem', // no inner vertical scrollbars at min width, via trial and error
  },
  {
    version: 5,
    path: '/data-user-code-of-conduct-v5.html',
    height: '165em', // no inner vertical scrollbars at min width, via trial and error
  },
];

export const getDuccRenderingInfo = (versionToFind: number): VersionInfo => {
  return VERSIONS.find(({ version }) => version === versionToFind);
};

export const canRenderSignedDucc = (versionToFind: number): boolean =>
  !!getDuccRenderingInfo(versionToFind);
