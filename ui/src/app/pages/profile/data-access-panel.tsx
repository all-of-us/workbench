import * as fp from 'lodash/fp';
import * as React from 'react';

import {Link} from 'app/components/buttons';
import {FlexRow} from 'app/components/flex';
import {CheckCircle, ControlledTierBadge, RegisteredTierBadge} from 'app/components/icons';
import {styles} from 'app/pages/profile/profile-styles';
import colors from 'app/styles/colors';
import {useId} from 'app/utils';
import {AccessTierDisplayNames, AccessTierShortNames} from 'app/utils/access-tiers';
import {isTierPresentInEnvironment} from 'app/utils/access-utils';
import {useNavigation} from 'app/utils/navigation';

interface TierProps {
  userHasAccess: boolean;
}
const RegisteredTierSection = (props: TierProps) => {
  const {userHasAccess} = props;

  return isTierPresentInEnvironment(AccessTierShortNames.Registered) ? <div style={{
    marginBottom: '0.9rem',
    display: 'grid',
    columnGap: '0.25rem',
    width: 459,
    gridTemplateColumns: 'fit-content(2rem) fit-content(10rem) 1fr',
    gridTemplateAreas: `"rtBadge rtLabel rtAvailable"
                          ". rtPrimary rtPrimary"
                          ". rtSecondary rtSecondary"`
  }}>
    <RegisteredTierBadge style={{gridArea: 'rtBadge'}}/>
    <div style={{...styles.inputLabel, gridArea: 'rtLabel'}}>{AccessTierDisplayNames.Registered}</div>
    {userHasAccess
      ? <CheckCircle style={{gridArea: 'rtAvailable'}} color={colors.success} size={23}/>
      : <div style={{ ...styles.dataAccessText, gridArea: 'rtPrimary'}}>
          Please complete the data access requirements to gain access to registered tier data.
        </div>
    }
  </div> : null;
};

const ControlledTierSection = (props: TierProps) => {
  const {userHasAccess} = props;

  return isTierPresentInEnvironment(AccessTierShortNames.Controlled) ? <div style={{
    marginBottom: '0.9rem',
    display: 'grid',
    columnGap: '0.25rem',
    width: 459,
    gridTemplateColumns: 'fit-content(2rem) fit-content(10rem) 1fr',
    gridTemplateAreas: `"ctBadge ctLabel ctAvailable"
                          ". ctPrimary ctPrimary"
                          ". ctSecondary ctSecondary"`
  }}>
    <ControlledTierBadge style={{gridArea: 'ctBadge'}}/>
    <div style={{...styles.inputLabel, gridArea: 'ctLabel'}}>{AccessTierDisplayNames.Controlled}</div>
    {userHasAccess
        ? <CheckCircle style={{gridArea: 'ctAvailable'}} color={colors.success} size={23}/>
        : <div style={{...styles.dataAccessText, gridArea: 'ctPrimary'}}>
          Please complete the data access requirements to gain access to controlled tier data.
        </div>
    }
  </div> : null;
};

export interface DataAccessPanelProps {
  accessTierShortNames: string[];
}
export const DataAccessPanel = (props: DataAccessPanelProps) => {
  const {accessTierShortNames} = props;

  const [navigate, ] = useNavigation();

  const sectionId = useId();
  return <section aria-labelledby={sectionId} style={{marginLeft: '1rem'}}>
    <FlexRow id={sectionId}>
      <div style={styles.title}>Data access</div>
      <Link style={{marginLeft: 'auto'}} onClick={() => navigate(['data-access-requirements'])}>Manage data access</Link>
    </FlexRow>
    <hr style={{...styles.verticalLine}}/>
    <RegisteredTierSection userHasAccess={fp.some(v => v === AccessTierShortNames.Registered, accessTierShortNames)}/>
    <ControlledTierSection userHasAccess={fp.some(v => v === AccessTierShortNames.Controlled, accessTierShortNames)}/>
  </section>;
};
