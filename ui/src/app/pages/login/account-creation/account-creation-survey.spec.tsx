import {mount, ReactWrapper} from "enzyme";
import * as React from "react";

import {serverConfigStore} from 'app/utils/navigation';
import {ProfileApi} from 'generated/fetch';
import {createEmptyProfile} from 'app/pages/login/sign-in';
import {profileApi, registerApiClient} from 'app/services/swagger-fetch-clients';
import {ProfileApiStub} from 'testing/stubs/profile-api-stub';
import SpyInstance = jest.SpyInstance;
import {AccountCreationSurvey, AccountCreationSurveyProps} from "app/pages/login/account-creation/account-creation-survey";
import {waitOneTickAndUpdate} from "testing/react-test-helpers";

let props: AccountCreationSurveyProps;
let mockCreateAccount: SpyInstance;

const defaultConfig = {gsuiteDomain: 'researchallofus.org', enableNewAccountCreation: true};

function getSubmitButton(wrapper: ReactWrapper): ReactWrapper {
  return wrapper.find('[data-test-id="submit-button"]');
}

beforeEach(() => {
  serverConfigStore.next(defaultConfig);

  registerApiClient(ProfileApi, new ProfileApiStub());
  mockCreateAccount = jest.spyOn(profileApi(), 'createAccount');

  props = {
    invitationKey: '',
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

  getSubmitButton(wrapper).simulate('click');
  // We need to await one tick to allow async processing of the error response to resolve.
  await waitOneTickAndUpdate(wrapper);

  const errorModal = wrapper.find('Modal[data-test-id="create-account-error"]');
  // Ensure the error modal contains explanatory intro text.
  expect(errorModal.getDOMNode().textContent).toContain('An error occurred while creating your account');
  // Ensure the error modal contains the server-side error message.
  expect(errorModal.getDOMNode().textContent).toContain('Could not create account: invalid institutional affiliation');
});