import * as React from 'react';

import { FlexColumn, FlexRow } from 'app/components/flex';
import { AoU } from 'app/components/text-wrappers';
import { formatInitialCreditsUSD } from 'app/utils';

import { styles } from './profile-styles';

interface Props {
  freeTierUsage: number;
  freeTierDollarQuota: number;
}
export const InitialCreditsPanel = (props: Props) => (
  <FlexRow style={styles.initialCreditsBox}>
    <FlexColumn style={{ marginLeft: '1.2rem' }}>
      <div style={{ marginTop: '0.6rem' }}>
        <AoU /> initial credits used:
      </div>
      <div>
        Remaining <AoU /> initial credits:
      </div>
    </FlexColumn>
    <FlexColumn style={{ alignItems: 'flex-end', marginLeft: '1.5rem' }}>
      <div style={{ marginTop: '0.6rem', fontWeight: 600 }}>
        {formatInitialCreditsUSD(props.freeTierUsage)}
      </div>
      <div style={{ fontWeight: 600, backgroundColor: 'black' }}>
        {formatInitialCreditsUSD(
          props.freeTierDollarQuota - (props.freeTierUsage ?? 0)
        )}
      </div>
    </FlexColumn>
  </FlexRow>
);
