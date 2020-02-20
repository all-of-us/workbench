import * as React from 'react';

import {notebooksClusterApi, registerApiClient as registerApiClientNotebooks} from 'app/services/notebooks-swagger-fetch-clients';
import {clusterApi, registerApiClient} from 'app/services/swagger-fetch-clients';
import {ClusterInitializer} from 'app/utils/cluster-initializer';
import {Cluster} from 'generated/fetch';
import {ClusterApi} from 'generated/fetch/api';
import {ClusterApiStub} from 'testing/stubs/cluster-api-stub';
import {ClusterStatus} from 'generated/fetch';
import SpyInstance = jest.SpyInstance;
import expect = jest.Expect;
import {NotebooksApi, ClusterApi as NotebooksClusterApi} from 'notebooks-generated/fetch';
import {NotebooksApiStub} from 'testing/stubs/notebooks-api-stub';
import {NotebooksClusterApiStub} from 'testing/stubs/notebooks-cluster-api-stub';
import {ClusterInitializerOptions} from 'app/utils/cluster-initializer';

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
    jest.resetAllMocks();
    registerApiClient(ClusterApi, new ClusterApiStub());
    // registerApiClientNotebooks(NotebooksApi, new NotebooksApiStub());
    registerApiClientNotebooks(NotebooksClusterApi, new NotebooksClusterApiStub());

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
    // We configure the test initializer to have an extremely-short polling delay. Unfortunately,
    // it's not possible to use jest.useFakeTimers() to control timing of async functions and
    // promises (see https://github.com/facebook/jest/issues/7151), so our simplest workaround is
    // to just set very short delays.
    return new ClusterInitializer({
      workspaceNamespace: 'aou-rw-12345',
      initialPollingDelay: 10,
      maxPollingDelay: 20,
      overallTimeout: 300,
      ...options,
    });
  };

  it('should resolve promise if cluster is in ready state', async() => {
    // This tests the simplest case of the initializer: a single get call retrieves a cluster
    // in a running state. The Promise should
    mockGetClusterCalls([baseCluster]);
    const initializer = createInitializer();
    const cluster = await initializer.initialize();
    expect(cluster.status).toEqual(ClusterStatus.Running);
  });

  it('should resume cluster if it is initially stopped', async() => {
    mockGetClusterCalls([
      {...baseCluster, status: ClusterStatus.Stopped},
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
      expect(error).toMatch(/max time allowed/i);
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
      expect(error).toMatch(/aborted/i);
      return Promise.resolve();
    });
  });
});
