import { Runtime, RuntimeStatus } from 'generated/fetch';
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
import { RuntimeApiStub } from 'testing/stubs/runtime-api-stub';

import { runtimePresets } from './runtime-presets';

let mockGetRuntime: jest.SpyInstance;
let mockCreateRuntime: jest.SpyInstance;
let mockStartRuntime: jest.SpyInstance;

const baseRuntime: Runtime = {
  runtimeName: 'aou-rw-3',
  googleProject: 'aou-rw-12345',
  status: RuntimeStatus.RUNNING,
  createdDate: '08/08/2018',
  toolDockerImage: 'docker',
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
      targetRuntime: runtimePresets().generalAnalysis.runtimeTemplate,
    });

    expect(mockCreateRuntime).toHaveBeenCalled();
    expect(runtime.status).toEqual(RuntimeStatus.RUNNING);
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
        targetRuntime: runtimePresets().generalAnalysis.runtimeTemplate,
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
        runtimePresets().generalAnalysis.runtimeTemplate
      );
    }
  });
});
