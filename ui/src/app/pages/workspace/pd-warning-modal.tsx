import * as React from 'react';

import { Button, StyledExternalLink } from 'app/components/buttons';
import {
  Modal,
  ModalBody,
  ModalFooter,
  ModalTitle,
} from 'app/components/modals';
import colors from 'app/styles/colors';

interface Props {
  onCancel: () => void;
  onConfirm: () => void;
}

const USER_SUPPORT_OFFICE_HOURS_URL =
  'https://support.researchallofus.org/hc/en-us/sections/6000285700372-Office-Hour-Recordings';

export const PdWarningModal = ({ onCancel, onConfirm }: Props) => {
  return (
    <Modal>
      {/* TITLE */}
      <ModalTitle
        style={{
          fontSize: '20px',
          fontWeight: 600,
          color: colors.primary,
          lineHeight: '28px',
        }}
      >
        You still have data on your persistent disk
      </ModalTitle>

      {/* BODY */}
      <ModalBody>
        <div
          style={{
            fontSize: '14px',
            color: colors.dark,
            lineHeight: '20px',
          }}
        >
          Persistent disk data will not be migrated. Make sure you move this
          data to your cloud bucket for it to stay intact.{' '}
          <StyledExternalLink
            href={USER_SUPPORT_OFFICE_HOURS_URL}
            style={{
              color: colors.accent,
              textDecoration: 'underline',
            }}
            target='_blank'
          >
            Learn more
          </StyledExternalLink>
        </div>
      </ModalBody>

      {/* FOOTER */}
      <ModalFooter
        style={{
          display: 'flex',
          justifyContent: 'flex-end',
          gap: '12px',
          paddingTop: '1.5rem',
        }}
      >
        {/* GO BACK (text style) */}
        <div
          onClick={onCancel}
          style={{
            cursor: 'pointer',
            fontSize: '13px',
            fontWeight: 600,
            color: colors.primary,
            alignSelf: 'center',
          }}
        >
          GO BACK
        </div>

        {/* PRIMARY CTA */}
        <Button
          style={{
            background: colors.primary,
            color: colors.white,
            padding: '10px 16px',
            borderRadius: '6px',
            fontWeight: 600,
            fontSize: '13px',
          }}
          onClick={onConfirm}
        >
          CONTINUE WITH MIGRATION
        </Button>
      </ModalFooter>
    </Modal>
  );
};
