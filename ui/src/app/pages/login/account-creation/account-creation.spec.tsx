import * as React from 'react';
import * as fp from 'lodash/fp';
import { mount, ReactWrapper, shallow, ShallowWrapper } from 'enzyme';

import { createEmptyProfile } from 'app/pages/login/sign-in';
import colors from 'app/styles/colors';
import { serverConfigStore } from 'app/utils/stores';

import {
  AccountCreation,
  AccountCreationProps,
  countryDropdownOption,
  stateCodeErrorMessage,
} from './account-creation';

let props: AccountCreationProps;
const component = () => {
  return mount(<AccountCreation {...props} />);
};

const shallowComponent = () => {
  return shallow(<AccountCreation {...props} />);
};

function getCharactersLimitProps(wrapper: ReactWrapper) {
  return wrapper.find('[data-test-id="characterLimit"]').get(0).props;
}

function findStateField(wrapper: ShallowWrapper) {
  return wrapper.find('[dataTestId="state"]');
}

function findCountryDropdownField(wrapper: ShallowWrapper) {
  return wrapper.find('[data-test-id="country-dropdown"]');
}

function findCountryInputField(wrapper: ShallowWrapper) {
  return wrapper.find('[data-test-id="non-usa-country-input"]');
}

const defaultConfig = { gsuiteDomain: 'researchallofus.org' };

beforeEach(() => {
  serverConfigStore.set({ config: defaultConfig });
  props = {
    profile: createEmptyProfile(),
    onComplete: () => {},
    onPreviousClick: () => {},
  };
});

it('should handle given name validity', () => {
  const wrapper = component();
  const testInput = fp.repeat(101, 'a');
  expect(wrapper.exists('#givenName')).toBeTruthy();
  expect(wrapper.exists('#givenNameError')).toBeFalsy();
  wrapper
    .find('input#givenName')
    .simulate('change', { target: { value: testInput } });
  wrapper.update();
  expect(wrapper.exists('#givenNameError')).toBeTruthy();
});

it('should handle family name validity', () => {
  const wrapper = component();
  const testInput = fp.repeat(101, 'a');
  expect(wrapper.exists('#familyName')).toBeTruthy();
  expect(wrapper.exists('#familyNameError')).toBeFalsy();
  wrapper
    .find('input#familyName')
    .simulate('change', { target: { value: testInput } });
  expect(wrapper.exists('#familyNameError')).toBeTruthy();
});

it('should handle username validity starts with .', () => {
  const wrapper = component();
  expect(wrapper.exists('#username')).toBeTruthy();
  expect(wrapper.exists('#usernameError')).toBeFalsy();
  expect(wrapper.exists('#usernameConflictError')).toBeFalsy();
  wrapper
    .find('input#username')
    .simulate('change', { target: { value: '.startswith' } });
  expect(wrapper.exists('#usernameError')).toBeTruthy();
});

it('should handle username validity ends with .', () => {
  const wrapper = component();
  expect(wrapper.exists('#username')).toBeTruthy();
  expect(wrapper.exists('#usernameError')).toBeFalsy();
  wrapper
    .find('input#username')
    .simulate('change', { target: { value: 'endswith.' } });
  expect(wrapper.exists('#usernameError')).toBeTruthy();
});

test.each([
  'user@name',
  '.user',
  'user.',
  'user..',
  "O'Riley",
  '50%',
  'no+plus',
  'ælfred',
  'money$man',
])('should mark username %s as invalid', (username) => {
  const wrapper = component();
  expect(wrapper.exists('#username')).toBeTruthy();
  expect(wrapper.exists('#usernameError')).toBeFalsy();
  wrapper
    .find('input#username')
    .simulate('change', { target: { value: username } });
  expect(wrapper.exists('#usernameError')).toBeTruthy();
});

it('should handle username validity long but has mismatch at end', () => {
  const wrapper = component();
  expect(wrapper.exists('#username')).toBeTruthy();
  expect(wrapper.exists('#usernameError')).toBeFalsy();
  // if username is long (not too long) but has a mismatch at end
  let testInput = fp.repeat(50, 'abc');
  testInput = testInput + ' abc';
  wrapper
    .find('input#username')
    .simulate('change', { target: { value: testInput } });
  expect(wrapper.exists('#usernameError')).toBeTruthy();
});

it('should handle username validity length less than 3 characters', () => {
  const wrapper = component();
  expect(wrapper.exists('#username')).toBeTruthy();
  expect(wrapper.exists('#usernameError')).toBeFalsy();
  wrapper.find('input#username').simulate('change', { target: { value: 'a' } });
  expect(wrapper.exists('#usernameError')).toBeTruthy();
});

it('should handle username validity if name is valid', () => {
  const wrapper = component();
  expect(wrapper.exists('#username')).toBeTruthy();
  expect(wrapper.exists('#usernameError')).toBeFalsy();
  wrapper
    .find('input#username')
    .simulate('change', { target: { value: 'username' } });
  expect(wrapper.exists('#usernameError')).toBeFalsy();
});

it('should display characters over message if research purpose character length is more than 2000', () => {
  const wrapper = component();

  const areaOfResearchTextBox = wrapper.find('[id="areaOfResearch"]');
  expect(getCharactersLimitProps(wrapper).children).toEqual(
    '2000 characters remaining'
  );

  let testInput = fp.repeat(2000, 'a');
  areaOfResearchTextBox
    .find('textarea#areaOfResearch')
    .simulate('change', { target: { value: testInput } });
  expect(getCharactersLimitProps(wrapper).children).toEqual(
    '0 characters remaining'
  );
  expect(getCharactersLimitProps(wrapper).style.color).toBe(colors.danger);

  testInput = fp.repeat(2010, 'a');
  areaOfResearchTextBox
    .find('textarea#areaOfResearch')
    .simulate('change', { target: { value: testInput } });
  expect(getCharactersLimitProps(wrapper).children).toEqual(
    '10 characters over'
  );
  expect(getCharactersLimitProps(wrapper).style.color).toBe(colors.danger);
  // Characters remaining message should not be displayed
  expect(wrapper.find('[data-test-id="charRemaining"]').get(0)).toBeUndefined();
});

it('should display a text input field for non-US countries', () => {
  const wrapper = shallowComponent();
  expect(wrapper.find('[data-test-id="country-input"]').exists()).toBeFalsy();
  findCountryDropdownField(wrapper).simulate(
    'change',
    countryDropdownOption.unitedStates
  );
  expect(wrapper.find('[data-test-id="country-input"]').exists()).toBeFalsy();
  findCountryDropdownField(wrapper).simulate(
    'change',
    countryDropdownOption.other
  );
  expect(findCountryInputField(wrapper).exists()).toBeTruthy();
  findCountryInputField(wrapper).simulate('change', 'Canada');
  expect(findCountryInputField(wrapper).prop('value')).toEqual('Canada');
});

it('should capitalize a state code when selecting USA', function () {
  const wrapper = shallowComponent();
  findStateField(wrapper).simulate('change', 'ny');
  expect(findStateField(wrapper).prop('value')).toEqual('ny');
  findCountryDropdownField(wrapper).simulate(
    'change',
    countryDropdownOption.unitedStates
  );
  expect(findStateField(wrapper).prop('value')).toEqual('NY');
});

it('should change a state name to a state code after selecting USA', function () {
  const wrapper = shallowComponent();
  findStateField(wrapper).simulate('change', 'new york');
  expect(findStateField(wrapper).prop('value')).toEqual('new york');
  findCountryDropdownField(wrapper).simulate(
    'change',
    countryDropdownOption.unitedStates
  );
  expect(findStateField(wrapper).prop('value')).toEqual('NY');
});

it('should mark US states as invalid if not a 2-letter code', function () {
  const wrapper = shallowComponent();
  expect(wrapper.exists('#stateError')).toBeFalsy();
  expect(
    (wrapper.instance() as AccountCreation).validate()['address.state']
  ).not.toContain(stateCodeErrorMessage);
  findCountryDropdownField(wrapper).simulate(
    'change',
    countryDropdownOption.unitedStates
  );
  findStateField(wrapper).simulate('change', 'new york');
  expect(wrapper.exists('#stateError')).toBeTruthy();
  expect(
    (wrapper.instance() as AccountCreation).validate()['address.state']
  ).toContain(stateCodeErrorMessage);
});
