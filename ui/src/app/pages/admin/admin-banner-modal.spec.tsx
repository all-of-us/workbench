import { StatusAlert, StatusAlertLocation } from 'generated/fetch';

import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MILLIS_PER_DAY, MILLIS_PER_HOUR } from 'app/utils/dates';

import {
  expectButtonElementDisabled,
  expectTooltip,
  renderModal,
} from 'testing/react-test-helpers';

import { AdminBannerModal } from './admin-banner-modal';
import { validateBannerAlert } from './admin-banner-modal-validation';

describe('AdminBannerModal', () => {
  const defaultBanner: StatusAlert = {
    title: '',
    message: '',
    link: '',
    alertLocation: null,
    startTimeEpochMillis: Date.now(),
  };

  const mockSetBanner = jest.fn();
  const mockOnClose = jest.fn();
  const mockOnCreate = jest.fn();

  const defaultProps = {
    banner: defaultBanner,
    setBanner: mockSetBanner,
    onClose: mockOnClose,
    onCreate: mockOnCreate,
  };

  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('should render correctly', () => {
    renderModal(<AdminBannerModal {...defaultProps} />);

    expect(screen.getByText('Create New Banner')).toBeInTheDocument();
    expect(screen.getByText('Title')).toBeInTheDocument();
    expect(screen.getByText('Message')).toBeInTheDocument();
    expect(screen.getByText('Link (Optional)')).toBeInTheDocument();
    expect(screen.getByText('Location')).toBeInTheDocument();
    expect(screen.getByText('Cancel')).toBeInTheDocument();
    expect(screen.getByText('Create Banner')).toBeInTheDocument();
  });

  it('should disable the Create Banner button when required fields are empty', async () => {
    renderModal(<AdminBannerModal {...defaultProps} />);

    const createButton = await screen.findByText('Create Banner');
    expectButtonElementDisabled(createButton);
  });

  it('should enable the Create Banner button when all required fields are filled', () => {
    const filledBanner: StatusAlert = {
      title: 'Test Title',
      message: 'Test Message',
      link: '',
      alertLocation: StatusAlertLocation.AFTER_LOGIN,
      startTimeEpochMillis: Date.now(),
    };

    renderModal(<AdminBannerModal {...defaultProps} banner={filledBanner} />);

    const createButton = screen.getByText('Create Banner');
    expect(createButton).not.toBeDisabled();
  });

  it('should call onClose when Cancel button is clicked', async () => {
    const user = userEvent.setup({ delay: 100 });
    renderModal(<AdminBannerModal {...defaultProps} />);

    await user.click(screen.getByText('Cancel'));
    expect(mockOnClose).toHaveBeenCalledTimes(1);
  });

  it('should call onCreate when Create Banner button is clicked and form is valid', async () => {
    const user = userEvent.setup();
    const filledBanner: StatusAlert = {
      title: 'Test Title',
      message: 'Test Message',
      link: '',
      alertLocation: StatusAlertLocation.AFTER_LOGIN,
      startTimeEpochMillis: Date.now(),
    };

    renderModal(<AdminBannerModal {...defaultProps} banner={filledBanner} />);

    await user.click(screen.getByText('Create Banner'));
    expect(mockOnCreate).toHaveBeenCalledTimes(1);
  });

  it('should disable title input when BEFORE_LOGIN location is selected', async () => {
    const user = userEvent.setup();
    const beforeLoginBanner: StatusAlert = {
      title: 'Scheduled Downtime Notice for the Researcher Workbench',
      message: '',
      link: '',
      alertLocation: StatusAlertLocation.BEFORE_LOGIN,
      startTimeEpochMillis: Date.now(),
    };

    renderModal(
      <AdminBannerModal {...defaultProps} banner={beforeLoginBanner} />
    );

    const titleInput = await screen.findByPlaceholderText('Enter banner title');
    expect(titleInput).toBeDisabled();

    await user.hover(titleInput);
    await expectTooltip(
      titleInput,
      '"Before Login" banner has a fixed headline.',
      user
    );
  });

  it('should set fixed title when changing location to BEFORE_LOGIN', async () => {
    const user = userEvent.setup();
    const filledBanner: StatusAlert = {
      title: 'Test Title',
      message: 'Test Message',
      link: '',
      alertLocation: StatusAlertLocation.AFTER_LOGIN,
      startTimeEpochMillis: Date.now(),
    };
    renderModal(<AdminBannerModal {...defaultProps} banner={filledBanner} />);

    const locationDropdown = await screen.findByText('After Login');
    await user.click(locationDropdown);

    const beforeLoginOption = screen.getByText('Before Login');
    await user.click(beforeLoginOption);

    expect(mockSetBanner).toHaveBeenCalledWith(
      expect.objectContaining({
        title: 'Scheduled Downtime Notice for the Researcher Workbench',
        alertLocation: StatusAlertLocation.BEFORE_LOGIN,
      })
    );
  });

  it('should set empty title when changing location to AFTER_LOGIN', async () => {
    const user = userEvent.setup();
    const beforeLoginBanner: StatusAlert = {
      title: 'Scheduled Downtime Notice for the Researcher Workbench',
      message: '',
      link: '',
      alertLocation: StatusAlertLocation.BEFORE_LOGIN,
      startTimeEpochMillis: Date.now(),
    };

    renderModal(
      <AdminBannerModal {...defaultProps} banner={beforeLoginBanner} />
    );

    const locationDropdown = await screen.findByText('Before Login');
    await user.click(locationDropdown);

    const afterLoginOption = screen.getByText('After Login');
    await user.click(afterLoginOption);

    expect(mockSetBanner).toHaveBeenCalledWith(
      expect.objectContaining({
        title: '',
        alertLocation: StatusAlertLocation.AFTER_LOGIN,
      })
    );
  });

  it('should reset end time when start time is set to a time after the existing end time', async () => {
    const user = userEvent.setup();
    const startTime = new Date('2023-01-01T10:00:00.000Z');
    const endTime = new Date(startTime.getTime() + MILLIS_PER_HOUR);

    const bannerWithDates: StatusAlert = {
      title: 'Test Title',
      message: 'Test Message',
      link: '',
      alertLocation: StatusAlertLocation.AFTER_LOGIN,
      startTimeEpochMillis: null,
      endTimeEpochMillis: endTime.getTime(),
    };

    renderModal(
      <AdminBannerModal {...defaultProps} banner={bannerWithDates} />
    );

    // Find the start time input and set it to a time after the end time
    const startTimeInput = screen.getByLabelText('Start Time (Local)');

    const newStartTime = new Date(endTime.getTime() + MILLIS_PER_DAY);
    await user.click(startTimeInput);
    await user.paste(newStartTime.toISOString().slice(0, 16));

    // Verify end time was reset
    await waitFor(() => {
      expect(mockSetBanner).toHaveBeenCalledWith(
        expect.objectContaining({
          endTimeEpochMillis: null,
        })
      );
    });
  });
});

describe('AdminBannerModal - form validation', () => {
  it('returns no errors for completed form', () => {
    // Arrange
    const filledBanner: StatusAlert = {
      title: 'Test Title',
      message: 'Test Message',
      link: '',
      alertLocation: StatusAlertLocation.AFTER_LOGIN,
      startTimeEpochMillis: Date.now(),
    };

    // Act
    const errors = validateBannerAlert(filledBanner);

    // Assert
    const expectedErrors: Record<string, string[]> | undefined = undefined;
    expect(errors).toEqual(expectedErrors);
  });

  it('returns errors empty form', () => {
    // Arrange
    const filledBanner: StatusAlert = {} as Partial<StatusAlert> as StatusAlert;

    // Act
    const errors = validateBannerAlert(filledBanner);
    // Assert
    const expectedErrors: Record<string, string[]> = {
      title: ['Please enter a banner title'],
      message: ['Please enter a banner message'],
      startTimeEpochMillis: ['Please enter a start time'],
      alertLocation: ['Please select a banner location'],
    };
    expect(errors).toEqual(expectedErrors);
  });
});
