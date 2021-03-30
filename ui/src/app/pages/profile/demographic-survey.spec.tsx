import {mount} from 'enzyme';
import * as React from 'react';

import {AccountCreationOptions} from 'app/pages/login/account-creation/account-creation-options';
import {createEmptyProfile} from 'app/pages/login/sign-in';
import {DemographicSurvey, Props} from 'app/pages/profile/demographic-survey';
import {registerApiClient} from 'app/services/swagger-fetch-clients';
import {serverConfigStore} from 'app/utils/navigation';
import {Disability, Ethnicity, GenderIdentity, ProfileApi, Race, SexAtBirth} from 'generated/fetch';
import {waitOneTickAndUpdate} from 'testing/react-test-helpers';
import {ProfileApiStub} from 'testing/stubs/profile-api-stub';


let props: Props;
const defaultConfig = {gsuiteDomain: 'researchallofus.org'};


beforeEach(() => {
  serverConfigStore.next(defaultConfig);

  registerApiClient(ProfileApi, new ProfileApiStub());

  props = {
    profile: createEmptyProfile(),
    onPreviousClick: () => {},
    onCancelClick: () => {},
    saveProfile: () => Promise.resolve(createEmptyProfile()),
    enableCaptcha: false,
    enablePrevious: false,
    showStepCount: false,
    stackdriverErrorReporterContext: {
      reportError: (e: Error|string) => {}
    }
  };
});

it('should render', async() => {
  const wrapper = mount(<DemographicSurvey {...props} />);
  expect(wrapper.exists()).toBeTruthy();
});

it('should load existing profile data', async() => {
  const {demographicSurvey} = props.profile;
  demographicSurvey.race = [Race.AIAN, Race.AA];
  demographicSurvey.genderIdentityList = [GenderIdentity.MAN];
  demographicSurvey.sexAtBirth = [SexAtBirth.MALE];
  demographicSurvey.identifiesAsLgbtq = true;
  demographicSurvey.ethnicity = Ethnicity.HISPANIC;
  const wrapper = mount(<DemographicSurvey {...props} />);

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
  expect(wrapper.find('[data-test-id="dropdown-ethnicity"]').first().prop('value')).toEqual(Ethnicity.HISPANIC);
});

it('should handle error when submitting the survey', async() => {
  const {demographicSurvey} = props.profile;
  demographicSurvey.race = [Race.AIAN, Race.AA];
  demographicSurvey.genderIdentityList = [GenderIdentity.MAN];
  demographicSurvey.sexAtBirth = [SexAtBirth.MALE];
  demographicSurvey.identifiesAsLgbtq = true;
  demographicSurvey.ethnicity = Ethnicity.HISPANIC;
  demographicSurvey.disability = Disability.True;
  demographicSurvey.yearOfBirth = 2000;
  demographicSurvey.education = AccountCreationOptions.levelOfEducation[0].value;

  const errorResponseJson = {
    message: 'Could not create account: invalid institutional affiliation',
    statusCode: 412
  };

  const wrapper = mount(<DemographicSurvey
    {...props}
    saveProfile={() => { throw new Response(JSON.stringify(errorResponseJson), {status: 412}); }}
  />);

  wrapper.find('[data-test-id="submit-button"]').simulate('click');
  // We need to await one tick to allow async processing of the error response to resolve.
  await waitOneTickAndUpdate(wrapper);

  expect(wrapper.find('Modal[role="alertdialog"]').length).toEqual(1);
});
