import * as React from 'react';

import { Button } from 'app/components/buttons';
import { FlexColumn, FlexRow } from 'app/components/flex';
import { CheckCircle } from 'app/components/icons';
import colors from 'app/styles/colors';

import { DataDetail, styles } from './data-access-requirements';

type CtPlusState = 'NOT_AVAILABLE' | 'AVAILABLE' | 'COMPLETED';

export const ControlledTierPlusCard = (props: {
  institutionDisplayName: string;
  state: CtPlusState;
  onGetStarted: () => void;
}) => {
  const { institutionDisplayName, state, onGetStarted } = props;
  const isDisabled = state === 'NOT_AVAILABLE';
  const isCompleted = state === 'COMPLETED';

  return (
    <FlexRow style={styles.card} data-test-id='controlled-tier-plus-card'>
      {/* LEFT */}
      <FlexColumn>
        <div style={styles.cardHeader}>
          Controlled Tier + data – <i>Optional</i>
        </div>

        <div style={styles.dataDetails}>
          You are eligible to apply for access to Controlled Tier + data.
        </div>

        <div style={styles.dataDetails}>
          In addition to Registered Tier and Controlled Tier data, the
          Controlled Tier + dataset contains:
        </div>

        <DataDetail icon='genomic' text='Expanded base phenotypic dataset' />
        <DataDetail icon='additional' text='Zip5' />
        <DataDetail icon='individual' text='Pediatrics' />
      </FlexColumn>

      {/* RIGHT */}
      <FlexColumn style={styles.modulesContainer}>
        {/* Agreement row */}
        <FlexRow style={styles.backgroundModuleBox}>
          <div style={styles.moduleIcon}>
            <CheckCircle style={{ color: colors.success }} />
          </div>
          <div style={styles.backgroundModuleText}>
            {institutionDisplayName} must sign an institutional agreement
          </div>
        </FlexRow>

        {/* Button + helper text */}
        <FlexRow
          style={{
            ...styles.backgroundModuleBox,
            marginTop: '1.25em',
            alignItems: 'center',
            gap: '1em',
          }}
        >
          <Button
            style={styles.ctaButton}
            disabled={isDisabled}
            onClick={!isDisabled ? onGetStarted : undefined}
          >
            {isCompleted ? 'ENROLLED' : 'GET STARTED'}
          </Button>

          <div
            style={{
              fontSize: 13,
              opacity: isDisabled ? 0.5 : 0.8,
              whiteSpace: 'nowrap',
            }}
          >
            Workspace must be approved to access Controlled Tier + Data
          </div>
        </FlexRow>
      </FlexColumn>
    </FlexRow>
  );
};
