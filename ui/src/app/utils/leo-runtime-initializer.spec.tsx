import * as React from 'react';

import {leoRuntimesApi, registerApiClient as registerApiClientNotebooks} from 'app/services/notebooks-swagger-fetch-clients';
import {runtimeApi, registerApiClient} from 'app/services/swagger-fetch-clients';
import {LeoRuntimeInitializer, LeoRuntimeInitializerOptions} from 'app/utils/leo-runtime-initializer';
import {Runtime} from 'generated/fetch';
import {RuntimeStatus} from 'generated/fetch';
import {RuntimeApi} from 'generated/fetch/api';
import SpyInstance = jest.SpyInstance;
import expect = jest.Expect;
import {RuntimesApi as LeoRuntimesApi} from 'notebooks-generated/fetch';
import {defaultRuntime, RuntimeApiStub} from 'testing/stubs/runtime-api-stub';
import {LeoRuntimesApiStub} from 'testing/stubs/leo-runtimes-api-stub';
import {RuntimeConfigurationType} from 'generated/fetch';
import {serverConfigStore} from "./navigation";
import {runtimePresets} from './runtime-presets';

let mockGetRuntime: SpyInstance;
let mockCreateRuntime: SpyInstance;
let mockDeleteRuntime: SpyInstance;
let mockStartRuntime: SpyInstance;

const baseRuntime: Runtime = {
  runtimeName: 'aou-rw-3',
  googleProject: 'aou-rw-12345',
  status: RuntimeStatus.Running,
  createdDate: '08/08/2018',
  toolDockerImage: 'docker',
  configurationType: RuntimeConfigurationType.GeneralAnalysis
};

const workspaceNamespace = 'aou-rw-12345';

describe('RuntimeInitializer', () => {

  beforeEach(() => {
    jest.useFakeTimers();

    registerApiClient(RuntimeApi, new RuntimeApiStub());
    registerApiClientNotebooks(LeoRuntimesApi, new LeoRuntimesApiStub());

    mockGetRuntime = jest.spyOn(runtimeApi(), 'getRuntime');
    mockCreateRuntime = jest.spyOn(runtimeApi(), 'createRuntime');
    mockDeleteRuntime = jest.spyOn(runtimeApi(), 'deleteRuntime');
    mockStartRuntime = jest.spyOn(leoRuntimesApi(), 'startRuntime');

    serverConfigStore.next({gsuiteDomain: 'researchallofus.org'});
  });

  afterEach(() => {
    jest.useRealTimers();
    jest.clearAllMocks();
  });

  const mockGetRuntimeCalls = (baseOverrides: Array<Partial<Runtime>>) => {
    const runtimes: Array<Runtime> = baseOverrides.map(
      override => {
        return {...baseRuntime, ...override};
      });
    for (const runtime of runtimes) {
      mockGetRuntime.mockImplementationOnce((workspaceNamespace) => {
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
  const runTimersUntilSettled = async(p: Promise<any>, maxLoops: number = 20) => {
    let isSettled = false;
    p.then(() => isSettled = true).catch((e) => {
      isSettled = true;
    });
    let i = 0;
    while (!isSettled) {
      i++;
      if (i > maxLoops) {
        throw new Error('Max number of timer cycles reached waiting for Promise to settle');
      }

      await new Promise(setImmediate);
      jest.runAllTimers();
    }
  };

  const runInitializerAndTimers = async(options?: Partial<LeoRuntimeInitializerOptions>, maxLoops?: number): Promise<Runtime> => {
    const runtimePromise = LeoRuntimeInitializer.initialize({
      workspaceNamespace: workspaceNamespace,
      ...options
    });
    await runTimersUntilSettled(runtimePromise, maxLoops);
    return await runtimePromise;
  };

  it('should resolve promise if runtime is in ready state', async() => {
    // This tests the simplest case of the initializer. No polling necessary.
    mockGetRuntimeCalls([baseRuntime]);
    const runtime = await runInitializerAndTimers();
    expect(runtime.status).toEqual(RuntimeStatus.Running);
  });

  it('should resume runtime if it is initially stopping / stopped', async() => {
    // This test case also includes some repeated statuses, since that is a more realistic
    // reflection of how we poll Leo & get occasional new statuses.
    mockGetRuntimeCalls([
      {status: RuntimeStatus.Stopping},
      {status: RuntimeStatus.Stopping},
      {status: RuntimeStatus.Stopped},
      {status: RuntimeStatus.Stopped},
      // Here is where we expect, chronologically, a call to startRuntime. The next two
      // runtime statuses mock out what we might expect to see after that call.
      {status: RuntimeStatus.Starting},
      {status: RuntimeStatus.Starting},
      {status: RuntimeStatus.Starting},
      {status: RuntimeStatus.Running}
    ]);
    const runtime = await runInitializerAndTimers();
    expect(mockStartRuntime).toHaveBeenCalled();
    expect(runtime.status).toEqual(RuntimeStatus.Running);
  });

  it('should call callback with runtime status', async() => {
    mockGetRuntimeCalls([
      {status: RuntimeStatus.Stopped},
    ]);
    mockGetRuntime.mockRejectedValueOnce(new Response(null, {status: 404}));
    mockGetRuntimeCalls([
      {status: RuntimeStatus.Starting},
      {status: RuntimeStatus.Starting},
      {status: RuntimeStatus.Running}
    ]);
    const statuses = [];
    await runInitializerAndTimers({onPoll: (runtime) => statuses.push(runtime ? runtime.status : null)});

    expect(statuses).toEqual([
      RuntimeStatus.Stopped,
      // A null value is passed when a runtime is not found.
      null,
      // Note: onStatusUpdate will be called for every status received, not just when the status
      // value is changed.
      RuntimeStatus.Starting,
      RuntimeStatus.Starting,
      RuntimeStatus.Running]
    );
  });

  it('should create runtime if it is initially nonexistent', async() => {
    mockGetRuntime.mockRejectedValueOnce(new Response(null, {status: 404}));
    mockCreateRuntime.mockImplementationOnce(async(workspaceNamespace) => {
      return {status: RuntimeStatus.Creating};
    });
    mockGetRuntimeCalls([
      {status: RuntimeStatus.Starting},
      {status: RuntimeStatus.Running}
    ]);
    const runtime = await runInitializerAndTimers();

    expect(mockCreateRuntime).toHaveBeenCalled();
    expect(runtime.status).toEqual(RuntimeStatus.Running);
  });

  it('should lazily create user\'s most runtime if a valid one exists', async() => {
    serverConfigStore.next({gsuiteDomain: 'researchallofus.org'});
    mockGetRuntime.mockImplementation(namespace => {
      return {
        ...defaultRuntime(),
        configurationType: RuntimeConfigurationType.UserOverride,
        gceConfig: {
          diskSize: 777,
          machineType: 'n1-standard-16'
        },
        status: RuntimeStatus.Deleted
      }; });

    LeoRuntimeInitializer.initialize({
      workspaceNamespace: workspaceNamespace,
    });
    await new Promise(setImmediate);

    expect(mockCreateRuntime).toHaveBeenCalledWith(workspaceNamespace, jasmine.objectContaining({
      gceConfig: {
        diskSize: 777,
        machineType: 'n1-standard-16'
      }
    }), jasmine.any(Object));
  });

  it('should use preset values during lazy runtime creation if a preset was selected', async() => {
    serverConfigStore.next({gsuiteDomain: 'researchallofus.org'});
    mockGetRuntime.mockImplementation(namespace => {
      return {
        ...defaultRuntime(),
        configurationType: RuntimeConfigurationType.GeneralAnalysis,
        gceConfig: {
          diskSize: 777,
          machineType: 'n1-standard-16'
        },
        status: RuntimeStatus.Deleted
      }; });

    LeoRuntimeInitializer.initialize({
      workspaceNamespace: workspaceNamespace,
    });
    await new Promise(setImmediate);

    expect(mockCreateRuntime).toHaveBeenCalledWith(workspaceNamespace, jasmine.objectContaining({
      gceConfig: {
        diskSize: runtimePresets.generalAnalysis.runtimeTemplate.gceConfig.diskSize,
        machineType: runtimePresets.generalAnalysis.runtimeTemplate.gceConfig.machineType
      }
    }), jasmine.any(Object));
  });

  it('should delete runtime if in an error state', async() => {
    // A runtime in an error state should trigger a deletion request.
    mockGetRuntimeCalls([
      {status: RuntimeStatus.Creating},
      {status: RuntimeStatus.Error},
    ]);
    mockDeleteRuntime.mockImplementationOnce(async(workspaceNamespace) => {
      return {};
    });
    mockGetRuntimeCalls([
      {status: RuntimeStatus.Deleting},
      {status: RuntimeStatus.Deleting},
    ]);
    // After some period of "deleting" status, we expect the runtime to become nonexistent...
    mockGetRuntime.mockRejectedValueOnce(new Response(null, {status: 404}));
    // which should trigger a creation request...
    mockCreateRuntime.mockImplementationOnce(async(workspaceNamespace) => {
      return {status: RuntimeStatus.Creating};
    });
    // and eventually give us a good runtime.
    mockGetRuntimeCalls([
      {status: RuntimeStatus.Starting},
      {status: RuntimeStatus.Running}
    ]);

    const runtime = await runInitializerAndTimers();

    expect(mockDeleteRuntime).toHaveBeenCalled();
    expect(mockCreateRuntime).toHaveBeenCalled();
    expect(runtime.status).toEqual(RuntimeStatus.Running);
  });

  it('should recover from intermittent 500s', async() => {
    mockGetRuntimeCalls([
      {status: RuntimeStatus.Creating},
      {status: RuntimeStatus.Creating},
    ]);
    mockGetRuntime.mockRejectedValueOnce(new Response(null, {status: 503}));
    mockGetRuntime.mockRejectedValueOnce(new Response(null, {status: 503}));
    mockGetRuntimeCalls([
      {status: RuntimeStatus.Running},
    ]);

    const runtime = await runInitializerAndTimers();

    expect(runtime.status).toEqual(RuntimeStatus.Running);
  });

  it('should give up after too many server errors', async() => {
    mockGetRuntimeCalls([
      {status: RuntimeStatus.Creating},
    ]);
    for (let i = 0; i < 20; i++) {
      mockGetRuntime.mockRejectedValueOnce(new Response(null, {status: 503}));
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

  it('should timeout after max delay', async() => {
    mockGetRuntime.mockImplementation(async(workspaceNamespace) => {
      return {status: RuntimeStatus.Starting};
    });

    // There's some nuance / awkwardness to this test: the LeoRuntimeInitializer uses Date.now() to get
    // the current timestamp, but Jest doesn't support fake clock functionality (see
    // https://github.com/facebook/jest/issues/2684). So we just set a very quick timeout here to
    // ensure the threshold is reached after a couple polling loops.
    expect.assertions(1);
    try {
      await runInitializerAndTimers({overallTimeout: 30}, /* maxLoops */ 20000);
    } catch (error) {
      expect(error.message).toMatch(/max time allowed/i);
    }
  });

  it('should reject promise after abort signal', async() => {
    mockGetRuntime.mockImplementation(async(workspaceNamespace) => {
      return {status: RuntimeStatus.Starting};
    });
    const aborter = new AbortController();

    const initializePromise = runInitializerAndTimers({pollAbortSignal: aborter.signal});
    // Wait a reasonably-short amount of time, at least one polling delay period, before sending
    // an abort signal.
    await new Promise(resolve => setTimeout(resolve, 20));
    aborter.abort();

    expect.assertions(1);
    try {
      await initializePromise;
    } catch (error) {
      expect(error.message).toMatch(/aborted/i);
    }
  });

  it('should respect the maxDeleteCount option', async() => {
    // Mock out getRuntime API responses which simulate a runtime in an error state, which is then
    // reset, but ends up in an error state again. This scenario should warrant two deleteRuntime
    // calls, but the initializer is configured to max out at 1 and should return an error.
    mockGetRuntimeCalls([
      {status: RuntimeStatus.Error},
      {status: RuntimeStatus.Deleting},
      {status: RuntimeStatus.Creating},
      {status: RuntimeStatus.Error},
    ]);
    mockDeleteRuntime.mockImplementation(async(workspaceNamespace) => {
      return {};
    });
    mockCreateRuntime.mockImplementation(async(workspaceNamespace) => {
      return {status: RuntimeStatus.Creating};
    });

    expect.assertions(2);
    try {
     await runInitializerAndTimers({maxDeleteCount: 1});
    } catch (error) {
      expect(mockDeleteRuntime).toHaveBeenCalledTimes(1);
      expect(error.message).toMatch(/max runtime delete count/i);
    }
  });

  it('should respect the maxCreateCount option', async() => {
    // Ensure that the initializer won't take action on a NOT_FOUND runtime if the maxCreateCount
    // is set to disallow create requests.
    mockGetRuntime.mockRejectedValueOnce(new Response(null, {status: 404}));
    try {
      await runInitializerAndTimers({maxCreateCount: 0});
    } catch (error) {
      expect(mockCreateRuntime).not.toHaveBeenCalled();
      expect(error.message).toMatch(/max runtime create count/i);
    }
  });

  it('should respect the maxResumeCount option', async() => {
    mockGetRuntimeCalls([
      {status: RuntimeStatus.Stopped},
    ]);
    try {
      await runInitializerAndTimers({maxResumeCount: 0});
    } catch (error) {
      expect(mockCreateRuntime).not.toHaveBeenCalled();
      expect(error.message).toMatch(/max runtime resume count/i);
    }
  });

  it('should respect custom resolutionCondition', async() => {
    mockGetRuntimeCalls([{status: RuntimeStatus.Stopped}]);
    await runInitializerAndTimers({resolutionCondition: (runtime) => runtime.status === RuntimeStatus.Stopped});
    expect(mockStartRuntime).not.toHaveBeenCalled();
  })
});
