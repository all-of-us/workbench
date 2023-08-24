import * as React from 'react';
import * as fp from 'lodash/fp';

import { ProfileApi } from 'generated/fetch';

import { screen } from '@testing-library/dom';
import { render } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { createEmptyProfile } from 'app/pages/login/sign-in';
import { registerApiClient } from 'app/services/swagger-fetch-clients';
import { serverConfigStore } from 'app/utils/stores';

import { ProfileApiStub } from 'testing/stubs/profile-api-stub';

import {
  AccountCreation,
  AccountCreationProps,
  countryDropdownOption,
  stateCodeErrorMessage,
} from './account-creation';

let props: AccountCreationProps;
const component = () => {
  return render(<AccountCreation {...props} />);
};

function findStateField() {
  return screen.getByLabelText('State') as HTMLInputElement;
}

function findCountryDropdownField() {
  return screen.getByLabelText('Country dropdown') as HTMLSelectElement;
}

function findCountryInputField() {
  return screen.getByLabelText('Country input') as HTMLInputElement;
}

const defaultConfig = { gsuiteDomain: 'researchallofus.org' };

beforeEach(() => {
  serverConfigStore.set({ config: defaultConfig });
  registerApiClient(ProfileApi, new ProfileApiStub());
  props = {
    profile: createEmptyProfile(),
    onComplete: () => {},
    onPreviousClick: () => {},
  };
});

it('should handle given name validity', async () => {
  const user = userEvent.setup();
  const { container } = component();
  const testInput = fp.repeat(101, 'a');
  expect(container.querySelector('#givenName')).not.toBeNull();
  expect(container.querySelector('#givenNameError')).toBeNull();
  await user.type(container.querySelector('input#givenName'), testInput);
  expect(container.querySelector('#givenNameError')).not.toBeNull();
});

it('should handle family name validity', async () => {
  const user = userEvent.setup();
  const { container } = component();
  const testInput = fp.repeat(101, 'a');
  expect(container.querySelector('#familyName')).not.toBeNull();
  expect(container.querySelector('#familyNameError')).toBeNull();
  await user.type(container.querySelector('input#familyName'), testInput);
  expect(container.querySelector('#familyNameError')).not.toBeNull();
});

it('should handle username validity starts with .', async () => {
  const user = userEvent.setup();
  const { container } = component();
  expect(container.querySelector('#username')).not.toBeNull();
  expect(container.querySelector('#usernameError')).toBeNull();
  expect(container.querySelector('#usernameConflictError')).toBeNull();
  await user.type(container.querySelector('input#username'), '.startswith');
  expect(container.querySelector('#usernameError')).not.toBeNull();
});

it('should handle username validity ends with .', async () => {
  const user = userEvent.setup();
  const { container } = component();
  expect(container.querySelector('#username')).not.toBeNull();
  expect(container.querySelector('#usernameError')).toBeNull();
  await user.type(container.querySelector('input#username'), 'endswith.');
  expect(container.querySelector('#usernameError')).not.toBeNull();
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
])('should mark username %s as invalid', async (username) => {
  const user = userEvent.setup();
  const { container } = component();
  expect(container.querySelector('#username')).not.toBeNull();
  expect(container.querySelector('#usernameError')).toBeNull();
  await user.type(container.querySelector('input#username'), username);
  expect(container.querySelector('#usernameError')).not.toBeNull();
});

it('should handle username validity long but has mismatch at end', async () => {
  const user = userEvent.setup();
  const { container } = component();
  expect(container.querySelector('#username')).not.toBeNull();
  expect(container.querySelector('#usernameError')).toBeNull();
  // if username is long (not too long) but has a mismatch at end
  let testInput = fp.repeat(50, 'abc');
  testInput = testInput + ' abc';
  await user.type(container.querySelector('input#username'), testInput);
  expect(container.querySelector('#usernameError')).not.toBeNull();
});

it('should handle username validity length less than 3 characters', async () => {
  const user = userEvent.setup();
  const { container } = component();
  expect(container.querySelector('#username')).not.toBeNull();
  expect(container.querySelector('#usernameError')).toBeNull();
  await user.type(container.querySelector('input#username'), 'a');
  expect(container.querySelector('#usernameError')).not.toBeNull();
});

it('should handle username validity if name is valid', async () => {
  const user = userEvent.setup();
  const { container } = component();
  expect(container.querySelector('#username')).not.toBeNull();
  expect(container.querySelector('#usernameError')).toBeNull();
  await user.type(container.querySelector('input#username'), 'username');
  expect(container.querySelector('#usernameError')).toBeNull();
});

it('should display characters over message if research purpose character length is more than 2000', async () => {
  const user = userEvent.setup();
  const { container } = component();

  const areaOfResearchTextBox = container.querySelector(
    'textarea#areaOfResearch'
  );
  expect(screen.queryByText('2000 characters remaining')).not.toBeNull();

  let testInput = fp.repeat(2000, 'a');
  await user.click(areaOfResearchTextBox);
  await user.paste(testInput);
  expect(screen.queryByText('0 characters remaining')).not.toBeNull();

  testInput = fp.repeat(2010, 'a');
  await user.dblClick(areaOfResearchTextBox);
  await user.paste(testInput);
  expect(screen.queryByText('10 characters over')).not.toBeNull();

  // Characters remaining message should not be displayed
  expect(container.querySelector('[data-test-id="charRemaining"]')).toBeNull();
});

it('should display a text input field for non-US countries', async () => {
  const user = userEvent.setup();
  const { container } = component();
  expect(container.querySelector('[data-test-id="country-input"]')).toBeNull();
  await user.click(findCountryDropdownField());
  await user.paste(countryDropdownOption.unitedStates);
  await user.keyboard('{enter}');
  expect(container.querySelector('[data-test-id="country-input"]')).toBeNull();
  await user.clear(findCountryDropdownField());
  await user.click(findCountryDropdownField());
  await user.paste(countryDropdownOption.other);
  await user.keyboard('{enter}');
  expect(findCountryInputField()).not.toBeNull();
  await user.type(findCountryInputField(), 'Canada');
  expect(findCountryInputField().value).toEqual('Canada');
});

it('should capitalize a state code when selecting USA', async () => {
  const user = userEvent.setup();
  component();
  await user.type(findStateField(), 'ny');
  expect(findStateField().value).toEqual('ny');
  await user.click(findCountryDropdownField());
  await user.paste(countryDropdownOption.unitedStates);
  await user.keyboard('{enter}');
  expect(findStateField().value).toEqual('NY');
});

it('should change a state name to a state code after selecting USA', async () => {
  const user = userEvent.setup();
  component();
  await user.type(findStateField(), 'new york');
  expect(findStateField().value).toEqual('new york');
  await user.click(findCountryDropdownField());
  await user.paste(countryDropdownOption.unitedStates);
  await user.keyboard('{enter}');
  expect(findStateField().value).toEqual('NY');
});

it('should mark US states as invalid if not a 2-letter code', async () => {
  const user = userEvent.setup();
  const { container } = component();
  expect(container.querySelector('#stateError')).toBeNull();
  expect(screen.queryByText(stateCodeErrorMessage)).toBeNull();
  await user.click(findCountryDropdownField());
  await user.paste(countryDropdownOption.unitedStates);
  await user.keyboard('{enter}');
  await user.type(findStateField(), 'new york');
  expect(container.querySelector('#stateError')).not.toBeNull();
  expect(screen.queryByText(stateCodeErrorMessage)).not.toBeNull();
});
