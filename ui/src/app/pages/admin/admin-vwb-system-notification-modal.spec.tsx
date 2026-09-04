import '@testing-library/jest-dom';

import * as React from 'react';

import {
  VwbSystemNotification,
  VwbSystemNotificationPriority,
  VwbSystemNotificationType,
} from 'generated/fetch';

import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

import {
  expectButtonElementDisabled,
  expectButtonElementEnabled,
  renderModal,
} from 'testing/react-test-helpers';

import {
  AdminVwbSystemNotificationModal,
  MAX_VWB_NOTIFICATION_MESSAGE,
  MAX_VWB_NOTIFICATION_TITLE,
  validateVwbSystemNotification,
} from './admin-vwb-system-notification-modal';

const validNotification = (): VwbSystemNotification => ({
  title: 'Scheduled maintenance',
  message: 'Verily Workbench will be unavailable on Saturday.',
  notificationType: VwbSystemNotificationType.PASSIVE,
  notificationPriority: VwbSystemNotificationPriority.INFO,
  startTimeEpochMillis: Date.now(),
});

const getCreateButton = () =>
  screen.getByRole('button', { name: 'Create Banner' });

describe('AdminVwbSystemNotificationModal', () => {
  const mockSetNotification = jest.fn();
  const mockOnClose = jest.fn();
  const mockOnCreate = jest.fn();

  const component = (notification: VwbSystemNotification) =>
    renderModal(
      <AdminVwbSystemNotificationModal
        notification={notification}
        setNotification={mockSetNotification}
        onClose={mockOnClose}
        onCreate={mockOnCreate}
      />
    );

  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('should tell the admin the audience is All of Us only', () => {
    component(validNotification());

    // Admins need to know this is not a system-wide Verily Workbench banner.
    expect(
      screen.getByText(/Audience: All of Us customers only/i)
    ).toBeInTheDocument();
    expect(
      screen.getByText(/not a system-wide Verily Workbench banner/i)
    ).toBeInTheDocument();
    // The notification is listed in the table and deleting it there removes it from VWB.
    expect(
      screen.getByText(
        /deleting it there removes it from Verily Workbench too/i
      )
    ).toBeInTheDocument();
  });

  it('should create a banner when title and message are provided', async () => {
    const user = userEvent.setup();
    component(validNotification());

    const createButton = getCreateButton();
    expectButtonElementEnabled(createButton);

    await user.click(createButton);
    expect(mockOnCreate).toHaveBeenCalled();
  });

  it('should disable create when the title is blank', () => {
    component({ ...validNotification(), title: '   ' });

    expectButtonElementDisabled(getCreateButton());
    expect(mockOnCreate).not.toHaveBeenCalled();
  });

  it('should disable create when the message is blank', () => {
    component({ ...validNotification(), message: '' });

    expectButtonElementDisabled(getCreateButton());
    expect(mockOnCreate).not.toHaveBeenCalled();
  });

  describe(validateVwbSystemNotification.name, () => {
    it('should accept a fully populated banner', () => {
      expect(validateVwbSystemNotification(validNotification())).toEqual([]);
    });

    it('should require a title and a message', () => {
      expect(
        validateVwbSystemNotification({
          ...validNotification(),
          title: '',
          message: '',
        })
      ).toEqual(['Title is required', 'Message is required']);
    });

    it('should reject an over-long title', () => {
      const errors = validateVwbSystemNotification({
        ...validNotification(),
        title: 'a'.repeat(MAX_VWB_NOTIFICATION_TITLE + 1),
      });
      expect(errors).toEqual([
        `Title must be ${MAX_VWB_NOTIFICATION_TITLE} characters or fewer`,
      ]);
    });

    it('should reject an over-long message', () => {
      const errors = validateVwbSystemNotification({
        ...validNotification(),
        message: 'a'.repeat(MAX_VWB_NOTIFICATION_MESSAGE + 1),
      });
      expect(errors).toEqual([
        `Message must be ${MAX_VWB_NOTIFICATION_MESSAGE} characters or fewer`,
      ]);
    });

    it('should reject an end time at or before the start time', () => {
      const start = Date.now();
      expect(
        validateVwbSystemNotification({
          ...validNotification(),
          startTimeEpochMillis: start,
          endTimeEpochMillis: start,
        })
      ).toEqual(['End time must be after start time']);
    });

    it('should accept a missing end time, which means never expires', () => {
      expect(
        validateVwbSystemNotification({
          ...validNotification(),
          endTimeEpochMillis: null,
        })
      ).toEqual([]);
    });
  });
});
