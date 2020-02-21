import {mount, ReactWrapper, ShallowWrapper} from 'enzyme';
import * as React from 'react';

import {serverConfigStore} from 'app/utils/navigation';
import {AccountCreationSurvey, AccountCreationSurveyProps} from 'app/pages/login/account-creation/account-creation-survey';
import {getEmptyProfile} from 'app/pages/login/test-utils';
import {Ethnicity, Race} from 'generated/fetch';

type AnyWrapper = (ShallowWrapper|ReactWrapper);
const getPrivacyCheckbox = (wrapper: AnyWrapper): AnyWrapper => {
  return wrapper.find('CheckBox[data-test-id="privacy-statement-check"]');
};
const getTosCheckbox = (wrapper: AnyWrapper): AnyWrapper => {
  return wrapper.find('CheckBox[data-test-id="terms-of-service-check"]');
};
const getNextButton = (wrapper: AnyWrapper ): AnyWrapper => {
  return wrapper.find('[data-test-id="next-button"]');
};

let props: AccountCreationSurveyProps;
const onCompleteSpy = jest.fn();
const onPreviousSpy = jest.fn();

const defaultConfig = {gsuiteDomain: 'researchallofus.org', enableNewAccountCreation: false};

beforeEach(() => {
  serverConfigStore.next(defaultConfig);

  props = {
    invitationKey: 'asdf',
    profile: getEmptyProfile(),
    onPreviousClick: onPreviousSpy,
    onComplete: onCompleteSpy,
  };
});

it('should render', async() => {
  const wrapper = mount(<AccountCreationSurvey {...props} />);
  expect(wrapper.exists()).toBeTruthy();
});

it('should load existing profile data', async() => {
  props.profile.demographicSurvey.race = [Race.AIAN, Race.AA];
  props.profile.demographicSurvey.identifiesAsLgbtq = true;
  props.profile.demographicSurvey.ethnicity = Ethnicity.HISPANIC
  const wrapper = mount(<AccountCreationSurvey {...props} />);

  expect(wrapper.find('CheckBox[data-test-id="checkbox-AIAN"]').prop('checked')).toBeTruthy();
  expect(wrapper.find('CheckBox[data-test-id="checkbox-AA"]').prop('checked')).toBeTruthy();
  expect(wrapper.find('CheckBox[data-test-id="checkbox-WHITE"]').prop('checked')).toBeFalsy();

  // We use the .hostNodes() call to filter down to just the React component in the result set.
  expect(wrapper.find('[data-test-id="radio-lgbtq-yes"]').hostNodes().prop('checked')).toBeTruthy();
  expect(wrapper.find('[data-test-id="dropdown-ethnicity"]').prop('value')).toEqual(Ethnicity.HISPANIC);
});
