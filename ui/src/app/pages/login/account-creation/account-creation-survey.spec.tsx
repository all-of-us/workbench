import {AccountCreationSurvey, AccountCreationSurveyProps} from 'app/pages/login/account-creation/account-creation-survey';
import {createEmptyProfile} from 'app/pages/login/sign-in';
import {profileApi, registerApiClient} from 'app/services/swagger-fetch-clients';
import {serverConfigStore} from 'app/utils/stores';
import {mount} from 'enzyme';
import {ProfileApi} from 'generated/fetch';
import * as React from 'react';
import {ProfileApiStub} from 'testing/stubs/profile-api-stub';

let props: AccountCreationSurveyProps;

const defaultConfig = {gsuiteDomain: 'researchallofus.org'};

beforeEach(() => {
  serverConfigStore.set({config: defaultConfig});

  registerApiClient(ProfileApi, new ProfileApiStub());
  jest.spyOn(profileApi(), 'createAccount');

  props = {
    termsOfServiceVersion: 0,
    profile: createEmptyProfile(),
    onComplete: () => {},
    onPreviousClick: () => {}
  };
});

it('should render', async () => {
  const wrapper = mount(<AccountCreationSurvey {...props} />);
  expect(wrapper.exists()).toBeTruthy();
});
