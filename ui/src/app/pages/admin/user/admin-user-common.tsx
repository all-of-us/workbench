// this is a temporary file to assist with the migration from (class-based) AdminUser to (functional) AdminUserProfile
// for RW-7536

// TODO how many of these conditions can be removed now that AdminUser is gone?

import * as React from 'react';
import { CSSProperties } from 'react';
import { Link } from 'react-router-dom';
import * as fp from 'lodash/fp';
import { Dropdown } from 'primereact/dropdown';
import { faLink } from '@fortawesome/free-solid-svg-icons';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';

import {
  AccessBypassRequest,
  AccessModule,
  AccountDisabledStatus,
  AccountPropertyUpdate,
  InstitutionalRole,
  Profile,
  PublicInstitutionDetails,
  VerifiedInstitutionalAffiliation,
} from 'generated/fetch';

import { cond } from '@terra-ui-packages/core-utils';
import { CommonToggle } from 'app/components/admin/common-toggle';
import { StyledRouterLink } from 'app/components/buttons';
import { FlexColumn, FlexRow } from 'app/components/flex';
import {
  ClrIcon,
  ControlledTierBadge,
  RegisteredTierBadge,
} from 'app/components/icons';
import { TextInputWithLabel } from 'app/components/inputs';
import { BulletAlignedUnorderedList } from 'app/components/lists';
import { TooltipTrigger } from 'app/components/popups';
import { AccountCreationOptions } from 'app/pages/login/account-creation/account-creation-options';
import {
  institutionApi,
  userAdminApi,
} from 'app/services/swagger-fetch-clients';
import colors, { colorWithWhiteness } from 'app/styles/colors';
import { formatInitialCreditsUSD, reactStyles } from 'app/utils';
import {
  AccessRenewalStatus,
  computeRenewalDisplayDates,
  getAccessModuleConfig,
  getAccessModuleStatusByName,
  isBypassed,
} from 'app/utils/access-utils';
import { formatDate } from 'app/utils/dates';
import { getAdminUrl } from 'app/utils/institutions';

export const commonStyles = reactStyles({
  semiBold: {
    fontWeight: 600,
  },
  label: {
    color: colors.primary,
    fontSize: '14px',
    fontWeight: 'bold',
  },
  textInput: {
    width: '21rem',
    opacity: '100%',
  },
  textInputContainer: {
    marginTop: '1.5rem',
  },
  dropdown: {
    minWidth: '70px',
    width: '21rem',
  },
  fadeBox: {
    margin: 'auto',
    paddingTop: '1.5rem',
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
  incompleteOrExpiringModule: {
    color: colors.warning,
    fontWeight: 'bold',
  },
  expiredModule: {
    color: colors.danger,
    fontWeight: 'bold',
  },
  completeModule: {
    color: colors.black,
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
  const STEP1 = 25;
  const START1 = 300;
  const END1 = 500;

  const STEP2 = 100;
  const START2 = 500;
  const END2 = 1000;

  const STEP3 = 500;
  const START3 = 1000;
  const END3 = 10000;

  // gotcha: argument order for rangeStep is (step, start, end)
  // IntelliJ incorrectly believes the order is (start, end, step)
  const group1 = fp.rangeStep(STEP1, START1, END1 + 1);
  const group2 = fp.rangeStep(STEP2, START2, END2 + 1);
  const group3 = fp.rangeStep(STEP3, START3, END3 + 1);

  // the override value often duplicates one of the step values (like $400)
  // this is fine because it's a set
  const defaultsPlusMaybeOverride = new Set([
    ...group1, // 300, 325, ..., 500
    ...group2, // 500, 600, 700, 800, 900, 1000
    ...group3, // 1000, 1500, ..., 10000
    initialCreditLimitOverride ?? START1,
  ]);

  // gotcha: JS sorts numbers lexicographically by default
  const numericallySorted = Array.from(defaultsPlusMaybeOverride).sort(
    (a, b) => a - b
  );

  return numericallySorted.map((value) => ({
    value,
    label: formatInitialCreditsUSD(value),
  }));
};

export const getInitialCreditsUsage = (profile: Profile): string => {
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

const getModuleStatus = (
  profile: Profile,
  moduleName: AccessModule
): AccessRenewalStatus =>
  computeRenewalDisplayDates(
    getAccessModuleStatusByName(profile, moduleName),
    profile.duccSignedVersion
  ).moduleStatus;

const moduleStatusStyle = (moduleStatus) =>
  cond(
    [
      moduleStatus === AccessRenewalStatus.INCOMPLETE ||
        moduleStatus === AccessRenewalStatus.EXPIRING_SOON,
      () => commonStyles.incompleteOrExpiringModule,
    ],
    [
      moduleStatus === AccessRenewalStatus.EXPIRED,
      () => commonStyles.expiredModule,
    ],
    () => commonStyles.completeModule
  );

const displayModuleStatusAndDate = (
  profile: Profile,
  moduleName: AccessModule,
  child: string
): JSX.Element => (
  <div style={moduleStatusStyle(getModuleStatus(profile, moduleName))}>
    {child}
  </div>
);

// Some modules may never expire (eg GOOGLE TWO STEP NOTIFICATION, ERA COMMONS etc),
// in such cases set the expiry date as NEVER
// For other modules display the expiry date if known, else display '-' (say in case of bypass)
const getNullStringForExpirationDate = (moduleName: AccessModule): string =>
  getAccessModuleConfig(moduleName).expirable ? '-' : 'Never';

export const displayModuleStatus = (
  profile: Profile,
  moduleName: AccessModule
): JSX.Element =>
  displayModuleStatusAndDate(
    profile,
    moduleName,
    getModuleStatus(profile, moduleName)
  );

export const displayModuleCompletionDate = (
  profile: Profile,
  moduleName: AccessModule
): JSX.Element =>
  displayModuleStatusAndDate(
    profile,
    moduleName,
    formatDate(
      getAccessModuleStatusByName(profile, moduleName)?.completionEpochMillis,
      '-'
    )
  );

export const displayModuleExpirationDate = (
  profile: Profile,
  moduleName: AccessModule
): JSX.Element =>
  displayModuleStatusAndDate(
    profile,
    moduleName,
    formatDate(
      getAccessModuleStatusByName(profile, moduleName)?.expirationEpochMillis,
      getNullStringForExpirationDate(moduleName)
    )
  );

export const TierBadgesMaybe = (props: {
  profile: Profile;
  moduleName: AccessModule;
}) => {
  const { moduleName } = props;

  const rtRequired = getAccessModuleConfig(moduleName)?.requiredForRTAccess;

  const ctRequired = getAccessModuleConfig(moduleName)?.requiredForCTAccess;

  // fake a sub-table to keep RTs aligned with RTs
  return (
    <FlexRow data-test-id='tier-badges' style={{ justifyContent: 'center' }}>
      <div style={{ width: '30px' }}>
        {rtRequired && <RegisteredTierBadge />}
      </div>
      <div style={{ width: '30px' }}>
        {ctRequired && <ControlledTierBadge />}
      </div>
    </FlexRow>
  );
};

// would this AccessBypassRequest actually change the profile state?
// allows un-toggling of bypass for a module
export const wouldUpdateBypassState = (
  oldProfile: Profile,
  request: AccessBypassRequest
): boolean =>
  isBypassed(getAccessModuleStatusByName(oldProfile, request.moduleName)) !==
  request.bypassed;

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
  const { username, initialCreditsExpirationBypassed } = updatedProfile;

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
    ])?.trim(),
    affiliation: getUpdatedProfileValue(oldProfile, updatedProfile, [
      'verifiedInstitutionalAffiliation',
    ]),
    initialCreditsExpirationBypassed,
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
    <FlexColumn data-test-id={dataTestId}>
      {highlightOnChange && currentValue !== previousValue && (
        <style>{dropdownHighlightStyling}</style>
      )}
      <label style={{ ...commonStyles.label, ...labelStyle }}>{label}</label>
      <Dropdown
        appendTo='self'
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
  label: string;
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
  label,
}: InitialCreditsDropdownProps) => {
  return (
    <DropdownWithLabel
      dataTestId='initial-credits-dropdown'
      className='initial-credits'
      label={label}
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
  currentInstitution?: VerifiedInstitutionalAffiliation;
  previousInstitution?: VerifiedInstitutionalAffiliation;
  highlightOnChange?: boolean;
  onChange: Function;
  labelStyle?: CSSProperties;
  dropdownStyle?: CSSProperties;
  showGoToInstitutionLink: boolean;
}

export const InstitutionDropdown = ({
  institutions,
  currentInstitution,
  previousInstitution,
  highlightOnChange,
  onChange,
  labelStyle,
  dropdownStyle,
  showGoToInstitutionLink,
}: InstitutionDropdownProps) => {
  const options = fp.map(
    ({ displayName, shortName }) => ({ label: displayName, value: shortName }),
    institutions
  );

  const label = (
    <>
      Verified institution
      {showGoToInstitutionLink && (
        <StyledRouterLink
          style={{ paddingLeft: '0.5rem' }}
          path={getAdminUrl(currentInstitution?.institutionShortName)}
          target='_blank'
        >
          <TooltipTrigger
            content={`Click here to go to the
                '${currentInstitution?.institutionDisplayName}' Details Page`}
          >
            <FontAwesomeIcon icon={faLink} />
          </TooltipTrigger>
        </StyledRouterLink>
      )}
    </>
  );

  return institutions ? (
    <DropdownWithLabel
      dataTestId='verifiedInstitution'
      className='institution'
      label={label}
      options={options}
      currentValue={currentInstitution?.institutionShortName}
      previousValue={previousInstitution?.institutionShortName}
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
  return institutions && currentAffiliation ? (
    <DropdownWithLabel
      dataTestId='institutionalRole'
      className='institutional-role'
      label='Institutional role'
      disabled={!currentAffiliation?.institutionShortName}
      options={AccountCreationOptions.institutionalRoleOptions}
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
      inputId={'institutionalRoleOtherText'}
      labelText={'Institutional role description'}
      value={affiliation?.institutionalRoleOtherText}
      previousValue={previousOtherText}
      highlightOnChange={highlightOnChange}
      labelStyle={{ ...commonStyles.label, ...labelStyle }}
      inputStyle={{ ...commonStyles.textInput, ...inputStyle }}
      containerStyle={containerStyle}
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

interface InitialCreditBypassSwitchProps {
  currentlyBypassed: boolean | null;
  previouslyBypassed: boolean | null;
  onChange: (bypassed: boolean) => void;
  label: string;
}

export const InitialCreditBypassSwitch = ({
  currentlyBypassed,
  previouslyBypassed,
  onChange,
  label,
}: InitialCreditBypassSwitchProps) => {
  return (
    <FlexColumn style={{ paddingTop: '1.5rem' }}>
      <label style={{ ...commonStyles.label, padding: 0 }}>{label}</label>

      <CommonToggle
        name={
          currentlyBypassed ? 'Credits will not expire' : 'Credits will expire'
        }
        checked={currentlyBypassed}
        onToggle={onChange}
        style={{
          flex: 1,
          ...(currentlyBypassed !== previouslyBypassed && {
            backgroundColor: colors.highlight,
          }),
        }}
      />
    </FlexColumn>
  );
};

// list the access modules in the desired order
export const orderedAccessModules: Array<AccessModule> = [
  AccessModule.TWO_FACTOR_AUTH,
  AccessModule.COMPLIANCE_TRAINING,
  AccessModule.CT_COMPLIANCE_TRAINING,
  AccessModule.DATA_USER_CODE_OF_CONDUCT,
  AccessModule.IDENTITY,
  AccessModule.PROFILE_CONFIRMATION,
  AccessModule.PUBLICATION_CONFIRMATION,
];
