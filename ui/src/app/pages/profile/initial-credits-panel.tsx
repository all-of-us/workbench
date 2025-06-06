import * as React from 'react';

import { StyledExternalLink } from 'app/components/buttons';
import { FlexColumn, FlexRow } from 'app/components/flex';
import { AoU } from 'app/components/text-wrappers';
import { formatInitialCreditsUSD } from 'app/utils';
import { displayDateWithoutHours } from 'app/utils/dates';
import { supportUrls } from 'app/utils/zendesk';

import { styles } from './profile-styles';

interface Props {
  initialCreditsUsage: number;
  initialCreditsLimit: number;
  expirationDate: number;
}
export const InitialCreditsPanel = (props: Props) => {
  const { expirationDate, initialCreditsUsage, initialCreditsLimit } = props;

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
              <AoU /> initial credits expiration date:
            </div>
          )}
        </FlexColumn>
        <FlexColumn style={{ flex: 1, alignItems: 'flex-end' }}>
          <div style={{ fontWeight: 600 }}>
            {formatInitialCreditsUSD(initialCreditsUsage)}
          </div>
          <div style={{ fontWeight: 600 }}>
            {formatInitialCreditsUSD(
              initialCreditsLimit - (initialCreditsUsage ?? 0)
            )}
          </div>
          {expirationDate && (
            <div style={{ fontWeight: 600 }}>
              {displayDateWithoutHours(expirationDate)}
            </div>
          )}
        </FlexColumn>
      </FlexRow>
      <FlexRow>
        <StyledExternalLink
          href={supportUrls.initialCredits}
          style={{ marginTop: '1rem' }}
        >
          Learn more about credits & setting up a billing account
        </StyledExternalLink>
      </FlexRow>
    </div>
  );
};
