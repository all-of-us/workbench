import '@testing-library/jest-dom';

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
import { Country } from 'app/utils/constants';
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
  onSubmit: () => {},
  captureCaptchaResponse: (token) => {
    console.log(token);
  },
  // // ToDo change this
  captchaRef: null,
});

const setup = (props = createProps()) => {
  return {
    container: render(<AccountCreation {...props} />).container,
    user: userEvent.setup(),
  };
};

function getStateField(): HTMLInputElement {
  return screen.getByLabelText(formLabels.state);
}

function getCountryDropdownField(): HTMLSelectElement {
  return screen.getByLabelText('Country dropdown');
}

function queryOtherCountryInputField(): HTMLInputElement {
  return screen.queryByLabelText('Other country input');
}

function getAreaOfResearchTextArea(): HTMLTextAreaElement {
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
  await user.click(screen.getByLabelText(formLabels.username));
  await user.paste('username');
  await user.click(screen.getByLabelText(formLabels.givenName));
  await user.paste('Firstname');
  await user.click(screen.getByLabelText(formLabels.familyName));
  await user.paste('Lastname');

  await user.click(screen.getByLabelText(formLabels.streetAddress1));
  await user.paste('1 Main Street');
  await user.click(screen.getByLabelText(formLabels.city));
  await user.paste('Boston');
  await user.click(getStateField());
  await user.paste('MA');
  await user.click(screen.getByLabelText(formLabels.zipCode));
  await user.paste('02115');
  await user.click(getCountryDropdownField());
  await user.paste(Country.US);
  await user.keyboard('{enter}');

  await user.click(getAreaOfResearchTextArea());
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
  expect(profile.address.country).toEqual(Country.US);
  expect(profile.areaOfResearch).toEqual(
    'I am an undergraduate learning genomics.'
  );
});

it('should handle given name validity', async () => {
  const { container, user } = setup();
  const testInput = fp.repeat(101, 'a');
  expect(container.querySelector('#givenName')).not.toBeNull();
  expect(container.querySelector('#givenNameError')).toBeNull();
  await user.click(screen.getByLabelText(formLabels.givenName));
  await user.paste(testInput);
  expect(container.querySelector('#givenNameError')).not.toBeNull();
});

it('should handle family name validity', async () => {
  const { container, user } = setup();
  const testInput = fp.repeat(101, 'a');
  expect(container.querySelector('#familyName')).not.toBeNull();
  expect(container.querySelector('#familyNameError')).toBeNull();
  await user.click(screen.getByLabelText(formLabels.familyName));
  await user.paste(testInput);
  expect(container.querySelector('#familyNameError')).not.toBeNull();
});

it('should handle username validity starts with .', async () => {
  const { container, user } = setup();
  expect(container.querySelector('#username')).not.toBeNull();
  expect(container.querySelector('#usernameError')).toBeNull();
  expect(container.querySelector('#usernameConflictError')).toBeNull();
  await user.click(screen.getByLabelText(formLabels.username));
  await user.paste('.startswith');
  expect(container.querySelector('#usernameError')).not.toBeNull();
});

it('should handle username validity ends with .', async () => {
  const { container, user } = setup();
  expect(container.querySelector('#username')).not.toBeNull();
  expect(container.querySelector('#usernameError')).toBeNull();
  await user.click(screen.getByLabelText(formLabels.username));
  await user.paste('endswith.');
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
  await user.click(screen.getByLabelText(formLabels.username));
  await user.paste(username);
  expect(container.querySelector('#usernameError')).not.toBeNull();
});

it('should handle username validity long but has mismatch at end', async () => {
  const { container, user } = setup();
  expect(container.querySelector('#username')).not.toBeNull();
  expect(container.querySelector('#usernameError')).toBeNull();
  // if username is long (not too long) but has a mismatch at end
  let testInput = fp.repeat(50, 'abc');
  testInput = testInput + ' abc';
  await user.click(screen.getByLabelText(formLabels.username));
  await user.paste(testInput);
  expect(container.querySelector('#usernameError')).not.toBeNull();
});

it('should handle username validity length less than 3 characters', async () => {
  const { container, user } = setup();
  expect(container.querySelector('#username')).not.toBeNull();
  expect(container.querySelector('#usernameError')).toBeNull();
  await user.click(screen.getByLabelText(formLabels.username));
  await user.paste('a');
  expect(container.querySelector('#usernameError')).not.toBeNull();
});

it('should handle username validity if name is valid', async () => {
  const { container, user } = setup();
  expect(container.querySelector('#username')).not.toBeNull();
  expect(container.querySelector('#usernameError')).toBeNull();
  await user.click(screen.getByLabelText(formLabels.username));
  await user.paste('username');
  expect(container.querySelector('#usernameError')).toBeNull();
});

it('should display characters over message if research purpose character length is more than 2000', async () => {
  const { container, user } = setup();

  expect(screen.queryByText('2000 characters remaining')).not.toBeNull();

  let testInput = fp.repeat(2000, 'a');
  await user.click(getAreaOfResearchTextArea());
  await user.paste(testInput);
  expect(screen.queryByText('0 characters remaining')).not.toBeNull();

  testInput = fp.repeat(2010, 'a');
  await user.clear(getAreaOfResearchTextArea());
  await user.click(getAreaOfResearchTextArea());
  await user.paste(testInput);
  expect(screen.queryByText('10 characters over')).not.toBeNull();

  // Characters remaining message should not be displayed
  expect(container.querySelector('[data-test-id="charRemaining"]')).toBeNull();
});

it('should display a dropdown for non-US countries', async () => {
  const { user } = setup();
  expect(queryOtherCountryInputField()).toBeNull();
  await user.click(getCountryDropdownField());
  await user.paste(Country.US);
  expect(getCountryDropdownField().value).toEqual('United States');
  expect(queryOtherCountryInputField()).toBeNull();
  await user.clear(getCountryDropdownField());
  await user.click(getCountryDropdownField());
  await user.paste(Country.CA);
  expect(getCountryDropdownField().value).toEqual('Canada');
  expect(queryOtherCountryInputField()).toBeNull();
});

it('should display a text input field for "other" non-US countries', async () => {
  const { user } = setup();
  expect(queryOtherCountryInputField()).toBeNull();
  await user.click(getCountryDropdownField());
  await user.paste(Country.OTHER);
  await user.keyboard('{enter}');
  expect(queryOtherCountryInputField()).not.toBeNull();
  await user.click(queryOtherCountryInputField());
  await user.paste('Sokovia');
  expect(queryOtherCountryInputField().value).toEqual('Sokovia');
});

it('should capitalize a state code when selecting USA', async () => {
  const { user } = setup();
  await user.click(getStateField());
  await user.paste('ny');
  expect(getStateField().value).toEqual('ny');
  await user.click(getCountryDropdownField());
  await user.paste(Country.US);
  await user.keyboard('{enter}');
  expect(getStateField().value).toEqual('NY');
});

it('should change a state name to a state code after selecting USA', async () => {
  const { user } = setup();
  await user.click(getStateField());
  await user.paste('new york');
  expect(getStateField().value).toEqual('new york');
  await user.click(getCountryDropdownField());
  await user.paste(Country.US);
  await user.keyboard('{enter}');
  expect(getStateField().value).toEqual('NY');
});

it('should mark US states as invalid if not a 2-letter code', async () => {
  const { container, user } = setup();
  expect(container.querySelector('#stateError')).toBeNull();
  expect(screen.queryByText(stateCodeErrorMessage)).toBeNull();
  await user.click(getCountryDropdownField());
  await user.paste(Country.US);
  await user.keyboard('{enter}');
  await user.click(getStateField());
  await user.paste('new york');
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
    GeneralDiscoverySource.SOCIAL_MEDIA,
    GeneralDiscoverySource.FRIENDS_OR_COLLEAGUES,
  ]);
  expect(profile.partnerDiscoverySources).toEqual([
    PartnerDiscoverySource.ALL_OF_US_RESEARCH_PROGRAM_STAFF,
    PartnerDiscoverySource.ASIAN_HEALTH_COALITION,
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
  await user.click(screen.getByPlaceholderText('Please Describe'));
  await user.paste('general discovery source - other');
  await user.click(otherCheckboxes[1]);
  await user.click(screen.getAllByPlaceholderText('Please Describe')[1]);
  await user.paste('partner discovery source - other');

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
  await user.click(screen.getByPlaceholderText('Please Describe'));
  await user.paste('general discovery source - other');
  await user.click(otherCheckboxes[1]);
  await user.click(screen.getAllByPlaceholderText('Please Describe')[1]);
  await user.paste('partner discovery source - other');

  await user.click(otherCheckboxes[0]);
  await user.click(otherCheckboxes[1]);

  await user.click(screen.getByLabelText('Next'));
  expect(onComplete).toHaveBeenCalled();
  const profile: Profile = onComplete.mock.calls[0][0];

  expect(profile.generalDiscoverySourceOtherText).toBeNull();
  expect(profile.partnerDiscoverySourceOtherText).toBeNull();
});

it('Should show submit button if country is not US', async () => {
  const onSubmit = jest.fn();
  const { user } = setup({
    ...createProps(),
    onSubmit,
  });
  await user.click(getCountryDropdownField());
  await user.paste(Country.AL);
  await user.keyboard('{enter}');
  expect(screen.getByText('Albania')).toBeInTheDocument();
  expect(screen.getByText('Submit')).toBeInTheDocument();
});
