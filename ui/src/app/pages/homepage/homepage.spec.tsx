import '@testing-library/jest-dom';

import { ProfileApi } from 'generated/fetch';
import {
  CohortsApi,
  ConceptSetsApi,
  UserMetricsApi,
  WorkspacesApi,
} from 'generated/fetch';

import { screen } from '@testing-library/react';
import { registerApiClient } from 'app/services/swagger-fetch-clients';
import {
  cdrVersionStore,
  profileStore,
  serverConfigStore,
} from 'app/utils/stores';

import { renderWithRouter } from 'testing/react-test-helpers';
import { cdrVersionTiersResponse } from 'testing/stubs/cdr-versions-api-stub';
import { CohortsApiStub } from 'testing/stubs/cohorts-api-stub';
import { ConceptSetsApiStub } from 'testing/stubs/concept-sets-api-stub';
import {
  ProfileApiStub,
  ProfileStubVariables,
} from 'testing/stubs/profile-api-stub';
import { stubResource } from 'testing/stubs/resources-stub';
import { UserMetricsApiStub } from 'testing/stubs/user-metrics-api-stub';
import { WorkspacesApiStub } from 'testing/stubs/workspaces-api-stub';

import { Homepage } from './homepage';

describe('HomepageComponent', () => {
  const profile = ProfileStubVariables.PROFILE_STUB;
  let profileApi: ProfileApiStub;

  const component = () => {
    return renderWithRouter(<Homepage hideSpinner={() => {}} />);
  };

  const load = jest.fn();
  const reload = jest.fn();
  const updateCache = jest.fn();

  beforeEach(() => {
    profileApi = new ProfileApiStub();

    registerApiClient(ProfileApi, profileApi);
    registerApiClient(CohortsApi, new CohortsApiStub());
    registerApiClient(WorkspacesApi, new WorkspacesApiStub());
    registerApiClient(ConceptSetsApi, new ConceptSetsApiStub());
    registerApiClient(UserMetricsApi, new UserMetricsApiStub());

    // mocking because we don't have access to the angular service
    reload.mockImplementation(async () => {
      const newProfile = await profileApi.getMe();
      profileStore.set({ profile: newProfile, load, reload, updateCache });
    });

    profileStore.set({ profile, load, reload, updateCache: () => {} });
    serverConfigStore.set({
      config: {
        gsuiteDomain: 'fake-research-aou.org',
        projectId: 'aaa',
        publicApiKeyForErrorReports: 'aaa',
        enableEraCommons: true,
      },
    });
    cdrVersionStore.set(cdrVersionTiersResponse);

    stubResource.notebook = {
      name: '',
      path: '',
      lastModifiedTime: 0,
    };
  });

  it('should render the homepage', () => {
    const { container } = component();
    expect(container).toBeInTheDocument();
  });

  it('should display quick tour when clicked', () => {
    // Mock offsetWidth needed for horizontal scroll for quick tour/video list
    const originalOffsetWidth = Object.getOwnPropertyDescriptor(
      HTMLElement.prototype,
      'offsetWidth'
    );
    Object.defineProperty(HTMLElement.prototype, 'offsetWidth', {
      configurable: true,
      value: 1000,
    });
    component();

    screen.getByAltText('show quick tour').click();
    expect(screen.getByTestId('quick-tour-react')).toBeInTheDocument();
    // set offsetWidth back to original
    Object.defineProperty(
      HTMLElement.prototype,
      'offsetWidth',
      originalOffsetWidth
    );
  });

  it('should not auto-display quick tour it not first visit', () => {
    const newProfile = {
      ...profile,
      pageVisits: [{ page: 'homepage' }],
    };
    profileStore.set({ profile: newProfile, load, reload, updateCache });
    component();
    screen.logTestingPlaygroundURL();
    expect(screen.queryByTestId('quick-tour-react')).not.toBeInTheDocument();
  });

  it('should display quick tour if first visit', async () => {
    component();
    expect(screen.getByTestId('quick-tour-react')).toBeInTheDocument();
  });

  it('should not display the zero workspace UI while workspaces are being fetched', async () => {
    component();
    screen.logTestingPlaygroundURL();
    expect(
      screen.queryByText('Here are some tips to get you started')
    ).not.toBeInTheDocument();
  });
});
