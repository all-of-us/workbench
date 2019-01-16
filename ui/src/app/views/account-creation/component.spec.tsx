import {shallow} from 'enzyme';
import * as React from 'react';
import * as fp from 'lodash';

import {
  AccountCreationProps,
  AccountCreationReact,
  AccountCreationState
} from './component';

let props: AccountCreationProps;
const component = () => {
  return shallow<AccountCreationReact,
    AccountCreationProps,
    AccountCreationState>(<AccountCreationReact {...props}/>);
};

beforeEach(() => {
  props = {
    invitationKey: '',
    setProfile: () => {},
    onAccountCreation: () => {},
  };
});

it('should handle given name validity', () => {
  const wrapper = component();
  const testInput = fp.repeat('a', 101);
  expect(wrapper.exists('#givenName')).toBeTruthy();
  expect(wrapper.exists('#givenNameError')).toBeFalsy();
  wrapper.find('#givenName')
    .simulate('change', {target: {value: testInput}});
  wrapper.update();
  expect(wrapper.exists('#givenNameError')).toBeTruthy();
});

it ('should handle family name validity', () => {
  const wrapper = component();
  const testInput = fp.repeat('a', 101);
  expect(wrapper.exists('#familyName')).toBeTruthy();
  expect(wrapper.exists('#familyNameError')).toBeFalsy();
  wrapper.find('#familyName').simulate('change', {target: {value: testInput}});
  expect(wrapper.exists('#familyNameError')).toBeTruthy();
});

it ('should handle organization validity', () => {
  const wrapper = component();
  const testInput = fp.repeat('a', 300);
  expect(wrapper.exists('#organization')).toBeTruthy();
  expect(wrapper.exists('#organizationError')).toBeFalsy();
  wrapper.find('#organization').simulate('change', {target: {value: testInput}});
  expect(wrapper.exists('#organizationError')).toBeTruthy();
});

it ('should handle current position validity', () => {
  const wrapper = component();
  const testInput = fp.repeat('a', 300);
  expect(wrapper.exists('#currentPosition')).toBeTruthy();
  expect(wrapper.exists('#currentPositionError')).toBeFalsy();
  wrapper.find('#currentPosition').simulate('change', {target: {value: testInput}});
  expect(wrapper.exists('#currentPositionError')).toBeTruthy();
});

it ('should handle username validity', () => {
  const wrapper = component();
  expect(wrapper.exists('#username')).toBeTruthy();
  expect(wrapper.exists('#usernameError')).toBeFalsy();
  expect(wrapper.exists('#usernameConflictError')).toBeFalsy();

  // if username starts with .
  let testInput = '.startswith';
  wrapper.find('#username').simulate('change', {target: {value: testInput}});
  expect(wrapper.exists('#usernameError')).toBeTruthy();

  // if username ends with .
  testInput = 'endswith.';
  wrapper.find('#username').simulate('change', {target: {value: testInput}});
  expect(wrapper.exists('#usernameError')).toBeTruthy();

  // if username contains special chars
  testInput = 'user@name';
  wrapper.find('#username').simulate('change', {target: {value: testInput}});
  expect(wrapper.exists('#usernameError')).toBeTruthy();

  // if username is long (not too long) but has a mismatch at end
  testInput = fp.repeat('a', 50);
  testInput = testInput + ' a';
  wrapper.find('#username').simulate('change', {target: {value: testInput}});
  expect(wrapper.exists('#usernameError')).toBeTruthy();

  // if username is valid
  testInput = 'username';
  wrapper.find('#username').simulate('change', {target: {value: testInput}});
  expect(wrapper.exists('#usernameError')).toBeFalsy();
});
