/**
 * @jest-environment jsdom
 */
import {
  Configuration as FetchConfiguration,
  WorkspacesApi,
} from 'generated/fetch';

import { MatchersV3, PactV3 } from '@pact-foundation/pact';
import {
  clearApiClients,
  registerApiClient,
  workspacesApi,
} from 'app/services/swagger-fetch-clients';
import path from 'path';
import portableFetch from 'portable-fetch';

import { buildWorkspaceResponseStubs } from 'testing/stubs/workspaces';

const { arrayContaining, like } = MatchersV3;

const provider = new PactV3({
  dir: path.resolve(process.cwd(), 'pacts'),
  consumer: 'AoURWUI',
  provider: 'AoURWAPI_Workspaces',
});

const runTestOnMockServer = (testFunction) => {
  return provider.executeTest(async (mockserver) => {
    // Act: test our API client behaves correctly
    registerApiClient(
      WorkspacesApi,
      new (class extends WorkspacesApi {
        constructor() {
          super();
          this.configuration = new FetchConfiguration({
            basePath: mockserver.url,
          });
          this.basePath = mockserver.url;
          this.fetch = portableFetch;
        }
      })()
    );

    await testFunction();
  });
};

describe('Workspaces', () => {
  afterEach(() => {
    clearApiClients();
  });
  it('Get workspaces when they exist', async () => {
    provider
      .given('User has workspaces')
      .uponReceiving('a request for workspaces associated with a user')
      .withRequest({
        method: 'GET',
        path: '/v1/workspaces',
        headers: {
          authorization: like('Bearer oranges'),
        },
      })
      .willRespondWith({
        status: 200,
        headers: { 'Content-Type': 'application/json' },
        body: {
          items: arrayContaining(
            ...buildWorkspaceResponseStubs(['A', 'B', 'C'])
          ),
        },
      });

    runTestOnMockServer(async () => {
      const workspacesReceived = (await workspacesApi().getWorkspaces()).items;
      expect(workspacesReceived.length).toEqual(3);
      const namespaces = workspacesReceived.map(
        (workspace) => workspace.workspace.namespace
      );
      expect(namespaces).toEqual(
        expect.arrayContaining([
          'defaultNamespaceC',
          'defaultNamespaceB',
          'defaultNamespaceA',
        ])
      );
    });
  });
});
