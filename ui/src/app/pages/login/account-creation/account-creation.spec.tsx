import {mount} from 'enzyme';
import * as fp from 'lodash/fp';
import * as React from 'react';

import {serverConfigStore} from 'app/utils/navigation';
import {Profile} from 'generated/fetch';
import {createEmptyProfile} from 'app/pages/login/sign-in';
import {AccountCreation, AccountCreationProps} from './account-creation';
import {AccountCreationOptions} from './account-creation-options';

let props: AccountCreationProps;
const component = () => {
  return mount(<AccountCreation {...props}/>);
};

const defaultConfig = {
  gsuiteDomain: 'researchallofus.org',
  enableNewAccountCreation: false,
  requireInstitutionalVerification: false
};

beforeEach(() => {
  serverConfigStore.next(defaultConfig);
  props = {
    profile: createEmptyProfile(defaultConfig.requireInstitutionalVerification),
    invitationKey: '',
    onComplete: (profile: Profile) => {},
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

// TODO update these tests like so:
// serverConfigStore.next({...defaultConfig, enableNewAccountCreation: true, requireInstitutionalVerification: true});

it('should display Institution name and role option by default', () => {
  serverConfigStore.next({...defaultConfig, enableNewAccountCreation: true, requireInstitutionalVerification: false});
  const wrapper = component();
  const institutionName = wrapper.find('[data-test-id="institutionname"]');
  expect(institutionName).toBeTruthy();
  const institutionRole = wrapper.find('[data-test-id="institutionRole"]');
  expect(institutionRole).toBeTruthy();
  expect(institutionRole.find('li').length).toBe(AccountCreationOptions.roles.length);
  expect(institutionRole.find('li').get(0).props.children).toBe(AccountCreationOptions.roles[0].label);
});

it('should display Affiliation information if No is selected', () => {
  serverConfigStore.next({...defaultConfig, enableNewAccountCreation: true, requireInstitutionalVerification: false});
  const wrapper = component();
  const institutionAffilationOption = wrapper.find('[data-test-id="show-institution-no"]')
    .find('input');
  expect(institutionAffilationOption).toBeTruthy();
  institutionAffilationOption.simulate('click');
  const affiliationDropDown = wrapper.find('Dropdown');
  const affiliationOptions = affiliationDropDown.find('DropdownItem');
  expect(affiliationOptions.length).toBe(AccountCreationOptions.nonAcademicAffiliations.length);
  expect(affiliationOptions.find('li').get(0).props.children)
    .toBe(AccountCreationOptions.nonAcademicAffiliations[0].label);
});

it('should display Affiliation Roles should change as per affiliation', () => {
  serverConfigStore.next({...defaultConfig, enableNewAccountCreation: true, requireInstitutionalVerification: false});
  const wrapper = component();
  const institutionAffilationOption = wrapper.find('[data-test-id="show-institution-no"]')
    .find('input');
  expect(institutionAffilationOption).toBeTruthy();
  institutionAffilationOption.simulate('click');
  const affiliationDropDown = wrapper.find('Dropdown');
  affiliationDropDown.simulate('click');
  const affiliationOptions = affiliationDropDown.find('DropdownItem');

  // Industry affiliation
  affiliationOptions.find('DropdownItem').find('li').first().simulate('click');
  // affiliationOptions.childAt(0).simulate('click');
  let affilationRoleDropDowns = wrapper.find('[data-test-id="affiliationrole"]');
  expect(affilationRoleDropDowns).toBeTruthy();
  expect(affilationRoleDropDowns.find('li').length).toBe(AccountCreationOptions.industryRole.length);

  expect(affilationRoleDropDowns.find('li').get(0).props.children)
    .toBe(AccountCreationOptions.industryRole[0].label);

  // Education affiliation
  affiliationOptions.find('DropdownItem').find('li').at(1).simulate('click');
  // affiliationOptions.childAt(0).simulate('click');
  affilationRoleDropDowns = wrapper.find('[data-test-id="affiliationrole"]');
  expect(affilationRoleDropDowns).toBeTruthy();
  expect(affilationRoleDropDowns.find('li').length).toBe(AccountCreationOptions.educationRole.length);

  expect(affilationRoleDropDowns.find('li').get(0).props.children)
    .toBe(AccountCreationOptions.educationRole[0].label);

  // Community Scientist
  affiliationOptions.find('DropdownItem').find('li').at(2).simulate('click');
  // affiliationOptions.childAt(0).simulate('click');
  affilationRoleDropDowns = wrapper.find('[data-test-id="affiliationrole"]');
  expect(affilationRoleDropDowns.length).toBe(0);
});

// TODO end
