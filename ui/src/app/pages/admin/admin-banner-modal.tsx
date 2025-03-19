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

interface AdminBannerModalProps {
  banner: StatusAlert;
  setBanner: React.Dispatch<React.SetStateAction<StatusAlert>>;
  onClose: () => void;
  onCreate: () => void;
}

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
    const date = e.target.value ? new Date(e.target.value) : null;
    setBanner({ ...banner, startTimeEpochMillis: date?.getTime() });
  };

  const handleEndTimeChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const date = e.target.value ? new Date(e.target.value) : null;
    setBanner({ ...banner, endTimeEpochMillis: date?.getTime() });
  };

  const formatDateTimeLocal = (timestamp: number | null | undefined) => {
    if (!timestamp) {
      return '';
    }
    const date = new Date(timestamp);
    return date.toISOString().slice(0, 16); // Format as YYYY-MM-DDThh:mm
  };

  return (
    <Modal onRequestClose={onClose}>
      <ModalTitle>Create New Banner</ModalTitle>
      <ModalBody>
        <div style={{ marginBottom: '1rem' }}>
          <label>Title</label>
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
              />
            </div>
          </TooltipTrigger>
        </div>
        <div style={{ marginBottom: '1rem' }}>
          <label>Message</label>
          <TextInput
            value={banner.message}
            onChange={(value) => setBanner({ ...banner, message: value })}
            placeholder='Enter banner message'
          />
        </div>
        <div style={{ marginBottom: '1rem' }}>
          <label>Link (Optional)</label>
          <TextInput
            value={banner.link}
            onChange={(value) => setBanner({ ...banner, link: value })}
            placeholder='Enter banner link'
          />
        </div>
        <div style={{ marginBottom: '1rem' }}>
          <label>Location</label>
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
          <label>Start Time (Optional)</label>
          <input
            type='datetime-local'
            value={formatDateTimeLocal(banner.startTimeEpochMillis)}
            onChange={handleStartTimeChange}
            style={{ width: '100%' }}
          />
        </div>
        <div>
          <label>End Time (Optional)</label>
          <input
            type='datetime-local'
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
