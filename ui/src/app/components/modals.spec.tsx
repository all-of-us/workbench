import '@testing-library/jest-dom';

import {
  render,
  screen,
  waitFor,
  waitForElementToBeRemoved,
} from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { NotificationModal } from 'app/components/modals';
import { notificationStore } from 'app/utils/stores';

describe('NotificationModal', () => {
  const component = () => {
    return render(
      <div>
        <NotificationModal />
      </div>
    );
  };

  beforeEach(async () => {
    notificationStore.set(null);
  });

  it('should appear and disappear based on store changes', async () => {
    component();
    const meta = { title: 'Hello', message: 'World' };
    // Notification modal should not render
    expect(screen.queryByText(meta.title)).not.toBeInTheDocument();
    notificationStore.set(meta);

    // When the store has data it should render
    await waitFor(() => {
      expect(screen.getByText(meta.title)).toBeInTheDocument();
      expect(screen.getByText(meta.message)).toBeInTheDocument();
    });

    // Click button to dismiss - modal should not render
    userEvent.click(screen.getByRole('button'));

    // Modal should be gone
    await waitForElementToBeRemoved(() => screen.queryByText(meta.title));
  });
});
