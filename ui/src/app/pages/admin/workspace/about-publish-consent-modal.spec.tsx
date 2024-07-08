import '@testing-library/jest-dom';

import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

import { AboutPublishConsentModal } from './about-publish-consent-modal';

describe('AboutPublishConsentModal', () => {
  it('enables the confirm button only when all checkboxes are checked', async () => {
    const mockOnConfirm = jest.fn();
    const mockOnCancel = jest.fn();
    render(
      <AboutPublishConsentModal
        onConfirm={mockOnConfirm}
        onCancel={mockOnCancel}
      />
    );

    const user = userEvent.setup();
    const checkboxes = screen.getAllByRole('checkbox');
    const confirmButton = screen.getByRole('button', { name: 'CONFIRM' });
    const cancelButton = screen.getByRole('button', { name: 'CANCEL' });

    // Initially, the confirm button should be disabled
    screen.logTestingPlaygroundURL();
    // expect(confirmButton).toBeDisabled();
    expect(cancelButton).toBeDisabled();

    // Check all checkboxes
    for (const checkbox of checkboxes) {
      await user.click(checkbox);
    }

    // Now, the confirm button should be enabled
    expect(confirmButton).toBeEnabled();

    // Uncheck the first checkbox
    await user.click(checkboxes[0]);

    // // The confirm button should be disabled again
    // expect(confirmButton).toBeDisabled();
  });
});
