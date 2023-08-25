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
  FORM_LABELS,
  stateCodeErrorMessage,
} from './account-creation';

const createProps = (): AccountCreationProps => ({
  profile: createEmptyProfile(),
  onComplete: () => {},
  onPreviousClick: () => {},
});

const component = (props = createProps()) => {
  return render(<AccountCreation {...props} />);
};

function findStateField() {
  return screen.getByLabelText(FORM_LABELS.state) as HTMLInputElement;
}

function findCountryDropdownField() {
  return screen.getByLabelText('Country dropdown') as HTMLSelectElement;
}

function findCountryInputField() {
  return screen.getByLabelText('Country input') as HTMLInputElement;
}

function getAreaOfResearchTextBox() {
  return screen.getByLabelText(
    'Your research background, experience, and research interests'
  );
}

const defaultConfig = { gsuiteDomain: 'researchallofus.org' };

beforeEach(() => {
  serverConfigStore.set({ config: defaultConfig });
  registerApiClient(ProfileApi, new ProfileApiStub());
});

it('should allow completing the account creation form', async () => {
  const user = userEvent.setup();
  const onComplete = jest.fn();
  component({
    ...createProps(),
    onComplete,
  });

  await user.type(screen.getByLabelText(FORM_LABELS.username), 'username');
  await user.type(screen.getByLabelText(FORM_LABELS.givenName), 'Firstname');
  await user.type(screen.getByLabelText(FORM_LABELS.familyName), 'Lastname');

  await user.type(
    screen.getByLabelText(FORM_LABELS.streetAddress1),
    '1 Main Street'
  );
  await user.type(screen.getByLabelText(FORM_LABELS.city), 'Boston');
  await user.type(findStateField(), 'MA');
  await user.type(screen.getByLabelText(FORM_LABELS.zipCode), '02115');
  await user.click(findCountryDropdownField());
  await user.paste(countryDropdownOption.unitedStates);
  await user.keyboard('{enter}');

  await user.click(getAreaOfResearchTextBox());
  await user.paste('I am an undergraduate learning genomics.');

  await user.click(screen.getByLabelText('Next'));

  expect(onComplete).toHaveBeenCalled();
  const profile = onComplete.mock.calls[0][0];

  expect(profile.username).toEqual('username');
  expect(profile.givenName).toEqual('Firstname');
  expect(profile.familyName).toEqual('Lastname');
  expect(profile.address.streetAddress1).toEqual('1 Main Street');
  expect(profile.address.city).toEqual('Boston');
  expect(profile.address.state).toEqual('MA');
  expect(profile.address.zipCode).toEqual('02115');
  expect(profile.address.country).toEqual('United States of America');
  expect(profile.areaOfResearch).toEqual(
    'I am an undergraduate learning genomics.'
  );
});

it('should handle given name validity', async () => {
  const user = userEvent.setup();
  const { container } = component();
  const testInput = fp.repeat(101, 'a');
  expect(container.querySelector('#givenName')).not.toBeNull();
  expect(container.querySelector('#givenNameError')).toBeNull();
  await user.type(screen.getByLabelText(FORM_LABELS.givenName), testInput);
  expect(container.querySelector('#givenNameError')).not.toBeNull();
});

it('should handle family name validity', async () => {
  const user = userEvent.setup();
  const { container } = component();
  const testInput = fp.repeat(101, 'a');
  expect(container.querySelector('#familyName')).not.toBeNull();
  expect(container.querySelector('#familyNameError')).toBeNull();
  await user.type(screen.getByLabelText(FORM_LABELS.familyName), testInput);
  expect(container.querySelector('#familyNameError')).not.toBeNull();
});

it('should handle username validity starts with .', async () => {
  const user = userEvent.setup();
  const { container } = component();
  expect(container.querySelector('#username')).not.toBeNull();
  expect(container.querySelector('#usernameError')).toBeNull();
  expect(container.querySelector('#usernameConflictError')).toBeNull();
  await user.type(screen.getByLabelText(FORM_LABELS.username), '.startswith');
  expect(container.querySelector('#usernameError')).not.toBeNull();
});

it('should handle username validity ends with .', async () => {
  const user = userEvent.setup();
  const { container } = component();
  expect(container.querySelector('#username')).not.toBeNull();
  expect(container.querySelector('#usernameError')).toBeNull();
  await user.type(screen.getByLabelText(FORM_LABELS.username), 'endswith.');
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
  await user.type(screen.getByLabelText(FORM_LABELS.username), username);
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
  await user.type(screen.getByLabelText(FORM_LABELS.username), testInput);
  expect(container.querySelector('#usernameError')).not.toBeNull();
});

it('should handle username validity length less than 3 characters', async () => {
  const user = userEvent.setup();
  const { container } = component();
  expect(container.querySelector('#username')).not.toBeNull();
  expect(container.querySelector('#usernameError')).toBeNull();
  await user.type(screen.getByLabelText(FORM_LABELS.username), 'a');
  expect(container.querySelector('#usernameError')).not.toBeNull();
});

it('should handle username validity if name is valid', async () => {
  const user = userEvent.setup();
  const { container } = component();
  expect(container.querySelector('#username')).not.toBeNull();
  expect(container.querySelector('#usernameError')).toBeNull();
  await user.type(screen.getByLabelText(FORM_LABELS.username), 'username');
  expect(container.querySelector('#usernameError')).toBeNull();
});

it('should display characters over message if research purpose character length is more than 2000', async () => {
  const user = userEvent.setup();
  const { container } = component();

  expect(screen.queryByText('2000 characters remaining')).not.toBeNull();

  let testInput = fp.repeat(2000, 'a');
  await user.click(getAreaOfResearchTextBox());
  await user.paste(testInput);
  expect(screen.queryByText('0 characters remaining')).not.toBeNull();

  testInput = fp.repeat(2010, 'a');
  await user.clear(getAreaOfResearchTextBox());
  await user.click(getAreaOfResearchTextBox());
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
