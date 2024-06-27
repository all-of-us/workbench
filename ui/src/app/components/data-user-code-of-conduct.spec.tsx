import '@testing-library/jest-dom';

import * as React from 'react';
import { MemoryRouter } from 'react-router-dom';

import { Profile, ProfileApi } from 'generated/fetch';

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

describe(DataUserCodeOfConduct.name, () => {
  const load = jest.fn();
  const reload = jest.fn();
  const updateCache = jest.fn();

  const updateProfile = (newUpdates: Partial<Profile>) => {
    profileStore.set({
      profile: { ...ProfileStubVariables.PROFILE_STUB, ...newUpdates },
      load,
      reload,
      updateCache,
    });
  };

  const component = (
    signatureState: DuccSignatureState = DuccSignatureState.UNSIGNED
  ) =>
    render(
      <MemoryRouter>
        <DataUserCodeOfConduct
          {...{ signatureState }}
          hideSpinner={() => {}}
          showSpinner={() => {}}
        />
      </MemoryRouter>
    );

  beforeEach(() => {
    registerApiClient(ProfileApi, new ProfileApiStub());
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
    serverConfigStore.set({ config: defaultServerConfig });
  });

  it('should render', () => {
    const { container } = component();
    expect(container).toBeTruthy();
  });

  it('should display the Content and Signature pages in SIGNED mode if the user is up to date', async () => {
    const { queryByTestId } = component(DuccSignatureState.SIGNED);
    expect(queryByTestId('ducc-content-page')).toBeInTheDocument();
    expect(queryByTestId('ducc-signature-page')).toBeInTheDocument();
  });

  it('should not display the Content and Signature pages in SIGNED mode if the user has not signed a DUCC', async () => {
    updateProfile({ duccSignedVersion: undefined });

    const { queryByTestId } = component(DuccSignatureState.SIGNED);
    expect(queryByTestId('ducc-content-page')).not.toBeInTheDocument();
    expect(queryByTestId('ducc-signature-page')).not.toBeInTheDocument();
  });

  it('should not display the Content and Signature pages in SIGNED mode if the user has signed an older DUCC', async () => {
    updateProfile({ duccSignedVersion: 2 });

    const { queryByTestId } = component(DuccSignatureState.SIGNED);
    expect(queryByTestId('ducc-content-page')).not.toBeInTheDocument();
    expect(queryByTestId('ducc-signature-page')).not.toBeInTheDocument();
  });
});
