import {mount, ReactWrapper} from 'enzyme';
import * as React from 'react';

import {
  AccountCreationSurvey,
  AccountCreationSurveyProps,
} from 'app/pages/login/account-creation/account-creation-survey';
import {serverConfigStore} from 'app/utils/navigation';
import {Ethnicity, GenderIdentity, Race, SexAtBirth} from 'generated/fetch';
import {createEmptyProfile} from 'app/pages/login/sign-in';
import {profileApi, registerApiClient} from 'app/services/swagger-fetch-clients';
import {ProfileApi} from 'generated/fetch';
import {ProfileApiStub} from 'testing/stubs/profile-api-stub';
import SpyInstance = jest.SpyInstance;
import {waitOneTickAndUpdate} from 'testing/react-test-helpers';

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
    invitationKey: 'asdf',
    profile: createEmptyProfile(),
    onPreviousClick: () => {},
    onComplete: () => {},
  };
});

it('should render', async() => {
  const wrapper = mount(<AccountCreationSurvey {...props} />);
  expect(wrapper.exists()).toBeTruthy();
});

it('should load existing profile data', async() => {
  const {demographicSurvey} = props.profile;
  demographicSurvey.race = [Race.AIAN, Race.AA];
  demographicSurvey.genderIdentityList = [GenderIdentity.MAN];
  demographicSurvey.sexAtBirth = [SexAtBirth.MALE];
  demographicSurvey.identifiesAsLgbtq = true;
  demographicSurvey.ethnicity = Ethnicity.HISPANIC;
  const wrapper = mount(<AccountCreationSurvey {...props} />);

  // Race
  expect(wrapper.find('CheckBox[data-test-id="checkbox-AIAN"]').prop('checked')).toBeTruthy();
  expect(wrapper.find('CheckBox[data-test-id="checkbox-AA"]').prop('checked')).toBeTruthy();
  expect(wrapper.find('CheckBox[data-test-id="checkbox-WHITE"]').prop('checked')).toBeFalsy();

  // Gender identity
  expect(wrapper.find('CheckBox[data-test-id="checkbox-MAN"]').prop('checked')).toBeTruthy();

  // Sex at birth
  expect(wrapper.find('CheckBox[data-test-id="checkbox-MALE"]').prop('checked')).toBeTruthy();

  // LGBTQ
  // We use the .hostNodes() call to filter down to just the React component in the result set.
  expect(wrapper.find('[data-test-id="radio-lgbtq-yes"]').hostNodes().prop('checked')).toBeTruthy();

  // Ethnicity
  expect(wrapper.find('[data-test-id="dropdown-ethnicity"]').prop('value')).toEqual(Ethnicity.HISPANIC);
});

it('should handle error when creating an account', async() => {
  const errorResponseJson = {
    message: 'Could not create account: invalid institutional affiliation',
    statusCode: 412
  };
  mockCreateAccount.mockRejectedValueOnce(
    new Response(JSON.stringify(errorResponseJson), {status: 412}));

  const wrapper = mount(<AccountCreationSurvey {...props} />);
  // Note: mostly unrelated to this test, but here we call a method to 'register' a Captcha response.
  // Without this, the submit button won't work since it's disabled until captcha is complete.
  const surveyComponent: AccountCreationSurvey = wrapper.instance() as AccountCreationSurvey;
  surveyComponent.captureCaptchaResponse('asdf');

  getSubmitButton(wrapper).simulate('click');
  // We need to await one tick to allow async processing of the error response to resolve.
  await waitOneTickAndUpdate(wrapper);

  const errorModal = wrapper.find('Modal[data-test-id="create-account-error"]');
  // Ensure the error modal contains explanatory intro text.
  expect(errorModal.getDOMNode().textContent).toContain('An error occurred while creating your account');
  // Ensure the error modal contains the server-side error message.
  expect(errorModal.getDOMNode().textContent).toContain('Could not create account: invalid institutional affiliation');
});
