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
    registerApiClient(ClusterApi, new ClusterApiStub());
    registerApiClientNotebooks(NotebooksClusterApi, new NotebooksClusterApiStub());

    jest.resetAllMocks();
    mockGetCluster = jest.spyOn(clusterApi(), 'getCluster');
    mockCreateCluster = jest.spyOn(clusterApi(), 'createCluster');
    mockDeleteCluster = jest.spyOn(clusterApi(), 'deleteCluster');
    mockStartCluster = jest.spyOn(notebooksClusterApi(), 'startCluster');
  });

  const mockGetClusterCalls = (responses: Array<Cluster>) => {
    for (const response of responses) {
      mockGetCluster.mockImplementationOnce((workspaceNamespace) => {
        return response;
      });
    }
  };

  const createInitializer = (options?: Partial<ClusterInitializerOptions>): ClusterInitializer => {
    // It's not possible to use jest.useFakeTimers() to control timing of async functions and
    // promises (see https://github.com/facebook/jest/issues/7151), so we configure the initializer
    // to have an extremely-short polling delay.
    return new ClusterInitializer({
      workspaceNamespace: 'aou-rw-12345',
      initialPollingDelay: 10,
      maxPollingDelay: 20,
      overallTimeout: 300,
      ...options,
    });
  };

  it('should resolve promise if cluster is in ready state', async() => {
    // This tests the simplest case of the initializer. No polling necessary.
    mockGetClusterCalls([baseCluster]);
    const initializer = createInitializer();
    const cluster = await initializer.initialize();
    expect(cluster.status).toEqual(ClusterStatus.Running);
  });

  it('should resume cluster if it is initially stopped', async() => {
    mockGetClusterCalls([
      {...baseCluster, status: ClusterStatus.Stopped},
      // Here is where we expect, chronologically, a call to startCluster. The next two
      // cluster statuses mock out what we might expect to see after that call.
      {...baseCluster, status: ClusterStatus.Starting},
      {...baseCluster, status: ClusterStatus.Running}
    ]);
    const initializer = createInitializer();
    const cluster = await initializer.initialize();

    expect(mockStartCluster).toHaveBeenCalled();
    expect(cluster.status).toEqual(ClusterStatus.Running);
  });

  it('should call callback with cluster status', async() => {
    mockGetClusterCalls([
      {...baseCluster, status: ClusterStatus.Stopped},
      {...baseCluster, status: ClusterStatus.Starting},
      {...baseCluster, status: ClusterStatus.Running}
    ]);
    const mockFn = jest.fn();
    const initializer = createInitializer({
      onStatusUpdate: mockFn
    });

    const cluster = await initializer.initialize();

    expect(mockFn.mock.calls.length).toEqual(3);
    expect(mockFn.mock.calls).toEqual([
      [ClusterStatus.Stopped],
      [ClusterStatus.Starting],
      [ClusterStatus.Running]]
    );
  });

  it('should create cluster if it is initially nonexistent', async() => {
    mockGetCluster.mockImplementationOnce((workspaceNamespace) => {
      throw new Response(null, {status: 404});
    });
    mockCreateCluster.mockImplementationOnce(async(workspaceNamespace) => {
      return {...baseCluster, status: ClusterStatus.Creating};
    });
    mockGetClusterCalls([
      {...baseCluster, status: ClusterStatus.Starting},
      {...baseCluster, status: ClusterStatus.Running}
    ]);
    const initializer = createInitializer();
    const cluster = await initializer.initialize();

    expect(mockCreateCluster).toHaveBeenCalled();
    expect(cluster.status).toEqual(ClusterStatus.Running);
  });

  it('should delete cluster if in an error state', async() => {
    // A cluster in an error state should trigger a deletion request.
    mockGetClusterCalls([
      {...baseCluster, status: ClusterStatus.Creating},
      {...baseCluster, status: ClusterStatus.Error},
    ]);
    mockDeleteCluster.mockImplementationOnce(async(workspaceNamespace) => {
      return {};
    });
    mockGetClusterCalls([
      {...baseCluster, status: ClusterStatus.Deleting},
      {...baseCluster, status: ClusterStatus.Deleting},
    ]);
    // After some period of "deleting" status, we expect the cluster to become nonexistent...
    mockGetCluster.mockImplementationOnce(async(workspaceNamespace) => {
      throw new Response(null, {status: 404});
    });
    // which should trigger a creation request...
    mockCreateCluster.mockImplementationOnce(async(workspaceNamespace) => {
      return {...baseCluster, status: ClusterStatus.Creating};
    });
    // and eventually give us a good cluster.
    mockGetClusterCalls([
      {...baseCluster, status: ClusterStatus.Starting},
      {...baseCluster, status: ClusterStatus.Running}
    ]);

    const initializer = createInitializer();
    const cluster = await initializer.initialize();

    expect(mockDeleteCluster).toHaveBeenCalled();
    expect(mockCreateCluster).toHaveBeenCalled();
    expect(cluster.status).toEqual(ClusterStatus.Running);
  });

  it('Should recover from intermittent 500s', async() => {
    mockGetClusterCalls([
      {...baseCluster, status: ClusterStatus.Creating},
      {...baseCluster, status: ClusterStatus.Creating},
    ]);
    mockGetCluster.mockImplementationOnce(async(workspaceNamespace) => {
      throw new Response(null, {status: 503});
    });
    mockGetCluster.mockImplementationOnce(async(workspaceNamespace) => {
      throw new Response(null, {status: 503});
    });
    mockGetClusterCalls([
      {...baseCluster, status: ClusterStatus.Running},
    ]);

    const initializer = createInitializer();
    const cluster = await initializer.initialize();

    expect(cluster.status).toEqual(ClusterStatus.Running);
  });

  it('Should timeout after max delay', async() => {
    mockGetCluster.mockImplementation(async(workspaceNamespace) => {
      return {...baseCluster, status: ClusterStatus.Starting};
    });

    const initializer = createInitializer({
      overallTimeout: 100
    });

    // Tell Jest that we plan to have 1 assertion. This ensures that the test won't
    // pass if the promise fails.
    expect.assertions(1);
    return initializer.initialize().catch(error => {
      expect(error.message).toMatch(/max time allowed/i);
    });
  });

  it('Should reject promise after abort signal', async() => {
    mockGetCluster.mockImplementation(async(workspaceNamespace) => {
      return {...baseCluster, status: ClusterStatus.Starting};
    });
    const aborter = new AbortController();
    const initializer = createInitializer({
      abortSignal: aborter.signal
    });

    const initializePromise = initializer.initialize();

    await new Promise(resolve => setTimeout(resolve, 20));
    aborter.abort();

    expect.assertions(1);
    return initializePromise.catch(error => {
      expect(error.message).toMatch(/aborted/i);
      return Promise.resolve();
    });
  });

  it('Should reject promise before starting another restart cycle', async() => {
    // A cluster in an error state should trigger a deletion request.
    mockGetClusterCalls([
      {...baseCluster, status: ClusterStatus.Error},
      {...baseCluster, status: ClusterStatus.Deleting},
      {...baseCluster, status: ClusterStatus.Creating},
      {...baseCluster, status: ClusterStatus.Error},
    ]);
    mockDeleteCluster.mockImplementation(async(workspaceNamespace) => {
      return {};
    });
    mockCreateCluster.mockImplementation(async(workspaceNamespace) => {
      return {...baseCluster, status: ClusterStatus.Creating};
    });

    const initializer = createInitializer();
    const initializePromise = initializer.initialize();

    expect.assertions(1);
    return initializePromise.catch(error => {
      expect(error.message).toMatch(/max cluster delete count/i);
      return Promise.resolve();
    });
  });
});
