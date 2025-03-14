import * as React from 'react';

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
  const locationOptions = [
    { value: StatusAlertLocation.AFTER_LOGIN, label: 'After Login' },
    { value: StatusAlertLocation.BEFORE_LOGIN, label: 'Before Login' },
  ];

  const isBeforeLogin =
    banner.alertLocation === StatusAlertLocation.BEFORE_LOGIN;

  return (
    <Modal onRequestClose={onClose}>
      <ModalTitle>Create New Banner</ModalTitle>
      <ModalBody>
        <div style={{ marginBottom: '1rem' }}>
          <label>Title</label>
          <TooltipTrigger
            content={
              isBeforeLogin && '"Before Login" banner has a fixed headline.'
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
        <div>
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
          onClick={onCreate}
          disabled={!banner.title || !banner.message || !banner.alertLocation}
        >
          Create Banner
        </Button>
      </ModalFooter>
    </Modal>
  );
};
