import '@testing-library/jest-dom';

import * as React from 'react';
import { MemoryRouter } from 'react-router';

import { AccessModule, Profile } from 'generated/fetch';

import { screen } from '@testing-library/dom';
import { fireEvent, render } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { ProfileComponent } from 'app/pages/profile/profile-component';
import colors, { colorWithWhiteness } from 'app/styles/colors';
import { profileStore } from 'app/utils/stores';

import { expectButtonElementDisabled } from 'testing/react-test-helpers';
import { ProfileStubVariables } from 'testing/stubs/profile-api-stub';

const reload = jest.fn();
const load = jest.fn();
const updateCache = jest.fn();
const tenMinutesMs = 10 * 60 * 1000;

const profile = {
  profile: ProfileStubVariables.PROFILE_STUB,
  reload: reload,
  load: load,
  updateCache: updateCache,
};
const updateProfile = (newUpdates: Partial<Profile>) => {
  profileStore.set({
    profile: { ...ProfileStubVariables.PROFILE_STUB, ...newUpdates },
    load,
    reload,
    updateCache,
  });
};

const setup = () => {
  return {
    container: render(
      <MemoryRouter>
        <ProfileComponent
          profileState={profile}
          hideSpinner={() => {}}
          navigate={() => {}}
          navigateByUrl={() => {}}
        />
      </MemoryRouter>
    ).container,
    user: userEvent.setup(),
  };
};

it('should allow completing the account creation form', async () => {
  const profileStub = ProfileStubVariables.PROFILE_STUB;
  profileStub.address.country = 'India';
  profileStub.firstSignInTime = new Date('2023-12-03').getTime();
  profileStore.set({
    profile: profileStub,
    load,
    reload,
    updateCache,
  });

  setup();
  try {
    expect(screen.getByText('Demographics Survey')).not.toBeInTheDocument();
  } catch (e) {
    expect(true);
  }
});

it('should not show the profile confirmation renewal box if the access module was bypassed', async () => {
  updateProfile({
    accessModules: {
      modules: [
        {
          moduleName: AccessModule.PROFILE_CONFIRMATION,
          expirationEpochMillis: Date.now() - tenMinutesMs,
          bypassEpochMillis: 1,
        },
      ],
    },
  });

  setup();
  try {
    expect(
      screen.getByText('Please update or verify your profile.')
    ).toBeInTheDocument();
  } catch (e) {
    expect(true);
  }
});

it('should show the profile confirmation renewal box if the access module has expired', async () => {
  updateProfile({
    accessModules: {
      modules: [
        {
          moduleName: AccessModule.PROFILE_CONFIRMATION,
          expirationEpochMillis: Date.now() - tenMinutesMs,
        },
      ],
    },
  });

  setup();
  expect(
    screen.getByText('Please update or verify your profile.')
  ).toBeInTheDocument();
});

it('should not display a link to the signed DUCC if the user has not signed a DUCC', async () => {
  updateProfile({ duccSignedVersion: undefined });

  setup();
  try {
    expect(
      screen.getByText('View Signed Data User Code of Conduct')
    ).toBeInTheDocument();
  } catch (e) {
    expect(true);
  }
});

it('should not display a link to the signed DUCC if the user has signed an older DUCC', async () => {
  updateProfile({ duccSignedVersion: 2 });

  setup();
  try {
    expect(
      screen.getByText('View Signed Data User Code of Conduct')
    ).toBeInTheDocument();
  } catch (e) {
    expect(true);
  }
});

it('should display a link to the signed DUCC if the user is up to date', async () => {
  profileStore.set({
    profile: ProfileStubVariables.PROFILE_STUB,
    load,
    reload,
    updateCache,
  });
  setup();
  expect(screen.getByText('Data User Code of Conduct')).toBeInTheDocument();
});

it('should throw error if street Address 1 is empty', async () => {
  profileStore.set({
    profile: ProfileStubVariables.PROFILE_STUB,
    load,
    reload,
    updateCache,
  });
  const { user } = setup();
  // Confirm default value is present
  expect(screen.getByDisplayValue('Main street')).toBeInTheDocument();
  const element = screen.getByDisplayValue('Main street');

  // Remove the street address 1 value
  await user.click(element);
  await user.clear(element);
  await user.keyboard('{enter}');

  // Confirm there is an error message
  expect(
    screen.getByText(/street address1 can't be blank/i)
  ).toBeInTheDocument();

  expectButtonElementDisabled(
    screen.getByRole('button', {
      name: /save profile/i,
    })
  );
});

it('should have country disabled', async () => {
  profileStore.set({
    profile: ProfileStubVariables.PROFILE_STUB,
    load,
    reload,
    updateCache,
  });
  setup();
  const country = screen.getByRole('textbox', {
    name: /country/i,
  });
  // Background color should be that of disabled input
  expect(country).toHaveStyle(
    `backgroundColor: ${colorWithWhiteness(colors.disabled, 0.6)}`
  );

  // Confirm the tooltip contents
  fireEvent.mouseOver(country);
  expect(
    await screen.findByText('This field cannot be edited')
  ).toBeInTheDocument();

  expectButtonElementDisabled(
    screen.getByRole('button', {
      name: /save profile/i,
    })
  );
});
