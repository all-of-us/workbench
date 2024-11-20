import '@testing-library/jest-dom';

import * as React from 'react';
import { MemoryRouter } from 'react-router-dom';

import { Profile, ProfileApi } from 'generated/fetch';

import { workspaceDataStub } from '../../testing/stubs/workspaces';
import { InvalidBillingBanner } from '../pages/workspace/invalid-billing-banner';
import { plusDays } from '../utils/dates';
import { currentWorkspaceStore } from '../utils/navigation';
import { screen } from '@testing-library/dom';
import { render } from '@testing-library/react';
import {
  DataUserCodeOfConduct,
  DuccSignatureState,
} from 'app/components/data-user-code-of-conduct';
import {
  profileApi,
  registerApiClient,
} from 'app/services/swagger-fetch-clients';
import { profileStore, serverConfigStore } from 'app/utils/stores';

import defaultServerConfig from 'testing/default-server-config';
import {
  ProfileApiStub,
  ProfileStubVariables,
} from 'testing/stubs/profile-api-stub';

describe('InvalidBillingBanner', () => {
  const load = jest.fn();
  const reload = jest.fn();
  const updateCache = jest.fn();
  const warningThresholdDays = 5; // arbitrary
  const me = ProfileStubVariables.PROFILE_STUB.username;
  const someOneElse = 'not-owner@fake-research-aou.org';

  const component = (
    signatureState: DuccSignatureState = DuccSignatureState.UNSIGNED
  ) =>
    render(
      <MemoryRouter>
        <InvalidBillingBanner
          {...{ signatureState }}
          hideSpinner={() => {}}
          showSpinner={() => {}}
        />
      </MemoryRouter>
    );

  beforeEach(() => {
    registerApiClient(ProfileApi, new ProfileApiStub());

    // Do I need this?
    reload.mockImplementation(async () => {
      const newProfile = await profileApi().getMe();
      profileStore.set({ profile: newProfile, load, reload, updateCache });
    });

    profileStore.set({
      profile: ProfileStubVariables.PROFILE_STUB,
      load,
      reload,
      updateCache,
    });
    serverConfigStore.set({
      config: {
        ...defaultServerConfig,
        initialCreditsExpirationWarningDays: warningThresholdDays,
      },
    });
  });

  const setupWorkspace = (
    exhausted: boolean,
    expired: boolean,
    expiringSoon: boolean,
    ownedByMe: boolean
  ) => {
    // Set expiration date to be in the past if expired, in the future if not.
    // If expiring soon, set it to be within the warning threshold, and just outside otherwise.
    // Expired and expiringSoon are mutually exclusive.
    const daysUntilExpiration = expired
      ? -1
      : warningThresholdDays + (expiringSoon ? -1 : 1);
    currentWorkspaceStore.next({
      ...workspaceDataStub,
      initialCredits: {
        exhausted,
        expired,
        expirationEpochMillis: plusDays(Date.now(), daysUntilExpiration),
      },
      creator: ownedByMe ? me : someOneElse,
    });
  };

  const setProfileExtensionEligibility = (isEligible: boolean) => {
    profileStore.set({
      profile: {
        ...ProfileStubVariables.PROFILE_STUB,
        eligibleForInitialCreditsExtension: isEligible,
      },
      load,
      reload,
      updateCache,
    });
  };

  it('should show expiring soon banner to user who created the workspace', async () => {
    setupWorkspace(false, false, true, true);
    setProfileExtensionEligibility(true);

    component();

    await screen.findByText('Workspace credits are expiring soon');
    expect(getBannerText()).toMatch(
      'Your initial credits are expiring soon. You can request an extension here. For more ' +
        'information, read the Using All of Us Initial Credits article on the User Support Hub.'
    );
  });

  it('should show expiring soon banner to user who did not create the workspace', async () => {
    setupWorkspace(false, false, true, false);
    setProfileExtensionEligibility(true);

    component();

    await screen.findByText('Workspace credits are expiring soon');
    expect(getBannerText()).toMatch(
      'This workspace creator’s initial credits are expiring soon. This workspace was ' +
        'created by First Name Last Name.You can request an extension here. For more information, ' +
        'read the Using All of Us Initial Credits article on the User Support Hub.'
    );
  });

  it('should show expired banner with option to extend to eligible user who created the workspace', async () => {
    setupWorkspace(false, true, false, true);
    setProfileExtensionEligibility(true);

    component();

    await screen.findByText('Workspace credits have expired');
    expect(getBannerText()).toMatch(
      'Your initial credits have expired. You can request an extension here. For more ' +
        'information, read the Using All of Us Initial Credits article on the User Support Hub.'
    );
  });

  it('should show expired banner to user who did not create the workspace but the owner is eligible for extension', async () => {
    setupWorkspace(false, true, false, false);
    setProfileExtensionEligibility(true);

    component();

    await screen.findByText('Workspace credits have expired');
    expect(getBannerText()).toMatch(
      'This workspace creator’s initial credits have expired. This workspace was created by ' +
        'First Name Last Name.You can request an extension here. For more information, read the ' +
        'Using All of Us Initial Credits article on the User Support Hub.'
    );
  });

  // it('should display the Content and Signature pages in SIGNED mode if the user is up to date', async () => {
  //   const { queryByTestId } = component(DuccSignatureState.SIGNED);
  //   expect(queryByTestId('ducc-content-page')).toBeInTheDocument();
  //   expect(queryByTestId('ducc-signature-page')).toBeInTheDocument();
  // });

  /* All banners have "initial credits" in the text. Banner text can have one or more links in it.
   * React Testing Library has a hard time finding text that is split across multiple elements
   * (like text and links), so we can't use getByText to find the banner text. Instead, this
   * function will return the textContent of the element that contains the banner text. This will
   * include the plain text and the text found in the links.
   */

  const getBannerText = () => screen.getByText(/initial credits/).textContent;
});
