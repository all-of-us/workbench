import * as React from 'react';
import { useState } from 'react';
import validate from 'validate.js';

import { StatusAlert, StatusAlertLocation } from 'generated/fetch';

import { Button } from 'app/components/buttons';
import { Select, TextInput } from 'app/components/inputs';
import {
  Modal,
  ModalBody,
  ModalFooter,
  ModalTitle,
} from 'app/components/modals';
import { TooltipTrigger } from 'app/components/popups';
import colors from 'app/styles/colors';
import { reactStyles } from 'app/utils';
import {
  convertLocalDateTimeToEpochMillis,
  formatDateTimeLocal,
  MILLIS_PER_YEAR,
} from 'app/utils/dates';

const styles = reactStyles({
  label: {
    color: colors.primary,
  },
  input: {
    color: colors.primary,
  },
  modalField: {
    marginBottom: '1rem',
  },
});

interface AdminBannerModalProps {
  banner: StatusAlert;
  setBanner: React.Dispatch<React.SetStateAction<StatusAlert>>;
  onClose: () => void;
  onCreate: () => void;
}

// Form field component to reduce repetition
interface ModalFieldProps {
  label: string;
  children: React.ReactNode;
  fieldId?: string;
}

const ModalField = ({ label, children, fieldId }: ModalFieldProps) => (
  <div style={styles.modalField}>
    <label style={styles.label} htmlFor={fieldId}>
      {label}
    </label>
    {children}
  </div>
);

const BANNER_VALIDATION_CONSTRAINTS = {
  title: {
    presence: {
      allowEmpty: false,
      message: 'Please enter a banner title',
    },
  },
  message: {
    presence: {
      allowEmpty: false,
      message: 'Please enter a banner message',
    },
  },
  startTimeEpochMillis: {
    presence: {
      allowEmpty: false,
      message: 'Please enter a start time',
    },
  },
  alertLocation: {
    presence: {
      message: 'Please select a banner location',
    },
  },
};

export const AdminBannerModal = ({
  banner,
  setBanner,
  onClose,
  onCreate,
}: AdminBannerModalProps) => {
  const [isCreating, setIsCreating] = useState(false);

  const isBeforeLogin =
    banner.alertLocation === StatusAlertLocation.BEFORE_LOGIN;

  const locationOptions = [
    { value: StatusAlertLocation.AFTER_LOGIN, label: 'After Login' },
    { value: StatusAlertLocation.BEFORE_LOGIN, label: 'Before Login' },
  ];

  const getValidationErrors = (): string[] => {
    if (isCreating) {
      return ['Creating banner...'];
    }

    const validationResult = validate(banner, BANNER_VALIDATION_CONSTRAINTS, {
      fullMessages: false,
    });
    if (!validationResult) {
      return [];
    }

    const errors = validationResult as Record<string, string[]>;
    const errorMessages: string[] = [];
    Object.keys(errors).forEach((key) => {
      if (Array.isArray(errors[key])) {
        errorMessages.push(...errors[key]);
      }
    });

    return errorMessages;
  };

  const errors: string[] = getValidationErrors();

  // Event handlers
  const handleCreate = async () => {
    if (isCreating || errors.length > 0) {
      return;
    }
    setIsCreating(true);
    try {
      await onCreate();
    } finally {
      setIsCreating(false);
    }
  };

  const handleBannerChange = (field: keyof StatusAlert, value: any) => {
    setBanner({ ...banner, [field]: value });
  };

  const handleLocationChange = (value: StatusAlertLocation) => {
    setBanner({
      ...banner,
      title:
        value === StatusAlertLocation.AFTER_LOGIN
          ? ''
          : 'Scheduled Downtime Notice for the Researcher Workbench',
      alertLocation: value,
    });
  };

  const handleStartTimeChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const selectedDateEpochMillis = convertLocalDateTimeToEpochMillis(
      e.target.value
    );

    setBanner({
      ...banner,
      startTimeEpochMillis: selectedDateEpochMillis,
      endTimeEpochMillis:
        banner.endTimeEpochMillis &&
        banner.endTimeEpochMillis <= selectedDateEpochMillis
          ? null // Clear end time if it's before the new start time
          : banner.endTimeEpochMillis, // Keep end time if it's after start time
    });
  };

  const handleEndTimeChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const selectedDateEpochMillis = convertLocalDateTimeToEpochMillis(
      e.target.value
    );

    setBanner({
      ...banner,
      startTimeEpochMillis:
        selectedDateEpochMillis &&
        selectedDateEpochMillis <= banner.startTimeEpochMillis
          ? null
          : banner.startTimeEpochMillis,
      endTimeEpochMillis: selectedDateEpochMillis,
    });
  };

  return (
    <Modal onRequestClose={onClose}>
      <ModalTitle>Create New Banner</ModalTitle>
      <ModalBody>
        <ModalField label='Title' fieldId='banner-title'>
          <TooltipTrigger
            content={
              isBeforeLogin
                ? '"Before Login" banner has a fixed headline.'
                : null
            }
            side='right'
          >
            <div>
              <TextInput
                id='banner-title'
                value={banner.title}
                onChange={(value) => handleBannerChange('title', value)}
                placeholder='Enter banner title'
                disabled={isBeforeLogin}
                style={styles.input}
              />
            </div>
          </TooltipTrigger>
        </ModalField>

        <ModalField label='Message' fieldId='banner-message'>
          <TextInput
            id='banner-message'
            value={banner.message}
            onChange={(value) => handleBannerChange('message', value)}
            placeholder='Enter banner message'
            style={styles.input}
          />
        </ModalField>

        <ModalField label='Link (Optional)' fieldId='banner-link'>
          <TextInput
            id='banner-link'
            value={banner.link}
            onChange={(value) => handleBannerChange('link', value)}
            placeholder='Enter banner link'
            style={styles.input}
          />
        </ModalField>

        <ModalField label='Location' fieldId='banner-location'>
          <Select
            id='banner-location'
            value={banner.alertLocation}
            options={locationOptions}
            onChange={handleLocationChange}
          />
        </ModalField>

        <ModalField label='Start Time (Local)' fieldId='start-time'>
          <input
            id='start-time'
            type='datetime-local'
            min={formatDateTimeLocal(Date.now())}
            max={formatDateTimeLocal(
              banner.endTimeEpochMillis || Date.now() + MILLIS_PER_YEAR
            )}
            value={formatDateTimeLocal(banner.startTimeEpochMillis)}
            onChange={handleStartTimeChange}
            style={styles.input}
          />
        </ModalField>

        <ModalField label='End Time (Optional)' fieldId='end-time'>
          <input
            id='end-time'
            type='datetime-local'
            min={formatDateTimeLocal(banner.startTimeEpochMillis + 1)}
            max={formatDateTimeLocal(Date.now() + MILLIS_PER_YEAR)}
            value={formatDateTimeLocal(banner.endTimeEpochMillis)}
            onChange={handleEndTimeChange}
            style={styles.input}
          />
        </ModalField>
      </ModalBody>

      <ModalFooter>
        <Button
          type='secondary'
          onClick={onClose}
          style={{ marginRight: '1rem' }}
        >
          Cancel
        </Button>
        <TooltipTrigger
          content={
            errors.length > 0 ? (
              <div>
                {errors.map((error, index) => (
                  <div key={index}>{error}</div>
                ))}
              </div>
            ) : null
          }
          side='top'
        >
          <div>
            <Button onClick={handleCreate} disabled={errors.length > 0}>
              Create Banner
            </Button>
          </div>
        </TooltipTrigger>
      </ModalFooter>
    </Modal>
  );
};
