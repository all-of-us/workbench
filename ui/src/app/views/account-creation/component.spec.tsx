import {shallow} from 'enzyme';
import * as React from 'react';

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
  let testInput = createInput(101);
  expect(wrapper.exists('#givenName')).toBeTruthy();
  expect(wrapper.exists('#givenNameError')).toBeFalsy();
  wrapper.find('#givenName')
    .simulate('change', {target: {value: testInput}});
  wrapper.update();
  expect(wrapper.exists('#givenNameError')).toBeTruthy();
});

it ('should handle family name validity', () => {
  const wrapper = component();
  let testInput = createInput(101);
  expect(wrapper.exists('#familyName')).toBeTruthy();
  expect(wrapper.exists('#familyNameError')).toBeFalsy();
  wrapper.find('#familyName').simulate('change', {target: {value: testInput}});
  expect(wrapper.exists('#givenNameError')).toBeTruthy();
});

it ('should handle organization validity', () => {
  const wrapper = component();
  let testInput = createInput(300);
  expect(wrapper.exists('#organization')).toBeTruthy();
  expect(wrapper.exists('#organizationError')).toBeFalsy();
  wrapper.find('#organization').simulate('change', {target: {value: testInput}});
  expect(wrapper.exists('#organizationError')).toBeTruthy();
});

function createInput(lengthWanted: number): string {
  let stringToReturn = '';
  while (lengthWanted > 0) {
    stringToReturn = stringToReturn.concat('a');
    lengthWanted--;
  }
  return stringToReturn;
}
