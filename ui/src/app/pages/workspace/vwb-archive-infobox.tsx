import * as React from 'react';

import { FlexColumn } from 'app/components/flex';
import colors, { colorWithWhiteness } from 'app/styles/colors';

export const VwbArchiveInfoBox = () => {
  return (
    <FlexColumn
      style={{
        background: colors.white,
        border: `1px solid ${colorWithWhiteness(colors.dark, 0.7)}`,
        borderRadius: '8px',
        padding: '24px',
        flex: 1,
        marginBottom: '1.5rem',
      }}
    >
      <div
        style={{
          fontSize: '16px',
          fontWeight: 600,
          color: colors.primary,
          marginBottom: '18px',
        }}
      >
        Archived data retrieval
      </div>

      <div
        style={{
          fontSize: '14px',
          color: colors.dark,
          lineHeight: '1.6',
          marginBottom: '18px',
        }}
      >
        This transfer may take a little while to accomplish. You will be emailed
        once your data has been retrieved.
      </div>

      <div
        style={{
          fontSize: '14px',
          color: colors.dark,
          lineHeight: '1.6',
        }}
      >
        At the point of archiving only notebooks, scripts, and workspace bucket
        files were saved. You may need to setup new workspaces and environments
        after recovery.
      </div>
    </FlexColumn>
  );
};
