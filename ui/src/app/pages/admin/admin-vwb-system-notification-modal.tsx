import * as React from 'react';
import { useState } from 'react';

import {
  VwbSystemNotification,
  VwbSystemNotificationPriority,
  VwbSystemNotificationType,
} from 'generated/fetch';

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
  note: {
    color: colors.primary,
    fontSize: 12,
    marginBottom: '1rem',
    display: 'flex',
    flexDirection: 'column',
    gap: '0.25rem',
  },
});

export const MAX_VWB_NOTIFICATION_TITLE = 200;
export const MAX_VWB_NOTIFICATION_MESSAGE = 4000;

interface AdminVwbSystemNotificationModalProps {
  notification: VwbSystemNotification;
  setNotification: React.Dispatch<React.SetStateAction<VwbSystemNotification>>;
  onClose: () => void;
  onCreate: () => void;
}

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

export const validateVwbSystemNotification = (
  notification: VwbSystemNotification
): string[] => {
  const errors: string[] = [];
  if (!notification.title?.trim()) {
    errors.push('Title is required');
  } else if (notification.title.length > MAX_VWB_NOTIFICATION_TITLE) {
    errors.push(
      `Title must be ${MAX_VWB_NOTIFICATION_TITLE} characters or fewer`
    );
  }
  if (!notification.message?.trim()) {
    errors.push('Message is required');
  } else if (notification.message.length > MAX_VWB_NOTIFICATION_MESSAGE) {
    errors.push(
      `Message must be ${MAX_VWB_NOTIFICATION_MESSAGE} characters or fewer`
    );
  }
  if (
    notification.endTimeEpochMillis &&
    notification.startTimeEpochMillis &&
    notification.endTimeEpochMillis <= notification.startTimeEpochMillis
  ) {
    errors.push('End time must be after start time');
  }
  return errors;
};

export const AdminVwbSystemNotificationModal = ({
  notification,
  setNotification,
  onClose,
  onCreate,
}: AdminVwbSystemNotificationModalProps) => {
  const [isCreating, setIsCreating] = useState(false);

  const typeOptions = [
    {
      value: VwbSystemNotificationType.PASSIVE,
      label: 'Passive (notification center)',
    },
    {
      value: VwbSystemNotificationType.BLOCKING,
      label: 'Blocking (requires acknowledgement)',
    },
  ];

  const priorityOptions = [
    { value: VwbSystemNotificationPriority.INFO, label: 'Info' },
    { value: VwbSystemNotificationPriority.WARNING, label: 'Warning' },
    { value: VwbSystemNotificationPriority.ERROR, label: 'Error' },
  ];

  const errors: string[] = isCreating
    ? ['Creating banner...']
    : validateVwbSystemNotification(notification);

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

  const handleChange = (field: keyof VwbSystemNotification, value: any) =>
    setNotification({ ...notification, [field]: value });

  const handleStartTimeChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const selected = convertLocalDateTimeToEpochMillis(e.target.value);
    setNotification({
      ...notification,
      startTimeEpochMillis: selected,
      endTimeEpochMillis:
        notification.endTimeEpochMillis &&
        notification.endTimeEpochMillis <= selected
          ? null
          : notification.endTimeEpochMillis,
    });
  };

  const handleEndTimeChange = (e: React.ChangeEvent<HTMLInputElement>) =>
    setNotification({
      ...notification,
      endTimeEpochMillis: convertLocalDateTimeToEpochMillis(e.target.value),
    });

  return (
    <Modal onRequestClose={onClose}>
      <ModalTitle>Create Verily Workbench Banner</ModalTitle>
      <ModalBody>
        <div style={styles.note}>
          <div style={{ fontWeight: 600 }}>
            Audience: All of Us customers only.
          </div>
          <div>
            This creates an organization-scoped notification in Verily
            Workbench, visible to users in the All of Us organization. It is not
            a system-wide Verily Workbench banner and will not reach customers
            of other organizations.
          </div>
          <div>
            Verily Workbench owns the notification itself. It is listed in the
            table on this page with Verily Workbench set to Yes, and deleting it
            there removes it from Verily Workbench too.
          </div>
        </div>

        <ModalField label='Title' fieldId='vwb-banner-title'>
          <TextInput
            id='vwb-banner-title'
            value={notification.title}
            onChange={(value) => handleChange('title', value)}
            placeholder='Enter banner title'
            style={styles.input}
          />
        </ModalField>

        <ModalField label='Message' fieldId='vwb-banner-message'>
          <TextInput
            id='vwb-banner-message'
            value={notification.message}
            onChange={(value) => handleChange('message', value)}
            placeholder='Enter banner message'
            style={styles.input}
          />
        </ModalField>

        <ModalField label='Type' fieldId='vwb-banner-type'>
          <Select
            id='vwb-banner-type'
            value={notification.notificationType}
            options={typeOptions}
            onChange={(value) => handleChange('notificationType', value)}
          />
        </ModalField>

        <ModalField label='Priority' fieldId='vwb-banner-priority'>
          <Select
            id='vwb-banner-priority'
            value={notification.notificationPriority}
            options={priorityOptions}
            onChange={(value) => handleChange('notificationPriority', value)}
          />
        </ModalField>

        <ModalField label='Start Time (Local)' fieldId='vwb-start-time'>
          <input
            id='vwb-start-time'
            type='datetime-local'
            min={formatDateTimeLocal(Date.now())}
            max={formatDateTimeLocal(
              notification.endTimeEpochMillis || Date.now() + MILLIS_PER_YEAR
            )}
            value={formatDateTimeLocal(notification.startTimeEpochMillis)}
            onChange={handleStartTimeChange}
            style={styles.input}
          />
        </ModalField>

        <ModalField label='End Time (Optional)' fieldId='vwb-end-time'>
          <input
            id='vwb-end-time'
            type='datetime-local'
            min={formatDateTimeLocal(notification.startTimeEpochMillis + 1)}
            max={formatDateTimeLocal(Date.now() + MILLIS_PER_YEAR)}
            value={formatDateTimeLocal(notification.endTimeEpochMillis)}
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
