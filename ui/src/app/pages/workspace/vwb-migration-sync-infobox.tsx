import * as React from 'react';
import { faArrowUpRightFromSquare } from '@fortawesome/free-solid-svg-icons';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';

import { Button } from 'app/components/buttons';
import { FlexColumn, FlexRow } from 'app/components/flex';
import colors, { colorWithWhiteness } from 'app/styles/colors';

export const VwbMigrationSyncInfoBox = () => {
  return (
    <FlexRow>
      <FlexColumn
        style={{
          background: colors.white,
          border: `1px solid ${colorWithWhiteness(colors.dark, 0.7)}`,
          borderRadius: '8px',
          padding: '24px',
          maxWidth: '500px',
          marginBottom: '1.5rem',
        }}
      >
        {/* Title */}
        <div
          style={{
            fontSize: '16px',
            fontWeight: 600,
            color: colors.primary,
            marginBottom: '18px',
          }}
        >
          Move files between platforms
        </div>

        {/* Description */}
        <div
          style={{
            fontSize: '14px',
            color: colors.dark,
            marginBottom: '18px',
            lineHeight: '1.5',
          }}
        >
          This workspace has been migrated to Researcher Workbench 2.0. Folders
          in this workspace can be copied from legacy Workbench to Researcher
          Workbench 2.0
        </div>
      </FlexColumn>
      <FlexColumn>
        <Button
          type='secondaryOutline'
          style={{ height: '36px', marginLeft: '14px' }}
          onClick={() =>
            window.open(
              'https://support.researchallofus.org/hc/en-us/articles/48266066855188',
              '_blank'
            )
          }
        >
          <FontAwesomeIcon
            icon={faArrowUpRightFromSquare}
            style={{ marginRight: '8px' }}
          />
          Open User Support Hub
        </Button>
      </FlexColumn>
    </FlexRow>
  );
};
