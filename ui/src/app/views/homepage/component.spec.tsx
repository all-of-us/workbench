import {mount} from 'enzyme';
import * as React from 'react';

import {registerApiClient} from 'app/services/swagger-fetch-clients';
import {ConfigApi, ProfileApi} from 'generated/fetch';
import {Profile} from 'generated';
import {ProfileApiStub, ProfileStubVariables} from 'testing/stubs/profile-api-stub';
import {userProfileStore} from 'app/utils/navigation';
import {ConfigApiStub} from 'testing/stubs/config-api-stub';

import {Homepage, WorkbenchAccessTasks, WorkbenchAccessTasksProps} from './component';
import {CohortsApi, ConceptSetsApi, UserMetricsApi, WorkspacesApi} from "generated/fetch/api";
import {CohortsApiStub} from "testing/stubs/cohorts-api-stub";
import {UserMetricsApiStub} from "testing/stubs/user-metrics-api-stub";
import {ConceptSetsApiStub} from "testing/stubs/concept-sets-api-stub";
import {WorkspacesApiStub} from "testing/stubs/workspaces-api-stub";
import {waitOneTickAndUpdate} from "../../../testing/react-test-helpers";

describe('WorkbenchAccessTasks', () => {
  let props: WorkbenchAccessTasksProps;

  const component = () => {
    return mount<WorkbenchAccessTasks, WorkbenchAccessTasksProps, {trainingWarningOpen: boolean}>
    (<WorkbenchAccessTasks {...props}/>);
  };

  beforeEach(() => {
    registerApiClient(ProfileApi, new ProfileApiStub());
    props  = {
      eraCommonsLinked: false,
      eraCommonsError: '',
      trainingCompleted: false,
      firstVisitTraining: true,
    }
  });

  it('should render', () => {
    const wrapper = component();
    expect(wrapper.exists()).toBeTruthy();
  });

  it('should show an error if passed an error message', () => {
    const errorMessage = 'error message!';
    props.eraCommonsError = errorMessage;
    const wrapper = component();
    expect(wrapper.find('[data-test-id="era-commons-error"]').first().text())
        .toContain(errorMessage);
  });

});

describe('HomepageComponent', () => {

  const profile = ProfileStubVariables.PROFILE_STUB as unknown as Profile;
  let profileApi: ProfileApiStub;

  const component = () => {
    return mount(<Homepage/>);
  };

  const reload = jest.fn();

  beforeEach(() => {
    profileApi = new ProfileApiStub();

    registerApiClient(ProfileApi, profileApi);
    registerApiClient(ConfigApi, new ConfigApiStub());
    registerApiClient(CohortsApi, new CohortsApiStub());
    registerApiClient(WorkspacesApi, new WorkspacesApiStub());
    registerApiClient(ConceptSetsApi, new ConceptSetsApiStub());
    registerApiClient(UserMetricsApi, new UserMetricsApiStub());

    // mocking because we don't have access to the angular service
    reload.mockImplementation(async () => {
      const newProfile = await profileApi.getMe();
      userProfileStore.next({profile: newProfile as unknown as Profile, reload});
    });

    userProfileStore.next({profile, reload});
    profileApi.profile.trainingCompletionTime = null;
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
      eraLinkedNihUsername: 'test'
    };
    userProfileStore.next({profile: newProfile as unknown as Profile, reload});
    const wrapper = component();
    expect(wrapper.find('[data-test-id="quick-tour-react"]').exists()).toBeFalsy();
  });

  it('should display quick tour if first visit', async () => {
    profileApi.profile.trainingCompletionTime = 1;
    const newProfile = {
      ...profile,
      eraLinkedNihUsername: 'test',
    };
    userProfileStore.next({profile: newProfile as unknown as Profile, reload});
    const wrapper = component();
    await waitOneTickAndUpdate(wrapper);
    expect(wrapper.find('[data-test-id="quick-tour-react"]').exists()).toBeTruthy();
  });

  it('should show access tasks dashboard if the user has no nih username', async () => {
    profileApi.profile.trainingCompletionTime = 1;

    const wrapper = component();
    await waitOneTickAndUpdate(wrapper);
    expect(wrapper.find('[data-test-id="login"]').first().text()).toEqual('Login');
  });

  it('should show access tasks dashboard if the user has not completed training', async () => {
    const newProfile = {
      ...profile,
      eraLinkedNihUsername: 'test'
    };
    userProfileStore.next({profile: newProfile as unknown as Profile, reload});
    const wrapper = component();
    await waitOneTickAndUpdate(wrapper);
    expect(wrapper.find('[data-test-id="complete-training"]')
        .first().text()).toEqual('Complete Training');
  });

  it('should not display the quick tour if access tasks dashboard is open', async () => {
    const wrapper = component();
    await waitOneTickAndUpdate(wrapper);
    expect(wrapper.find('[data-test-id="quick-tour-react"]').exists()).toBeFalsy();
  });

  it('should not show access tasks dashboard if user has completed all access tasks', async () => {
    profileApi.profile.trainingCompletionTime = 1;
    const newProfile = {
        ...profile,
      eraLinkedNihUsername: 'test',
    };
    userProfileStore.next({profile: newProfile as unknown as Profile, reload});
    const wrapper = component();
    await waitOneTickAndUpdate(wrapper);
    expect(wrapper.find('[data-test-id="login"]').exists()).toBeFalsy();
  });
});
