import {mount} from 'enzyme';
import * as React from 'react';

import {
  AccountCreationSurvey,
  AccountCreationSurveyProps,
} from 'app/pages/login/account-creation/account-creation-survey';
import {serverConfigStore} from 'app/utils/navigation';
import {Ethnicity, GenderIdentity, Race, SexAtBirth} from 'generated/fetch';
import {createEmptyProfile} from "../sign-in";

let props: AccountCreationSurveyProps;
const onCompleteSpy = jest.fn();
const onPreviousSpy = jest.fn();

const defaultConfig = {gsuiteDomain: 'researchallofus.org', enableNewAccountCreation: false};

beforeEach(() => {
  serverConfigStore.next(defaultConfig);

  props = {
    invitationKey: 'asdf',
    profile: createEmptyProfile(),
    onPreviousClick: onPreviousSpy,
    onComplete: onCompleteSpy,
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
