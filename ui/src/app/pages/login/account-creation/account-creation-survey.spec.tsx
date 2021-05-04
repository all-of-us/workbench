import {mount, ReactWrapper} from 'enzyme';
import * as React from 'react';

import SpyInstance = jest.SpyInstance;
import {AccountCreationSurvey, AccountCreationSurveyProps} from 'app/pages/login/account-creation/account-creation-survey';
import {createEmptyProfile} from 'app/pages/login/sign-in';
import {profileApi, registerApiClient} from 'app/services/swagger-fetch-clients';
import {serverConfigStore} from 'app/utils/stores';
import {ProfileApi} from 'generated/fetch';
import {ProfileApiStub} from 'testing/stubs/profile-api-stub';

let props: AccountCreationSurveyProps;
let mockCreateAccount: SpyInstance;

const defaultConfig = {gsuiteDomain: 'researchallofus.org'};

function getSubmitButton(wrapper: ReactWrapper): ReactWrapper {
  return wrapper.find('[data-test-id="submit-button"]');
}

beforeEach(() => {
  serverConfigStore.set({config: defaultConfig});

  registerApiClient(ProfileApi, new ProfileApiStub());
  mockCreateAccount = jest.spyOn(profileApi(), 'createAccount');

  props = {
    termsOfServiceVersion: 0,
    profile: createEmptyProfile(),
    onComplete: () => {},
    onPreviousClick: () => {}
  };
});

it('should render', async() => {
  const wrapper = mount(<AccountCreationSurvey {...props} />);
  expect(wrapper.exists()).toBeTruthy();
});
