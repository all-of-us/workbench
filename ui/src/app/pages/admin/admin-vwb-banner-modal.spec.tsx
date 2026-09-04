import '@testing-library/jest-dom';

import * as React from 'react';

import { VwbBanner, VwbBannerPriority, VwbBannerType } from 'generated/fetch';

import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

import {
  expectButtonElementDisabled,
  expectButtonElementEnabled,
  renderModal,
} from 'testing/react-test-helpers';

import {
  AdminVwbBannerModal,
  MAX_VWB_BANNER_MESSAGE,
  MAX_VWB_BANNER_TITLE,
  validateVwbBanner,
} from './admin-vwb-banner-modal';

const validBanner = (): VwbBanner => ({
  title: 'Scheduled maintenance',
  message: 'Verily Workbench will be unavailable on Saturday.',
  notificationType: VwbBannerType.PASSIVE,
  notificationPriority: VwbBannerPriority.INFO,
  startTimeEpochMillis: Date.now(),
});

const getCreateButton = () =>
  screen.getByRole('button', { name: 'Create Banner' });

describe('AdminVwbBannerModal', () => {
  const mockSetBanner = jest.fn();
  const mockOnClose = jest.fn();
  const mockOnCreate = jest.fn();

  const component = (banner: VwbBanner) =>
    renderModal(
      <AdminVwbBannerModal
        banner={banner}
        setBanner={mockSetBanner}
        onClose={mockOnClose}
        onCreate={mockOnCreate}
      />
    );

  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('should tell the admin the audience is All of Us only', () => {
    component(validBanner());

    // Admins need to know this is not a system-wide Verily Workbench banner.
    expect(
      screen.getByText(/Audience: All of Us customers only/i)
    ).toBeInTheDocument();
    expect(
      screen.getByText(/not a system-wide Verily Workbench banner/i)
    ).toBeInTheDocument();
  });

  it('should create a banner when title and message are provided', async () => {
    const user = userEvent.setup();
    component(validBanner());

    const createButton = getCreateButton();
    expectButtonElementEnabled(createButton);

    await user.click(createButton);
    expect(mockOnCreate).toHaveBeenCalled();
  });

  it('should disable create when the title is blank', () => {
    component({ ...validBanner(), title: '   ' });

    expectButtonElementDisabled(getCreateButton());
    expect(mockOnCreate).not.toHaveBeenCalled();
  });

  it('should disable create when the message is blank', () => {
    component({ ...validBanner(), message: '' });

    expectButtonElementDisabled(getCreateButton());
    expect(mockOnCreate).not.toHaveBeenCalled();
  });

  describe(validateVwbBanner.name, () => {
    it('should accept a fully populated banner', () => {
      expect(validateVwbBanner(validBanner())).toEqual([]);
    });

    it('should require a title and a message', () => {
      expect(
        validateVwbBanner({ ...validBanner(), title: '', message: '' })
      ).toEqual(['Title is required', 'Message is required']);
    });

    it('should reject an over-long title', () => {
      const errors = validateVwbBanner({
        ...validBanner(),
        title: 'a'.repeat(MAX_VWB_BANNER_TITLE + 1),
      });
      expect(errors).toEqual([
        `Title must be ${MAX_VWB_BANNER_TITLE} characters or fewer`,
      ]);
    });

    it('should reject an over-long message', () => {
      const errors = validateVwbBanner({
        ...validBanner(),
        message: 'a'.repeat(MAX_VWB_BANNER_MESSAGE + 1),
      });
      expect(errors).toEqual([
        `Message must be ${MAX_VWB_BANNER_MESSAGE} characters or fewer`,
      ]);
    });

    it('should reject an end time at or before the start time', () => {
      const start = Date.now();
      expect(
        validateVwbBanner({
          ...validBanner(),
          startTimeEpochMillis: start,
          endTimeEpochMillis: start,
        })
      ).toEqual(['End time must be after start time']);
    });

    it('should accept a missing end time, which means never expires', () => {
      expect(
        validateVwbBanner({ ...validBanner(), endTimeEpochMillis: null })
      ).toEqual([]);
    });
  });
});
