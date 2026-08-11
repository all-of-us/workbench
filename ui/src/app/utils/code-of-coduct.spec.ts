import {
  getDuccRenderingInfo,
  getLiveDUCCVersion,
} from 'app/utils/code-of-conduct';
import { serverConfigStore } from 'app/utils/stores';

describe('code-of-conduct', () => {
  it('returns DUCC v7 rendering info', () => {
    expect(getDuccRenderingInfo(7)).toEqual({
      version: 7,
      path: '/data-user-code-of-conduct-v7.html',
      height: '175em',
    });
  });

  it('prefers latestDuccVersion when available', () => {
    serverConfigStore.set({
      config: {
        ...serverConfigStore.get().config,
        currentDuccVersions: [6, 7],
        latestDuccVersion: 7,
      },
    });

    expect(getLiveDUCCVersion()).toBe(7);
  });

  it('falls back to max currentDuccVersions when latestDuccVersion is unavailable', () => {
    serverConfigStore.set({
      config: {
        ...serverConfigStore.get().config,
        currentDuccVersions: [6, 7],
        latestDuccVersion: undefined,
      },
    });

    expect(getLiveDUCCVersion()).toBe(7);
  });
});
