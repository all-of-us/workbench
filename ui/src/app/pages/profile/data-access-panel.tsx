import * as fp from 'lodash/fp';
import * as React from 'react';

import {Link, StyledAnchorTag} from 'app/components/buttons';
import {FlexRow} from 'app/components/flex';
import {CheckCircle, ControlledTierBadge} from 'app/components/icons';
import {styles} from 'app/pages/profile/profile-styles';
import colors from 'app/styles/colors';
import * as Utils from 'app/utils';
import {AccessTierDisplayNames, AccessTierShortNames} from 'app/utils/access-tiers';
import {useNavigation} from 'app/utils/navigation';

const needsAgreementText = 'Contains expanded participant data, including genomics. Before you can access controlled tier data, ' +
  'your institution will need to sign an amended agreement with the All of Us Data and Research Center.';

const RegisteredTierSection = ({isInRegisteredTier = false}) => {
  return <div style={{
    marginBottom: '1rem',
    display: 'grid',
    gridTemplateColumns: 'fit-content(10rem) 1fr',
    gridTemplateAreas: `"regPrimary regAvailable"
                          "regSecondary regSecondary"`
  }}>
    <div style={{...styles.inputLabel, gridArea: 'regPrimary', marginRight: '0.5rem'}}>{AccessTierDisplayNames.Registered}</div>
    {isInRegisteredTier
      ? <CheckCircle style={{gridArea: 'regAvailable'}} color={colors.success} size={23}/>
      : <div style={{ ...styles.dataAccessText, gridArea: 'regSecondary'}}>
          Please complete data access requirements below to gain access to registered tier data.
        </div>
    }
  </div>;
};

const ControlledTierSection = ({ hasInstitutionalAgreement = false, isInControlledTier = false, userRevoked = false}) => {
  return <div style={{
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
    {Utils.cond<React.ReactElement>(
      // TODO: Remove update href and remove _blank target from Anchor tags
      [hasInstitutionalAgreement && userRevoked, () => <React.Fragment>
          <div style={{ ...styles.dataAccessText, gridArea: 'ctPrimary'}}>Access to controlled tier data is revoked.</div>
          <div style={{ ...styles.dataAccessText, gridArea: 'ctSecondary'}}>
            To gain access <StyledAnchorTag style={{textDecoration: 'underline'}}
              href='about:blank' target='_blank'>contact admin.</StyledAnchorTag>
          </div>
        </React.Fragment>],
      [hasInstitutionalAgreement && isInControlledTier,
        () => <CheckCircle style={{gridArea: 'ctAvailable'}}color={colors.success} size={23}/>],
      [hasInstitutionalAgreement, () => <React.Fragment>
          <div style={{ ...styles.dataAccessText, gridArea: 'ctPrimary'}}>You must complete the Controlled Tier Data Training.</div>
          <StyledAnchorTag style={{gridArea: 'ctSecondary', textDecoration: 'underline'}}
            href='about:blank' target='_blank'>Get Started</StyledAnchorTag>
        </React.Fragment>],
      () => <React.Fragment>
          <div style={{gridArea: 'ctPrimary', color: colors.primary}}>{needsAgreementText}</div>
          <StyledAnchorTag style={{gridArea: 'ctSecondary', textDecoration: 'underline'}}
            href='about:blank' target='_blank'>Learn More</StyledAnchorTag>
        </React.Fragment>
    )}
  </div>;
};

export const DataAccessPanel = ({
    tiers = [], hasInstitutionalAgreement = false, userRevoked = false
  }) => {
  const [navigate, ] = useNavigation();
  const sectionId = Utils.useId();
  return <section aria-labelledby={sectionId}>
    <FlexRow id={sectionId}>
      <div style={styles.title}>Data access</div>
      <Link style={{marginLeft: 'auto'}} onClick={() => navigate(['data-access-requirements'])}>Manage data access</Link>
    </FlexRow>
    <hr style={{...styles.verticalLine}}/>
    <RegisteredTierSection isInRegisteredTier={fp.some(v => v === AccessTierShortNames.Registered, tiers)}/>
    <ControlledTierSection
      hasInstitutionalAgreement={hasInstitutionalAgreement}
      isInControlledTier={fp.some(v => v === AccessTierShortNames.Controlled, tiers)}
      userRevoked={userRevoked}/>
  </section>;
};
