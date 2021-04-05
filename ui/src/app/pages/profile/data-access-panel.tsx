import {StyledAnchorTag} from 'app/components/buttons';
import {styles} from 'app/pages/profile/profile-styles'
import {CheckCircle, ControlledTierBadge} from 'app/components/icons'
import colors from 'app/styles/colors';
import * as React from 'react';

const needsAgreementText = 'Contains expanded participant data, including genomics. Before you can access controlled tier data, ' + 
  'your institution will need to sign an amended agreement with the All of Us Data and Research Center.'

const RegisteredTierSection = () => {
  return <div style={{display: 'flex'}}>
    <div style={{...styles.inputLabel, marginRight: '0.5rem'}}>Registered tier</div>
    <CheckCircle color={colors.success} size={23}/>
  </div>
}

const ControlledTierSection = ({ctAvailable = false, hasInstitutionalAgreement = false}) => {
  return <div style={{
      marginBottom: '0.9rem',
      display: 'grid', 
      columnGap: '0.5rem',
      width: 459,
      gridTemplateColumns: 'auto auto auto',
      gridTemplateAreas: `"ctBadge ctLabel ctAvailable"
                          ". ctPrimary ctPrimary"
                          ". ctSecondary ctSecondary"`
    }}>
    <ControlledTierBadge style={{gridArea: 'ctBadge'}}/>
    <div style={{...styles.inputLabel, gridArea: 'ctLabel'}}>Controlled tier</div>
    {ctAvailable && <CheckCircle color={colors.success} size={23}/>}
    {!!hasInstitutionalAgreement && <div>Get Started</div>}
    {!hasInstitutionalAgreement && <div style={{gridArea:'ctPrimary'}}>{needsAgreementText}</div>}
  </div>
}

/*
1. Registered Tier State: Allowed, Incomplete
2. Controlled Tier State: Institutional Access -> User Training -> Allowed -> Revoked
*/
export const DataAccessPanel = () => {
  return <div>
    <div style={styles.title}>Data access</div>
    <hr style={{...styles.verticalLine}}/>
    <RegisteredTierSection/>
    <ControlledTierSection/>
  </div>
  
}