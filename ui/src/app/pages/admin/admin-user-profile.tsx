import React, { useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
import * as fp from 'lodash/fp';
import { Column } from 'primereact/column';
import { DataTable } from 'primereact/datatable';
import validate from 'validate.js';

import {
  AccessBypassRequest,
  AccessModule,
  InstitutionalRole,
  Profile,
  PublicInstitutionDetails,
  VerifiedInstitutionalAffiliation,
} from 'generated/fetch';

import { AlertDanger } from 'app/components/alert';
import { Button } from 'app/components/buttons';
import { FadeBox } from 'app/components/containers';
import { FlexColumn, FlexRow, FlexSpacer } from 'app/components/flex';
import { CaretRight, ClrIcon } from 'app/components/icons';
import { Toggle } from 'app/components/inputs';
import { WithSpinnerOverlayProps } from 'app/components/with-spinner-overlay';
import colors, { colorWithWhiteness } from 'app/styles/colors';
import { isBlank, reactStyles } from 'app/utils';
import { displayNameForTier } from 'app/utils/access-tiers';
import { getAccessModuleConfig } from 'app/utils/access-utils';
import {
  checkInstitutionalEmail,
  getEmailValidationErrorMessage,
} from 'app/utils/institutions';
import { MatchParams, serverConfigStore, useStore } from 'app/utils/stores';

import {
  adminGetProfile,
  commonStyles,
  ContactEmailTextInput,
  displayModuleCompletionDate,
  displayModuleExpirationDate,
  displayModuleStatus,
  displayTierBadgeByRequiredModule,
  ErrorsTooltip,
  getEraNote,
  getInitalCreditsUsage,
  getPublicInstitutionDetails,
  InitialCreditsDropdown,
  InstitutionalRoleDropdown,
  InstitutionalRoleOtherTextInput,
  InstitutionDropdown,
  isBypassed,
  profileNeedsUpdate,
  updateAccountProperties,
  UserAdminTableLink,
  UserAuditLink,
} from './admin-user-common';
import { EgressEventsTable } from './egress-events-table';

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
  tableHeader: {
    color: colors.primary,
    fontSize: '18px',
    fontWeight: 'bold',
    paddingTop: '2em',
    lineHeight: '22px',
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
    marginRight: '20px',
  },
  editableFields: {
    width: '601px',
    paddingTop: '1em',
  },
});

const UneditableField = (props: {
  label: string;
  value: string | JSX.Element;
}) => (
  <FlexColumn>
    <div style={styles.label}>{props.label}</div>
    <div style={styles.value}>{props.value}</div>
  </FlexColumn>
);

const UneditableFields = (props: { profile: Profile }) => {
  const { givenName, familyName, username, accessTierShortNames } =
    props.profile;
  const accessTiers =
    accessTierShortNames?.length === 0 ? (
      <span>
        <i>No data access</i>
      </span>
    ) : (
      accessTierShortNames.map(displayNameForTier).join(', ')
    );
  return (
    <FlexRow style={styles.uneditableFields}>
      <FlexColumn>
        <div style={styles.subHeader}>Researcher information</div>
        <UneditableField label='Name' value={`${givenName} ${familyName}`} />
        <UneditableField
          label='Initial Credits Used'
          value={getInitalCreditsUsage(props.profile)}
        />
      </FlexColumn>
      <FlexColumn style={{ paddingLeft: '80px' }}>
        <div style={styles.uneditableFieldsSpacer} />
        <UneditableField label='User name' value={username} />
        <UneditableField label='Data Access Tiers' value={accessTiers} />
      </FlexColumn>
    </FlexRow>
  );
};

enum EmailValidationStatus {
  UNCHECKED,
  VALID,
  INVALID,
  API_ERROR,
}

interface EditableFieldsProps {
  oldProfile: Profile;
  updatedProfile: Profile;
  institutions?: PublicInstitutionDetails[];
  emailValidationStatus: EmailValidationStatus;
  onChangeEmail: (contactEmail: string) => void;
  onChangeInitialCreditsLimit: (limit: number) => void;
  onChangeInstitution: (institutionShortName: string) => void;
  onChangeInstitutionalRole: (institutionalRoleEnum: InstitutionalRole) => void;
  onChangeInstitutionOtherText: (otherText: string) => void;
}

const EditableFields = ({
  oldProfile,
  updatedProfile,
  institutions,
  emailValidationStatus,
  onChangeEmail,
  onChangeInitialCreditsLimit,
  onChangeInstitution,
  onChangeInstitutionalRole,
  onChangeInstitutionOtherText,
}: EditableFieldsProps) => {
  const institution: PublicInstitutionDetails = institutions.find(
    (i) =>
      i.shortName ===
      updatedProfile?.verifiedInstitutionalAffiliation?.institutionShortName
  );
  return (
    <FlexRow style={styles.editableFields}>
      <FlexColumn>
        <div style={styles.subHeader}>Edit information</div>
        <FlexRow>
          <ContactEmailTextInput
            contactEmail={updatedProfile.contactEmail}
            previousContactEmail={oldProfile.contactEmail}
            highlightOnChange
            onChange={(email) => onChangeEmail(email)}
          />
          <InstitutionDropdown
            institutions={institutions}
            currentInstitutionShortName={
              updatedProfile.verifiedInstitutionalAffiliation
                ?.institutionShortName
            }
            previousInstitutionShortName={
              oldProfile.verifiedInstitutionalAffiliation?.institutionShortName
            }
            highlightOnChange
            onChange={(event) => onChangeInstitution(event.value)}
          />
        </FlexRow>
        {emailValidationStatus === EmailValidationStatus.INVALID && (
          <div style={{ paddingLeft: '1em' }}>
            {getEmailValidationErrorMessage(institution)}
          </div>
        )}
        <FlexRow>
          <InitialCreditsDropdown
            currentLimit={updatedProfile.freeTierDollarQuota}
            previousLimit={oldProfile.freeTierDollarQuota}
            highlightOnChange
            onChange={(event) => onChangeInitialCreditsLimit(event.value)}
          />
          <InstitutionalRoleDropdown
            institutions={institutions}
            currentAffiliation={updatedProfile.verifiedInstitutionalAffiliation}
            previousRole={
              oldProfile.verifiedInstitutionalAffiliation?.institutionalRoleEnum
            }
            highlightOnChange
            onChange={(event) => onChangeInstitutionalRole(event.value)}
          />
        </FlexRow>
        <FlexRow>
          <FlexSpacer />
          <InstitutionalRoleOtherTextInput
            affiliation={updatedProfile.verifiedInstitutionalAffiliation}
            previousOtherText={
              oldProfile.verifiedInstitutionalAffiliation
                ?.institutionalRoleOtherText
            }
            highlightOnChange
            onChange={(value) => onChangeInstitutionOtherText(value)}
          />
        </FlexRow>
      </FlexColumn>
    </FlexRow>
  );
};

// list the access modules in the desired order
const accessModulesForTable = [
  AccessModule.TWOFACTORAUTH,
  AccessModule.ERACOMMONS,
  AccessModule.COMPLIANCETRAINING,
  AccessModule.CTCOMPLIANCETRAINING,
  AccessModule.DATAUSERCODEOFCONDUCT,
  AccessModule.RASLINKLOGINGOV,
  AccessModule.PROFILECONFIRMATION,
  AccessModule.PUBLICATIONCONFIRMATION,
];

interface AccessModuleTableProps {
  oldProfile: Profile;
  updatedProfile: Profile;
  pendingBypassRequests: AccessBypassRequest[];
  bypassUpdate: (accessBypassRequest: AccessBypassRequest) => void;
}

interface ToggleProps extends AccessModuleTableProps {
  moduleName: AccessModule;
}

const ToggleForModule = (props: ToggleProps) => {
  const {
    moduleName,
    oldProfile,
    updatedProfile,
    pendingBypassRequests,
    bypassUpdate,
  } = props;

  const previouslyBypassed = isBypassed(oldProfile, moduleName);
  const pendingBypassState = pendingBypassRequests.find(
    (r) => r.moduleName === moduleName
  );
  const isModuleBypassed = pendingBypassState
    ? pendingBypassState.isBypassed
    : isBypassed(updatedProfile, moduleName);
  const highlightStyle =
    isModuleBypassed !== previouslyBypassed
      ? { background: colors.highlight }
      : {};

  return (
    <div style={highlightStyle}>
      <Toggle
        name=' '
        style={{ paddingBottom: 0, flexGrow: 0 }}
        checked={isModuleBypassed}
        data-test-id={`${moduleName}-toggle`}
        onToggle={() =>
          bypassUpdate({ moduleName, isBypassed: !isModuleBypassed })
        }
      />
    </div>
  );
};

interface TableRow {
  moduleName: string;
  moduleStatus: JSX.Element;
  completionDate: JSX.Element;
  expirationDate: JSX.Element;
  bypassToggle: JSX.Element;
}

const AccessModuleTable = (props: AccessModuleTableProps) => {
  const { updatedProfile } = props;

  const tableData: TableRow[] = fp.flatMap((moduleName) => {
    const { adminPageTitle, bypassable, isEnabledInEnvironment } =
      getAccessModuleConfig(moduleName);

    return isEnabledInEnvironment
      ? [
          {
            moduleName: adminPageTitle,
            moduleStatus: displayModuleStatus(props.updatedProfile, moduleName),
            completionDate: displayModuleCompletionDate(
              updatedProfile,
              moduleName
            ),
            expirationDate: displayModuleExpirationDate(
              updatedProfile,
              moduleName
            ),
            accessTierBadge: displayTierBadgeByRequiredModule(
              props.updatedProfile,
              moduleName
            ),
            bypassToggle: bypassable && (
              <ToggleForModule moduleName={moduleName} {...props} />
            ),
          },
        ]
      : [];
  }, accessModulesForTable);

  return (
    <DataTable
      rowHover
      style={{ paddingTop: '1em' }}
      value={tableData}
      footer={
        getAccessModuleConfig(AccessModule.ERACOMMONS)
          .isEnabledInEnvironment && (
          <div style={{ textAlign: 'left', fontWeight: 'normal' }}>
            {getEraNote(updatedProfile)}
          </div>
        )
      }
    >
      <Column field='moduleName' header='Access Module' />
      <Column field='moduleStatus' header='Status' />
      <Column field='completionDate' header='Last completed on' />
      <Column field='expirationDate' header='Expires on' />
      <Column field='accessTierBadge' header='Required for tier access' />
      <Column field='bypassToggle' header='Bypass' />
    </DataTable>
  );
};

const DisabledToggle = (props: {
  currentlyDisabled: boolean;
  previouslyDisabled: boolean;
  toggleDisabled: () => void;
}) => {
  const { currentlyDisabled, previouslyDisabled, toggleDisabled } = props;
  const highlightStyle =
    currentlyDisabled !== previouslyDisabled
      ? { background: colors.highlight }
      : {};

  return (
    <div style={highlightStyle}>
      <Toggle
        style={{ paddingTop: '2em', paddingLeft: '2em', flexGrow: 0 }}
        name={currentlyDisabled ? 'Account disabled' : 'Account enabled'}
        checked={!currentlyDisabled}
        onToggle={() => toggleDisabled()}
      />
    </div>
  );
};

export const AdminUserProfile = (spinnerProps: WithSpinnerOverlayProps) => {
  const {
    config: { gsuiteDomain },
  } = useStore(serverConfigStore);
  const { usernameWithoutGsuiteDomain } = useParams<MatchParams>();
  const [oldProfile, setOldProfile] = useState<Profile>(null);
  const [updatedProfile, setUpdatedProfile] = useState<Profile>(null);
  const [bypassChangeRequests, setBypassChangeRequests] = useState<
    AccessBypassRequest[]
  >([]);
  const [institutions, setInstitutions] = useState<PublicInstitutionDetails[]>(
    []
  );
  const [emailValidationAborter, setEmailValidationAborter] =
    useState<AbortController>(null);
  const [emailValidationStatus, setEmailValidationStatus] = useState(
    EmailValidationStatus.UNCHECKED
  );
  const [profileLoadingError, setProfileLoadingError] = useState<string>(null);
  const [institutionsLoadingError, setInstitutionsLoadingError] =
    useState<string>(null);

  useEffect(() => {
    const onMount = async () => {
      try {
        const p = await adminGetProfile(
          usernameWithoutGsuiteDomain + '@' + gsuiteDomain
        );
        setOldProfile(p);
        setUpdatedProfile(p);
        setProfileLoadingError(null);
      } catch (error) {
        setProfileLoadingError(
          'Could not find user - please check spelling of username and try again'
        );
      }
      try {
        setInstitutions(await getPublicInstitutionDetails());
        setInstitutionsLoadingError(null);
      } catch (error) {
        setInstitutionsLoadingError(
          'Could not get list of verified institutions - please try again later'
        );
      }
      spinnerProps.hideSpinner();
    };
    onMount();
  }, []);

  // clean up any currently-running or previously-run validation
  const clearEmailValidation = () => {
    if (emailValidationAborter) {
      emailValidationAborter.abort();
    }

    // clean up any previously-run validation
    setEmailValidationStatus(EmailValidationStatus.UNCHECKED);
  };

  const validateAffiliation = async (
    contactEmail: string,
    institutionShortName: string
  ) => {
    clearEmailValidation();

    if (!isBlank(contactEmail) && !isBlank(institutionShortName)) {
      const aborter = new AbortController();
      setEmailValidationAborter(aborter);
      try {
        const result = await checkInstitutionalEmail(
          contactEmail,
          institutionShortName,
          aborter
        );
        setEmailValidationStatus(
          result?.isValidMember
            ? EmailValidationStatus.VALID
            : EmailValidationStatus.INVALID
        );
      } catch (e) {
        setEmailValidationStatus(EmailValidationStatus.API_ERROR);
      }
    }
  };

  const updateProfile = (newUpdates: Partial<Profile>) => {
    setUpdatedProfile({ ...updatedProfile, ...newUpdates });
  };

  const updateContactEmail = async (contactEmail: string) => {
    updateProfile({ contactEmail });
    await validateAffiliation(
      contactEmail.trim(),
      updatedProfile.verifiedInstitutionalAffiliation?.institutionShortName
    );
  };

  const updateInstitution = async (institutionShortName: string) => {
    await validateAffiliation(
      updatedProfile.contactEmail,
      institutionShortName
    );

    const verifiedInstitutionalAffiliation: VerifiedInstitutionalAffiliation = {
      institutionShortName,
      institutionDisplayName: institutions.find(
        (i) => i.shortName === institutionShortName
      )?.displayName,
      institutionalRoleEnum: undefined,
      institutionalRoleOtherText: undefined,
    };
    updateProfile({ verifiedInstitutionalAffiliation });
  };

  const updateInstitutionalRole = (
    institutionalRoleEnum: InstitutionalRole
  ) => {
    const verifiedInstitutionalAffiliation: VerifiedInstitutionalAffiliation = {
      ...updatedProfile.verifiedInstitutionalAffiliation,
      institutionalRoleEnum,
      institutionalRoleOtherText: undefined,
    };
    updateProfile({ verifiedInstitutionalAffiliation });
  };

  const updateInstitutionalRoleOtherText = (
    institutionalRoleOtherText: string
  ) => {
    const verifiedInstitutionalAffiliation: VerifiedInstitutionalAffiliation = {
      ...updatedProfile.verifiedInstitutionalAffiliation,
      institutionalRoleOtherText,
    };
    updateProfile({ verifiedInstitutionalAffiliation });
  };

  const updateModuleBypassStatus = (
    accessBypassRequest: AccessBypassRequest
  ) => {
    const otherModuleRequests = bypassChangeRequests.filter(
      (r) => r.moduleName !== accessBypassRequest.moduleName
    );
    setBypassChangeRequests([...otherModuleRequests, accessBypassRequest]);
  };

  const errors = validate(
    {
      contactEmail: !isBlank(updatedProfile?.contactEmail),
      verifiedInstitutionalAffiliation:
        !!updatedProfile?.verifiedInstitutionalAffiliation,
      institutionShortName:
        !!updatedProfile?.verifiedInstitutionalAffiliation
          ?.institutionShortName,
      institutionalRoleEnum:
        !!updatedProfile?.verifiedInstitutionalAffiliation
          ?.institutionalRoleEnum,
      institutionalRoleOtherText: !!(
        updatedProfile?.verifiedInstitutionalAffiliation
          ?.institutionalRoleEnum !== InstitutionalRole.OTHER ||
        updatedProfile?.verifiedInstitutionalAffiliation
          ?.institutionalRoleOtherText
      ),
      institutionMembership:
        emailValidationStatus === EmailValidationStatus.VALID ||
        emailValidationStatus === EmailValidationStatus.UNCHECKED,
    },
    {
      contactEmail: { truthiness: true },
      verifiedInstitutionalAffiliation: { truthiness: true },
      institutionShortName: { truthiness: true },
      institutionalRoleEnum: { truthiness: true },
      institutionalRoleOtherText: { truthiness: true },
      institutionMembership: { truthiness: true },
    }
  );

  return (
    <FadeBox style={styles.fadeBox}>
      {profileLoadingError && (
        <AlertDanger style={{ fontSize: 14 }}>
          <ClrIcon shape='exclamation-circle' />
          {profileLoadingError}
        </AlertDanger>
      )}
      {institutionsLoadingError && (
        <AlertDanger style={{ fontSize: 14 }}>
          <ClrIcon shape='exclamation-circle' />
          {institutionsLoadingError}
        </AlertDanger>
      )}
      {updatedProfile && (
        <FlexColumn>
          <FlexRow style={{ alignItems: 'center' }}>
            <UserAdminTableLink />
            <span style={styles.header}>User Profile Information</span>
            <UserAuditLink
              style={styles.auditLink}
              usernameWithoutDomain={usernameWithoutGsuiteDomain}
            >
              AUDIT <CaretRight />
            </UserAuditLink>
          </FlexRow>
          <FlexRow style={{ paddingTop: '1em' }}>
            <UneditableFields profile={oldProfile} />
            <EditableFields
              oldProfile={oldProfile}
              updatedProfile={updatedProfile}
              institutions={institutions}
              emailValidationStatus={emailValidationStatus}
              onChangeEmail={(contactEmail: string) =>
                updateContactEmail(contactEmail)
              }
              onChangeInitialCreditsLimit={(freeTierDollarQuota: number) =>
                updateProfile({ freeTierDollarQuota })
              }
              onChangeInstitution={(institutionShortName: string) =>
                updateInstitution(institutionShortName)
              }
              onChangeInstitutionalRole={(
                institutionalRoleEnum: InstitutionalRole
              ) => updateInstitutionalRole(institutionalRoleEnum)}
              onChangeInstitutionOtherText={(otherText: string) =>
                updateInstitutionalRoleOtherText(otherText)
              }
            />
          </FlexRow>
          <FlexRow>
            <FlexColumn>
              <FlexRow>
                <div style={styles.tableHeader}>Access status</div>
                <DisabledToggle
                  currentlyDisabled={updatedProfile.disabled}
                  previouslyDisabled={oldProfile.disabled}
                  toggleDisabled={() =>
                    updateProfile({ disabled: !updatedProfile.disabled })
                  }
                />
              </FlexRow>
              <AccessModuleTable
                oldProfile={oldProfile}
                updatedProfile={updatedProfile}
                pendingBypassRequests={bypassChangeRequests}
                bypassUpdate={(accessBypassRequest) =>
                  updateModuleBypassStatus(accessBypassRequest)
                }
              />
            </FlexColumn>
          </FlexRow>
          <FlexRow style={{ paddingTop: '1em' }}>
            <ErrorsTooltip errors={errors}>
              <Button
                type='primary'
                disabled={
                  !!errors ||
                  !profileNeedsUpdate(
                    oldProfile,
                    updatedProfile,
                    bypassChangeRequests
                  )
                }
                onClick={async () => {
                  spinnerProps.showSpinner();
                  const response = await updateAccountProperties(
                    oldProfile,
                    updatedProfile,
                    bypassChangeRequests
                  );
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
              disabled={
                !profileNeedsUpdate(
                  oldProfile,
                  updatedProfile,
                  bypassChangeRequests
                )
              }
              onClick={() => {
                setBypassChangeRequests([]);
                setEmailValidationStatus(EmailValidationStatus.UNCHECKED);
                setUpdatedProfile(oldProfile);
              }}
            >
              Cancel
            </Button>
          </FlexRow>
          <FlexRow>
            <h2>Egress event history</h2>
          </FlexRow>
          <FlexRow>
            <EgressEventsTable
              displayPageSize={10}
              sourceUserEmail={updatedProfile.username}
            />
          </FlexRow>
        </FlexColumn>
      )}
    </FadeBox>
  );
};
