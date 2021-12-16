import React, {useEffect, useState} from 'react';
import {useParams} from 'react-router-dom';
import validate from 'validate.js';

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
  ContactEmailTextInput,
  enableSave,
  getUpdatedProfileValue,
  updateAccountProperties,
  ErrorsTooltip,
} from './admin-user-common';
import {FadeBox} from 'app/components/containers';
import {WithSpinnerOverlayProps} from 'app/components/with-spinner-overlay';
import {MatchParams, serverConfigStore, useStore} from 'app/utils/stores';
import {CaretRight} from 'app/components/icons';
import {FlexColumn, FlexRow} from 'app/components/flex';
import {displayNameForTier} from 'app/utils/access-tiers';
import colors, {colorWithWhiteness} from 'app/styles/colors';
import {isBlank, reactStyles} from 'app/utils';
import {InstitutionalRole, Profile, PublicInstitutionDetails} from 'generated/fetch';
import {Button} from 'app/components/buttons';
import {checkInstitutionalEmail} from 'app/utils/institutions';

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
  oldProfile: Profile,
  updatedProfile: Profile,
  institutions?: PublicInstitutionDetails[],
  onChangeEmail: (contactEmail: string) => void,
}
const EditableFields = ({oldProfile, updatedProfile, institutions, onChangeEmail}: EditableFieldsProps) => {
  return <FlexRow style={styles.editableFields}>
    <FlexColumn>
      <div style={styles.subHeader}>Edit information</div>
      <FlexRow>
        <ContactEmailTextInput
          contactEmail={updatedProfile.contactEmail}
          onChange={email => onChangeEmail(email)}/>
        <InstitutionDropdown
          institutions={institutions}
          initialInstitutionShortName={updatedProfile.verifiedInstitutionalAffiliation?.institutionShortName}
          //onChange={async(event) => this.setVerifiedInstitutionOnProfile(event.value)}
          onChange={() => {}}/>
      </FlexRow>
      <FlexRow>
        <FreeCreditsDropdown
          initialLimit={oldProfile.freeTierDollarQuota}
          currentLimit={updatedProfile.freeTierDollarQuota}
          //onChange={async(event) => this.setFreeTierCreditDollarLimit(event.value)}
          onChange={() => {}}/>
        <InstitutionalRoleDropdown
          institutions={institutions}
          initialAffiliation={updatedProfile.verifiedInstitutionalAffiliation}
          //onChange={(event) => this.setInstitutionalRoleOnProfile(event.value)}
          onChange={() => {}}/>
        <InstitutionalRoleOtherTextInput
          affiliation={updatedProfile.verifiedInstitutionalAffiliation}
          // onChange={(value) => this.setState(
          //   fp.set(['updatedProfile', 'verifiedInstitutionalAffiliation', 'institutionalRoleOtherText'], value))
          // }
          onChange={() => {}}/>
      </FlexRow>
    </FlexColumn>
  </FlexRow>;
}

enum EmailValidationStatus {
  UNCHECKED,
  VALID,
  INVALID,
  API_ERROR
}

export const AdminUserProfile = (spinnerProps: WithSpinnerOverlayProps) => {
  const {config: {gsuiteDomain}} = useStore(serverConfigStore);
  const {usernameWithoutGsuiteDomain} = useParams<MatchParams>();
  const [oldProfile, setOldProfile] = useState<Profile>(null);
  const [updatedProfile, setUpdatedProfile] = useState<Profile>(null);
  const [institutions, setInstitutions] = useState<PublicInstitutionDetails[]>([]);
  const [emailValidationAborter, setEmailValidationAborter] = useState<AbortController>(null);
  const [emailValidationStatus, setEmailValidationStatus] = useState(EmailValidationStatus.UNCHECKED);

  useEffect(() => {
    const onMount = async() => {
      const p = await adminGetProfile(usernameWithoutGsuiteDomain + '@' + gsuiteDomain);
      setOldProfile(p);
      setUpdatedProfile(p);
      setInstitutions(await getPublicInstitutionDetails());
      spinnerProps.hideSpinner();
    }
    onMount();
  }, []);

  const validateAffiliation = async(contactEmail: string, institutionShortName: string) => {
    // clean up any currently-running validation
    if (emailValidationAborter) {
      emailValidationAborter.abort();
    }

    // clean up any previously-run validation
    setEmailValidationStatus(EmailValidationStatus.UNCHECKED);

    if (!isBlank(contactEmail) && !isBlank(institutionShortName)) {
      const aborter = new AbortController();
      setEmailValidationAborter(aborter);
      try {
        const result = await checkInstitutionalEmail(contactEmail, institutionShortName, aborter);
        setEmailValidationStatus(result?.isValidMember ? EmailValidationStatus.VALID : EmailValidationStatus.INVALID);
      } catch (e) {
        setEmailValidationStatus(EmailValidationStatus.API_ERROR);
      }
    }
  }

  const changed = (field): boolean => !!getUpdatedProfileValue(oldProfile, updatedProfile, field);

  useEffect(() => {
    const onProfileChange = async () => {
      const {contactEmail, verifiedInstitutionalAffiliation} = updatedProfile;
      if (changed('contactEmail') || changed('verifiedInstitutionalAffiliation')) {
        await validateAffiliation(contactEmail, verifiedInstitutionalAffiliation?.institutionShortName);
      }
    }

    if (updatedProfile) {
      onProfileChange();
    }
  }, [updatedProfile])

  const updateProfile = (newUpdates: Partial<Profile>) => {
    setUpdatedProfile({ ...updatedProfile, ...newUpdates});
  }

  const errors = validate({
    'contactEmail': !!updatedProfile?.contactEmail,
    'verifiedInstitutionalAffiliation': !!updatedProfile?.verifiedInstitutionalAffiliation,
    'institutionShortName': !!updatedProfile?.verifiedInstitutionalAffiliation?.institutionShortName,
    'institutionalRoleEnum': !!updatedProfile?.verifiedInstitutionalAffiliation?.institutionalRoleEnum,
    'institutionalRoleOtherText':
      !!(updatedProfile?.verifiedInstitutionalAffiliation?.institutionalRoleEnum !== InstitutionalRole.OTHER
        || updatedProfile?.verifiedInstitutionalAffiliation?.institutionalRoleOtherText),
    'institutionMembership':
      (emailValidationStatus === EmailValidationStatus.VALID || emailValidationStatus === EmailValidationStatus.UNCHECKED),
  }, {
    contactEmail: {truthiness: true},
    verifiedInstitutionalAffiliation: {truthiness: true},
    institutionShortName: {truthiness: true},
    institutionalRoleEnum: {truthiness: true},
    institutionalRoleOtherText: {truthiness: true},
    institutionMembership: {truthiness: true}
  });

  return updatedProfile && <FadeBox style={styles.fadeBox}>
    <FlexRow style={{alignItems: 'center'}}>
      <UserAdminTableLink/>
      <span style={styles.header}>User Profile Information</span>
      <UserAuditLink style={styles.auditLink} usernameWithoutDomain={usernameWithoutGsuiteDomain}>AUDIT <CaretRight/></UserAuditLink>
    </FlexRow>
    <FlexRow style={{paddingTop: '1em'}}>
      <UneditableFields profile={updatedProfile}/>
      <EditableFields
        oldProfile={oldProfile}
        updatedProfile={updatedProfile}
        institutions={institutions}
        onChangeEmail={(contactEmail) => updateProfile({contactEmail: contactEmail.trim()})}/>
    </FlexRow>
    <FlexRow style={{paddingTop: '1em'}}>
      <ErrorsTooltip errors={errors}>
        <Button
          type='primary'
          disabled={!enableSave(oldProfile, updatedProfile, errors)}
          onClick={async() => {
            spinnerProps.showSpinner();
            const response = await updateAccountProperties(oldProfile, updatedProfile);
            setOldProfile(response);
            setUpdatedProfile(response);
            spinnerProps.hideSpinner();
          }}
        >
          Save
      </Button>
      </ErrorsTooltip>
      <Button
        type='secondary'
        onClick={() => setUpdatedProfile(oldProfile)}
      >
        Cancel
      </Button>
    </FlexRow>
  </FadeBox>;
}
