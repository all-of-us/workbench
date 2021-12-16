// this is a temporary file to assist with the migration from (class-based) AdminUser to (functional) AdminUserProfile
// for RW-7536

import * as React from 'react';
import {CSSProperties} from 'react';
import {Link} from 'react-router-dom';
import * as fp from 'lodash/fp';
import {Dropdown} from 'primereact/dropdown';

import {formatFreeCreditsUSD, reactStyles} from 'app/utils';
import colors, {colorWithWhiteness} from 'app/styles/colors';
import {
  AccountPropertyUpdate,
  InstitutionalRole,
  Profile,
  PublicInstitutionDetails,
  VerifiedInstitutionalAffiliation
} from 'generated/fetch';
import {serverConfigStore} from 'app/utils/stores';
import {institutionApi, userAdminApi} from 'app/services/swagger-fetch-clients';
import {ClrIcon} from 'app/components/icons';
import {FlexColumn} from 'app/components/flex';
import {getRoleOptions} from 'app/utils/institutions';
import {TextInputWithLabel} from 'app/components/inputs';

export const commonStyles = reactStyles({
  semiBold: {
    fontWeight: 600
  },
  label: {
    color: colors.primary,
    fontSize: '14px',
    fontWeight: 'bold',
    paddingLeft: '1em',
    paddingTop: '1em',
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
    color: colors.primary
  },
  userAdminArrow: {
    backgroundColor: colorWithWhiteness(colors.accent, .85),
    color: colors.accent,
    borderRadius: '18px',
    transform: 'rotate(270deg)'
  },
});

export const adminGetProfile = async(usernameWithoutGsuiteDomain: string): Promise<Profile> => {
  const {gsuiteDomain} = serverConfigStore.get().config;
  return userAdminApi().getUserByUsername(usernameWithoutGsuiteDomain + '@' + gsuiteDomain);
}

export const getPublicInstitutionDetails = async(): Promise<PublicInstitutionDetails[]> => {
  const institutionsResponse = await institutionApi().getPublicInstitutionDetails();
  const institutions = institutionsResponse?.institutions;
  return fp.sortBy('displayName', institutions);
}

/**
 * Present an ordered list of dollar options with the following values:
 * $300 to $1000, in $100 increments
 * $1000 to $10,000, in $500 increments
 * Plus the user's current quota value, if it's not already one of these
 */
export const getFreeCreditLimitOptions = (freeTierDollarQuota?: number) => {
  const START1 = 300;
  const END1 = 1000;
  const END2 = 10000;

  // gotcha: argument order for rangeStep is (step, start, end)
  // IntelliJ incorrectly believes the order is (start, end, step)
  const below1000 = fp.rangeStep(100, START1, END1+1);
  const over1000 = fp.rangeStep(500, END1, END2+1);

  const defaultsPlusMaybeOverride = new Set([...below1000, ...over1000, freeTierDollarQuota ?? START1]);

  // gotcha: JS sorts numbers lexicographically by default
  const numericallySorted = Array.from(defaultsPlusMaybeOverride).sort((a, b) => a - b);

  return fp.map((limit) => ({label: formatFreeCreditsUSD(limit), value: limit}), numericallySorted);
}

export const getFreeCreditUsage = (profile: Profile): string => {
  const {freeTierDollarQuota, freeTierUsage} = profile;
  return `${formatFreeCreditsUSD(freeTierUsage)} used of ${formatFreeCreditsUSD(freeTierDollarQuota)} limit`;
}

// returns the updated profile value only if it has changed
export const getUpdatedProfileValue = (oldProfile: Profile, updatedProfile: Profile, attribute: string) => {
  const oldValue = fp.get([attribute], oldProfile);
  const updatedValue = fp.get([attribute], updatedProfile);
  if (!fp.isEqual(oldValue, updatedValue)) {
    return updatedValue;
  } else {
    return null;
  }
}

export const enableSave = (oldProfile: Profile, updatedProfile: Profile, errors): boolean =>
  !errors && !fp.isEqual(oldProfile, updatedProfile);

export const updateAccountProperties = async(oldProfile: Profile, updatedProfile: Profile): Promise<Profile> => {
  const {username} = updatedProfile;
  const request: AccountPropertyUpdate = {
    username,
    freeCreditsLimit: getUpdatedProfileValue(oldProfile, updatedProfile, 'freeTierDollarQuota'),
    contactEmail: getUpdatedProfileValue(oldProfile, updatedProfile, 'contactEmail'),
    affiliation: getUpdatedProfileValue(oldProfile, updatedProfile, 'verifiedInstitutionalAffiliation'),
    accessBypassRequests: [],  // coming soon: RW-4958
  };

  return userAdminApi().updateAccountProperties(request);
}

export const DropdownWithLabel =
  ({label, options, initialValue, onChange, disabled= false, dataTestId, labelStyle = {}, dropdownStyle = {}}) => {
  return <FlexColumn data-test-id={dataTestId} style={{marginTop: '1em'}}>
    <label style={{...commonStyles.label, ...labelStyle}}>{label}</label>
    <Dropdown
      style={{...commonStyles.dropdown, ...dropdownStyle}}
      options={options}
      onChange={(e) => onChange(e)}
      value={initialValue}
      disabled={disabled}
    />
  </FlexColumn>;
};

interface ContactEmailTextInputProps {
  contactEmail: string,
  onChange: Function,
  labelStyle?: CSSProperties,
  inputStyle?: CSSProperties,
  containerStyle?: CSSProperties,
}
export const ContactEmailTextInput = ({contactEmail, onChange, labelStyle, inputStyle, containerStyle}: ContactEmailTextInputProps) => {
  return <TextInputWithLabel
    inputId={'contactEmail'}
    labelText={'Contact email'}
    value={contactEmail}
    labelStyle={{...commonStyles.label, ...labelStyle}}
    inputStyle={{...commonStyles.textInput, ...inputStyle}}
    containerStyle={{...commonStyles.textInputContainer, ...containerStyle}}
    onChange={value => onChange(value)}/>;
}

interface FreeCreditsDropdownProps {
  initialLimit?: number,
  currentLimit?: number,
  onChange: Function,
  labelStyle?: CSSProperties,
  dropdownStyle?: CSSProperties,
}
export const FreeCreditsDropdown = ({initialLimit, currentLimit, onChange, labelStyle, dropdownStyle}: FreeCreditsDropdownProps) => {
  return <DropdownWithLabel
    dataTestId={'freeTierDollarQuota'}
    label={'Free credit limit'}
    options={getFreeCreditLimitOptions(initialLimit)}
    initialValue={currentLimit}
    labelStyle={labelStyle}
    dropdownStyle={dropdownStyle}
    onChange={(event) => onChange(event)}/>;
}

interface InstitutionDropdownProps {
  institutions?: PublicInstitutionDetails[],
  initialInstitutionShortName?: string,
  onChange: Function,
  labelStyle?: CSSProperties,
  dropdownStyle?: CSSProperties,
}
export const InstitutionDropdown =
  ({institutions, initialInstitutionShortName, onChange, labelStyle, dropdownStyle}: InstitutionDropdownProps) => {
  const options = fp.map(({displayName, shortName}) => ({label: displayName, value: shortName}), institutions);
  return institutions
    ? <DropdownWithLabel
      dataTestId={'verifiedInstitution'}
      label={'Verified institution'}
      options={options}
      initialValue={initialInstitutionShortName}
      labelStyle={labelStyle}
      dropdownStyle={dropdownStyle}
      onChange={(event) => onChange(event)}/>
    : null;
}

interface InstitutionalRoleDropdownProps {
  institutions?: PublicInstitutionDetails[],
  initialAffiliation?: VerifiedInstitutionalAffiliation,
  onChange: Function,
  labelStyle?: CSSProperties,
  dropdownStyle?: CSSProperties,
}
export const InstitutionalRoleDropdown =
  ({institutions, initialAffiliation, onChange, labelStyle, dropdownStyle}: InstitutionalRoleDropdownProps) => {
  const options = getRoleOptions(institutions, initialAffiliation?.institutionShortName);
  return (institutions && initialAffiliation)
    ? <DropdownWithLabel
      dataTestId={'institutionalRole'}
      label={'Institutional role'}
      disabled={!initialAffiliation.institutionShortName}
      options={options}
      initialValue={initialAffiliation.institutionalRoleEnum}
      labelStyle={labelStyle}
      dropdownStyle={dropdownStyle}
      onChange={(event) => onChange(event)}/>
    : null;
}

interface InstitutionalRoleOtherTextProps {
  affiliation?: VerifiedInstitutionalAffiliation,
  onChange: Function,
  labelStyle?: CSSProperties,
  inputStyle?: CSSProperties,
  containerStyle?: CSSProperties,
}
export const InstitutionalRoleOtherTextInput =
  ({affiliation, onChange, labelStyle, inputStyle, containerStyle}: InstitutionalRoleOtherTextProps) => {
  return (affiliation?.institutionalRoleEnum === InstitutionalRole.OTHER)
    ? <TextInputWithLabel
      dataTestId={'institutionalRoleOtherText'}
      labelText={'Institutional role description'}
      placeholder={affiliation?.institutionalRoleOtherText}
      labelStyle={{...commonStyles.label, ...labelStyle}}
      inputStyle={{...commonStyles.textInput, ...inputStyle}}
      containerStyle={{...commonStyles.textInputContainer, ...containerStyle}}
      onChange={(value) => onChange(value)}/>
    : null;
}

export const UserAdminTableLink = () => <Link to='/admin/users'>
  <ClrIcon
    shape='arrow'
    size={37}
    style={commonStyles.userAdminArrow}
  />
</Link>;

export const UserAuditLink = (props: {usernameWithoutDomain: string, style?: CSSProperties, children}) => <Link
  style={props.style}
  to={`/admin/user-audit/${props.usernameWithoutDomain}`}
  target='_blank'
>
  {props.children}
</Link>
