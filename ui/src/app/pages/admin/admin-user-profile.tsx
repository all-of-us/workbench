import React, {useEffect, useState} from 'react';
import {useParams} from 'react-router-dom';

import {
  adminGetProfile,
  UserAdminTableLink,
  UserAuditLink,
  commonStyles,
  getFreeCreditUsage,
  FreeCreditsDropdown,
  InstitutionDropdown,
  InstitutionalRoleDropdown,
  InstitutionalRoleOtherTextInput,
  getPublicInstitutionDetails,
  ContactEmailTextInput
} from './admin-user-common';
import {FadeBox} from 'app/components/containers';
import {WithSpinnerOverlayProps} from 'app/components/with-spinner-overlay';
import {MatchParams} from 'app/utils/stores';
import {CaretRight} from 'app/components/icons';
import {FlexColumn, FlexRow} from 'app/components/flex';
import {displayNameForTier} from 'app/utils/access-tiers';
import colors, {colorWithWhiteness} from 'app/styles/colors';
import {reactStyles} from 'app/utils';
import {Profile, PublicInstitutionDetails} from 'generated/fetch';

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
  uneditableFieldsSpacer: {
    height: '24px',
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

const UneditableFields = (props: {profile: Profile}) => {
  const {givenName, familyName, username, accessTierShortNames} = props.profile;
  return <FlexRow style={styles.uneditableFields}>
    <FlexColumn>
      <div style={styles.subHeader}>Researcher information</div>
      <UneditableField label='Name' value={`${givenName} ${familyName}`}/>
      <UneditableField label='Free Credits Used' value={getFreeCreditUsage(props.profile)}/>
    </FlexColumn>
    <FlexColumn style={{paddingLeft: '80px'}}>
      <div style={styles.uneditableFieldsSpacer}/>
      <UneditableField label='User name' value={username}/>
      <UneditableField label='Access Tiers' value={accessTierShortNames.map(displayNameForTier).join(' ,')}/>
    </FlexColumn>
  </FlexRow>
}

interface EditableFieldsProps {
  profile: Profile,
  originalProfile: Profile,
  institutions?: PublicInstitutionDetails[],
}
const EditableFields = ({profile, originalProfile, institutions}: EditableFieldsProps) => {
  return <FlexRow style={styles.editableFields}>
    <FlexColumn>
      <div style={styles.subHeader}>Edit information</div>
      <FlexRow>
        <ContactEmailTextInput
          contactEmail={profile.contactEmail}
          //onChange={email => this.setContactEmail(email)}
          onChange={() => {}}/>
        <InstitutionDropdown
          institutions={institutions}
          initialInstitutionShortName={profile.verifiedInstitutionalAffiliation?.institutionShortName}
          //onChange={async(event) => this.setVerifiedInstitutionOnProfile(event.value)}
          onChange={() => {}}/>
      </FlexRow>
      <FlexRow>
        <FreeCreditsDropdown
          initialLimit={originalProfile.freeTierDollarQuota}
          currentLimit={profile.freeTierDollarQuota}
          //onChange={async(event) => this.setFreeTierCreditDollarLimit(event.value)}
          onChange={() => {}}/>
        <InstitutionalRoleDropdown
          institutions={institutions}
          initialAffiliation={profile.verifiedInstitutionalAffiliation}
          //onChange={(event) => this.setInstitutionalRoleOnProfile(event.value)}
          onChange={() => {}}/>
        <InstitutionalRoleOtherTextInput
          affiliation={profile.verifiedInstitutionalAffiliation}
          // onChange={(value) => this.setState(
          //   fp.set(['updatedProfile', 'verifiedInstitutionalAffiliation', 'institutionalRoleOtherText'], value))
          // }
          onChange={() => {}}/>
      </FlexRow>
    </FlexColumn>
  </FlexRow>;
}

export const AdminUserProfile = (spinnerProps: WithSpinnerOverlayProps) => {
  const {usernameWithoutGsuiteDomain} = useParams<MatchParams>();
  const [profile, setProfile] = useState(null);
  const [originalProfile, setOriginalProfile] = useState(null);
  const [institutions, setInstitutions] = useState([]);

  useEffect(() => {
    const onMount = async() => {
      const p = await adminGetProfile(usernameWithoutGsuiteDomain);
      setOriginalProfile(p);
      setProfile(p);
      setInstitutions(await getPublicInstitutionDetails());
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
      <EditableFields profile={profile} originalProfile={originalProfile} institutions={institutions}/>
    </FlexRow>
  </FadeBox>;
}
