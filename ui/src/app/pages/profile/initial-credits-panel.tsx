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
    <FlexColumn style={{ marginLeft: '0.8rem' }}>
      <div style={{ marginTop: '0.4rem' }}>
        <AoU /> initial credits used:
      </div>
      <div>
        Remaining <AoU /> initial credits:
      </div>
    </FlexColumn>
    <FlexColumn style={{ alignItems: 'flex-end', marginLeft: '1.0rem' }}>
      <div style={{ marginTop: '0.4rem', fontWeight: 600 }}>
        {formatInitialCreditsUSD(props.freeTierUsage)}
      </div>
      <div style={{ fontWeight: 600 }}>
        {formatInitialCreditsUSD(
          props.freeTierDollarQuota - props.freeTierUsage
        )}
      </div>
    </FlexColumn>
  </FlexRow>
);
