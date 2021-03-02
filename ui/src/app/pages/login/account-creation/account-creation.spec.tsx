import {mount, ReactWrapper} from 'enzyme';
import * as fp from 'lodash/fp';
import * as React from 'react';

import {serverConfigStore} from 'app/utils/navigation';
import {Profile} from 'generated/fetch';
import {createEmptyProfile} from 'app/pages/login/sign-in';
import {AccountCreation, AccountCreationProps} from './account-creation';
import colors from 'app/styles/colors';

let props: AccountCreationProps;
const component = () => {
  return mount(<AccountCreation {...props}/>);
};

function getCharactersLimitProps(wrapper: ReactWrapper) {
  return wrapper.find('[data-test-id="characterLimit"]').get(0).props;
}

const defaultConfig = {gsuiteDomain: 'researchallofus.org'};

beforeEach(() => {
  serverConfigStore.next(defaultConfig);
  props = {
    profile: createEmptyProfile(),
    onComplete: (profile: Profile) => {},
    onPreviousClick: (profile: Profile) => {}
  };
});

it('should handle given name validity', () => {
  const wrapper = component();
  const testInput = fp.repeat(101, 'a');
  expect(wrapper.exists('#givenName')).toBeTruthy();
  expect(wrapper.exists('#givenNameError')).toBeFalsy();
  wrapper.find('input#givenName')
    .simulate('change', {target: {value: testInput}});
  wrapper.update();
  expect(wrapper.exists('#givenNameError')).toBeTruthy();
});

it('should handle family name validity', () => {
  const wrapper = component();
  const testInput = fp.repeat(101, 'a');
  expect(wrapper.exists('#familyName')).toBeTruthy();
  expect(wrapper.exists('#familyNameError')).toBeFalsy();
  wrapper.find('input#familyName').simulate('change', {target: {value: testInput}});
  expect(wrapper.exists('#familyNameError')).toBeTruthy();
});

it('should handle username validity starts with .', () => {
  const wrapper = component();
  expect(wrapper.exists('#username')).toBeTruthy();
  expect(wrapper.exists('#usernameError')).toBeFalsy();
  expect(wrapper.exists('#usernameConflictError')).toBeFalsy();
  wrapper.find('input#username').simulate('change', {target: {value: '.startswith'}});
  expect(wrapper.exists('#usernameError')).toBeTruthy();
});

it('should handle username validity ends with .', () => {
  const wrapper = component();
  expect(wrapper.exists('#username')).toBeTruthy();
  expect(wrapper.exists('#usernameError')).toBeFalsy();
  wrapper.find('input#username').simulate('change', {target: {value: 'endswith.'}});
  expect(wrapper.exists('#usernameError')).toBeTruthy();
});

it('should handle username validity contains special chars', () => {
  const wrapper = component();
  expect(wrapper.exists('#username')).toBeTruthy();
  expect(wrapper.exists('#usernameError')).toBeFalsy();
  wrapper.find('input#username').simulate('change', {target: {value: 'user@name'}});
  expect(wrapper.exists('#usernameError')).toBeTruthy();
});

it('should handle username validity long but has mismatch at end', () => {
  const wrapper = component();
  expect(wrapper.exists('#username')).toBeTruthy();
  expect(wrapper.exists('#usernameError')).toBeFalsy();
  // if username is long (not too long) but has a mismatch at end
  let testInput = fp.repeat(50, 'abc');
  testInput = testInput + ' abc';
  wrapper.find('input#username').simulate('change', {target: {value: testInput}});
  expect(wrapper.exists('#usernameError')).toBeTruthy();
});

it('should handle username validity length less than 3 characters', () => {
  const wrapper = component();
  expect(wrapper.exists('#username')).toBeTruthy();
  expect(wrapper.exists('#usernameError')).toBeFalsy();
  wrapper.find('input#username').simulate('change', {target: {value: 'a'}});
  expect(wrapper.exists('#usernameError')).toBeTruthy();
});

it('should handle username validity if name is valid', () => {
  const wrapper = component();
  expect(wrapper.exists('#username')).toBeTruthy();
  expect(wrapper.exists('#usernameError')).toBeFalsy();
  wrapper.find('input#username').simulate('change', {target: {value: 'username'}});
  expect(wrapper.exists('#usernameError')).toBeFalsy();
});

it('should display characters over message if research purpose character length is more than 2000', () => {
  const wrapper = component();

  const areaOfResearchTextBox = wrapper.find('[id="areaOfResearch"]');
  expect(getCharactersLimitProps(wrapper).children).toEqual('2000 characters remaining');

  let testInput = fp.repeat(2000, 'a');
  areaOfResearchTextBox.find('textarea#areaOfResearch').simulate('change', {target: {value: testInput}});
  expect(getCharactersLimitProps(wrapper).children)
    .toEqual('0 characters remaining');
  expect(getCharactersLimitProps(wrapper).style.color).toBe(colors.danger);

  testInput = fp.repeat(2010, 'a');
  areaOfResearchTextBox.find('textarea#areaOfResearch').simulate('change', {target: {value: testInput}});
  expect(getCharactersLimitProps(wrapper).children).toEqual('10 characters over');
  expect(getCharactersLimitProps(wrapper).style.color).toBe(colors.danger);
  // Characters remaining message should not be displayed
  expect(wrapper.find('[data-test-id="charRemaining"]').get(0)).toBeUndefined();
});

