import '@testing-library/jest-dom';

import * as React from 'react';
import { MemoryRouter } from 'react-router-dom';

import { ProfileApi, User } from 'generated/fetch';

import { screen } from '@testing-library/dom';
import { render } from '@testing-library/react';
import { InvalidBillingBanner } from 'app/pages/workspace/invalid-billing-banner';
import { registerApiClient } from 'app/services/swagger-fetch-clients';
import { plusDays } from 'app/utils/dates';
import { currentWorkspaceStore } from 'app/utils/navigation';
import { serverConfigStore } from 'app/utils/stores';

import defaultServerConfig from 'testing/default-server-config';
import {
  ProfileApiStub,
  ProfileStubVariables,
} from 'testing/stubs/profile-api-stub';
import { workspaceDataStub } from 'testing/stubs/workspaces';

// Mock ReactDOM.createPortal since ToastBanner uses portals
jest.mock('react-dom', () => {
  return {
    ...jest.requireActual('react-dom'),
    createPortal: (element) => element,
  };
});

describe('InvalidBillingBanner', () => {
  const warningThresholdDays = 5; // arbitrary
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

  const component = () => {
    const workspace = currentWorkspaceStore.getValue();
    const rendered = render(
      <MemoryRouter>
        <InvalidBillingBanner
          onClose={jest.fn()}
          workspace={workspace}
          profile={{
            ...ProfileStubVariables.PROFILE_STUB,
            ...me,
          }}
        />
      </MemoryRouter>
    );
    return rendered;
  };

  const expectEditWorkspaceButtonExists = () =>
    screen.getAllByRole('button', { name: /edit workspace/i });

  const expectEditWorkspaceButtonDoesNotExist = () =>
    expect(
      screen.queryByRole('button', { name: /edit workspace/i })
    ).not.toBeInTheDocument();

  beforeAll(() => {
    jest.useFakeTimers().setSystemTime(new Date('December 13, 2016 14:54:00'));
  });

  afterAll(() => {
    jest.useRealTimers();
  });

  beforeEach(() => {
    registerApiClient(ProfileApi, new ProfileApiStub());

    serverConfigStore.set({
      config: {
        ...defaultServerConfig,
        initialCreditsExpirationWarningDays: warningThresholdDays,
      },
    });
  });

  const setupWorkspace = (
    expired: boolean,
    exhausted: boolean,
    ownedByMe: boolean
  ) => {
    const workspace = {
      ...workspaceDataStub,
      namespace: 'test-namespace',
      terraName: 'test-workspace',
      initialCredits: {
        exhausted,
        expirationEpochMillis: plusDays(Date.now(), expired ? -1 : 1),
        expirationBypassed: false,
      },
      creatorUser: ownedByMe ? me : someOneElse,
    };
    currentWorkspaceStore.next(workspace);
  };

  /* All banners have "initial credits" in the text. Banner text can have one or more links in it.
   * React Testing Library has a hard time finding text that is split across multiple elements
   * (like text and links), so we can't use getByText to find the banner text. Instead, this
   * function will return the textContent of the element that contains the banner text. This will
   * include the plain text and the text found in the links.
   */

  it('should show expired banner to expired user who created the workspace', async () => {
    const expired = true;
    const exhausted = false;
    const ownedByMe = true;
    setupWorkspace(expired, exhausted, ownedByMe);

    component();

    await screen.findByText('This workspace is out of initial credits');
    screen.getByText(
      /your initial credits have run out\. to use the workspace, a valid billing account needs to be added \. to learn more about establishing a billing account, read "" on the user support hub\./i
    );
    expectEditWorkspaceButtonExists();
  });

  it('should show expired banner to non-creator when creator is expired', async () => {
    const expired = true;
    const exhausted = false;
    const ownedByMe = false;
    setupWorkspace(expired, exhausted, ownedByMe);

    component();

    await screen.findByText('This workspace is out of initial credits');
    screen.getByText(
      /this workspace creator's initial credits have run out\. this workspace was created by someone else\. to use the workspace, a valid billing account needs to be added\. to learn more about establishing a billing account, read "" on the user support hub\./i
    );
    expectEditWorkspaceButtonDoesNotExist();
  });

  it('should show expired banner to exhausted user who created the workspace', async () => {
    const expired = false;
    const exhausted = true;
    const ownedByMe = true;
    setupWorkspace(expired, exhausted, ownedByMe);

    component();

    await screen.findByText('This workspace is out of initial credits');
    screen.logTestingPlaygroundURL();
    screen.getByText(
      /your initial credits have run out\. to use the workspace, a valid billing account needs to be added \. to learn more about establishing a billing account, read "" on the user support hub\./i
    );
    expectEditWorkspaceButtonExists();
  });

  it('should show expired banner to non-creator when creator is exhausted', async () => {
    const expired = false;
    const exhausted = true;
    const ownedByMe = false;
    setupWorkspace(expired, exhausted, ownedByMe);

    component();

    await screen.findByText('This workspace is out of initial credits');
    screen.getByText(
      /this workspace creator's initial credits have run out\. this workspace was created by someone else\. to use the workspace, a valid billing account needs to be added\. to learn more about establishing a billing account, read "" on the user support hub\./i
    );
    expectEditWorkspaceButtonDoesNotExist();
  });
});
