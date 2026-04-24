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

        {/* Description */}
        <div
          style={{
            fontSize: '14px',
            color: colors.dark,
            marginBottom: '18px',
            lineHeight: '1.5',
          }}
        >
          Once you start the migration process, you will temporarily be unable
          to access this workspace in Researcher Workbench 1.0. Once migration
          is complete, you can return to the workspace if needed, but we highly
          encourage you to continue work in Researcher Workbench 2.0.
        </div>

        <div
          style={{
            fontSize: '14px',
            color: colors.dark,
            marginBottom: '18px',
            lineHeight: '1.5',
          }}
        >
          Please note, Researcher Workbench 2.0 billing pods you have been added
          to within the last few hours may not immediately display in the drop
          down menu below. If you've recently been added to a billing pod and
          don't see it listed as an option, please check back in 12-24 hours.
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
            <li>Your workspace and files in your workspace bucket</li>
          </ul>
        </div>

        {/* What will not be migrated */}
        <div
          style={{
            fontSize: '14px',
            color: colors.dark,
            marginBottom: '8px',
          }}
        >
          What will not be migrated:
          <ul style={{ margin: 0 }}>
            <li>Any files in your persistent disk</li>
            <li>
              Any cohorts, concept sets, or datasets in the Cohort Builder and
              Dataset Builder UI
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
