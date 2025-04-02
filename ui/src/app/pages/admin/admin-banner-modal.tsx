import * as React from 'react';
import { useState, useMemo } from 'react';
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
import { convertLocalDateTimeToEpochMillis, formatDateTimeLocal, ONE_YEAR } from 'app/utils/dates';

const styles = reactStyles({
  label: {
    color: colors.primary,
  },
  input: {
    color: colors.primary,
  },
  formField: {
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
interface FormFieldProps {
  label: string;
  children: React.ReactNode;
  tooltip?: React.ReactNode;
}

const FormField: React.FC<FormFieldProps> = ({ label, children, tooltip }) => (
  <div style={styles.formField}>
    <label style={styles.label}>{label}</label>
    {tooltip ? (
      <TooltipTrigger content={tooltip} side='right'>
        <div>{children}</div>
      </TooltipTrigger>
    ) : (
      children
    )}
  </div>
);

export const AdminBannerModal = ({
  banner,
  setBanner,
  onClose,
  onCreate,
}: AdminBannerModalProps) => {
  const [isCreating, setIsCreating] = useState(false);
  
  const isBeforeLogin = banner.alertLocation === StatusAlertLocation.BEFORE_LOGIN;
  
  const locationOptions = [
    { value: StatusAlertLocation.AFTER_LOGIN, label: 'After Login' },
    { value: StatusAlertLocation.BEFORE_LOGIN, label: 'Before Login' },
  ];

  // Validation
  const constraints = useMemo(() => ({
    title: {
      presence: {
        allowEmpty: false,
        message: 'Please enter a banner title'
      }
    },
    message: {
      presence: {
        allowEmpty: false,
        message: 'Please enter a banner message'
      }
    },
    startTimeEpochMillis: {
      presence: {
        allowEmpty: false,
        message: 'Please enter a start time'
      }
    },
    alertLocation: {
      presence: {
        message: 'Please select a banner location'
      }
    }
  }), []);

  const getValidationErrors = (): string[] => {
    if (isCreating) {
      return ['Creating banner...'];
    }
    
    const validationResult = validate(banner, constraints, {fullMessages: false});
    if (!validationResult) {
      return [];
    }
    
    const errors = validationResult as Record<string, string[]>;
    const errorMessages: string[] = [];
    Object.keys(errors).forEach(key => {
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
      title: value === StatusAlertLocation.AFTER_LOGIN 
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
        <FormField 
          label="Title" 
          tooltip={isBeforeLogin ? '"Before Login" banner has a fixed headline.' : null}
        >
          <TextInput
            value={banner.title}
            onChange={(value) => handleBannerChange('title', value)}
            placeholder='Enter banner title'
            disabled={isBeforeLogin}
            style={styles.input}
          />
        </FormField>
        
        <FormField label="Message">
          <TextInput
            value={banner.message}
            onChange={(value) => handleBannerChange('message', value)}
            placeholder='Enter banner message'
            style={styles.input}
          />
        </FormField>
        
        <FormField label="Link (Optional)">
          <TextInput
            value={banner.link}
            onChange={(value) => handleBannerChange('link', value)}
            placeholder='Enter banner link'
            style={styles.input}
          />
        </FormField>
        
        <FormField label="Location">
          <Select
            value={banner.alertLocation}
            options={locationOptions}
            onChange={handleLocationChange}
          />
        </FormField>
        
        <FormField label="Start Time (Local)">
          <input
            id='start-time'
            type='datetime-local'
            min={formatDateTimeLocal(Date.now())}
            max={formatDateTimeLocal(
              banner.endTimeEpochMillis || Date.now() + ONE_YEAR
            )}
            value={formatDateTimeLocal(banner.startTimeEpochMillis)}
            onChange={handleStartTimeChange}
            style={styles.input}
          />
        </FormField>
        
        <FormField label="End Time (Optional)">
          <input
            id='end-time'
            type='datetime-local'
            min={formatDateTimeLocal(banner.startTimeEpochMillis + 1)}
            max={formatDateTimeLocal(Date.now() + ONE_YEAR)}
            value={formatDateTimeLocal(banner.endTimeEpochMillis)}
            onChange={handleEndTimeChange}
            style={styles.input}
          />
        </FormField>
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
          content={errors.length > 0 ? (
            <div>
              {errors.map((error, index) => (
                <div key={index}>{error}</div>
              ))}
            </div>
          ) : null}
          side='top'
        >
          <div>
            <Button
              onClick={handleCreate}
              disabled={errors.length > 0}
            >
              Create Banner
            </Button>
          </div>
        </TooltipTrigger>
      </ModalFooter>
    </Modal>
  );
};
