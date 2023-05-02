/**
 * @jest-environment jsdom
 */
import * as React from 'react';
import { ReactWrapper } from 'enzyme';

import {
  Configuration as FetchConfiguration,
  WorkspacesApi,
} from 'generated/fetch';

import { MatchersV3, PactV3 } from '@pact-foundation/pact';
import { Spinner } from 'app/components/spinners';
import { WorkspaceList } from 'app/pages/workspace/workspace-list';
import { exposeAccessTokenSetter } from 'app/services/setup';
import {
  clearApiClients,
  registerApiClient,
} from 'app/services/swagger-fetch-clients';
import { getAccessToken } from 'app/utils/authentication';
import { LOCAL_STORAGE_KEY_TEST_ACCESS_TOKEN } from 'app/utils/cookies';
import { authStore, profileStore, serverConfigStore } from 'app/utils/stores';
import path from 'path';
import portableFetch from 'portable-fetch';

import {
  mountWithRouter,
  waitForSelectorMissing,
} from 'testing/react-test-helpers';
import {
  ProfileApiStub,
  ProfileStubVariables,
} from 'testing/stubs/profile-api-stub';
import { buildWorkspaceResponseStubs } from 'testing/stubs/workspaces';

const { like } = MatchersV3;

// Create a 'pact' between the two applications in the integration we are testing
const provider = new PactV3({
  dir: path.resolve(process.cwd(), 'pacts'),
  consumer: 'AoURWUI',
  provider: 'AoURWAPI',
});

describe('WorkspaceList', () => {
  const profile = ProfileStubVariables.PROFILE_STUB;
  let profileApi: ProfileApiStub;
  const load = jest.fn();
  const reload = jest.fn();
  const updateCache = jest.fn();

  const props = {
    hideSpinner: () => {},
    showSpinner: () => {},
  };

  const component = () => {
    return mountWithRouter(<WorkspaceList {...props} />);
  };

  function getCardNames(wrapper: ReactWrapper) {
    return wrapper
      .find('[data-test-id="workspace-card-name"]')
      .map((c) => c.text());
  }

  beforeEach(() => {
    reload.mockImplementation(async () => {
      const newProfile = await profileApi.getMe();
      profileStore.set({ profile: newProfile, load, reload, updateCache });
    });
    authStore.set({ authLoaded: true, isSignedIn: true });
    profileStore.set({ profile, load, reload, updateCache });
    serverConfigStore.set({
      config: { gsuiteDomain: 'abc', enableResearchReviewPrompt: true },
    });
    exposeAccessTokenSetter();
    window.localStorage.setItem(LOCAL_STORAGE_KEY_TEST_ACCESS_TOKEN, 'oranges');
  });

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
          items: buildWorkspaceResponseStubs(['A', 'B', 'C']),
        },
      });

    return provider.executeTest(async (mockserver) => {
      // Act: test our API client behaves correctly
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
      const wrapper = component();
      await waitForSelectorMissing(Spinner, wrapper);
      expect(getCardNames(wrapper)).toEqual([
        'defaultWorkspaceA',
        'defaultWorkspaceB',
        'defaultWorkspaceC',
      ]);
    });
  });
});
