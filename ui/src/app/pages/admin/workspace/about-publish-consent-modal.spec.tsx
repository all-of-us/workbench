import '@testing-library/jest-dom';

import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

import {
  expectButtonElementDisabled,
  expectButtonElementEnabled,
} from 'testing/react-test-helpers';

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

    const confirmButton = screen.getByRole('button', { name: /confirm/i });
    const cancelButton = screen.getByRole('button', { name: 'CANCEL' });

    // Initially, the confirm button should be disabled
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
});
