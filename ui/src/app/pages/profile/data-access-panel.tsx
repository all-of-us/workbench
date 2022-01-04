import * as React from 'react';
import {Link} from 'react-router-dom';

import {FlexRow} from 'app/components/flex';
import {CheckCircle, ControlledTierBadge, RegisteredTierBadge} from 'app/components/icons';
import {styles} from 'app/pages/profile/profile-styles';
import colors from 'app/styles/colors';
import {useId} from 'app/utils';
import {AccessTierShortNames, displayNameForTier} from 'app/utils/access-tiers';
import {environment} from 'environments/environment';


interface TierProps {
  shortName: string;
  userHasAccess: boolean;
}
const Tier = (props: TierProps) => {
  const {shortName, userHasAccess} = props;
  const displayName = displayNameForTier(shortName);

  return environment.accessTiersVisibleToUsers.includes(shortName)
      ? <div style={styles.dataAccessTier}>
        {shortName === AccessTierShortNames.Registered
            ? <RegisteredTierBadge style={{gridArea: 'badge'}}/>
            : <ControlledTierBadge style={{gridArea: 'badge'}}/>}
        <div style={{...styles.inputLabel, gridArea: 'label'}}>{displayName}</div>
        {userHasAccess
            ? <CheckCircle
                data-test-id={`${shortName}-tier-access-granted`}
                style={{gridArea: 'available'}}
                color={colors.success}
                size={23}/>
            : <div data-test-id={`${shortName}-tier-access-denied`} style={{ ...styles.dataAccessText, gridArea: 'primary'}}>
              Please complete the data access requirements to gain access.
            </div>}
      </div>
      : null;
};

export interface DataAccessPanelProps {
  userAccessTiers: string[];
}
export const DataAccessPanel = (props: DataAccessPanelProps) => {
  const {userAccessTiers} = props;

  const orderedTiers = [
    AccessTierShortNames.Registered,
    AccessTierShortNames.Controlled
  ];

  const sectionId = useId();
  return <section aria-labelledby={sectionId} style={{marginLeft: '1rem'}}>
    <FlexRow id={sectionId}>
      <div style={styles.title}>Data access</div>
      <Link style={{marginLeft: 'auto'}} to='/data-access-requirements'>Manage data access</Link>
    </FlexRow>
    <hr style={{...styles.verticalLine}}/>
    {orderedTiers.map(tier =>
      <Tier
            key={tier}
            shortName={tier}
            userHasAccess={userAccessTiers.includes(tier)}/>)}
  </section>;
};
