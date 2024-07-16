import '@testing-library/jest-dom';

import { WorkspacesApi } from 'generated/fetch';

import { act, render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {
  registerApiClient,
  workspacesApi,
} from 'app/services/swagger-fetch-clients';

import {
  expectButtonElementDisabled,
  expectButtonElementEnabled,
} from 'testing/react-test-helpers';
import { WorkspacesApiStub } from 'testing/stubs/workspaces-api-stub';

import { AboutPublishConsentModal } from './about-publish-consent-modal';

describe('AboutPublishConsentModal', () => {
  beforeEach(() => {
    registerApiClient(WorkspacesApi, new WorkspacesApiStub());
  });

  it('enables the confirm button only when all checkboxes are checked', async () => {
    const mockOnConfirm = jest.fn();
    const mockOnCancel = jest.fn();
    render(
      <AboutPublishConsentModal
        workspaceNamespace={'namespace'}
        onConfirm={mockOnConfirm}
        onCancel={mockOnCancel}
      />
    );

    const user = userEvent.setup();
    const checkboxes = screen.getAllByRole('checkbox');

    const confirmButton = screen.getByRole('button', { name: /confirm/i });
    const cancelButton = screen.getByRole('button', { name: 'CANCEL' });

    // Initially, the confirm button should be disabled
    expectButtonElementDisabled(confirmButton);
    expectButtonElementDisabled(confirmButton);
    // User can hit cancel to exit the modal
    expectButtonElementEnabled(cancelButton);

    // Check all checkboxes
    for (const checkbox of checkboxes) {
      await user.click(checkbox);
    }

    // Now, the confirm button should be enabled
    expectButtonElementEnabled(confirmButton);

    // Uncheck the first checkbox
    await user.click(checkboxes[0]);

    // The confirm button should be disabled again
    expectButtonElementDisabled(confirmButton);
  });

  it('Verify that an error message box is displayed when the API returns an error', async () => {
    const mockOnConfirm = jest.fn();
    const mockOnCancel = jest.fn();
    render(
      <AboutPublishConsentModal
        workspaceNamespace={'namespace'}
        onConfirm={mockOnConfirm}
        onCancel={mockOnCancel}
      />
    );

    const user = userEvent.setup();
    const checkboxes = screen.getAllByRole('checkbox');

    const confirmButton = screen.getByRole('button', { name: /confirm/i });

    // Check all checkboxes
    for (const checkbox of checkboxes) {
      await user.click(checkbox);
    }

    // Now, the confirm button should be enabled
    expectButtonElementEnabled(confirmButton);

    const message = 'bad request workspace already published';
    jest
      .spyOn(workspacesApi(), 'markWorkspaceAsFeatured')
      .mockRejectedValueOnce(
        new Response(JSON.stringify({ message }), { status: 400 })
      );

    await act(async () => {
      await confirmButton.click();
    });

    console.log(screen.logTestingPlaygroundURL());
    expect(
      screen.getByText(
        /there was an error while publishing the workspace\. please try again later\./i
      )
    ).toBeInTheDocument();
  });

  it('call workspace API on confirm click', async () => {
    const mockOnConfirm = jest.fn();
    const mockOnCancel = jest.fn();
    render(
      <AboutPublishConsentModal
        workspaceNamespace={'namespace'}
        onConfirm={mockOnConfirm}
        onCancel={mockOnCancel}
      />
    );

    const user = userEvent.setup();
    const checkboxes = screen.getAllByRole('checkbox');

    const confirmButton = screen.getByRole('button', { name: /confirm/i });

    // Check all checkboxes
    for (const checkbox of checkboxes) {
      await user.click(checkbox);
    }

    // Now, the confirm button should be enabled
    expectButtonElementEnabled(confirmButton);

    jest.spyOn(workspacesApi(), 'markWorkspaceAsFeatured');

    await act(() => {
      confirmButton.click();
    });

    expectButtonElementDisabled(confirmButton);
    expect(workspacesApi().markWorkspaceAsFeatured).toHaveBeenCalled();
  });
});
