import {
  PersistentDiskRequest,
  Runtime,
  RuntimeConfigurationType,
  RuntimeStatus,
} from 'generated/fetch';
import { RuntimeApi } from 'generated/fetch';

import { expect } from '@jest/globals';
import {
  leoRuntimesApi,
  registerApiClient as registerApiClientNotebooks,
} from 'app/services/notebooks-swagger-fetch-clients';
import {
  registerApiClient,
  runtimeApi,
} from 'app/services/swagger-fetch-clients';
import {
  InitialRuntimeNotFoundError,
  LeoRuntimeInitializer,
  LeoRuntimeInitializerOptions,
  throwRuntimeNotFound,
} from 'app/utils/leo-runtime-initializer';
import { serverConfigStore } from 'app/utils/stores';
import { RuntimesApi as LeoRuntimesApi } from 'notebooks-generated/fetch';
import { setImmediate } from 'timers';

import { LeoRuntimesApiStub } from 'testing/stubs/leo-runtimes-api-stub';
import {
  defaultRuntime,
  defaultRuntimeWithPd,
  RuntimeApiStub,
} from 'testing/stubs/runtime-api-stub';

import { runtimePresets } from './runtime-presets';

import SpyInstance = jest.SpyInstance;
import { DEFAULT_MACHINE_NAME } from 'app/utils/machines';

import { stubDisk } from 'testing/stubs/disks-api-stub';

let mockGetRuntime: SpyInstance;
let mockCreateRuntime: SpyInstance;
let mockStartRuntime: SpyInstance;

const baseRuntime: Runtime = {
  runtimeName: 'aou-rw-3',
  googleProject: 'aou-rw-12345',
  status: RuntimeStatus.RUNNING,
  createdDate: '08/08/2018',
  toolDockerImage: 'docker',
  configurationType: RuntimeConfigurationType.GENERAL_ANALYSIS,
};

const workspaceNamespace = 'aou-rw-12345';

describe('RuntimeInitializer', () => {
  beforeEach(() => {
    jest.useFakeTimers();

    registerApiClient(RuntimeApi, new RuntimeApiStub());
    registerApiClientNotebooks(LeoRuntimesApi, new LeoRuntimesApiStub());

    mockGetRuntime = jest.spyOn(runtimeApi(), 'getRuntime');
    mockCreateRuntime = jest.spyOn(runtimeApi(), 'createRuntime');
    mockStartRuntime = jest.spyOn(leoRuntimesApi(), 'startRuntime');

    serverConfigStore.set({ config: { gsuiteDomain: 'researchallofus.org' } });
  });

  afterEach(() => {
    jest.useRealTimers();
    jest.clearAllMocks();
  });

  const mockGetRuntimeCalls = (baseOverrides: Array<Partial<Runtime>>) => {
    const runtimes: Array<Runtime> = baseOverrides.map((override) => {
      return { ...baseRuntime, ...override };
    });
    for (const runtime of runtimes) {
      mockGetRuntime.mockImplementationOnce(() => {
        return runtime;
      });
    }
  };

  /**
   * This helper function allows us to call Jest's mock-timer progression function, runAllTimers,
   * an arbitrary number of times until the given Promise is settled.
   *
   * This is helpful for testing the initializer, since it has a Promise-based API but relies on
   * a polling strategy using setTimeout calls in order to wait for the runtime to become ready.
   */
  const runTimersUntilSettled = async (
    p: Promise<any>,
    maxLoops: number = 20
  ) => {
    let isSettled = false;
    p.then(() => (isSettled = true)).catch(() => {
      isSettled = true;
    });
    let i = 0;
    while (!isSettled) {
      i++;
      if (i > maxLoops) {
        throw new Error(
          'Max number of timer cycles reached waiting for Promise to settle'
        );
      }

      await new Promise(setImmediate);
      jest.runAllTimers();
    }
  };

  const runInitializerAndTimers = async (
    options?: Partial<LeoRuntimeInitializerOptions>,
    maxLoops?: number
  ): Promise<Runtime> => {
    const runtimePromise = LeoRuntimeInitializer.initialize({
      workspaceNamespace: workspaceNamespace,
      ...options,
    });
    await runTimersUntilSettled(runtimePromise, maxLoops);
    return await runtimePromise;
  };

  it('should resolve promise if runtime is in ready state', async () => {
    // This tests the simplest case of the initializer. No polling necessary.
    mockGetRuntimeCalls([baseRuntime]);
    const runtime = await runInitializerAndTimers();
    expect(runtime.status).toEqual(RuntimeStatus.RUNNING);
  });

  it('should resume runtime if it is initially stopping / stopped', async () => {
    // This test case also includes some repeated statuses, since that is a more realistic
    // reflection of how we poll Leo & get occasional new statuses.
    mockGetRuntimeCalls([
      { status: RuntimeStatus.STOPPING },
      { status: RuntimeStatus.STOPPING },
      { status: RuntimeStatus.STOPPED },
      { status: RuntimeStatus.STOPPED },
      // Here is where we expect, chronologically, a call to startRuntime. The next two
      // runtime statuses mock out what we might expect to see after that call.
      { status: RuntimeStatus.STARTING },
      { status: RuntimeStatus.STARTING },
      { status: RuntimeStatus.STARTING },
      { status: RuntimeStatus.RUNNING },
    ]);
    const runtime = await runInitializerAndTimers();
    expect(mockStartRuntime).toHaveBeenCalled();
    expect(runtime.status).toEqual(RuntimeStatus.RUNNING);
  });

  it('should call callback with runtime status', async () => {
    mockGetRuntimeCalls([{ status: RuntimeStatus.STOPPED }]);
    mockGetRuntimeCalls([
      { status: RuntimeStatus.STARTING },
      { status: RuntimeStatus.STARTING },
      { status: RuntimeStatus.RUNNING },
    ]);
    const statuses = [];
    await runInitializerAndTimers({
      onPoll: (runtime) => statuses.push(runtime ? runtime.status : null),
    });

    expect(statuses).toEqual([
      RuntimeStatus.STOPPED,
      // Note: onStatusUpdate will be called for every status received, not just when the status
      // value is changed.
      RuntimeStatus.STARTING,
      RuntimeStatus.STARTING,
      RuntimeStatus.RUNNING,
    ]);
  });

  it('should create runtime if it is initially nonexistent', async () => {
    mockGetRuntime.mockRejectedValueOnce(new Response(null, { status: 404 }));
    mockCreateRuntime.mockImplementationOnce(async () => {
      return { status: RuntimeStatus.CREATING };
    });
    mockGetRuntimeCalls([
      { status: RuntimeStatus.STARTING },
      { status: RuntimeStatus.RUNNING },
    ]);
    const runtime = await runInitializerAndTimers({
      targetRuntime: runtimePresets.generalAnalysis.runtimeTemplate,
    });

    expect(mockCreateRuntime).toHaveBeenCalled();
    expect(runtime.status).toEqual(RuntimeStatus.RUNNING);
  });

  it("should error and suggest user's most runtime if a valid one exists", async () => {
    serverConfigStore.set({ config: { gsuiteDomain: 'researchallofus.org' } });
    mockGetRuntime.mockImplementation(() => {
      return {
        ...defaultRuntime(),
        configurationType: RuntimeConfigurationType.USER_OVERRIDE,
        gceConfig: {
          diskSize: 777,
          machineType: 'n1-standard-16',
        },
        status: RuntimeStatus.DELETED,
      };
    });

    try {
      await LeoRuntimeInitializer.initialize({
        workspaceNamespace: workspaceNamespace,
      });
      fail('expected initializer to throw an exception');
    } catch (e) {
      expect(e).toBeInstanceOf(InitialRuntimeNotFoundError);
      expect(e.defaultRuntime).toMatchObject({
        gceConfig: {
          diskSize: 777,
          machineType: 'n1-standard-16',
        },
      });
    }
  });

  it('should error and suggest preset values if a preset was selected for deleted runtime', async () => {
    serverConfigStore.set({ config: { gsuiteDomain: 'researchallofus.org' } });
    // Persistent disk is the only storage option for preset value General analysis
    mockGetRuntime.mockImplementation(() => {
      return {
        ...defaultRuntime(),
        configurationType: RuntimeConfigurationType.GENERAL_ANALYSIS,
        gceWithPdConfig: {
          machineType: DEFAULT_MACHINE_NAME,
          persistentDisk: {
            size: 120,
          },
        },
        status: RuntimeStatus.DELETED,
      };
    });

    try {
      await LeoRuntimeInitializer.initialize({
        workspaceNamespace: workspaceNamespace,
      });
      fail('expected initializer to throw an exception');
    } catch (e) {
      expect(e).toBeInstanceOf(InitialRuntimeNotFoundError);
      expect(e.defaultRuntime).toMatchObject({
        gceWithPdConfig: {
          persistentDisk: {
            size: runtimePresets.generalAnalysis.runtimeTemplate.gceWithPdConfig
              .persistentDisk.size,
          },
          machineType:
            runtimePresets.generalAnalysis.runtimeTemplate.gceWithPdConfig
              .machineType,
        },
      });
    }
  });

  it('should not automatically delete errored runtimes', async () => {
    mockGetRuntimeCalls([{ status: RuntimeStatus.ERROR }]);

    const runtime = await runInitializerAndTimers();

    expect(runtime.status).toEqual(RuntimeStatus.ERROR);
  });

  it('should recover from intermittent 500s', async () => {
    mockGetRuntimeCalls([
      { status: RuntimeStatus.CREATING },
      { status: RuntimeStatus.CREATING },
    ]);
    mockGetRuntime.mockRejectedValueOnce(new Response(null, { status: 503 }));
    mockGetRuntime.mockRejectedValueOnce(new Response(null, { status: 503 }));
    mockGetRuntimeCalls([{ status: RuntimeStatus.RUNNING }]);

    const runtime = await runInitializerAndTimers();

    expect(runtime.status).toEqual(RuntimeStatus.RUNNING);
  });

  it('should give up after too many server errors', async () => {
    mockGetRuntimeCalls([{ status: RuntimeStatus.CREATING }]);
    for (let i = 0; i < 20; i++) {
      mockGetRuntime.mockRejectedValueOnce(new Response(null, { status: 503 }));
    }

    // Tell Jest that we plan to have 1 assertion. This ensures that the test won't
    // pass if the promise fails.
    expect.assertions(1);
    try {
      await runInitializerAndTimers();
    } catch (error) {
      expect(error.message).toMatch(/max server error count/i);
    }
  });

  it('should timeout after max delay', async () => {
    mockGetRuntime.mockImplementation(async () => {
      return { status: RuntimeStatus.STARTING };
    });

    // There's some nuance / awkwardness to this test: the LeoRuntimeInitializer uses Date.now() to get
    // the current timestamp, but Jest doesn't support fake clock functionality (see
    // https://github.com/facebook/jest/issues/2684). So we just set a very quick timeout here to
    // ensure the threshold is reached after a couple polling loops.
    expect.assertions(1);
    try {
      await runInitializerAndTimers(
        { overallTimeout: 30 },
        /* maxLoops */ 20000
      );
    } catch (error) {
      expect(error.message).toMatch(/max time allowed/i);
    }
  });

  it('should reject promise after abort signal', async () => {
    mockGetRuntime.mockImplementation(async () => {
      return { status: RuntimeStatus.STARTING };
    });
    const aborter = new AbortController();

    const initializePromise = runInitializerAndTimers({
      pollAbortSignal: aborter.signal,
    });
    // Wait a reasonably-short amount of time, at least one polling delay period, before sending
    // an abort signal.
    await new Promise((resolve) => setTimeout(resolve, 20));
    aborter.abort();

    expect.assertions(1);
    try {
      await initializePromise;
    } catch (error) {
      expect(error.message).toMatch(/aborted/i);
    }
  });

  it('should respect the maxCreateCount option', async () => {
    // Ensure that the initializer won't take action on a NOT_FOUND runtime if the maxCreateCount
    // is set to disallow create requests.
    mockGetRuntime.mockRejectedValue(new Response(null, { status: 404 }));
    try {
      await runInitializerAndTimers({
        maxCreateCount: 0,
        targetRuntime: runtimePresets.generalAnalysis.runtimeTemplate,
      });
    } catch (error) {
      expect(error.message).toMatch(/max runtime create count/i);
    }
  });

  it('should respect the maxResumeCount option', async () => {
    mockGetRuntimeCalls([{ status: RuntimeStatus.STOPPED }]);
    try {
      await runInitializerAndTimers({ maxResumeCount: 0 });
    } catch (error) {
      expect(mockCreateRuntime).not.toHaveBeenCalled();
      expect(error.message).toMatch(/max runtime resume count/i);
    }
  });

  it('should respect custom resolutionCondition', async () => {
    mockGetRuntimeCalls([{ status: RuntimeStatus.STOPPED }]);
    await runInitializerAndTimers({
      resolutionCondition: (runtime) =>
        runtime.status === RuntimeStatus.STOPPED,
    });
    expect(mockStartRuntime).not.toHaveBeenCalled();
  });
});

// intended to document the current behavior of this peculiar method
describe(throwRuntimeNotFound.name, () => {
  it('should use the preset default if targetRuntime and currentRuntime both do not exist', () => {
    const currentRuntime = undefined;
    const gcePersistentDisk = undefined;

    try {
      const callSucceeded = true;
      throwRuntimeNotFound(currentRuntime, gcePersistentDisk);
      expect(callSucceeded).toBeFalsy();
    } catch (e) {
      expect(e).toBeInstanceOf(InitialRuntimeNotFoundError);
      expect(e.defaultRuntime).toEqual(
        runtimePresets.generalAnalysis.runtimeTemplate
      );
    }
  });

  it('should apply a preset override if targetRuntime does not exist but currentRuntime exists and is not DELETED', () => {
    const currentRuntime = {
      ...defaultRuntimeWithPd(),
      gceWithPdConfig: {
        ...defaultRuntimeWithPd().gceWithPdConfig,
        machineType: 'n1-standard-2',
      },
    };
    const gcePersistentDisk = undefined;

    // preconditions check to ensure test validity

    expect(currentRuntime.status).not.toEqual(RuntimeStatus.DELETED);
    expect(currentRuntime.dataprocConfig).toBeUndefined();
    expect(currentRuntime.gceConfig).toBeUndefined();
    expect(currentRuntime.gceWithPdConfig).not.toBeUndefined();

    // an arbitrary non-PD field - later we see that this has been overridden by the preset
    expect(currentRuntime.gceWithPdConfig.machineType).not.toEqual(
      runtimePresets.generalAnalysis.runtimeTemplate.gceWithPdConfig.machineType
    );
    // an arbitrary PD field besides name differs - later we see that this has been overridden by the preset
    expect(currentRuntime.gceWithPdConfig.persistentDisk.size).not.toEqual(
      runtimePresets.generalAnalysis.runtimeTemplate.gceWithPdConfig
        .persistentDisk.size
    );
    // later we see that the name field has NOT been overridden by the preset
    expect(currentRuntime.gceWithPdConfig.persistentDisk.name).not.toEqual(
      runtimePresets.generalAnalysis.runtimeTemplate.gceWithPdConfig
        .persistentDisk.name
    );

    // now begin actual test

    try {
      const callSucceeded = true;
      throwRuntimeNotFound(currentRuntime, gcePersistentDisk);
      expect(callSucceeded).toBeFalsy();
    } catch (e) {
      expect(e).toBeInstanceOf(InitialRuntimeNotFoundError);

      const runtimePresetConfigWithOriginalName = {
        ...runtimePresets.generalAnalysis.runtimeTemplate.gceWithPdConfig,
        persistentDisk: {
          ...runtimePresets.generalAnalysis.runtimeTemplate.gceWithPdConfig
            .persistentDisk,
          name: currentRuntime.gceWithPdConfig.persistentDisk.name,
        },
      };
      expect(e.defaultRuntime.gceWithPdConfig).toEqual(
        runtimePresetConfigWithOriginalName
      );
    }
  });

  it(
    'should apply a preset override and connect an existing disk when: ' +
      'targetRuntime does not exist, currentRuntime exists, currentRuntime is DELETED',
    () => {
      const currentRuntime = {
        ...defaultRuntime(),
        status: RuntimeStatus.DELETED,
        gceConfig: {
          ...defaultRuntime().gceConfig,
          machineType: 'n1-standard-2',
        },
      };
      const gcePersistentDisk = stubDisk();

      // preconditions check to ensure test validity

      expect(currentRuntime.dataprocConfig).toBeUndefined();
      expect(currentRuntime.gceConfig).not.toBeUndefined(); // deleted runtimes do not have PDs attached, by definition
      expect(currentRuntime.gceWithPdConfig).toBeUndefined();

      expect(
        runtimePresets.generalAnalysis.runtimeTemplate.gceWithPdConfig
          .persistentDisk.name
      ).not.toEqual(gcePersistentDisk.name);

      // an arbitrary field - later we see that this GCE field has been overridden by the preset GCE With PD field
      expect(currentRuntime.gceConfig.machineType).not.toEqual(
        runtimePresets.generalAnalysis.runtimeTemplate.gceWithPdConfig
          .machineType
      );

      // now begin actual test

      try {
        const callSucceeded = true;
        throwRuntimeNotFound(currentRuntime, gcePersistentDisk);
        expect(callSucceeded).toBeFalsy();
      } catch (e) {
        expect(e).toBeInstanceOf(InitialRuntimeNotFoundError);

        const runtimePresetPdWithExistingPdName: PersistentDiskRequest = {
          ...runtimePresets.generalAnalysis.runtimeTemplate.gceWithPdConfig
            .persistentDisk,
          name: gcePersistentDisk.name,
        };

        expect(e.defaultRuntime.dataprocConfig).toBeUndefined();
        expect(e.defaultRuntime.gceConfig).toBeUndefined(); // transformed by attaching the PD
        expect(e.defaultRuntime.gceWithPdConfig).not.toBeUndefined();
        expect(e.defaultRuntime.gceWithPdConfig.machineType).toEqual(
          runtimePresets.generalAnalysis.runtimeTemplate.gceWithPdConfig
            .machineType
        );
        expect(e.defaultRuntime.gceWithPdConfig.persistentDisk).toEqual(
          runtimePresetPdWithExistingPdName
        );
      }
    }
  );

  it(
    'should apply a preset override with an undefined persistent disk name when: ' +
      'targetRuntime does not exist, currentRuntime exists, currentRuntime is DELETED, gcePersistentDisk does not exist',
    () => {
      const currentRuntime = {
        ...defaultRuntime(),
        status: RuntimeStatus.DELETED,
        gceConfig: {
          ...defaultRuntime().gceConfig,
          machineType: 'n1-standard-2',
        },
      };
      const gcePersistentDisk = undefined;

      // preconditions check to ensure test validity

      expect(currentRuntime.dataprocConfig).toBeUndefined();
      expect(currentRuntime.gceConfig).not.toBeUndefined(); // deleted runtimes do not have PDs attached, by definition
      expect(currentRuntime.gceWithPdConfig).toBeUndefined();

      expect(
        runtimePresets.generalAnalysis.runtimeTemplate.gceWithPdConfig
          .persistentDisk.name
      ).not.toBeUndefined();

      // an arbitrary field - later we see that this GCE field has been overridden by the preset GCE With PD field
      expect(currentRuntime.gceConfig.machineType).not.toEqual(
        runtimePresets.generalAnalysis.runtimeTemplate.gceWithPdConfig
          .machineType
      );

      // now begin actual test

      try {
        const callSucceeded = true;
        throwRuntimeNotFound(currentRuntime, gcePersistentDisk);
        expect(callSucceeded).toBeFalsy();
      } catch (e) {
        expect(e).toBeInstanceOf(InitialRuntimeNotFoundError);

        const runtimePresetWithNoPd: PersistentDiskRequest = {
          ...runtimePresets.generalAnalysis.runtimeTemplate.gceWithPdConfig
            .persistentDisk,
          name: undefined,
        };

        expect(e.defaultRuntime.dataprocConfig).toBeUndefined();
        expect(e.defaultRuntime.gceConfig).toBeUndefined(); // transformed by attaching the PD
        expect(e.defaultRuntime.gceWithPdConfig).not.toBeUndefined();
        expect(e.defaultRuntime.gceWithPdConfig.machineType).toEqual(
          runtimePresets.generalAnalysis.runtimeTemplate.gceWithPdConfig
            .machineType
        );
        expect(e.defaultRuntime.gceWithPdConfig.persistentDisk).toEqual(
          runtimePresetWithNoPd
        );
      }
    }
  );
});
