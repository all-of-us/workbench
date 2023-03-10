/**
 * @jest-environment jsdom
 */

import 'setimmediate';
import '@testing-library/jest-dom';

import * as React from 'react';
import { ReactWrapper } from 'enzyme';

import {
  Configuration as FetchConfiguration,
  WorkspacesApi,
} from 'generated/fetch';

import { PactV3 } from '@pact-foundation/pact';
import { WorkspaceList } from 'app/pages/workspace/workspace-list';
import {
  clearApiClients,
  registerApiClient,
} from 'app/services/swagger-fetch-clients';
import { profileStore, serverConfigStore } from 'app/utils/stores';
import path from 'path';
import portableFetch from 'portable-fetch';

import {
  mountWithRouter,
  waitForFakeTimersAndUpdate,
} from 'testing/react-test-helpers';
import {
  ProfileApiStub,
  ProfileStubVariables,
} from 'testing/stubs/profile-api-stub';
import { buildWorkspaceResponseStubs } from 'testing/stubs/workspaces';
import { WorkspacesApiStub } from 'testing/stubs/workspaces-api-stub';

// Create a 'pact' between the two applications in the integration we are testing
const provider = new PactV3({
  dir: path.resolve(process.cwd(), 'pacts'),
  consumer: 'AoURWUI',
  provider: 'AoURWAPI',
});

describe('WorkspaceList', () => {
  const now = new Date('2000-01-01 03:00:00');
  const profile = ProfileStubVariables.PROFILE_STUB;
  let profileApi: ProfileApiStub;
  const load = jest.fn();
  const reload = jest.fn();
  const updateCache = jest.fn();

  let workspacesApiStub: WorkspacesApiStub;

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
    profileStore.set({ profile, load, reload, updateCache });
    serverConfigStore.set({
      config: { gsuiteDomain: 'abc', enableResearchReviewPrompt: true },
    });
  });

  afterEach(() => {
    jest.useRealTimers();
    clearApiClients();
  });
  it('displays the correct number of workspaces', async () => {
    provider
      .given('I have a list of dogs')
      .uponReceiving('a request for all dogs with the builder pattern')
      .withRequest({
        method: 'GET',
        path: '/v1/workspaces',
      })
      .willRespondWith({
        status: 200,
        headers: { 'Content-Type': 'application/json' },
        body: {
          items: buildWorkspaceResponseStubs(['Gunicorn']),
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
            });
            this.basePath = mockserver.url;
            this.fetch = portableFetch;
          }
        })()
      );
      const wrapper = component();
      await new Promise((r) => setTimeout(r, 25));
      // await waitOneTickAndUpdate(wrapper);
      // await waitOnTimersAndUpdate(wrapper);
      await waitForFakeTimersAndUpdate(wrapper);
      expect(getCardNames(wrapper)).toEqual(['defaultWorkspaceGunicorn']);
      expect(true).toBeTruthy();
    });
  });
});
