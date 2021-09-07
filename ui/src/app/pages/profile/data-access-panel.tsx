import * as React from 'react';

import {Link} from 'app/components/buttons';
import {FlexRow} from 'app/components/flex';
import {CheckCircle, ControlledTierBadge, RegisteredTierBadge} from 'app/components/icons';
import {styles} from 'app/pages/profile/profile-styles';
import colors from 'app/styles/colors';
import {useId} from 'app/utils';
import {AccessTierShortNames, displayNameForTier} from 'app/utils/access-tiers';
import {useNavigation} from 'app/utils/navigation';


interface TierProps {
  shortName: string;
  presentInEnvironment: boolean;
  userHasAccess: boolean;
}
const Tier = (props: TierProps) => {
  const {shortName, presentInEnvironment, userHasAccess} = props;
  const displayName = displayNameForTier(shortName);

  return presentInEnvironment ? <div style={{
    marginBottom: '0.9rem',
    display: 'grid',
    columnGap: '0.25rem',
    width: 459,
    gridTemplateColumns: 'fit-content(2rem) fit-content(10rem) 1fr',
    gridTemplateAreas: `"badge label available"
                          ". primary primary"`
  }}>
    {shortName === AccessTierShortNames.Registered
        ? <RegisteredTierBadge style={{gridArea: 'badge'}}/>
            : <ControlledTierBadge style={{gridArea: 'badge'}}/>}
    <div style={{...styles.inputLabel, gridArea: 'label'}}>{displayName}</div>
    {userHasAccess
        ? <CheckCircle data-test-id={`${shortName}-tier-access-granted`} style={{gridArea: 'available'}} color={colors.success} size={23}/>
        : <div data-test-id={`${shortName}-tier-access-denied`} style={{ ...styles.dataAccessText, gridArea: 'primary'}}>
          Please complete the data access requirements to gain access.
        </div>
    }
  </div> : null;
};

export interface DataAccessPanelProps {
  accessTiersInEnvironment: string[];
  userAccessTiers: string[];
}
export const DataAccessPanel = (props: DataAccessPanelProps) => {
  const {accessTiersInEnvironment, userAccessTiers} = props;

  const [navigate, ] = useNavigation();

  const orderedTiers = [
    AccessTierShortNames.Registered,
    AccessTierShortNames.Controlled
  ];

  const sectionId = useId();
  return <section aria-labelledby={sectionId} style={{marginLeft: '1rem'}}>
    <FlexRow id={sectionId}>
      <div style={styles.title}>Data access</div>
      <Link style={{marginLeft: 'auto'}} onClick={() => navigate(['data-access-requirements'])}>Manage data access</Link>
    </FlexRow>
    <hr style={{...styles.verticalLine}}/>
    {orderedTiers.map(tier =>
        <Tier
            key={tier}
            shortName={tier}
            presentInEnvironment={accessTiersInEnvironment.includes(tier)}
            userHasAccess={userAccessTiers.includes(tier)}/>)}
  </section>;
};
