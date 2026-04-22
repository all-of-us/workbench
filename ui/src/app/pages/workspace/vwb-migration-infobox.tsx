import * as React from 'react';
import { faArrowUpRightFromSquare } from '@fortawesome/free-solid-svg-icons';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';

import { Button } from 'app/components/buttons';
import { FlexColumn, FlexRow } from 'app/components/flex';
import colors, { colorWithWhiteness } from 'app/styles/colors';

export const VwbMigrationInfoBox = () => {
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
          Ready to migrate?
        </div>

        {/* What will be migrated */}
        <div
          style={{
            fontSize: '14px',
            color: colors.dark,
            marginBottom: '18px',
          }}
        >
          What will be migrated:
          <ul style={{ margin: 0 }}>
            <li>Your workspace and files in your cloud storage bucket</li>
          </ul>
        </div>

        {/* What won’t be migrated */}
        <div
          style={{
            fontSize: '14px',
            color: colors.dark,
            marginBottom: '8px',
          }}
        >
          What won’t be migrated:
          <ul style={{ margin: 0 }}>
            <li>Any files on your persistent disks</li>
            <li>
              Any cohorts or datasets built in Cohort Builder and Dataset
              Builder that have not been exported to an application (Jupyter
              notebook, RStudio, etc)
            </li>
          </ul>
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
