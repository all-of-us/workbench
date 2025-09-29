import '@testing-library/jest-dom';

import * as React from 'react';
import { MemoryRouter } from 'react-router-dom';

import { ProfileApi, User } from 'generated/fetch';

import { fireEvent, render, screen } from '@testing-library/react';
import { registerApiClient } from 'app/services/swagger-fetch-clients';

import {
  ProfileApiStub,
  ProfileStubVariables,
} from 'testing/stubs/profile-api-stub';
import { workspaceDataStub } from 'testing/stubs/workspaces';

import { BannerScenario } from './banner-config';
import { CreditBanner } from './credit-banner';

const workspace = {
  ...workspaceDataStub,
  namespace: 'test-namespace',
  terraName: 'test-workspace',
  creatorUser: {
    userName: ProfileStubVariables.PROFILE_STUB.username,
    givenName: 'Jane',
    familyName: 'Doe',
  },
};

const me: User = {
  userName: ProfileStubVariables.PROFILE_STUB.username,
  givenName: 'Me',
  familyName: 'Myself',
};
const someOneElse: User = {
  userName: 'someOneElse@fake-research-aou.org',
  givenName: 'Someone',
  familyName: 'Else',
};

jest.mock('react-dom', () => ({
  ...jest.requireActual('react-dom'),
  createPortal: (element: any) => element,
}));

describe('CreditBanner', () => {
  beforeEach(() => {
    registerApiClient(ProfileApi, new ProfileApiStub());
  });

  it('shows Edit Workspace button for creator', () => {
    render(
      <MemoryRouter>
        <CreditBanner
          banners={[
            {
              scenario: BannerScenario.ExpiringSoon,
              expirationDate: '2025-01-01',
              workspace: workspace,
              profile: {
                ...ProfileStubVariables.PROFILE_STUB,
                ...me,
              },
              onClose: jest.fn(),
            },
          ]}
        />
      </MemoryRouter>
    );
    expect(
      screen.getByRole('button', { name: /edit workspace/i })
    ).toBeInTheDocument();
  });

  it('shows link button for non-creator', () => {
    render(
      <MemoryRouter>
        <CreditBanner
          banners={[
            {
              scenario: BannerScenario.Expired,
              creatorName: 'Jane Doe',
              workspace: workspace,
              profile: {
                ...ProfileStubVariables.PROFILE_STUB,
                ...someOneElse,
              },
              onClose: jest.fn(),
            },
          ]}
        />
      </MemoryRouter>
    );
    expect(
      screen.getByRole('button', { name: /link to ush page/i })
    ).toBeInTheDocument();
  });

  it('calls onClose when Edit Workspace is clicked', () => {
    const onClose = jest.fn();
    render(
      <MemoryRouter>
        <CreditBanner
          banners={[
            {
              scenario: BannerScenario.ExpiringSoon,
              expirationDate: '2025-01-01',
              workspace: workspace,
              profile: {
                ...ProfileStubVariables.PROFILE_STUB,
                ...me,
              },
              onClose: onClose,
            },
          ]}
        />
      </MemoryRouter>
    );
    fireEvent.click(screen.getByRole('button', { name: /edit workspace/i }));
    expect(onClose).toHaveBeenCalled();
  });
});
