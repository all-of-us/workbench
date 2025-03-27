import * as React from 'react';
import { useState } from 'react';

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

const styles = reactStyles({
  label: {
    color: colors.primary,
  },
  input: {
    color: colors.primary,
  },
});

const ONE_YEAR = 365 * 24 * 60 * 60 * 1000;

interface AdminBannerModalProps {
  banner: StatusAlert;
  setBanner: React.Dispatch<React.SetStateAction<StatusAlert>>;
  onClose: () => void;
  onCreate: () => void;
}

// Expects a local date-time string in the format 'YYYY-MM-DDThh:mm' and returns the epoch milliseconds
const convertLocalDateTimeToEpochMillis = (localDateTime: string) => {
  if (!localDateTime) {
    return null;
  }
  const [datePart, timePart] = localDateTime.split('T');
  const [year, month, day] = datePart.split('-').map(Number);
  const [hours, minutes] = timePart.split(':').map(Number);
  const localDate = new Date(year, month - 1, day, hours, minutes);
  return localDate.getTime();
};

export const AdminBannerModal = ({
  banner,
  setBanner,
  onClose,
  onCreate,
}: AdminBannerModalProps) => {
  const [isCreating, setIsCreating] = useState(false);
  const locationOptions = [
    { value: StatusAlertLocation.AFTER_LOGIN, label: 'After Login' },
    { value: StatusAlertLocation.BEFORE_LOGIN, label: 'Before Login' },
  ];

  const isBeforeLogin =
    banner.alertLocation === StatusAlertLocation.BEFORE_LOGIN;

  const handleCreate = async () => {
    if (isCreating) {
      return;
    }
    setIsCreating(true);
    try {
      await onCreate();
    } finally {
      setIsCreating(false);
    }
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

  const formatDateTimeLocal = (timestamp: number | null | undefined) => {
    if (!timestamp) {
      return '';
    }
    const date = new Date(timestamp);

    // Format as YYYY-MM-DDThh:mm in local timezone
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0'); // +1 because months are 0-indexed
    const day = String(date.getDate()).padStart(2, '0');
    const hours = String(date.getHours()).padStart(2, '0');
    const minutes = String(date.getMinutes()).padStart(2, '0');

    return `${year}-${month}-${day}T${hours}:${minutes}`;
  };

  return (
    <Modal onRequestClose={onClose}>
      <ModalTitle>Create New Banner</ModalTitle>
      <ModalBody>
        <div style={{ marginBottom: '1rem' }}>
          <label style={styles.label}>Title</label>
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
                value={banner.title}
                onChange={(value) => setBanner({ ...banner, title: value })}
                placeholder='Enter banner title'
                disabled={isBeforeLogin}
                style={styles.input}
              />
            </div>
          </TooltipTrigger>
        </div>
        <div style={{ marginBottom: '1rem' }}>
          <label style={styles.label}>Message</label>
          <TextInput
            value={banner.message}
            onChange={(value) => setBanner({ ...banner, message: value })}
            placeholder='Enter banner message'
            style={styles.input}
          />
        </div>
        <div style={{ marginBottom: '1rem' }}>
          <label style={styles.label}>Link (Optional)</label>
          <TextInput
            value={banner.link}
            onChange={(value) => setBanner({ ...banner, link: value })}
            placeholder='Enter banner link'
            style={styles.input}
          />
        </div>
        <div style={{ marginBottom: '1rem' }}>
          <label style={styles.label}>Location</label>
          <Select
            value={banner.alertLocation}
            options={locationOptions}
            onChange={(value) =>
              setBanner({
                ...banner,
                title:
                  value === StatusAlertLocation.AFTER_LOGIN
                    ? ''
                    : 'Scheduled Downtime Notice for the Researcher Workbench',
                alertLocation: value,
              })
            }
          />
        </div>
        <div style={{ marginBottom: '1rem' }}>
          <label style={styles.label} htmlFor='start-time'>
            Start Time (Local)
          </label>
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
        </div>
        <div>
          <label style={styles.label} htmlFor='end-time'>
            End Time (Optional)
          </label>
          <input
            id='end-time'
            type='datetime-local'
            min={formatDateTimeLocal(banner.startTimeEpochMillis + 1)}
            max={formatDateTimeLocal(Date.now() + ONE_YEAR)}
            value={formatDateTimeLocal(banner.endTimeEpochMillis)}
            onChange={handleEndTimeChange}
            style={{ width: '100%' }}
          />
        </div>
      </ModalBody>
      <ModalFooter>
        <Button
          type='secondary'
          onClick={onClose}
          style={{ marginRight: '1rem' }}
        >
          Cancel
        </Button>
        <Button
          onClick={handleCreate}
          disabled={
            !banner.title ||
            !banner.message ||
            !banner.alertLocation ||
            isCreating
          }
        >
          Create Banner
        </Button>
      </ModalFooter>
    </Modal>
  );
};
