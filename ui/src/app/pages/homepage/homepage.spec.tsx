import {mount} from 'enzyme';
import * as React from 'react';

import {registerApiClient} from 'app/services/swagger-fetch-clients';
import {DataAccessLevel, Profile, ProfileApi} from 'generated/fetch';
import {ProfileApiStub, ProfileStubVariables} from 'testing/stubs/profile-api-stub';
import {serverConfigStore, userProfileStore} from 'app/utils/navigation';

import {Homepage} from './homepage';
import {CohortsApi, ConceptSetsApi, UserMetricsApi, WorkspacesApi} from 'generated/fetch/api';
import {CohortsApiStub} from 'testing/stubs/cohorts-api-stub';
import {UserMetricsApiStub} from 'testing/stubs/user-metrics-api-stub';
import {ConceptSetsApiStub} from 'testing/stubs/concept-sets-api-stub';
import {WorkspacesApiStub} from 'testing/stubs/workspaces-api-stub';
import {waitOneTickAndUpdate} from 'testing/react-test-helpers';


describe('HomepageComponent', () => {

  const profile = ProfileStubVariables.PROFILE_STUB as unknown as Profile;
  let profileApi: ProfileApiStub;

  const component = () => {
    return mount(<Homepage/>);
  };

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
      userProfileStore.next({profile: newProfile, reload, updateCache});
    });

    userProfileStore.next({profile, reload, updateCache: () => {}});
    serverConfigStore.next({
      enableDataUseAgreement: true,
      gsuiteDomain: 'fake-research-aou.org',
      projectId: 'aaa',
      publicApiKeyForErrorReports: 'aaa',
      enableEraCommons: true,
    });
  });

  it('should render the homepage', () => {
    const wrapper = component();
    expect(wrapper.exists()).toBeTruthy();
  });

  it('should display quick tour when clicked', () => {
    const wrapper = component();
    wrapper.find('[data-test-id="quick-tour-resource-0"]').first().simulate('click');
    expect(wrapper.find('[data-test-id="quick-tour-react"]').exists()).toBeTruthy();

  });

  it('should not auto-display quick tour it not first visit', () => {
    const newProfile = {
      ...profile,
      pageVisits: [{page: 'homepage'}],
    };
    userProfileStore.next({profile: newProfile, reload, updateCache});
    const wrapper = component();
    expect(wrapper.find('[data-test-id="quick-tour-react"]').exists()).toBeFalsy();
  });

  it('should display quick tour if first visit', async () => {
    const wrapper = component();
    await waitOneTickAndUpdate(wrapper);
    expect(wrapper.find('[data-test-id="quick-tour-react"]').exists()).toBeTruthy();
  });

  it('should show access tasks dashboard if the user is not registered', async () => {
    const newProfile = {
      ...profile,
      dataAccessLevel: DataAccessLevel.Unregistered
    };
    serverConfigStore.next({...serverConfigStore.getValue()});
    userProfileStore.next({profile: newProfile, reload, updateCache});
    const wrapper = component();
    await waitOneTickAndUpdate(wrapper);
    expect(wrapper.find('[data-test-id="registration-dashboard"]').length).toBeGreaterThanOrEqual(1);
  });

  it('should not display the quick tour if registration dashboard is open', async () => {
    const newProfile = {
      ...profile,
      dataAccessLevel: DataAccessLevel.Unregistered
    };
    serverConfigStore.next({...serverConfigStore.getValue()});
    userProfileStore.next({profile: newProfile, reload, updateCache});
    const wrapper = component();
    await waitOneTickAndUpdate(wrapper);
    expect(wrapper.find('[data-test-id="quick-tour-react"]').exists()).toBeFalsy();
  });

});
