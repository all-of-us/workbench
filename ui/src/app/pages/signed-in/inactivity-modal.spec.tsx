import * as React from 'react';

import { screen } from '@testing-library/dom';
import { render } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import {
  InactivityModal,
  InactivityModalProps,
} from 'app/pages/signed-in/inactivity-modal';

const createProps = (): InactivityModalProps => ({
  currentTimeMs: 0,
  signOutForInactivityTimeMs: 0,
  inactivityWarningBeforeMs: 0,
  closeFunction: () => {},
});

describe(InactivityModal.name, () => {
  const setup = (props = createProps()) => {
    render(
      <div id='popup-root'>
        <InactivityModal {...props} />)
      </div>
    );
    return userEvent.setup();
  };

  const queryForModal = () => {
    return screen.queryByText(/your session is about to expire/i);
  };

  it('should show the modal if there is less time until sign out than inactivityWarningBeforeMs', async () => {
    setup({
      ...createProps(),
      currentTimeMs: 901,
      signOutForInactivityTimeMs: 1000,
      inactivityWarningBeforeMs: 100,
    });

    expect(queryForModal()).not.toBeNull();
  });

  it('should not show the modal if there is more time until sign out than inactivityWarningBeforeMs', async () => {
    setup({
      ...createProps(),
      currentTimeMs: 899,
      signOutForInactivityTimeMs: 1000,
      inactivityWarningBeforeMs: 100,
    });

    expect(queryForModal()).toBeNull();
  });
});
