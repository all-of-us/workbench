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
        enableVWBHomepageBanner: true,
        restrictLegacyAccess: true,
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
    component();
    expect(
      screen.getByText('Welcome to your Researcher Workbench')
    ).toBeInTheDocument();
  });

  it('shows migration ended banner for non-migration-testing users', () => {
    profileStore.set({
      profile: { ...profile, migrationTestingGroup: false },
      load,
      reload,
      updateCache,
    });

    component();

    expect(screen.getByText('Migration has ended.')).toBeInTheDocument();
    expect(
      screen.queryByText('Migrate your Workspaces to Researcher Workbench 2.0')
    ).not.toBeInTheDocument();
  });

  it('shows migration banner for migration testing users', () => {
    profileStore.set({
      profile: { ...profile, migrationTestingGroup: true },
      load,
      reload,
      updateCache,
    });

    component();

    expect(
      screen.getByText('Migrate your Workspaces to Researcher Workbench 2.0')
    ).toBeInTheDocument();
    expect(screen.queryByText('Migration has ended.')).not.toBeInTheDocument();
  });

  it('hides homepage banners when the feature flag is disabled', () => {
    serverConfigStore.set({
      config: {
        ...serverConfigStore.get().config,
        enableVWBHomepageBanner: false,
      },
    });

    component();

    expect(
      screen.queryByText('Migrate your Workspaces to Researcher Workbench 2.0')
    ).not.toBeInTheDocument();
    expect(screen.queryByText('Migration has ended.')).not.toBeInTheDocument();
  });

  it('should not display the zero workspace UI while workspaces are being fetched', () => {
    component();

    expect(
      screen.queryByText('Create your first workspace')
    ).not.toBeInTheDocument();
  });
});
