import * as React from 'react';
import {
  faTriangleExclamation,
  faXmark,
} from '@fortawesome/free-solid-svg-icons';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';

import { Clickable } from 'app/components/buttons';
import { FlexRow } from 'app/components/flex';
import colors, { colorWithWhiteness } from 'app/styles/colors';

interface Props {
  title: string;
  message: string;
  actionText?: string;
  onAction?: () => void;
  onClose?: () => void;
}

export const VwbImportantBanner = ({
  title,
  message,
  actionText,
  onAction,
  onClose,
}: Props) => {
  return (
    <div
      style={{
        background: colorWithWhiteness(colors.warning, 0.9),
        border: `1px solid ${colors.warning}`,
        borderRadius: '6px',
        padding: '12px 16px',
        marginBottom: '1rem',
      }}
    >
      {/* LEFT */}
      <FlexRow
        style={{ color: colors.warningAlt, alignItems: 'center', gap: '10px' }}
      >
        <FontAwesomeIcon icon={faTriangleExclamation} />
        <div style={{ flex: 2, fontWeight: 600 }}>{title}</div>

        {/* RIGHT */}
        {actionText && (
          <Clickable
            style={{ fontWeight: 500, height: '24px' }}
            onClick={onAction}
          >
            {actionText}
          </Clickable>
        )}
        <Clickable style={{ cursor: 'pointer' }} onClick={onClose}>
          <FontAwesomeIcon icon={faXmark} />
        </Clickable>
      </FlexRow>

      <FlexRow>
        <div
          style={{ fontSize: '13px', color: colors.dark, marginLeft: '24px' }}
        >
          {message}
        </div>
      </FlexRow>
    </div>
  );
};
