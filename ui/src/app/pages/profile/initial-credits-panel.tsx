import * as React from 'react';

import { Button } from 'app/components/buttons';
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
    <div style={styles.initialCreditsBox}>
      <FlexRow>
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
            {formatInitialCreditsUSD(
              freeTierDollarQuota - (freeTierUsage ?? 0)
            )}
          </div>
          {expirationDate && (
            <div style={{ fontWeight: 600 }}>
              {displayDateWithoutHours(expirationDate)}
            </div>
          )}
        </FlexColumn>
      </FlexRow>
      {expirationDate && (
        <FlexRow>
          <Button
            type='primarySmall'
            style={{ marginTop: '1rem' }}
            onClick={() => console.log('Out of hand')}
          >
            Request credit extension
          </Button>
        </FlexRow>
      )}
      <FlexRow>
        <Button
          style={{
            maxWidth: 'none',
            padding: '0',
            textTransform: 'none',
          }}
          type='link'
          onClick={() => console.log('Come on love')}
        >
          Learn more about credits & setting up a billing account
        </Button>
      </FlexRow>
    </div>
  );
};
