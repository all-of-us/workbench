import {mount} from 'enzyme';
import * as fp from 'lodash/fp';
import * as React from 'react';

import {
  AccountCreation,
  AccountCreationProps,
  AccountCreationState
} from './account-creation';

let props: AccountCreationProps;
const component = () => {
  return mount<AccountCreation,
    AccountCreationProps,
    AccountCreationState>(<AccountCreation {...props}/>);
};

beforeEach(() => {
  props = {
    invitationKey: '',
    setProfile: () => {},
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

it('should handle organization validity', () => {
  const wrapper = component();
  const testInput = fp.repeat(300, 'a');
  expect(wrapper.exists('#organization')).toBeTruthy();
  expect(wrapper.exists('#organizationError')).toBeFalsy();
  wrapper.find('input#organization').simulate('change', {target: {value: testInput}});
  expect(wrapper.exists('#organizationError')).toBeTruthy();
});

it('should handle current position validity', () => {
  const wrapper = component();
  const testInput = fp.repeat(300, 'a');
  expect(wrapper.exists('#currentPosition')).toBeTruthy();
  expect(wrapper.exists('#currentPositionError')).toBeFalsy();
  wrapper.find('input#currentPosition').simulate('change', {target: {value: testInput}});
  expect(wrapper.exists('#currentPositionError')).toBeTruthy();
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

it('should handle invalid Email', () => {
  const wrapper = component();
  expect(wrapper.exists('#contactEmail')).toBeTruthy();
  expect(wrapper.exists('#invalidEmailError')).toBeFalsy();
  wrapper.find('input#contactEmail').simulate('change',
      {target: {value: 'username@'}});
  expect(wrapper.exists('#invalidEmailError')).toBeFalsy();
});
