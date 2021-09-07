import * as React from 'react';

import {Link} from 'app/components/buttons';
import {FlexRow} from 'app/components/flex';
import {CheckCircle, ControlledTierBadge, RegisteredTierBadge} from 'app/components/icons';
import {styles} from 'app/pages/profile/profile-styles';
import colors from 'app/styles/colors';
import {useId} from 'app/utils';
import {AccessTierDisplayNames, AccessTierShortNames} from 'app/utils/access-tiers';
import {useNavigation} from 'app/utils/navigation';


interface TierProps {
  shortName: string;
  displayName: string;
  presentInEnvironment: boolean;
  userHasAccess: boolean;
}
const Tier = (props: TierProps) => {
  const {shortName, displayName, presentInEnvironment, userHasAccess} = props;

  return presentInEnvironment ? <div style={{
    marginBottom: '0.9rem',
    display: 'grid',
    columnGap: '0.25rem',
    width: 459,
    gridTemplateColumns: 'fit-content(2rem) fit-content(10rem) 1fr',
    gridTemplateAreas: `"badge label available"
                          ". primary primary"
                          ". rtSecondary rtSecondary"`
  }}>
    {shortName === AccessTierShortNames.Registered
        ? <RegisteredTierBadge style={{gridArea: 'badge'}}/>
            : <ControlledTierBadge style={{gridArea: 'badge'}}/>}
    <div style={{...styles.inputLabel, gridArea: 'label'}}>{displayName}</div>
    {userHasAccess
        ? <CheckCircle data-test-id={`${shortName}-tier-access-granted`} style={{gridArea: 'available'}} color={colors.success} size={23}/>
        : <div data-test-id={`${shortName}-tier-access-denied`} style={{ ...styles.dataAccessText, gridArea: 'primary'}}>
          Please complete the data access requirements to gain access to {shortName} tier data.
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

  const sectionId = useId();
  return <section aria-labelledby={sectionId} style={{marginLeft: '1rem'}}>
    <FlexRow id={sectionId}>
      <div style={styles.title}>Data access</div>
      <Link style={{marginLeft: 'auto'}} onClick={() => navigate(['data-access-requirements'])}>Manage data access</Link>
    </FlexRow>
    <hr style={{...styles.verticalLine}}/>
    {/*<RegisteredTierSection userHasAccess={fp.some(v => v === AccessTierShortNames.Registered, accessTierShortNames)}/>*/}
    {/*<ControlledTierSection userHasAccess={fp.some(v => v === AccessTierShortNames.Controlled, accessTierShortNames)}/>*/}
    <Tier
        shortName={AccessTierShortNames.Registered}
        displayName={AccessTierDisplayNames.Registered}
        presentInEnvironment={accessTiersInEnvironment.includes(AccessTierShortNames.Registered)}
        userHasAccess={userAccessTiers.includes(AccessTierShortNames.Registered)}/>
    <Tier
        shortName={AccessTierShortNames.Controlled}
        displayName={AccessTierDisplayNames.Controlled}
        presentInEnvironment={accessTiersInEnvironment.includes(AccessTierShortNames.Controlled)}
        userHasAccess={userAccessTiers.includes(AccessTierShortNames.Controlled)}/>
  </section>;
};
