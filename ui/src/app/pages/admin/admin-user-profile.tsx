import React, {useEffect, useState} from 'react';
import {useParams} from 'react-router-dom';

import {adminGetProfile, UserAdminTableLink, UserAuditLink, commonStyles, getFreeCreditUsage} from './admin-user-common';
import {FadeBox} from 'app/components/containers';
import {WithSpinnerOverlayProps} from 'app/components/with-spinner-overlay';
import {MatchParams} from 'app/utils/stores';
import {CaretRight} from 'app/components/icons';
import {FlexColumn, FlexRow} from 'app/components/flex';
import {displayNameForTier} from 'app/utils/access-tiers';
import colors, {colorWithWhiteness} from 'app/styles/colors';
import {reactStyles} from 'app/utils';
import {Profile} from 'generated/fetch';

const styles = reactStyles({
  ...commonStyles,
  header: {
    color: colors.primary,
    fontSize: '18px',
    fontWeight: 600,
    padding: '1em',
  },
  subHeader: {
    color: colors.primary,
    fontSize: '16px',
    fontWeight: 'bold',
    paddingLeft: '1em',
  },
  spacer: {
    height: '24px',
  },
  label: {
    color: colors.primary,
    fontSize: '14px',
    fontWeight: 'bold',
    paddingLeft: '1em',
    paddingTop: '1em',
  },
  value: {
    color: colors.primary,
    fontSize: '14px',
    paddingLeft: '1em',
  },
  auditLink: {
    color: colors.accent,
    fontSize: '16px',
    fontWeight: 500,
  },
  uneditableFields: {
    height: '221px',
    width: '601px',
    borderRadius: '9px',
    backgroundColor: colorWithWhiteness(colors.light, 0.23),
    paddingTop: '1em',
  },
  editableFields: {
    height: '221px',
    width: '601px',
    paddingTop: '1em',
  },
});

const UneditableField = (props: {label: string, value: string}) => <FlexColumn>
  <div style={styles.label}>{props.label}</div>
  <div style={styles.value}>{props.value}</div>
</FlexColumn>

const Spacer = () => <div style={styles.spacer}/>

const UneditableFields = (props: {profile: Profile}) => {
  const {givenName, familyName, username, accessTierShortNames} = props.profile;
  return <FlexRow style={styles.uneditableFields}>
    <FlexColumn>
      <div style={styles.subHeader}>Researcher information</div>
      <UneditableField label='Name' value={`${givenName} ${familyName}`}/>
      <UneditableField label='Free Credits Used' value={getFreeCreditUsage(props.profile)}/>
    </FlexColumn>
    <FlexColumn style={{paddingLeft: '80px'}}>
      <Spacer/>
      <UneditableField label='User name' value={username}/>
      <UneditableField label='Access Tiers' value={accessTierShortNames.map(displayNameForTier).join(' ,')}/>
    </FlexColumn>
  </FlexRow>
}

export const AdminUserProfile = (spinnerProps: WithSpinnerOverlayProps) => {
  const {usernameWithoutGsuiteDomain} = useParams<MatchParams>();
  const [profile, setProfile] = useState(null);

  useEffect(() => {
    const onMount = async() => {
      setProfile(await adminGetProfile(usernameWithoutGsuiteDomain));
      spinnerProps.hideSpinner();
    }
    onMount();
  }, []);

  return profile && <FadeBox style={styles.fadeBox}>
    <FlexRow style={{alignItems: 'center'}}>
      <UserAdminTableLink/>
      <span style={styles.header}>User Profile Information</span>
      <UserAuditLink style={styles.auditLink} usernameWithoutDomain={usernameWithoutGsuiteDomain}>AUDIT <CaretRight/></UserAuditLink>
    </FlexRow>
    <FlexRow style={{paddingTop: '1em'}}>
      <UneditableFields profile={profile}/>
      <FlexColumn style={styles.editableFields}>
        <div style={styles.subHeader}>Edit information</div>
      </FlexColumn>
    </FlexRow>
  </FadeBox>;
}
