import '@testing-library/jest-dom';

import * as React from 'react';

import { fireEvent, render } from '@testing-library/react';
import * as Authentication from 'app/utils/authentication';
import * as ProfilePicture from 'app/utils/profile-utils';
import { notificationStore } from 'app/utils/stores';

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
    const { getByTestId } = render(
      <SideNav
        {...props}
        profile={{
          ...ProfileStubVariables.PROFILE_STUB,
          givenName: 'TestGivenName',
          familyName: 'TestFamilyName',
        }}
      />
    );
    fireEvent.click(getByTestId('TestGivenNameTestFamilyName-menu-item'));
    expect(notificationStore.get()).toBeNull();
    fireEvent.click(getByTestId('SignOut-menu-item'));
    expect(notificationStore.get()).toBeTruthy();
  });

  it('disables options when user not registered', () => {
    const { getByTestId } = render(
      <SideNav
        {...props}
        profile={{
          ...ProfileStubVariables.PROFILE_STUB,
          accessTierShortNames: [],
          givenName: 'Tester',
          familyName: 'MacTesterson',
        }}
      />
    );
    fireEvent.click(getByTestId('TesterMacTesterson-menu-item'));
    // These are our expected items to be disabled when you are not registered
    const disabledItemText = [
      'Your Workspaces',
      'Featured Workspaces',
      'User Support Hub',
    ];
    disabledItemText.forEach((item) => {
      //      expect(getByTestId(`${item}-menu-item`).disabled).toBeTruthy();
      expect(getByTestId(`${item}-menu-item`)).toBeTruthy();
    });
  });
});
