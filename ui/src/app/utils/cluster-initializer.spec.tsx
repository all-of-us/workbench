import * as React from 'react';

import {notebooksClusterApi, registerApiClient as registerApiClientNotebooks} from 'app/services/notebooks-swagger-fetch-clients';
import {clusterApi, registerApiClient} from 'app/services/swagger-fetch-clients';
import {ClusterInitializer} from 'app/utils/cluster-initializer';
import {ClusterInitializerOptions} from 'app/utils/cluster-initializer';
import {Cluster} from 'generated/fetch';
import {ClusterStatus} from 'generated/fetch';
import {ClusterApi} from 'generated/fetch/api';
import SpyInstance = jest.SpyInstance;
import expect = jest.Expect;
import {ClusterApi as NotebooksClusterApi} from 'notebooks-generated/fetch';
import {ClusterApiStub} from 'testing/stubs/cluster-api-stub';
import {NotebooksClusterApiStub} from 'testing/stubs/notebooks-cluster-api-stub';

let mockGetCluster: SpyInstance;
let mockCreateCluster: SpyInstance;
let mockDeleteCluster: SpyInstance;
let mockStartCluster: SpyInstance;

const baseCluster: Cluster = {
  clusterName: 'aou-rw-3',
  clusterNamespace: 'aou-rw-12345',
  status: ClusterStatus.Running
};

describe('ClusterInitializer', () => {

  beforeEach(() => {
    jest.useFakeTimers();

    registerApiClient(ClusterApi, new ClusterApiStub());
    registerApiClientNotebooks(NotebooksClusterApi, new NotebooksClusterApiStub());

    mockGetCluster = jest.spyOn(clusterApi(), 'getCluster');
    mockCreateCluster = jest.spyOn(clusterApi(), 'createCluster');
    mockDeleteCluster = jest.spyOn(clusterApi(), 'deleteCluster');
    mockStartCluster = jest.spyOn(notebooksClusterApi(), 'startCluster');
  });

  afterEach(() => {
    jest.useRealTimers();
    jest.clearAllMocks();
  });

  const mockGetClusterCalls = (baseOverrides: Array<Partial<Cluster>>) => {
    const clusters: Array<Cluster> = baseOverrides.map(
      override => {
        return {...baseCluster, ...override};
      });
    for (const cluster of clusters) {
      mockGetCluster.mockImplementationOnce((workspaceNamespace) => {
        return cluster;
      });
    }
  };

  /**
   * This helper function allows us to call Jest's mock-timer progression function, runAllTimers,
   * an arbitrary number of times until the given Promise is settled.
   *
   * This is helpful for testing the initializer, since it has a Promise-based API but relies on
   * a polling strategy using setTimeout calls in order to wait for the cluster to become ready.
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

  const runInitializerAndTimers = async(options?: Partial<ClusterInitializerOptions>, maxLoops?: number): Promise<Cluster> => {
    const clusterPromise = ClusterInitializer.initialize({
      workspaceNamespace: 'aou-rw-12345',
      ...options
    });
    await runTimersUntilSettled(clusterPromise, maxLoops);
    return await clusterPromise;
  };

  it('should resolve promise if cluster is in ready state', async() => {
    // This tests the simplest case of the initializer. No polling necessary.
    mockGetClusterCalls([baseCluster]);
    const cluster = await runInitializerAndTimers();
    expect(cluster.status).toEqual(ClusterStatus.Running);
  });

  it('should resume cluster if it is initially stopping / stopped', async() => {
    // This test case also includes some repeated statuses, since that is a more realistic
    // reflection of how we poll Leo & get occasional new statuses.
    mockGetClusterCalls([
      {status: ClusterStatus.Stopping},
      {status: ClusterStatus.Stopping},
      {status: ClusterStatus.Stopped},
      {status: ClusterStatus.Stopped},
      // Here is where we expect, chronologically, a call to startCluster. The next two
      // cluster statuses mock out what we might expect to see after that call.
      {status: ClusterStatus.Starting},
      {status: ClusterStatus.Starting},
      {status: ClusterStatus.Starting},
      {status: ClusterStatus.Running}
    ]);
    const cluster = await runInitializerAndTimers();
    expect(mockStartCluster).toHaveBeenCalled();
    expect(cluster.status).toEqual(ClusterStatus.Running);
  });

  it('should call callback with cluster status', async() => {
    mockGetClusterCalls([
      {status: ClusterStatus.Stopped},
    ]);
    mockGetCluster.mockRejectedValueOnce(new Response(null, {status: 404}));
    mockGetClusterCalls([
      {status: ClusterStatus.Starting},
      {status: ClusterStatus.Starting},
      {status: ClusterStatus.Running}
    ]);
    const statuses = [];
    await runInitializerAndTimers({onStatusUpdate: (status) => statuses.push(status)});

    expect(statuses).toEqual([
      ClusterStatus.Stopped,
      // A null value is passed when a cluster is not found.
      null,
      // Note: onStatusUpdate will be called for every status received, not just when the status
      // value is changed.
      ClusterStatus.Starting,
      ClusterStatus.Starting,
      ClusterStatus.Running]
    );
  });

  it('should create cluster if it is initially nonexistent', async() => {
    mockGetCluster.mockRejectedValueOnce(new Response(null, {status: 404}));
    mockCreateCluster.mockImplementationOnce(async(workspaceNamespace) => {
      return {status: ClusterStatus.Creating};
    });
    mockGetClusterCalls([
      {status: ClusterStatus.Starting},
      {status: ClusterStatus.Running}
    ]);
    const cluster = await runInitializerAndTimers();

    expect(mockCreateCluster).toHaveBeenCalled();
    expect(cluster.status).toEqual(ClusterStatus.Running);
  });

  it('should delete cluster if in an error state', async() => {
    // A cluster in an error state should trigger a deletion request.
    mockGetClusterCalls([
      {status: ClusterStatus.Creating},
      {status: ClusterStatus.Error},
    ]);
    mockDeleteCluster.mockImplementationOnce(async(workspaceNamespace) => {
      return {};
    });
    mockGetClusterCalls([
      {status: ClusterStatus.Deleting},
      {status: ClusterStatus.Deleting},
    ]);
    // After some period of "deleting" status, we expect the cluster to become nonexistent...
    mockGetCluster.mockRejectedValueOnce(new Response(null, {status: 404}));
    // which should trigger a creation request...
    mockCreateCluster.mockImplementationOnce(async(workspaceNamespace) => {
      return {status: ClusterStatus.Creating};
    });
    // and eventually give us a good cluster.
    mockGetClusterCalls([
      {status: ClusterStatus.Starting},
      {status: ClusterStatus.Running}
    ]);

    const cluster = await runInitializerAndTimers();

    expect(mockDeleteCluster).toHaveBeenCalled();
    expect(mockCreateCluster).toHaveBeenCalled();
    expect(cluster.status).toEqual(ClusterStatus.Running);
  });

  it('should recover from intermittent 500s', async() => {
    mockGetClusterCalls([
      {status: ClusterStatus.Creating},
      {status: ClusterStatus.Creating},
    ]);
    mockGetCluster.mockRejectedValueOnce(new Response(null, {status: 503}));
    mockGetCluster.mockRejectedValueOnce(new Response(null, {status: 503}));
    mockGetClusterCalls([
      {status: ClusterStatus.Running},
    ]);

    const cluster = await runInitializerAndTimers();

    expect(cluster.status).toEqual(ClusterStatus.Running);
  });

  it('should give up after too many server errors', async() => {
    mockGetClusterCalls([
      {status: ClusterStatus.Creating},
    ]);
    for (let i = 0; i < 20; i++) {
      mockGetCluster.mockRejectedValueOnce(new Response(null, {status: 503}));
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
    mockGetCluster.mockImplementation(async(workspaceNamespace) => {
      return {status: ClusterStatus.Starting};
    });

    // There's some nuance / awkwardness to this test: the ClusterInitializer uses Date.now() to get
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
    mockGetCluster.mockImplementation(async(workspaceNamespace) => {
      return {status: ClusterStatus.Starting};
    });
    const aborter = new AbortController();

    const initializePromise = runInitializerAndTimers({abortSignal: aborter.signal});
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
    // Mock out getCluster API responses which simulate a cluster in an error state, which is then
    // reset, but ends up in an error state again. This scenario should warrant two deleteCluster
    // calls, but the initializer is configured to max out at 1 and should return an error.
    mockGetClusterCalls([
      {status: ClusterStatus.Error},
      {status: ClusterStatus.Deleting},
      {status: ClusterStatus.Creating},
      {status: ClusterStatus.Error},
    ]);
    mockDeleteCluster.mockImplementation(async(workspaceNamespace) => {
      return {};
    });
    mockCreateCluster.mockImplementation(async(workspaceNamespace) => {
      return {status: ClusterStatus.Creating};
    });

    expect.assertions(2);
    try {
     await runInitializerAndTimers({maxDeleteCount: 1});
    } catch (error) {
      expect(mockDeleteCluster).toHaveBeenCalledTimes(1);
      expect(error.message).toMatch(/max cluster delete count/i);
    }
  });

  it('should respect the maxCreateCount option', async() => {
    // Ensure that the initializer won't take action on a NOT_FOUND cluster if the maxCreateCount
    // is set to disallow create requests.
    mockGetCluster.mockRejectedValueOnce(new Response(null, {status: 404}));
    try {
      await runInitializerAndTimers({maxCreateCount: 0});
    } catch (error) {
      expect(mockCreateCluster).not.toHaveBeenCalled();
      expect(error.message).toMatch(/max cluster create count/i);
    }
  });

  it('should respect the maxResumeCount option', async() => {
    mockGetClusterCalls([
      {status: ClusterStatus.Stopped},
    ]);
    try {
      await runInitializerAndTimers({maxResumeCount: 0});
    } catch (error) {
      expect(mockCreateCluster).not.toHaveBeenCalled();
      expect(error.message).toMatch(/max cluster resume count/i);
    }
  });

});
