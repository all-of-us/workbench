import '@testing-library/jest-dom';

import * as React from 'react';

import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { UserEvent } from '@testing-library/user-event/setup/setup';

import { MultiToastBanner } from './multi-toast-banner';
import { MultiToastMessage } from './multi-toast-message.model';
import { ToastType } from './toast-banner';

// Mock ReactDOM.createPortal since ToastBanner uses portals
jest.mock('react-dom', () => {
  return {
    ...jest.requireActual('react-dom'),
    createPortal: (element) => element,
  };
});

describe('MultiToastBanner', () => {
  // Setup sample messages for testing
  const sampleMessages: MultiToastMessage[] = [
    {
      id: 'message1',
      title: 'Test Message 1',
      message: 'This is test message 1',
      toastType: ToastType.INFO,
    },
    {
      id: 'message2',
      title: 'Test Message 2',
      message: 'This is test message 2',
      toastType: ToastType.WARNING,
    },
    {
      id: 'message3',
      title: 'Test Message 3',
      message: 'This is test message 3',
      toastType: ToastType.INFO,
      footer: <button data-testid='footer-button'>Action</button>,
    },
  ];

  const onDismissMock = jest.fn();
  let user: UserEvent;

  beforeEach(() => {
    // Reset mocks before each test
    jest.clearAllMocks();
    user = userEvent.setup();
  });

  it('renders nothing when messages array is empty', () => {
    const { container } = render(
      <MultiToastBanner messages={[]} onDismiss={onDismissMock} />
    );
    expect(container).toBeEmptyDOMElement();
  });

  it('renders the first message in the array by default', () => {
    render(
      <MultiToastBanner messages={sampleMessages} onDismiss={onDismissMock} />
    );

    expect(screen.getByText('Test Message 1')).toBeInTheDocument();
    expect(screen.getByText('This is test message 1')).toBeInTheDocument();
  });

  it('shows pagination controls when multiple messages exist', () => {
    render(
      <MultiToastBanner messages={sampleMessages} onDismiss={onDismissMock} />
    );

    expect(screen.getByText('1 of 3')).toBeInTheDocument();
    expect(screen.getByTitle('Previous message')).toBeInTheDocument();
    expect(screen.getByTitle('Next message')).toBeInTheDocument();
  });

  it('does not show pagination controls when only one message exists', () => {
    render(
      <MultiToastBanner
        messages={[sampleMessages[0]]}
        onDismiss={onDismissMock}
      />
    );

    expect(screen.queryByText('1 of 1')).not.toBeInTheDocument();
    expect(screen.queryByTitle('Previous message')).not.toBeInTheDocument();
    expect(screen.queryByTitle('Next message')).not.toBeInTheDocument();
  });

  it('navigates to the next message when clicking the right arrow', async () => {
    render(
      <MultiToastBanner messages={sampleMessages} onDismiss={onDismissMock} />
    );

    // Initially showing first message
    expect(screen.getByText('Test Message 1')).toBeInTheDocument();

    // Click the right arrow
    await user.click(screen.getByTitle('Next message'));

    // Should now show the second message
    expect(screen.getByText('Test Message 2')).toBeInTheDocument();
    expect(screen.getByText('This is test message 2')).toBeInTheDocument();
    expect(screen.getByText('2 of 3')).toBeInTheDocument();
  });

  it('navigates to the previous message when clicking the left arrow', async () => {
    render(
      <MultiToastBanner messages={sampleMessages} onDismiss={onDismissMock} />
    );

    // Navigate to the second message first
    await user.click(screen.getByTitle('Next message'));
    expect(screen.getByText('Test Message 2')).toBeInTheDocument();

    // Click the left arrow to go back to the first message
    await user.click(screen.getByTitle('Previous message'));

    // Should now show the first message again
    expect(screen.getByText('Test Message 1')).toBeInTheDocument();
    expect(screen.getByText('This is test message 1')).toBeInTheDocument();
    expect(screen.getByText('1 of 3')).toBeInTheDocument();
  });

  it('calls onDismiss with message id when close button is clicked', async () => {
    render(
      <MultiToastBanner messages={sampleMessages} onDismiss={onDismissMock} />
    );

    // Click the close icon using the title attribute
    await user.click(screen.getByTitle('Dismiss alert'));

    // Should call onDismiss with the correct message id
    expect(onDismissMock).toHaveBeenCalledWith('message1');
  });
});
