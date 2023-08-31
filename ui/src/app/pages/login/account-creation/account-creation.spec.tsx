import * as React from 'react';
import * as fp from 'lodash/fp';

import {
  GeneralDiscoverySource,
  PartnerDiscoverySource,
  Profile,
  ProfileApi,
} from 'generated/fetch';

import { screen } from '@testing-library/dom';
import { render } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { UserEvent } from '@testing-library/user-event/setup/setup';
import { createEmptyProfile } from 'app/pages/login/sign-in';
import { registerApiClient } from 'app/services/swagger-fetch-clients';
import Country from 'app/utils/countries';
import { serverConfigStore } from 'app/utils/stores';

import { ProfileApiStub } from 'testing/stubs/profile-api-stub';

import {
  AccountCreation,
  AccountCreationProps,
  formLabels,
  stateCodeErrorMessage,
} from './account-creation';

const createProps = (): AccountCreationProps => ({
  profile: createEmptyProfile(),
  onComplete: () => {},
  onPreviousClick: () => {},
});

const setup = (props = createProps()) => {
  return {
    container: render(<AccountCreation {...props} />).container,
    user: userEvent.setup(),
  };
};

function findStateField() {
  return screen.getByLabelText(formLabels.state) as HTMLInputElement;
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

const fillInMinimalFields = async (user: UserEvent) => {
  await user.type(screen.getByLabelText(formLabels.username), 'username');
  await user.type(screen.getByLabelText(formLabels.givenName), 'Firstname');
  await user.type(screen.getByLabelText(formLabels.familyName), 'Lastname');

  await user.type(
    screen.getByLabelText(formLabels.streetAddress1),
    '1 Main Street'
  );
  await user.type(screen.getByLabelText(formLabels.city), 'Boston');
  await user.type(findStateField(), 'MA');
  await user.type(screen.getByLabelText(formLabels.zipCode), '02115');
  await user.click(findCountryDropdownField());
  await user.paste(Country.US);
  await user.keyboard('{enter}');

  await user.click(getAreaOfResearchTextBox());
  await user.paste('I am an undergraduate learning genomics.');
};

it('should allow completing the account creation form', async () => {
  const onComplete = jest.fn();
  const { user } = setup({
    ...createProps(),
    onComplete,
  });

  await fillInMinimalFields(user);

  await user.click(screen.getByLabelText('Next'));

  expect(onComplete).toHaveBeenCalled();
  const profile: Profile = onComplete.mock.calls[0][0];

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
  const { container, user } = setup();
  const testInput = fp.repeat(101, 'a');
  expect(container.querySelector('#givenName')).not.toBeNull();
  expect(container.querySelector('#givenNameError')).toBeNull();
  await user.type(screen.getByLabelText(formLabels.givenName), testInput);
  expect(container.querySelector('#givenNameError')).not.toBeNull();
});

it('should handle family name validity', async () => {
  const { container, user } = setup();
  const testInput = fp.repeat(101, 'a');
  expect(container.querySelector('#familyName')).not.toBeNull();
  expect(container.querySelector('#familyNameError')).toBeNull();
  await user.type(screen.getByLabelText(formLabels.familyName), testInput);
  expect(container.querySelector('#familyNameError')).not.toBeNull();
});

it('should handle username validity starts with .', async () => {
  const { container, user } = setup();
  expect(container.querySelector('#username')).not.toBeNull();
  expect(container.querySelector('#usernameError')).toBeNull();
  expect(container.querySelector('#usernameConflictError')).toBeNull();
  await user.type(screen.getByLabelText(formLabels.username), '.startswith');
  expect(container.querySelector('#usernameError')).not.toBeNull();
});

it('should handle username validity ends with .', async () => {
  const { container, user } = setup();
  expect(container.querySelector('#username')).not.toBeNull();
  expect(container.querySelector('#usernameError')).toBeNull();
  await user.type(screen.getByLabelText(formLabels.username), 'endswith.');
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
  const { container, user } = setup();
  expect(container.querySelector('#username')).not.toBeNull();
  expect(container.querySelector('#usernameError')).toBeNull();
  await user.type(screen.getByLabelText(formLabels.username), username);
  expect(container.querySelector('#usernameError')).not.toBeNull();
});

it('should handle username validity long but has mismatch at end', async () => {
  const { container, user } = setup();
  expect(container.querySelector('#username')).not.toBeNull();
  expect(container.querySelector('#usernameError')).toBeNull();
  // if username is long (not too long) but has a mismatch at end
  let testInput = fp.repeat(50, 'abc');
  testInput = testInput + ' abc';
  await user.type(screen.getByLabelText(formLabels.username), testInput);
  expect(container.querySelector('#usernameError')).not.toBeNull();
});

it('should handle username validity length less than 3 characters', async () => {
  const { container, user } = setup();
  expect(container.querySelector('#username')).not.toBeNull();
  expect(container.querySelector('#usernameError')).toBeNull();
  await user.type(screen.getByLabelText(formLabels.username), 'a');
  expect(container.querySelector('#usernameError')).not.toBeNull();
});

it('should handle username validity if name is valid', async () => {
  const { container, user } = setup();
  expect(container.querySelector('#username')).not.toBeNull();
  expect(container.querySelector('#usernameError')).toBeNull();
  await user.type(screen.getByLabelText(formLabels.username), 'username');
  expect(container.querySelector('#usernameError')).toBeNull();
});

it('should display characters over message if research purpose character length is more than 2000', async () => {
  const { container, user } = setup();

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
  const { container, user } = setup();
  expect(container.querySelector('[data-test-id="country-input"]')).toBeNull();
  await user.click(findCountryDropdownField());
  await user.paste(Country.US);
  await user.keyboard('{enter}');
  expect(container.querySelector('[data-test-id="country-input"]')).toBeNull();
  await user.clear(findCountryDropdownField());
  await user.click(findCountryDropdownField());
  await user.paste(Country.OTHER);
  await user.keyboard('{enter}');
  expect(findCountryInputField()).not.toBeNull();
  await user.type(findCountryInputField(), 'Canada');
  expect(findCountryInputField().value).toEqual('Canada');
});

it('should capitalize a state code when selecting USA', async () => {
  const { user } = setup();
  await user.type(findStateField(), 'ny');
  expect(findStateField().value).toEqual('ny');
  await user.click(findCountryDropdownField());
  await user.paste(Country.US);
  await user.keyboard('{enter}');
  expect(findStateField().value).toEqual('NY');
});

it('should change a state name to a state code after selecting USA', async () => {
  const { user } = setup();
  await user.type(findStateField(), 'new york');
  expect(findStateField().value).toEqual('new york');
  await user.click(findCountryDropdownField());
  await user.paste(Country.US);
  await user.keyboard('{enter}');
  expect(findStateField().value).toEqual('NY');
});

it('should mark US states as invalid if not a 2-letter code', async () => {
  const { container, user } = setup();
  expect(container.querySelector('#stateError')).toBeNull();
  expect(screen.queryByText(stateCodeErrorMessage)).toBeNull();
  await user.click(findCountryDropdownField());
  await user.paste(Country.US);
  await user.keyboard('{enter}');
  await user.type(findStateField(), 'new york');
  expect(container.querySelector('#stateError')).not.toBeNull();
  expect(screen.queryByText(stateCodeErrorMessage)).not.toBeNull();
});

it('should allow entering discovery sources', async () => {
  const onComplete = jest.fn();
  const { user } = setup({
    ...createProps(),
    onComplete,
  });

  await fillInMinimalFields(user);

  await user.click(screen.getByRole('checkbox', { name: /social media/i }));
  await user.click(
    screen.getByRole('checkbox', { name: /friends or colleagues/i })
  );
  await user.click(
    screen.getByRole('checkbox', { name: /all of us research program staff/i })
  );
  await user.click(
    screen.getByRole('checkbox', { name: /asian health coalition/i })
  );

  await user.click(screen.getByLabelText('Next'));
  expect(onComplete).toHaveBeenCalled();
  const profile: Profile = onComplete.mock.calls[0][0];

  expect(profile.generalDiscoverySources).toEqual([
    GeneralDiscoverySource.SOCIALMEDIA,
    GeneralDiscoverySource.FRIENDSORCOLLEAGUES,
  ]);
  expect(profile.partnerDiscoverySources).toEqual([
    PartnerDiscoverySource.ALLOFUSRESEARCHPROGRAMSTAFF,
    PartnerDiscoverySource.ASIANHEALTHCOALITION,
  ]);
});

it('should allow entering details for "other" discovery sources', async () => {
  const onComplete = jest.fn();
  const { user } = setup({
    ...createProps(),
    onComplete,
  });

  await fillInMinimalFields(user);

  // Both forms contain an "Other" checkbox
  const otherCheckboxes = screen.getAllByRole('checkbox', { name: 'Other' });
  expect(otherCheckboxes.length).toEqual(2);

  await user.click(otherCheckboxes[0]);
  await user.type(
    screen.getByPlaceholderText('Please Describe'),
    'general discovery source - other'
  );
  await user.click(otherCheckboxes[1]);
  await user.type(
    screen.getAllByPlaceholderText('Please Describe')[1],
    'partner discovery source - other'
  );

  await user.click(screen.getByLabelText('Next'));
  expect(onComplete).toHaveBeenCalled();
  const profile: Profile = onComplete.mock.calls[0][0];

  expect(profile.generalDiscoverySourceOtherText).toEqual(
    'general discovery source - other'
  );
  expect(profile.partnerDiscoverySourceOtherText).toEqual(
    'partner discovery source - other'
  );
});

it('should reset "other" discovery source details to null if "other" is unselected', async () => {
  const onComplete = jest.fn();
  const { user } = setup({
    ...createProps(),
    onComplete,
  });

  await fillInMinimalFields(user);

  // Both forms contain an "Other" checkbox
  const otherCheckboxes = screen.getAllByRole('checkbox', { name: 'Other' });
  expect(otherCheckboxes.length).toEqual(2);

  await user.click(otherCheckboxes[0]);
  await user.type(
    screen.getByPlaceholderText('Please Describe'),
    'general discovery source - other'
  );
  await user.click(otherCheckboxes[1]);
  await user.type(
    screen.getAllByPlaceholderText('Please Describe')[1],
    'partner discovery source - other'
  );

  await user.click(otherCheckboxes[0]);
  await user.click(otherCheckboxes[1]);

  await user.click(screen.getByLabelText('Next'));
  expect(onComplete).toHaveBeenCalled();
  const profile: Profile = onComplete.mock.calls[0][0];

  expect(profile.generalDiscoverySourceOtherText).toBeNull();
  expect(profile.partnerDiscoverySourceOtherText).toBeNull();
});
