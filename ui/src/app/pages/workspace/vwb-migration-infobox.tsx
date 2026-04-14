import * as React from 'react';

import { Button } from 'app/components/buttons';
import { FlexColumn } from 'app/components/flex';
import colors, { colorWithWhiteness } from 'app/styles/colors';

export const VwbMigrationInfoBox = () => {
  return (
    <FlexColumn
      style={{
        background: colors.white,
        border: `1px solid ${colorWithWhiteness(colors.dark, 0.7)}`,
        borderRadius: '8px',
        padding: '16px',
        maxWidth: '420px',
        marginBottom: '1.5rem',
      }}
    >
      {/* Title */}
      <div
        style={{
          fontSize: '16px',
          fontWeight: 600,
          color: colors.primary,
          marginBottom: '8px',
        }}
      >
        Ready to migrate?
      </div>

      {/* Description */}
      <div
        style={{
          fontSize: '13px',
          color: colors.dark,
          marginBottom: '12px',
          lineHeight: '18px',
        }}
      >
        Once you start migration you will no longer be able to access this
        workspace on AoU Researcher Workbench.
      </div>

      {/* What will be migrated */}
      <div
        style={{
          fontSize: '13px',
          fontWeight: 600,
          color: colors.primary,
          marginBottom: '4px',
        }}
      >
        What will be migrated:
      </div>

      <div
        style={{
          fontSize: '13px',
          color: colors.dark,
          marginBottom: '10px',
        }}
      >
        • Your workspace and files in your cloud storage bucket
      </div>

      {/* What won’t be migrated */}
      <div
        style={{
          fontSize: '13px',
          fontWeight: 600,
          color: colors.primary,
          marginBottom: '4px',
        }}
      >
        What won’t be migrated:
      </div>

      <div
        style={{
          fontSize: '13px',
          color: colors.dark,
          marginBottom: '14px',
        }}
      >
        • Any files on your persistent disks
      </div>

      {/* CTA */}
      <Button
        type='secondary'
        style={{ alignSelf: 'flex-start' }}
        onClick={() =>
          window.open('https://support.researchallofus.org/', '_blank')
        }
      >
        Open User Support Hub
      </Button>
    </FlexColumn>
  );
};
