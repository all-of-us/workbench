import {mount} from 'enzyme';
import * as React from 'react';

import {SettingsReact, SettingsState} from './component';

import {clusterApi, registerApiClient} from 'app/services/swagger-fetch-clients';
import {userProfileStore} from 'app/utils/navigation';
import {waitOneTickAndUpdate} from 'testing/react-test-helpers';
import {ClusterApiStub} from 'testing/stubs/cluster-api-stub';
import {ProfileStubVariables} from 'testing/stubs/profile-service-stub';

import {ClusterApi} from 'generated/fetch/api';


describe('SettingsComponent', () => {
  const component = () => {
    return mount<SettingsReact, {}, SettingsState>(<SettingsReact/>);
  };

  beforeEach(() => {
    registerApiClient(ClusterApi, new ClusterApiStub());

    // the user profile is required for retrieving cluster information because we need
    // the user's free tier billing project

    userProfileStore.next({
      profile: ProfileStubVariables.PROFILE_STUB,
      reload: () => {},
      updateCache: (profile) => {}
    });
  });

  it('should not open the cluster reset modal when no cluster', () => {
    const wrapper = component();
    expect(wrapper.find('Modal[data-test-id="reset-notebook-modal"]').length).toBe(0);
    wrapper.find('[data-test-id="reset-notebook-button"]').at(0).simulate('click');
    expect(wrapper.find('Modal[data-test-id="reset-notebook-modal"]').length).toBe(0);
  });

  it('should allow deleting the cluster when there is one', async() => {
    const spy = jest.spyOn(clusterApi(), 'deleteCluster');
    const wrapper = component();
    await waitOneTickAndUpdate(wrapper);
    expect(wrapper.find('Modal[data-test-id="reset-notebook-modal"]').length).toBe(0);
    wrapper.find('[data-test-id="reset-notebook-button"]').at(0).simulate('click');
    expect(wrapper.find('Modal[data-test-id="reset-notebook-modal"]').length).toBe(1);
    wrapper.find('[data-test-id="reset-cluster-send"]').at(0).simulate('click');
    await waitOneTickAndUpdate(wrapper);
    expect(spy).toHaveBeenCalled();
    expect(wrapper.find('Modal[data-test-id="reset-notebook-modal"]').length).toBe(0);
  });
});
