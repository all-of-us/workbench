import * as Utils from 'app/utils'
import * as fp from 'lodash/fp';
import * as React from 'react';
import {StyledAnchorTag} from 'app/components/buttons';
import {styles} from 'app/pages/profile/profile-styles'
import {CheckCircle, ControlledTierBadge} from 'app/components/icons'
import colors from 'app/styles/colors';

const needsAgreementText = 'Contains expanded participant data, including genomics. Before you can access controlled tier data, ' + 
  'your institution will need to sign an amended agreement with the All of Us Data and Research Center.'

const RegisteredTierSection = ({isInRegisteredTier = false}) => {
  return <div style={{display: 'flex'}}>
    <div style={{...styles.inputLabel, marginRight: '0.5rem'}}>Registered tier</div>
    {isInRegisteredTier && <CheckCircle color={colors.success} size={23}/>} 
  </div>
}

const ControlledTierSection = ({ hasInstitutionalAgreement = false, isInControlledTier = false, userRevoked = false}) => {
  return <div style={{
      marginBottom: '0.9rem',
      display: 'grid', 
      columnGap: '0.25rem',
      width: 459,
      gridTemplateColumns: '1rem 4.5rem auto',
      gridTemplateAreas: `"ctBadge ctLabel ctAvailable"
                          ". ctPrimary ctPrimary"
                          ". ctSecondary ctSecondary"`
    }}>
    <ControlledTierBadge style={{gridArea: 'ctBadge'}}/>
    <div style={{...styles.inputLabel, gridArea: 'ctLabel'}}>Controlled tier</div>


    {Utils.cond<React.ReactElement>(
      [hasInstitutionalAgreement && userRevoked, () => <React.Fragment>
          <div style={{gridArea: 'ctPrimary', fontWeight: 500, color: colors.primary}}>Access to controlled tier data is revoked.</div>
          <div style={{gridArea: 'ctSecondary', fontWeight: 500, color: colors.primary}}>
            To gain access <StyledAnchorTag style={{textDecoration: 'underline'}} href="">contact admin.</StyledAnchorTag>
          </div>
        </React.Fragment>],
      [hasInstitutionalAgreement && isInControlledTier, () => <CheckCircle style={{gridArea: 'ctAvailable'}}color={colors.success} size={23}/>],
      [hasInstitutionalAgreement, () => <React.Fragment>
          <div style={{gridArea:'ctPrimary', fontWeight: 500, color: colors.primary}}>You must complete the Controlled Tier Data Training.</div>
          <StyledAnchorTag style={{gridArea:'ctSecondary', textDecoration: 'underline'}} href="">Get Started</StyledAnchorTag>
        </React.Fragment>],
      () => <React.Fragment>
          <div style={{gridArea:'ctPrimary', color: colors.primary}}>{needsAgreementText}</div>
          <StyledAnchorTag style={{gridArea:'ctSecondary', textDecoration: 'underline'}} href="">Learn More</StyledAnchorTag>
        </React.Fragment>
    )}
  </div>
}

export const DataAccessPanel = ({
    tiers = [], hasInstitutionalAgreement = false, userRevoked = false
  }) => {
  const sectionId = Utils.useId()
  return <section aria-labelledby={sectionId}>
    <div id={sectionId} style={styles.title}>Data access</div>
    <hr style={{...styles.verticalLine}}/>
    <RegisteredTierSection isInRegisteredTier={fp.some(v => v === 'registered', tiers)}/>
    <ControlledTierSection 
      hasInstitutionalAgreement={hasInstitutionalAgreement} 
      isInControlledTier={fp.some(v => v === 'controlled', tiers)} 
      userRevoked={userRevoked}/>
  </section>
}