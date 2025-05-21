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

import { InstitutionExpirationBypassExplanation } from 'app/components/admin/admin-institution-expiration-bypass-explanation';
import { CommonToggle } from 'app/components/admin/common-toggle';
import { AlertDanger } from 'app/components/alert';
import { Button } from 'app/components/buttons';
import { FadeBox } from 'app/components/containers';
import { FlexColumn, FlexRow } from 'app/components/flex';
import { CaretRight, ClrIcon } from 'app/components/icons';
import { TextArea, ValidationError } from 'app/components/inputs';
import {
  Modal,
  ModalBody,
  ModalFooter,
  ModalTitle,
} from 'app/components/modals';
import { TooltipTrigger } from 'app/components/popups';
import { WithSpinnerOverlayProps } from 'app/components/with-spinner-overlay';
import { EgressEventsTable } from 'app/pages/admin/egress-events-table';
import colors from 'app/styles/colors';
import { formatInitialCreditsUSD, isBlank, reactStyles } from 'app/utils';
import { badgeForTier } from 'app/utils/access-tiers';
import {
  getAccessModuleConfig,
  getAccessModuleStatusByName,
  isBypassed,
} from 'app/utils/access-utils';
import {
  AuthorityGuardedAction,
  hasAuthorityForAction,
  renderIfAuthorized,
} from 'app/utils/authorities';
import { formatDate } from 'app/utils/dates';
import {
  checkInstitutionalEmail,
  getEmailValidationErrorMessage,
} from 'app/utils/institutions';
import {
  MatchParams,
  profileStore,
  serverConfigStore,
  useStore,
} from 'app/utils/stores';

import {
  adminGetProfile,
  commonStyles,
  ContactEmailTextInput,
  displayModuleCompletionDate,
  displayModuleExpirationDate,
  displayModuleStatus,
  ErrorsTooltip,
  getPublicInstitutionDetails,
  InitialCreditBypassSwitch,
  InitialCreditsDropdown,
  InstitutionalRoleDropdown,
  InstitutionalRoleOtherTextInput,
  InstitutionDropdown,
  orderedAccessModules,
  profileNeedsUpdate,
  TierBadgesMaybe,
  updateAccountProperties,
  UserAdminTableLink,
  UserAuditLink,
} from './admin-user-common';
import { AdminUserDisabledEvents } from './admin-user-disabled-events';
import { AdminUserEgressBypass } from './admin-user-egress-bypass';

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
  },
  uneditableFieldsSpacer: {
    height: '24px',
  },
  value: {
    color: colors.primary,
    fontSize: '14px',
  },
  auditLink: {
    color: colors.accent,
    fontSize: '16px',
    fontWeight: 500,
  },
  editableFields: {
    paddingTop: '1em',
    flex: 1,
  },
  editRow: {
    gap: '1rem',
  },
  initialCreditsPanel: {
    border: '1px solid #cccccc',
    borderRadius: '3px',
    padding: '1rem',
    flex: '0 1 0',
  },
});

enum EmailValidationStatus {
  UNCHECKED,
  VALID,
  INVALID,
  API_ERROR,
}

const getInstitution = (
  institutionShortName: string,
  institutions: PublicInstitutionDetails[]
) => {
  return institutions.find((i) => i.shortName === institutionShortName);
};

const UserAccess = (props: { accessTierShortNames: string[] }) => {
  const { accessTierShortNames } = props;
  return (
    <FlexRow data-test-id='data-access-tiers' style={{ gap: '0.25rem' }}>
      {accessTierShortNames?.length === 0 ? (
        <span>
          <i>No data access</i>
        </span>
      ) : (
        accessTierShortNames.map((accessTierShortName) =>
          badgeForTier(accessTierShortName)
        )
      )}
    </FlexRow>
  );
};

const InstitutionalFields = ({
  oldProfile,
  updatedProfile,
  institutions,
  emailValidationStatus,
  onChangeEmail,
  onChangeInstitution,
  onChangeInstitutionalRole,
  onChangeInstitutionOtherText,
  institution,
  profile,
}) => {
  // Show the link to  redirect to institution detail page,
  // if the LOGGED IN USER has Institution admin authority and
  // institution name is populated
  const showGoToInstitutionLink =
    hasAuthorityForAction(profile, AuthorityGuardedAction.INSTITUTION_ADMIN) &&
    !!updatedProfile.verifiedInstitutionalAffiliation?.institutionShortName;
  return (
    <FlexColumn style={{ flex: 0, gap: '1rem' }}>
      <FlexColumn>
        <div
          data-test-id='name'
          style={{ ...styles.subHeader, marginRight: '1.5rem' }}
        >{`${updatedProfile.givenName} ${updatedProfile.familyName}`}</div>
        <FlexRow style={{ gap: '0.5rem' }}>
          <div data-test-id='user-name'>{updatedProfile.username}</div>
          <UserAccess
            accessTierShortNames={oldProfile.accessTierShortNames.sort()}
          />
        </FlexRow>
      </FlexColumn>
      <ContactEmailTextInput
        contactEmail={updatedProfile.contactEmail}
        previousContactEmail={oldProfile.contactEmail}
        highlightOnChange
        onChange={(email) => onChangeEmail(email)}
      />
      {emailValidationStatus === EmailValidationStatus.INVALID && (
        <div data-test-id='email-invalid' style={{ paddingLeft: '1em' }}>
          {getEmailValidationErrorMessage(institution)}
        </div>
      )}
      <InstitutionDropdown
        institutions={institutions}
        currentInstitution={updatedProfile.verifiedInstitutionalAffiliation}
        previousInstitution={oldProfile.verifiedInstitutionalAffiliation}
        highlightOnChange
        onChange={(event) => onChangeInstitution(event.value)}
        showGoToInstitutionLink={showGoToInstitutionLink}
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
      <InstitutionalRoleOtherTextInput
        affiliation={updatedProfile.verifiedInstitutionalAffiliation}
        previousOtherText={
          oldProfile.verifiedInstitutionalAffiliation
            ?.institutionalRoleOtherText
        }
        highlightOnChange
        onChange={(value) => onChangeInstitutionOtherText(value)}
      />
    </FlexColumn>
  );
};

const InitialCreditsCard = ({
  oldProfile,
  updatedProfile,
  onChangeInitialCreditsLimit,
  onChangeInitialCreditBypass,
  institution,
}) => {
  return (
    <FlexColumn style={{ flex: 0 }}>
      <FlexRow style={styles.initialCreditsPanel}>
        <FlexColumn>
          <div style={styles.subHeader}>Initial credits</div>
          <InstitutionExpirationBypassExplanation
            bypassed={
              !!institution?.institutionalInitialCreditsExpirationBypassed
            }
          />
          {!institution?.institutionalInitialCreditsExpirationBypassed &&
            oldProfile.initialCreditsExtensionEpochMillis && (
              <p style={{ color: colors.primary, fontWeight: 500 }}>
                User requested an extension on{' '}
                {formatDate(oldProfile.initialCreditsExtensionEpochMillis, '-')}
              </p>
            )}
          <FlexColumn>
            {!institution?.institutionalInitialCreditsExpirationBypassed && (
                <InitialCreditBypassSwitch
                  currentlyBypassed={
                    updatedProfile.initialCreditsExpirationBypassed
                  }
                  previouslyBypassed={
                    oldProfile.initialCreditsExpirationBypassed
                  }
                  expirationEpochMillis={
                    oldProfile.initialCreditsExpirationEpochMillis
                  }
                  onChange={(bypass) => onChangeInitialCreditBypass(bypass)}
                  label='Individual Expiration Bypass'
                />
              )}
            <FlexRow style={{ gap: '1rem', paddingTop: '1.5rem' }}>
              <div
                data-test-id='initial-credits-used'
                style={{ flex: '0 1 auto' }}
              >
                <div style={styles.label}>Usage</div>
                <div style={styles.value}>
                  {formatInitialCreditsUSD(updatedProfile.initialCreditsUsage)}
                </div>
              </div>
              <InitialCreditsDropdown
                currentLimit={updatedProfile.initialCreditsLimit}
                previousLimit={oldProfile.initialCreditsLimit}
                highlightOnChange
                onChange={(event) => onChangeInitialCreditsLimit(event.value)}
                label='Limit'
                dropdownStyle={{ width: '10rem', minWidth: 'auto' }}
              />
            </FlexRow>
          </FlexColumn>
        </FlexColumn>
      </FlexRow>
    </FlexColumn>
  );
};

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

  const previouslyBypassed = isBypassed(
    getAccessModuleStatusByName(oldProfile, moduleName)
  );
  const pendingBypassState = pendingBypassRequests.find(
    (r) => r.moduleName === moduleName
  );
  const isModuleBypassed = pendingBypassState
    ? pendingBypassState.bypassed
    : isBypassed(getAccessModuleStatusByName(updatedProfile, moduleName));
  const highlightStyle =
    isModuleBypassed !== previouslyBypassed
      ? { background: colors.highlight }
      : {};

  return (
    <FlexRow
      style={{
        ...highlightStyle,
        justifyContent: 'center',
      }}
    >
      <CommonToggle
        name=''
        checked={isModuleBypassed}
        onToggle={() =>
          bypassUpdate({ moduleName, bypassed: !isModuleBypassed })
        }
      />
    </FlexRow>
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
    const { adminPageTitle, isEnabledInEnvironment } =
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
            accessTierBadges: (
              <TierBadgesMaybe
                profile={props.updatedProfile}
                moduleName={moduleName}
              />
            ),
            bypassToggle: (
              <ToggleForModule moduleName={moduleName} {...props} />
            ),
          },
        ]
      : [];
  }, orderedAccessModules);

  return (
    <DataTable
      data-test-id='access-module-table'
      rowHover
      breakpoint='0px'
      style={{ paddingTop: '1em' }}
      value={tableData}
      className='condensed-table'
    >
      <Column field='moduleName' header='Access module' />
      <Column field='moduleStatus' header='Status' />
      <Column field='completionDate' header='Last completed on' />
      <Column field='expirationDate' header='Expires on' />
      <Column field='accessTierBadges' header='Required for tier access' />
      <Column field='bypassToggle' header='Bypass' />
    </DataTable>
  );
};

const isLoggedInUser = (userProfile: Profile): boolean => {
  const { profile } = profileStore.get();
  return userProfile?.username === profile?.username;
};

const DisabledToggle = (props: {
  currentlyDisabled: boolean;
  previouslyDisabled: boolean;
  profile: Profile;
  toggleDisabled: () => void;
}) => {
  const { currentlyDisabled, previouslyDisabled, profile, toggleDisabled } =
    props;
  const highlightStyle =
    currentlyDisabled !== previouslyDisabled
      ? { background: colors.highlight }
      : {};

  return (
    <div style={{ paddingLeft: '2em' }}>
      <div style={highlightStyle}>
        <CommonToggle
          name={currentlyDisabled ? 'Account disabled' : 'Account enabled'}
          checked={!currentlyDisabled}
          onToggle={() => toggleDisabled()}
          disabled={isLoggedInUser(profile)}
        />
      </div>
    </div>
  );
};

const AdminCommentModal = (props: {
  onCancel: Function;
  onSubmit: Function;
}) => {
  const { onCancel, onSubmit } = props;
  const [comments, setComments] = useState<string>();
  return (
    <Modal>
      <ModalTitle>Reason for disabling user</ModalTitle>
      <ModalBody>
        <TextArea value={comments} onChange={(v) => setComments(v)} />
        {comments?.trim().length > 255 && (
          <ValidationError>Max character limit is 255</ValidationError>
        )}
      </ModalBody>
      <ModalFooter>
        <Button onClick={onCancel} type='secondary'>
          Cancel
        </Button>
        <Button
          onClick={() => onSubmit(comments.trim())}
          type='primary'
          disabled={!comments || comments.trim().length > 255}
        >
          Submit
        </Button>
      </ModalFooter>
    </Modal>
  );
};

export const AdminUserProfile = (spinnerProps: WithSpinnerOverlayProps) => {
  const {
    config: { gsuiteDomain },
  } = useStore(serverConfigStore);
  const { profile } = useStore(profileStore);

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
  const [institution, setInstitution] =
    useState<PublicInstitutionDetails>(null);
  const [showAdminCommentModal, setShowAdminCommentModal] =
    useState<boolean>(false);

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
        const publicInstitutionDetails = await getPublicInstitutionDetails();
        setInstitutions(publicInstitutionDetails);
        setInstitution(
          getInstitution(
            updatedProfile?.verifiedInstitutionalAffiliation
              ?.institutionShortName,
            publicInstitutionDetails
          )
        );
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

  useEffect(() => {
    setInstitution(
      getInstitution(
        updatedProfile?.verifiedInstitutionalAffiliation?.institutionShortName,
        institutions
      )
    );
  }, [institutions, updatedProfile]);
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
          result?.validMember
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

  const saveProfile = async (accountDisabledReason?: string) => {
    spinnerProps.showSpinner();
    setShowAdminCommentModal(false);
    const response = await updateAccountProperties(
      oldProfile,
      updatedProfile,
      bypassChangeRequests,
      accountDisabledReason
    );
    setOldProfile(response);
    setUpdatedProfile(response);
    spinnerProps.hideSpinner();
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
          <FlexColumn style={{ paddingTop: '1em' }}>
            <FlexRow style={{ flexWrap: 'wrap', gap: '1rem' }}>
              <FlexColumn>
                <InstitutionalFields
                  onChangeEmail={(contactEmail: string) =>
                    updateContactEmail(contactEmail)
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
                  {...{
                    oldProfile,
                    updatedProfile,
                    institutions,
                    emailValidationStatus,
                    institution,
                    profile,
                  }}
                />
                <FlexRow style={{ paddingTop: '4em' }}>
                  <ErrorsTooltip errors={errors}>
                    <Button
                      data-test-id='update-profile'
                      type='primary'
                      disabled={
                        !!errors ||
                        !profileNeedsUpdate(
                          oldProfile,
                          updatedProfile,
                          bypassChangeRequests
                        )
                      }
                      onClick={() => {
                        if (updatedProfile.disabled && !oldProfile.disabled) {
                          setShowAdminCommentModal(true);
                        } else {
                          saveProfile();
                        }
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
              </FlexColumn>
              <InitialCreditsCard
                onChangeInitialCreditsLimit={(initialCreditsLimit: number) =>
                  updateProfile({ initialCreditsLimit })
                }
                onChangeInitialCreditBypass={(bypass: boolean) =>
                  updateProfile({ initialCreditsExpirationBypassed: bypass })
                }
                {...{
                  oldProfile,
                  updatedProfile,
                  institution,
                }}
              />
              <FlexColumn style={{ flex: 1 }}>
                <FlexRow>
                  <div style={styles.subHeader}>Access status</div>
                  <TooltipTrigger
                    disabled={!isLoggedInUser(updatedProfile)}
                    content={'Cannot change your own Access Status'}
                  >
                    <div>
                      <DisabledToggle
                        currentlyDisabled={updatedProfile.disabled}
                        previouslyDisabled={oldProfile.disabled}
                        toggleDisabled={() =>
                          updateProfile({ disabled: !updatedProfile.disabled })
                        }
                        profile={updatedProfile}
                      />
                    </div>
                  </TooltipTrigger>
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
          </FlexColumn>
          <FlexRow>
            <h2>Egress event history</h2>
          </FlexRow>
          <FlexRow>
            {renderIfAuthorized(
              profile,
              AuthorityGuardedAction.EGRESS_EVENTS,
              () => (
                <EgressEventsTable
                  displayPageSize={10}
                  sourceUserEmail={updatedProfile.username}
                />
              )
            )}
          </FlexRow>
          <FlexRow>
            <h2>Egress bypass requests for large file downloads</h2>
          </FlexRow>
          <FlexRow>
            {renderIfAuthorized(
              profile,
              AuthorityGuardedAction.EGRESS_BYPASS,
              () => (
                <AdminUserEgressBypass
                  {...{ profile }}
                  targetUserId={updatedProfile.userId}
                />
              )
            )}
          </FlexRow>
          <FlexRow>
            <h2>User Disabled Event History (all times local)</h2>
          </FlexRow>
          <FlexRow>
            <AdminUserDisabledEvents
              accountDisabledStatus={oldProfile.disabled}
              targetUserId={updatedProfile.userId}
            />
          </FlexRow>
        </FlexColumn>
      )}
      {showAdminCommentModal && (
        <AdminCommentModal
          onCancel={() => setShowAdminCommentModal(false)}
          onSubmit={(disabledReason: string) => saveProfile(disabledReason)}
        />
      )}
    </FadeBox>
  );
};
