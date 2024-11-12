import * as React from 'react';

import { FlexColumn, FlexRow } from 'app/components/flex';
import { AoU } from 'app/components/text-wrappers';
import { formatInitialCreditsUSD } from 'app/utils';
import { displayDateWithoutHours } from 'app/utils/dates';

import { styles } from './profile-styles';

interface Props {
  freeTierUsage: number;
  freeTierDollarQuota: number;
  expirationDate: number;
}
export const InitialCreditsPanel = (props: Props) => {
  const { expirationDate, freeTierUsage, freeTierDollarQuota } = props;
  return (
    <FlexRow style={styles.initialCreditsBox}>
      <FlexColumn>
        <div>
          <AoU /> initial credits used:
        </div>
        <div>
          Remaining <AoU /> initial credits:
        </div>
        {expirationDate && (
          <div>
            <AoU /> initial credits epiration date:
          </div>
        )}
      </FlexColumn>
      <FlexColumn style={{ flex: 1, alignItems: 'flex-end' }}>
        <div style={{ fontWeight: 600 }}>
          {formatInitialCreditsUSD(freeTierUsage)}
        </div>
        <div style={{ fontWeight: 600 }}>
          {formatInitialCreditsUSD(freeTierDollarQuota - (freeTierUsage ?? 0))}
        </div>
        <div style={{ fontWeight: 600 }}>
          {displayDateWithoutHours(expirationDate)}
        </div>
      </FlexColumn>
    </FlexRow>
  );
};
