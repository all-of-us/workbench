import {
  AppStatus,
  AppType,
  RuntimeStatus,
  TerraJobStatus,
} from 'generated/fetch';

// For testing stores
import * as useStoreModule from 'app/utils/stores';

export function minus<T>(a1: T[], a2: T[]): T[] {
  return a1.filter((e) => !a2.includes(e));
}

export const ALL_GKE_APP_STATUSES = Object.keys(AppStatus)
  .map((k) => AppStatus[k])
  .concat([null, undefined]);

export const ALL_TERRA_JOB_STATUSES = Object.keys(TerraJobStatus)
  .map((k) => TerraJobStatus[k])
  .concat([null, undefined]);

export const ALL_RUNTIME_STATUSES = Object.keys(RuntimeStatus)
  .map((k) => RuntimeStatus[k])
  .concat([null, undefined]);

export const ALL_GKE_APP_TYPES = Object.keys(AppType).map((k) => AppType[k]);

/**
 * Mock the useStore hook for testing with a mapping of stores to their mocked return values.
 * This simplifies tests that need to mock multiple stores.
 * 
 * @param storeMappings - A Map mapping store objects to their mocked state values
 * @returns The original spy on useStore to allow for cleanup
 * 
 * Example usage:
 * ```
 * const useStoreSpy = mockUseStore(new Map([
 *   [runtimeStore, { runtime: mockRuntime, runtimeLoaded: true }],
 *   [runtimeDiskStore, { gcePersistentDisk: mockDisk, gcePersistentDiskLoaded: true }]
 * ]));
 * 
 * // After the test
 * useStoreSpy.mockRestore();
 * ```
 */
export function mockUseStore(storeMappings: Map<any, any>) {
  return jest.spyOn(useStoreModule, 'useStore').mockImplementation((store) => {
    if (storeMappings.has(store)) {
      return storeMappings.get(store);
    }
    return null;
  });
}
