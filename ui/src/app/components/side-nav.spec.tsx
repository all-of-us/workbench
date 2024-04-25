import '@testing-library/jest-dom';

import * as React from 'react';

import { fireEvent, render, screen } from '@testing-library/react';
import * as Authentication from 'app/utils/authentication';
import * as ProfilePicture from 'app/utils/profile-utils';
import { notificationStore } from 'app/utils/stores';

import {
  expectButtonElementDisabled,
  expectMenuItemElementDisabled,
} from 'testing/react-test-helpers';
import { ProfileStubVariables } from 'testing/stubs/profile-api-stub';

import { SideNav, SideNavProps } from './side-nav';

describe(SideNav.name, () => {
  const props: SideNavProps = {
    profile: ProfileStubVariables.PROFILE_STUB,
    onToggleSideNav: () => {},
  };

  const spy = jest.spyOn(ProfilePicture, 'getProfilePictureSrc');
  spy.mockReturnValue('lol.png');

  it('should render', () => {
    const { getByLabelText } = render(<SideNav {...props} />);
    expect(getByLabelText('Side Navigation Bar')).toBeInTheDocument();
  });

  it('should show an error when signout fails', () => {
    const signOutSpy = jest.spyOn(Authentication, 'signOut');
    signOutSpy.mockImplementation(() => {
      throw new Error();
    });

    const givenName = 'TestGivenName';
    const familyName = 'TestFamilyName';
    const { getByText } = render(
      <SideNav
        {...props}
        profile={{
          ...ProfileStubVariables.PROFILE_STUB,
          givenName,
          familyName,
        }}
      />
    );

    fireEvent.click(getByText(`${givenName} ${familyName}`));
    expect(notificationStore.get()).toBeNull();

    fireEvent.click(getByText('Sign Out'));
    expect(notificationStore.get()).toBeTruthy();
  });

  it('disables options when user not registered', () => {
    const givenName = 'TestGivenName';
    const familyName = 'TestFamilyName';
    const { getByText, getByRole } = render(
      <SideNav
        {...props}
        profile={{
          ...ProfileStubVariables.PROFILE_STUB,
          accessTierShortNames: [],
          givenName,
          familyName,
        }}
      />
    );
    fireEvent.click(getByText(`${givenName} ${familyName}`));

    // These are our expected items to be disabled when you are not registered
    const disabledItemText = [
      'Your Workspaces',
      'Featured Workspaces',
      'User Support Hub',
    ];
    screen.debug();
    disabledItemText.forEach((name) =>
      expectButtonElementDisabled(getByRole('button', { name }))
    );
  });
});
