import * as React from 'react';
import { useState } from 'react';

import { VwbBanner, VwbBannerPriority, VwbBannerType } from 'generated/fetch';

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

export const MAX_VWB_BANNER_TITLE = 200;
export const MAX_VWB_BANNER_MESSAGE = 4000;

interface AdminVwbBannerModalProps {
  banner: VwbBanner;
  setBanner: React.Dispatch<React.SetStateAction<VwbBanner>>;
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

export const validateVwbBanner = (banner: VwbBanner): string[] => {
  const errors: string[] = [];
  if (!banner.title?.trim()) {
    errors.push('Title is required');
  } else if (banner.title.length > MAX_VWB_BANNER_TITLE) {
    errors.push(`Title must be ${MAX_VWB_BANNER_TITLE} characters or fewer`);
  }
  if (!banner.message?.trim()) {
    errors.push('Message is required');
  } else if (banner.message.length > MAX_VWB_BANNER_MESSAGE) {
    errors.push(
      `Message must be ${MAX_VWB_BANNER_MESSAGE} characters or fewer`
    );
  }
  if (
    banner.endTimeEpochMillis &&
    banner.startTimeEpochMillis &&
    banner.endTimeEpochMillis <= banner.startTimeEpochMillis
  ) {
    errors.push('End time must be after start time');
  }
  return errors;
};

export const AdminVwbBannerModal = ({
  banner,
  setBanner,
  onClose,
  onCreate,
}: AdminVwbBannerModalProps) => {
  const [isCreating, setIsCreating] = useState(false);

  const typeOptions = [
    { value: VwbBannerType.PASSIVE, label: 'Passive (notification center)' },
    {
      value: VwbBannerType.BLOCKING,
      label: 'Blocking (requires acknowledgement)',
    },
  ];

  const priorityOptions = [
    { value: VwbBannerPriority.INFO, label: 'Info' },
    { value: VwbBannerPriority.WARNING, label: 'Warning' },
    { value: VwbBannerPriority.ERROR, label: 'Error' },
  ];

  const errors: string[] = isCreating
    ? ['Creating banner...']
    : validateVwbBanner(banner);

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

  const handleBannerChange = (field: keyof VwbBanner, value: any) =>
    setBanner({ ...banner, [field]: value });

  const handleStartTimeChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const selected = convertLocalDateTimeToEpochMillis(e.target.value);
    setBanner({
      ...banner,
      startTimeEpochMillis: selected,
      endTimeEpochMillis:
        banner.endTimeEpochMillis && banner.endTimeEpochMillis <= selected
          ? null
          : banner.endTimeEpochMillis,
    });
  };

  const handleEndTimeChange = (e: React.ChangeEvent<HTMLInputElement>) =>
    setBanner({
      ...banner,
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
            It is stored in Verily Workbench rather than the All of Us database,
            so it will not appear in the table on this page.
          </div>
        </div>

        <ModalField label='Title' fieldId='vwb-banner-title'>
          <TextInput
            id='vwb-banner-title'
            value={banner.title}
            onChange={(value) => handleBannerChange('title', value)}
            placeholder='Enter banner title'
            style={styles.input}
          />
        </ModalField>

        <ModalField label='Message' fieldId='vwb-banner-message'>
          <TextInput
            id='vwb-banner-message'
            value={banner.message}
            onChange={(value) => handleBannerChange('message', value)}
            placeholder='Enter banner message'
            style={styles.input}
          />
        </ModalField>

        <ModalField label='Type' fieldId='vwb-banner-type'>
          <Select
            id='vwb-banner-type'
            value={banner.notificationType}
            options={typeOptions}
            onChange={(value) => handleBannerChange('notificationType', value)}
          />
        </ModalField>

        <ModalField label='Priority' fieldId='vwb-banner-priority'>
          <Select
            id='vwb-banner-priority'
            value={banner.notificationPriority}
            options={priorityOptions}
            onChange={(value) =>
              handleBannerChange('notificationPriority', value)
            }
          />
        </ModalField>

        <ModalField label='Start Time (Local)' fieldId='vwb-start-time'>
          <input
            id='vwb-start-time'
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

        <ModalField label='End Time (Optional)' fieldId='vwb-end-time'>
          <input
            id='vwb-end-time'
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
