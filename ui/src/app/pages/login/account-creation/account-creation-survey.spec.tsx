import { mount } from 'enzyme';
import * as React from 'react';

import {
  AccountCreationSurvey,
  AccountCreationSurveyProps,
} from 'app/pages/login/account-creation/account-creation-survey';
import { createEmptyProfile } from 'app/pages/login/sign-in';
import { registerApiClient } from 'app/services/swagger-fetch-clients';
import { serverConfigStore } from 'app/utils/stores';
import { ProfileApi } from 'generated/fetch';
import { ProfileApiStub } from 'testing/stubs/profile-api-stub';

let props: AccountCreationSurveyProps;

const defaultConfig = { gsuiteDomain: 'researchallofus.org' };

beforeEach(() => {
  serverConfigStore.set({ config: defaultConfig });

  registerApiClient(ProfileApi, new ProfileApiStub());

  props = {
    termsOfServiceVersion: 0,
    profile: createEmptyProfile(),
    onComplete: () => {},
    onPreviousClick: () => {},
  };
});

it('should render', async () => {
  const wrapper = mount(<AccountCreationSurvey {...props} />);
  expect(wrapper.exists()).toBeTruthy();
});
