import {mount} from 'enzyme';
import * as React from 'react';

import {serverConfigStore} from 'app/utils/navigation';
import {DemographicSurvey, Props} from 'app/pages/profile/demographic-survey';
import {Ethnicity, GenderIdentity, ProfileApi, Race, SexAtBirth} from 'generated/fetch';
import {createEmptyProfile} from 'app/pages/login/sign-in';
import {registerApiClient} from 'app/services/swagger-fetch-clients';
import {ProfileApiStub} from 'testing/stubs/profile-api-stub';
import {Profile} from "generated/fetch";

let props: Props;
const defaultConfig = {gsuiteDomain: 'researchallofus.org', requireInstitutionalVerification: false};


beforeEach(() => {
  serverConfigStore.next(defaultConfig);

  registerApiClient(ProfileApi, new ProfileApiStub());

  props = {
    profile: createEmptyProfile(),
    onPreviousClick: () => {},
    onCancelClick: () => {},
    onSubmit: () => Promise.resolve(createEmptyProfile()),
    enableCaptcha: false,
    enablePrevious: false,
    showStepCount: false
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
  expect(wrapper.find('[data-test-id="dropdown-ethnicity"]').prop('value')).toEqual(Ethnicity.HISPANIC);
});
