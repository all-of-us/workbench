import * as React from 'react';

import { Profile } from 'generated/fetch';

import { Button, StyledExternalLink } from 'app/components/buttons';
import { ExtendInitialCreditsModal } from 'app/components/extend-initial-credits-modal';
import { FlexColumn, FlexRow } from 'app/components/flex';
import { AoU } from 'app/components/text-wrappers';
import { formatInitialCreditsUSD } from 'app/utils';
import { displayDateWithoutHours } from 'app/utils/dates';
import { supportUrls } from 'app/utils/zendesk';

import { styles } from './profile-styles';

interface Props {
  freeTierUsage: number;
  freeTierDollarQuota: number;
  eligibleForExtension: boolean;
  expirationDate: number;
  updateInitialCredits: Function;
}
export const InitialCreditsPanel = (props: Props) => {
  const [showExtendInitialCreditsModal, setShowExtendInitialCreditsModal] =
    React.useState(false);
  const {
    eligibleForExtension,
    expirationDate,
    freeTierUsage,
    freeTierDollarQuota,
    updateInitialCredits,
  } = props;

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
      {eligibleForExtension && (
        <FlexRow>
          <Button
            type='primarySmall'
            style={{ marginTop: '1rem' }}
            onClick={() => setShowExtendInitialCreditsModal(true)}
          >
            Request credit extension
          </Button>
          {showExtendInitialCreditsModal && (
            <ExtendInitialCreditsModal
              onClose={(updatedProfile: Profile) => {
                if (updatedProfile) {
                  updateInitialCredits(
                    updatedProfile.initialCreditsExpirationEpochMillis,
                    updatedProfile.eligibleForInitialCreditsExtension
                  );
                }
                setShowExtendInitialCreditsModal(false);
              }}
              expirationDate={expirationDate}
            />
          )}
        </FlexRow>
      )}
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
