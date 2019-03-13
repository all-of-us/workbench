import {mount} from 'enzyme';
import * as React from 'react';

import {BugReportModal} from './component';
import {BugReportApiStub, EMPTY_BUG_REPORT} from 'testing/stubs/bug-report-api-stub';
import {bugReportApi, profileApi, registerApiClient} from 'app/services/swagger-fetch-clients';
import {BugReportApi, ProfileApi} from "generated/fetch/api";
import {Profile} from 'generated';
import {ProfileApiStub} from 'testing/stubs/profile-api-stub';
import {ProfileStubVariables} from 'testing/stubs/profile-service-stub';
import {userProfileStore} from 'app/utils/navigation';


describe('BugReport', () => {
  const description = 'test';
  const profile = ProfileStubVariables.PROFILE_STUB as unknown as Profile;
  const bugReport = {
    ...EMPTY_BUG_REPORT,
    shortDescription: description,
    contactEmail: profile.username
  };
  let profileApi: ProfileApiStub;
  const reload = jest.fn();

  const component = () => {
    return mount(<BugReportModal
        bugReportDescription={description}
        onClose={() => {}}
    />);
  };

  beforeEach(() => {
    registerApiClient(ProfileApi, new ProfileApiStub());
    registerApiClient(BugReportApi, new BugReportApiStub());

    // mocking because we don't have access to the angular service
    reload.mockImplementation(async () => {
      const newProfile = await profileApi.getMe();
      userProfileStore.next({profile: newProfile as unknown as Profile, reload});
    });

    userProfileStore.next({profile, reload});
  });

  it('submits a bug report', () => {
    const spy = jest.spyOn(bugReportApi(), 'sendBugReport');
    const wrapper = component();
    expect(wrapper.exists()).toBeTruthy();
    wrapper.find('[data-test-id="submit-bug-report"]').first().simulate('click');
    expect(spy).toHaveBeenCalledWith(bugReport);

  });

});
