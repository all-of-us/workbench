import {mount, ReactWrapper} from 'enzyme';
import * as React from "react";

import {serverConfigStore} from 'app/utils/navigation';
import {ProfileApi} from 'generated/fetch';
import {createEmptyProfile} from 'app/pages/login/sign-in';
import {profileApi, registerApiClient} from 'app/services/swagger-fetch-clients';
import {ProfileApiStub} from 'testing/stubs/profile-api-stub';
import SpyInstance = jest.SpyInstance;
import {AccountCreationSurvey, AccountCreationSurveyProps} from "app/pages/login/account-creation/account-creation-survey";
import {waitOneTickAndUpdate} from "testing/react-test-helpers";
import {AccountCreationOptions} from 'app/pages/login/account-creation/account-creation-options';
import * as fp from 'lodash/fp';

let props: AccountCreationSurveyProps;
let mockCreateAccount: SpyInstance;

const defaultConfig = {gsuiteDomain: 'researchallofus.org'};

function getSubmitButton(wrapper: ReactWrapper): ReactWrapper {
  return wrapper.find('[data-test-id="submit-button"]');
}

beforeEach(() => {
  serverConfigStore.next(defaultConfig);

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

it('should handle error when creating an account', async() => {
  const errorResponseJson = {
    message: 'Could not create account: invalid institutional affiliation',
    statusCode: 412
  };
  mockCreateAccount.mockRejectedValueOnce(
      new Response(JSON.stringify(errorResponseJson), {status: 412}));

  const wrapper = mount(<AccountCreationSurvey {...props} />);

  wrapper.find('[data-test-id="checkbox-race-PREFER_NO_ANSWER"]').at(1).simulate('change', {target: {checked: true}})
  wrapper.find('[data-test-id="checkbox-genderIdentityList-PREFER_NO_ANSWER"]').at(1).simulate('change', {target: {checked: true}})
  wrapper.find('[data-test-id="checkbox-sexAtBirth-PREFER_NO_ANSWER"]').at(1).simulate('change', {target: {checked: true}})

  wrapper.find('[data-test-id="radio-lgbtq-pnta"]').first().simulate('click');
  wrapper.find('[id="radio-disability-pnta"]').first().simulate('click');

  wrapper.find('[data-test-id="dropdown-ethnicity"]').first().simulate('click');
  wrapper.find('[data-test-id="dropdown-ethnicity"] [aria-label=" Prefer not to answer"]').first().simulate('click')

  wrapper.find('[data-test-id="year-of-birth"]').first().simulate('click');
  wrapper.find('[aria-label="Prefer not to answer"]').first().simulate('click')

  wrapper.find('[data-test-id="highest-education-level"]').first().simulate('click');
  wrapper.find('[data-test-id="highest-education-level"] [aria-label="Prefer not to answer"]').first().simulate('click');

  getSubmitButton(wrapper).simulate('click');
  // We need to await one tick to allow async processing of the error response to resolve.
  await waitOneTickAndUpdate(wrapper);

  expect(wrapper.find('Modal[role="alertdialog"]').length).toEqual(1);
});
