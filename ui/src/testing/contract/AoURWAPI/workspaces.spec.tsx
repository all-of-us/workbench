/**
 * @jest-environment jsdom
 */
import * as React from 'react';

import {
  Configuration as FetchConfiguration,
  WorkspacesApi,
} from 'generated/fetch';

import { MatchersV3, PactV3 } from '@pact-foundation/pact';
import { WorkspaceList } from 'app/pages/workspace/workspace-list';
import {
  clearApiClients,
  registerApiClient,
  workspacesApi,
} from 'app/services/swagger-fetch-clients';
import { getAccessToken } from 'app/utils/authentication';
import path from 'path';
import portableFetch from 'portable-fetch';

import { buildWorkspaceResponseTemplate } from 'testing/contract/utils';

const { arrayContaining, like, eachLike } = MatchersV3;

// Create a 'pact' between the two applications in the integration we are testing
const provider = new PactV3({
  dir: path.resolve(process.cwd(), 'pacts'),
  consumer: 'AoURWUI',
  provider: 'AoURWAPI_Workspaces',
});

const setup = (mockserver) => {
  registerApiClient(
    WorkspacesApi,
    new (class extends WorkspacesApi {
      constructor() {
        super();
        this.configuration = new FetchConfiguration({
          basePath: mockserver.url,
          accessToken: () => getAccessToken(),
        });
        this.basePath = mockserver.url;
        this.fetch = portableFetch;
      }
    })()
  );
};

describe('WorkspaceList', () => {
  afterEach(() => {
    clearApiClients();
  });
  it('displays the correct number of workspaces', async () => {
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
            ...buildWorkspaceResponseTemplate(['A', 'B', 'C'])
          ),
        },
      });

    return provider.executeTest(async (mockserver) => {
      setup(mockserver);
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
