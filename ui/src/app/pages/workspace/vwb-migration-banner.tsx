import * as React from 'react';

import { Button } from 'app/components/buttons';
import { FlexRow } from 'app/components/flex';
import { ClrIcon } from 'app/components/icons';
import colors, { colorWithWhiteness } from 'app/styles/colors';

interface Props {
  title: string;
  message: string;
  actionText?: string;
  onAction?: () => void;
}

export const VwbImportantBanner = ({
  title,
  message,
  actionText,
  onAction,
}: Props) => {
  return (
    <FlexRow
      style={{
        justifyContent: 'space-between',
        alignItems: 'center',
        background: colorWithWhiteness(colors.warning, 0.9),
        border: `1px solid ${colors.warning}`,
        borderRadius: '6px',
        padding: '12px 16px',
        marginBottom: '1rem',
      }}
    >
      {/* LEFT */}
      <FlexRow style={{ alignItems: 'center', gap: '10px' }}>
        <ClrIcon shape='exclamation-triangle' size={16} />

        <div>
          <div style={{ fontWeight: 600, color: colors.primary }}>{title}</div>
          <div style={{ fontSize: '13px', color: colors.dark }}>{message}</div>
        </div>
      </FlexRow>

      {/* RIGHT */}
      {actionText && (
        <Button type='secondary' onClick={onAction}>
          {actionText}
        </Button>
      )}
    </FlexRow>
  );
};
