// this is a temporary file to assist with the migration from (class-based) AdminUser to (functional) AdminUserProfile
// for RW-7536

import * as React from 'react';
import { CSSProperties } from 'react';
import { Link } from 'react-router-dom';
import * as fp from 'lodash/fp';
import { Dropdown } from 'primereact/dropdown';
import {
  ClrIcon,
  ControlledTierBadge,
  RegisteredTierBadge,
} from 'app/components/icons';

import { formatInitialCreditsUSD, isBlank, reactStyles } from 'app/utils';
import colors, { colorWithWhiteness } from 'app/styles/colors';
import {
  AccessBypassRequest,
  AccessModule,
  AccessModuleStatus,
  AccountDisabledStatus,
  AccountPropertyUpdate,
  InstitutionalRole,
  Profile,
  PublicInstitutionDetails,
  VerifiedInstitutionalAffiliation,
} from 'generated/fetch';
import {
  institutionApi,
  userAdminApi,
} from 'app/services/swagger-fetch-clients';
import { FlexColumn, FlexRow } from 'app/components/flex';
import { getRoleOptions } from 'app/utils/institutions';
import { TextInputWithLabel } from 'app/components/inputs';
import { BulletAlignedUnorderedList } from 'app/components/lists';
import { TooltipTrigger } from 'app/components/popups';
import { serverConfigStore } from 'app/utils/stores';
import {
  accessRenewalModules,
  computeRenewalDisplayDates,
  getAccessModuleConfig,
  getAccessModuleStatusByName,
} from 'app/utils/access-utils';
import {
  AccessTierShortNames,
  hasRegisteredTierAccess,
  orderedAccessTierShortNames,
  displayNameForTier,
} from 'app/utils/access-tiers';
import { formatDate } from 'app/utils/dates';

export const commonStyles = reactStyles({
  semiBold: {
    fontWeight: 600,
  },
  label: {
    color: colors.primary,
    fontSize: '14px',
    fontWeight: 'bold',
    paddingLeft: '1em',
  },
  textInput: {
    width: '14rem',
    opacity: '100%',
    marginLeft: '1em',
  },
  textInputContainer: {
    marginTop: '1rem',
  },
  dropdown: {
    minWidth: '70px',
    width: '14rem',
    marginLeft: '1em',
  },
  fadeBox: {
    margin: 'auto',
    paddingTop: '1rem',
    width: '96.25%',
    minWidth: '1232px',
    color: colors.primary,
  },
  userAdminArrow: {
    backgroundColor: colorWithWhiteness(colors.accent, 0.85),
    color: colors.accent,
    borderRadius: '18px',
    transform: 'rotate(270deg)',
  },
});

export const adminGetProfile = async (
  usernameWithDomain: string
): Promise<Profile> => {
  return userAdminApi().getUserByUsername(usernameWithDomain);
};

export const getPublicInstitutionDetails = async (): Promise<
  PublicInstitutionDetails[]
> => {
  const institutionsResponse =
    await institutionApi().getPublicInstitutionDetails();
  const institutions = institutionsResponse?.institutions;
  return fp.sortBy('displayName', institutions);
};

/**
 * Present an ordered list of dollar options with the following values:
 * $300 to $1000, in $100 increments
 * $1000 to $10,000, in $500 increments
 * Plus the user's current quota value, if it's not already one of these
 */
export const getInitialCreditLimitOptions = (
  initialCreditLimitOverride?: number
) => {
  const START1 = 300;
  const END1 = 1000;
  const START2 = 1000;
  const END2 = 10000;

  // gotcha: argument order for rangeStep is (step, start, end)
  // IntelliJ incorrectly believes the order is (start, end, step)
  const below1000 = fp.rangeStep(100, START1, END1 + 1);
  const over1000 = fp.rangeStep(500, START2, END2 + 1);

  const defaultsPlusMaybeOverride = new Set([
    ...below1000,
    ...over1000,
    initialCreditLimitOverride ?? START1,
  ]);

  // gotcha: JS sorts numbers lexicographically by default
  const numericallySorted = Array.from(defaultsPlusMaybeOverride).sort(
    (a, b) => a - b
  );

  return fp.map(
    (limit) => ({ label: formatInitialCreditsUSD(limit), value: limit }),
    numericallySorted
  );
};

export const getInitalCreditsUsage = (profile: Profile): string => {
  const { freeTierDollarQuota, freeTierUsage } = profile;
  return `${formatInitialCreditsUSD(
    freeTierUsage
  )} used of ${formatInitialCreditsUSD(freeTierDollarQuota)} limit`;
};

// returns the updated profile value only if it has changed
export const getUpdatedProfileValue = (
  oldProfile: Profile,
  updatedProfile: Profile,
  attributePath: string[]
) => {
  const oldValue = fp.get(attributePath, oldProfile);
  const updatedValue = fp.get(attributePath, updatedProfile);
  if (!fp.isEqual(oldValue, updatedValue)) {
    return updatedValue;
  } else {
    return undefined;
  }
};

export const isBypassed = (
  profile: Profile,
  moduleName: AccessModule
): boolean =>
  !!getAccessModuleStatusByName(profile, moduleName)?.bypassEpochMillis;

// Some modules may never expire (eg GOOGLE TWO STEP NOTIFICATION, ERA COMMONS etc),
// in such cases set the expiry date as NEVER
// For other modules display the expiry date if known, else display '-' (say in case of bypass)
const getNullStringForExpirationDate = (moduleName: AccessModule): string =>
  getAccessModuleConfig(moduleName).canExpire ? '-' : 'Never';

export const displayModuleCompletionDate = (
  profile: Profile,
  moduleName: AccessModule
): string =>
  formatDate(
    getAccessModuleStatusByName(profile, moduleName)?.completionEpochMillis,
    '-'
  );

export const displayModuleExpirationDate = (
  profile: Profile,
  moduleName: AccessModule
): string =>
  formatDate(
    getAccessModuleStatusByName(profile, moduleName)?.expirationEpochMillis,
    getNullStringForExpirationDate(moduleName)
  );

const isEraRequiredForTier = (
  profile: Profile,
  accessTierShortName: AccessTierShortNames
): boolean => {
  const tierEligibility = profile.tierEligibilities.find(
    (tier) => tier.accessTierShortName === accessTierShortName
  );
  return (
    getAccessModuleConfig(AccessModule.ERACOMMONS).isEnabledInEnvironment &&
    tierEligibility?.eraRequired
  );
};

export const displayTierBadgeByRequiredModule = (
  profile: Profile,
  moduleName: AccessModule
) => {
  return (
    <div>
      {(moduleName === AccessModule.ERACOMMONS
        ? isEraRequiredForTier(profile, AccessTierShortNames.Registered)
        : getAccessModuleConfig(moduleName)?.isRequiredByRT) && (
        <RegisteredTierBadge style={{ gridArea: 'badge' }} />
      )}
      {(moduleName === AccessModule.ERACOMMONS
        ? isEraRequiredForTier(profile, AccessTierShortNames.Controlled)
        : getAccessModuleConfig(moduleName)?.isRequiredByCT) && (
        <ControlledTierBadge style={{ gridArea: 'badge' }} />
      )}
    </div>
  );
};

export const getEraNote = (profile: Profile): string => {
  const requiredTierNames = orderedAccessTierShortNames
    .filter((name) => isEraRequiredForTier(profile, name))
    .map(displayNameForTier);

  const accessText =
    requiredTierNames.length === 0
      ? 'does not require eRA Commons'
      : 'requires eRA Commons for ' +
        (requiredTierNames.join(' and ') + ' access');
  const institutionName =
    profile.verifiedInstitutionalAffiliation?.institutionDisplayName;
  const note = '* eRA Commons requirements vary by institution.';
  return (
    note +
    (isBlank(institutionName)
      ? ` We don't have any institutional information for this user.`
      : ` This user's institution (${institutionName}) ${accessText}.`)
  );
};

// would this AccessBypassRequest actually change the profile state?
// allows un-toggling of bypass for a module
export const wouldUpdateBypassState = (
  oldProfile: Profile,
  request: AccessBypassRequest
): boolean => isBypassed(oldProfile, request.moduleName) !== request.isBypassed;

export const profileNeedsUpdate = (
  oldProfile: Profile,
  updatedProfile: Profile,
  bypassChangeRequests?: AccessBypassRequest[]
): boolean =>
  !fp.isEqual(oldProfile, updatedProfile) ||
  bypassChangeRequests?.some((request) =>
    wouldUpdateBypassState(oldProfile, request)
  );

export const updateAccountProperties = async (
  oldProfile: Profile,
  updatedProfile: Profile,
  accessBypassRequests?: AccessBypassRequest[]
): Promise<Profile> => {
  const { username } = updatedProfile;

  const updateDisabledMaybe: boolean = getUpdatedProfileValue(
    oldProfile,
    updatedProfile,
    ['disabled']
  );

  const accountDisabledStatus: AccountDisabledStatus =
    updateDisabledMaybe === undefined
      ? undefined
      : {
          disabled: updateDisabledMaybe, // play with null later
        };

  // only set these fields if they have changed (except username which we always want)
  const request: AccountPropertyUpdate = {
    username,
    accessBypassRequests,
    accountDisabledStatus,
    freeCreditsLimit: getUpdatedProfileValue(oldProfile, updatedProfile, [
      'freeTierDollarQuota',
    ]),
    contactEmail: getUpdatedProfileValue(oldProfile, updatedProfile, [
      'contactEmail',
    ]),
    affiliation: getUpdatedProfileValue(oldProfile, updatedProfile, [
      'verifiedInstitutionalAffiliation',
    ]),
  };

  return userAdminApi().updateAccountProperties(request);
};

export const DropdownWithLabel = ({
  label,
  className,
  options,
  currentValue,
  previousValue,
  highlightOnChange,
  onChange,
  disabled = false,
  dataTestId,
  labelStyle = {},
  dropdownStyle = {},
}) => {
  const dropdownHighlightStyling = `body .${className} .p-inputtext { background-color: ${colors.highlight}; }`;
  return (
    <FlexColumn data-test-id={dataTestId} style={{ marginTop: '1rem' }}>
      {highlightOnChange && currentValue !== previousValue && (
        <style>{dropdownHighlightStyling}</style>
      )}
      <label style={{ ...commonStyles.label, ...labelStyle }}>{label}</label>
      <Dropdown
        className={className}
        style={{ ...commonStyles.dropdown, ...dropdownStyle }}
        options={options}
        onChange={(e) => onChange(e)}
        value={currentValue}
        disabled={disabled}
      />
    </FlexColumn>
  );
};

interface ContactEmailTextInputProps {
  contactEmail: string;
  previousContactEmail?: string;
  highlightOnChange?: boolean;
  onChange: Function;
  labelStyle?: CSSProperties;
  inputStyle?: CSSProperties;
  containerStyle?: CSSProperties;
}

export const ContactEmailTextInput = ({
  contactEmail,
  previousContactEmail,
  highlightOnChange,
  onChange,
  labelStyle,
  inputStyle,
  containerStyle,
}: ContactEmailTextInputProps) => {
  return (
    <TextInputWithLabel
      inputId={'contactEmail'}
      labelText={'Contact email'}
      value={contactEmail}
      previousValue={previousContactEmail}
      highlightOnChange={highlightOnChange}
      labelStyle={{ ...commonStyles.label, ...labelStyle }}
      inputStyle={{ ...commonStyles.textInput, ...inputStyle }}
      containerStyle={{ ...commonStyles.textInputContainer, ...containerStyle }}
      onChange={(value) => onChange(value)}
    />
  );
};

interface InitialCreditsDropdownProps {
  currentLimit?: number;
  previousLimit?: number;
  highlightOnChange?: boolean;
  onChange: Function;
  labelStyle?: CSSProperties;
  dropdownStyle?: CSSProperties;
}

export const InitialCreditsDropdown = ({
  currentLimit,
  previousLimit,
  highlightOnChange,
  onChange,
  labelStyle,
  dropdownStyle,
}: InitialCreditsDropdownProps) => {
  return (
    <DropdownWithLabel
      dataTestId='initial-credits-dropdown'
      className='initial-credits'
      label='Initial credit limit'
      options={getInitialCreditLimitOptions(previousLimit)}
      currentValue={currentLimit}
      previousValue={previousLimit}
      highlightOnChange={highlightOnChange}
      labelStyle={labelStyle}
      dropdownStyle={dropdownStyle}
      onChange={(event) => onChange(event)}
    />
  );
};

interface InstitutionDropdownProps {
  institutions?: PublicInstitutionDetails[];
  currentInstitutionShortName?: string;
  previousInstitutionShortName?: string;
  highlightOnChange?: boolean;
  onChange: Function;
  labelStyle?: CSSProperties;
  dropdownStyle?: CSSProperties;
}

export const InstitutionDropdown = ({
  institutions,
  currentInstitutionShortName,
  previousInstitutionShortName,
  highlightOnChange,
  onChange,
  labelStyle,
  dropdownStyle,
}: InstitutionDropdownProps) => {
  const options = fp.map(
    ({ displayName, shortName }) => ({ label: displayName, value: shortName }),
    institutions
  );
  return institutions ? (
    <DropdownWithLabel
      dataTestId='verifiedInstitution'
      className='institution'
      label='Verified institution'
      options={options}
      currentValue={currentInstitutionShortName}
      previousValue={previousInstitutionShortName}
      highlightOnChange={highlightOnChange}
      labelStyle={labelStyle}
      dropdownStyle={dropdownStyle}
      onChange={(event) => onChange(event)}
    />
  ) : null;
};

interface InstitutionalRoleDropdownProps {
  institutions?: PublicInstitutionDetails[];
  currentAffiliation?: VerifiedInstitutionalAffiliation;
  previousRole?: InstitutionalRole;
  highlightOnChange?: boolean;
  onChange: Function;
  labelStyle?: CSSProperties;
  dropdownStyle?: CSSProperties;
}

export const InstitutionalRoleDropdown = ({
  institutions,
  currentAffiliation,
  previousRole,
  highlightOnChange,
  onChange,
  labelStyle,
  dropdownStyle,
}: InstitutionalRoleDropdownProps) => {
  const options = getRoleOptions(
    institutions,
    currentAffiliation?.institutionShortName
  );
  return institutions && currentAffiliation ? (
    <DropdownWithLabel
      dataTestId='institutionalRole'
      className='institutional-role'
      label='Institutional role'
      disabled={!currentAffiliation?.institutionShortName}
      options={options}
      currentValue={currentAffiliation?.institutionalRoleEnum}
      previousValue={previousRole}
      highlightOnChange={highlightOnChange}
      labelStyle={labelStyle}
      dropdownStyle={dropdownStyle}
      onChange={(event) => onChange(event)}
    />
  ) : null;
};

interface InstitutionalRoleOtherTextProps {
  affiliation?: VerifiedInstitutionalAffiliation;
  previousOtherText?: string;
  highlightOnChange?: boolean;
  onChange: Function;
  labelStyle?: CSSProperties;
  inputStyle?: CSSProperties;
  containerStyle?: CSSProperties;
}

export const InstitutionalRoleOtherTextInput = ({
  affiliation,
  previousOtherText,
  highlightOnChange,
  onChange,
  labelStyle,
  inputStyle,
  containerStyle,
}: InstitutionalRoleOtherTextProps) => {
  return affiliation?.institutionalRoleEnum === InstitutionalRole.OTHER ? (
    <TextInputWithLabel
      dataTestId={'institutionalRoleOtherText'}
      labelText={'Institutional role description'}
      value={affiliation?.institutionalRoleOtherText}
      previousValue={previousOtherText}
      highlightOnChange={highlightOnChange}
      labelStyle={{ ...commonStyles.label, ...labelStyle }}
      inputStyle={{ ...commonStyles.textInput, ...inputStyle }}
      containerStyle={{ ...commonStyles.textInputContainer, ...containerStyle }}
      onChange={(value) => onChange(value)}
    />
  ) : null;
};

export const UserAdminTableLink = () => (
  <Link to='/admin/users'>
    <ClrIcon shape='arrow' size={37} style={commonStyles.userAdminArrow} />
  </Link>
);

export const UserAuditLink = (props: {
  usernameWithoutDomain: string;
  style?: CSSProperties;
  children;
}) => (
  <Link
    style={props.style}
    to={`/admin/user-audit/${props.usernameWithoutDomain}`}
    target='_blank'
  >
    {props.children}
  </Link>
);

export const validationErrorMessages = {
  contactEmail: "Institutional contact email can't be left blank",
  verifiedInstitutionalAffiliation:
    "Verified institutional affiliation can't be unset or left blank",
  institutionShortName: 'You must choose an institution',
  institutionalRoleEnum: "You must select the user's role at the institution",
  institutionalRoleOtherText:
    "You must describe the user's role if you select Other",
  institutionMembership:
    "The user's contact email does not match the selected institution",
};

interface ErrorsTooltipProps {
  errors;
  children;
}

export const ErrorsTooltip = ({ errors, children }: ErrorsTooltipProps) => {
  return (
    <TooltipTrigger
      data-test-id='user-admin-errors-tooltip'
      content={
        errors && (
          <BulletAlignedUnorderedList>
            {Object.entries(validationErrorMessages).map(
              ([field, message], reactKey) =>
                errors?.hasOwnProperty(field) && (
                  <li key={reactKey}>{message}</li>
                )
            )}
          </BulletAlignedUnorderedList>
        )
      }
    >
      {children}
    </TooltipTrigger>
  );
};

interface ExpirationProps {
  profile: Profile;
}

export const AccessModuleExpirations = ({ profile }: ExpirationProps) => {
  // compliance training is feature-flagged in some environments
  const { enableComplianceTraining } = serverConfigStore.get().config;
  const moduleNames = enableComplianceTraining
    ? accessRenewalModules
    : accessRenewalModules.filter(
        (moduleName) => moduleName !== AccessModule.COMPLIANCETRAINING
      );

  const accessStatus = hasRegisteredTierAccess(profile) ? (
    <div style={{ color: colors.success }}>Enabled</div>
  ) : (
    <div style={{ color: colors.danger }}>Disabled</div>
  );

  const modules = profile?.accessModules?.modules;

  return (
    <FlexColumn style={{ marginTop: '1rem' }}>
      <label style={commonStyles.semiBold}>
        Data Access Status: {accessStatus}
      </label>
      {moduleNames.map((moduleName, zeroBasedStep) => {
        // return the status if found; init an empty status with the moduleName if not
        const status: AccessModuleStatus = modules.find(
          (s) => s.moduleName === moduleName
        ) || { moduleName };
        const { lastConfirmedDate, nextReviewDate } =
          computeRenewalDisplayDates(status);
        const { AARTitleComponent } = getAccessModuleConfig(moduleName);
        return (
          <FlexRow key={zeroBasedStep} style={{ marginTop: '0.5rem' }}>
            <FlexColumn>
              <label style={commonStyles.semiBold}>
                Step {zeroBasedStep + 1}: <AARTitleComponent />
              </label>
              <div>Last Updated On: {lastConfirmedDate}</div>
              <div>Next Review: {nextReviewDate}</div>
            </FlexColumn>
          </FlexRow>
        );
      })}
    </FlexColumn>
  );
};
