import * as React from 'react';
import * as fp from 'lodash/fp';
import { MultiSelect } from 'primereact/multiselect';
import validate from 'validate.js';

import {
  GeneralDiscoverySource,
  PartnerDiscoverySource,
  Profile,
} from 'generated/fetch';

import { Button } from 'app/components/buttons';
import { FlexColumn, FlexRow } from 'app/components/flex';
import { FormSection } from 'app/components/forms';
import { ClrIcon, InfoIcon, ValidationIcon } from 'app/components/icons';
import {
  ErrorMessage,
  FormValidationErrorMessage,
  Select,
  styles as inputStyles,
  TextAreaWithLengthValidationMessage,
  TextInput,
  TextInputWithLabel,
} from 'app/components/inputs';
import { BulletAlignedUnorderedList } from 'app/components/lists';
import { MultipleChoiceQuestion } from 'app/components/multiple-choice-question';
import { TooltipTrigger } from 'app/components/popups';
import { AoU } from 'app/components/text-wrappers';
import { PubliclyDisplayed } from 'app/icons/publicly-displayed-icon';
import { AccountCreationOptions } from 'app/pages/login/account-creation/account-creation-options';
import {
  commonStyles,
  Section,
  WhyWillSomeInformationBePublic,
} from 'app/pages/login/account-creation/common';
import { profileApi } from 'app/services/swagger-fetch-clients';
import colors from 'app/styles/colors';
import { isBlank, reactStyles } from 'app/utils';
import { AnalyticsTracker } from 'app/utils/analytics';
import { STATE_CODE_MAPPING } from 'app/utils/constants';
import { serverConfigStore } from 'app/utils/stores';
import { NOT_ENOUGH_CHARACTERS_RESEARCH_DESCRIPTION } from 'app/utils/strings';
import { canonicalizeUrl } from 'app/utils/urls';

const styles = reactStyles({
  ...commonStyles,
  multiInputSpacing: {
    marginLeft: '3rem',
  },
  publiclyDisplayedText: {
    fontSize: 12,
    fontWeight: 400,
  },
  textAreaStyleOverride: {
    width: '100%',
    minWidth: '45rem',
  },
  optionalText: {q
    fontSize: 12,
    fontStyle: 'italic',
    fontWeight: 400,
  },
});

const researchPurposeList = [
  <span>Your research training and background</span>,
  <span>
    How you hope to use <AoU /> data for your research
  </span>,
  <span>
    Your research approach and the tools you use for answering your research
    questions (eg: Large datasets of phenotypes and genotypes, community
    engagement and community-based participatory research methods, etc.)
  </span>,
  <span>
    Your experience working with underrepresented populations as a scientist or
    outside of research, and how that experience may inform your work with{' '}
    <AoU /> data
  </span>,
];

const nameLength = 80;

export const formLabels = {
  username: 'New Username',
  givenName: 'First Name',
  familyName: 'Last Name',
  streetAddress1: 'Street Address 1',
  streetAddress2: 'Street Address 2',
  city: 'City',
  state: 'State/Province/Region',
  zipCode: 'Zip/Postal Code',
  country: 'Country',
};

const areaOfResearchId = 'areaOfResearch';

export enum countryDropdownOption {
  unitedStates = 'United States of America',
  other = 'Other',
}

export const stateCodeErrorMessage =
  'State must be a valid 2-letter code (CA, TX, etc.)';

export const MultiSelectWithLabel = (props) => {
  return (
    <FlexColumn style={{ width: '18rem', ...props.containerStyle }}>
      <label style={{ ...styles.text, fontWeight: 600 }}>
        {props.labelText}
      </label>
      <FlexRow style={{ alignItems: 'center', marginTop: '0.15rem' }}>
        <MultiSelect
          className='create-account__degree-select'
          placeholder={props.placeholder}
          filter={false}
          value={props.value}
          onChange={props.onChange}
          options={props.options}
          data-test-id={props.dataTestId}
          style={{ ...styles.sectionInput, overflowY: 'hidden' }}
        />
        {props.children}
      </FlexRow>
    </FlexColumn>
  );
};

export interface AccountCreationProps {
  profile: Profile;
  onComplete: (profile: Profile) => void;
  onPreviousClick: (profile: Profile) => void;
}

export interface AccountCreationState {
  creatingAccount: boolean;
  errors: any;
  profile: Profile;
  showAllFieldsRequiredError: boolean;
  showMostInterestedInKnowingBlurb: boolean;
  usernameCheckInProgress: boolean;
  usernameConflictError: boolean;
  countryDropdownSelection: countryDropdownOption | null;
}

export class AccountCreation extends React.Component<
  AccountCreationProps,
  AccountCreationState
> {
  private usernameCheckTimeout: NodeJS.Timer;

  constructor(props: AccountCreationProps) {
    super(props);
    this.state = this.createInitialState();
  }

  createInitialState(): AccountCreationState {
    const state: AccountCreationState = {
      creatingAccount: false,
      errors: undefined,
      profile: this.props.profile,
      showAllFieldsRequiredError: false,
      showMostInterestedInKnowingBlurb: false,
      usernameCheckInProgress: false,
      usernameConflictError: false,
      countryDropdownSelection: null,
    };

    return state;
  }

  // Returns whether the current username is considered valid. Undefined is returned when the
  // username is empty, or if a username check is in progress.
  isUsernameValid(): boolean | undefined {
    if (
      isBlank(this.state.profile.username) ||
      this.state.usernameCheckInProgress
    ) {
      return undefined;
    }
    return !this.isUsernameValidationError();
  }

  isUsernameValidationError(): boolean {
    return this.state.usernameConflictError || this.usernameInvalidError();
  }

  usernameInvalidError(): boolean {
    const username = this.state.profile.username;
    if (isBlank(username)) {
      return false;
    }
    if (username.trim().length > 64 || username.trim().length < 3) {
      return true;
    }
    // reject these usernames because they would generate invalid emails
    if (username.includes('..') || username.endsWith('.')) {
      return true;
    }

    // Our intention here is to support alphanumeric characters, -'s, _'s, apostrophes, and single .'s in a row.
    // Our desired regex is /^[\w'-][\w.'-]*$/ to match more valid usernames (including apostrophes)
    // but Terra does not currently support that (RW-7618)
    // until they do, we must use a more restrictive regex /^[\w'][\w.']*$/ without apostrophes

    return !new RegExp(/^[\w-][\w.-]*$/).test(username);
  }

  usernameChanged(username: string): void {
    this.updateProfileObject('username', username);
    if (isBlank(username)) {
      return;
    }
    // TODO: This should use a debounce, rather than manual setTimeout()s.
    clearTimeout(this.usernameCheckTimeout);
    this.setState({
      usernameCheckInProgress: true,
      usernameConflictError: false,
    });
    this.usernameCheckTimeout = global.setTimeout(() => {
      profileApi()
        .isUsernameTaken(username)
        .then((body) => {
          this.setState({
            usernameCheckInProgress: false,
            usernameConflictError: body.isTaken,
          });
        })
        .catch((error) => {
          console.log(error);
          this.setState({ usernameCheckInProgress: false });
        });
    }, 300);
  }

  updateProfileObject(attribute: string, value) {
    this.setState(fp.set(['profile', attribute], value));
  }

  updateCountryDropdownSelection(value) {
    this.setState({
      countryDropdownSelection: value,
    });

    if (value === countryDropdownOption.unitedStates) {
      this.updateAddress('country', value);

      const stateCodeGuess = this.autoSelectStateCode(
        this.state.profile.address.state
      );
      if (stateCodeGuess != null) {
        this.updateAddress('state', stateCodeGuess);
      }
    } else {
      this.updateAddress('country', '');
    }
  }

  stateInvalidError(): boolean {
    const { state, country } = this.state.profile.address;
    if (country !== countryDropdownOption.unitedStates) {
      return false;
    }
    return !Object.values(STATE_CODE_MAPPING).includes(state);
  }

  // For a given user inputted state, returns our best guess
  // for which state code they are referring to.
  autoSelectStateCode(state: string): string | null {
    const formattedState = state.trim().toUpperCase();
    if (Object.values(STATE_CODE_MAPPING).includes(formattedState)) {
      return formattedState;
    }
    if (Object.keys(STATE_CODE_MAPPING).includes(formattedState)) {
      return STATE_CODE_MAPPING[formattedState];
    }
    return null;
  }

  updateAddress(attribute: string, value) {
    this.setState(fp.set(['profile', 'address', attribute], value));
  }

  validate(): { [key: string]: string } {
    const { gsuiteDomain } = serverConfigStore.get().config;

    // The validation data for this form is *almost* the raw Profile, except for the additional
    // 'usernameWithEmail' field we're adding, to be able to separate our validation on the
    // username itself from validation of the full email address.
    const validationData = {
      ...this.state.profile,
      usernameWithEmail: this.state.profile.username + '@' + gsuiteDomain,
    };

    const validationCheck = {
      username: {
        presence: {
          allowEmpty: false,
          message: '^Username cannot be blank',
        },
        length: {
          minimum: 4,
          maximum: 64,
        },
      },
      usernameWithEmail: isBlank(validationData.username)
        ? {}
        : {
            email: {
              message: '^Username contains invalid characters',
            },
          },
      givenName: {
        presence: {
          allowEmpty: false,
          message: '^First name cannot be blank',
        },
      },
      familyName: {
        presence: {
          allowEmpty: false,
          message: '^Last name cannot be blank',
        },
      },
      areaOfResearch: {
        presence: {
          allowEmpty: false,
          message: '^Research description cannot be blank',
        },
        length: {
          maximum: 2000,
          message: '^Research description must be 2000 characters or fewer',
        },
      },
      'address.streetAddress1': {
        presence: {
          allowEmpty: false,
          message: '^Street address cannot be blank',
        },
        length: {
          maximum: 95,
          message: '^Street address must be 95 characters or fewer',
        },
      },
      'address.streetAddress2': {
        length: {
          maximum: 95,
          message: '^Street address 2 must be 95 characters or fewer',
        },
      },
      'address.city': {
        presence: {
          allowEmpty: false,
          message: '^City cannot be blank',
        },
        length: {
          maximum: 95,
          message: '^City must be 95 characters or fewer',
        },
      },
      'address.state': {
        presence: {
          allowEmpty: false,
          message: '^State/province/region cannot be blank',
        },
        length: {
          maximum: 95,
          message: '^State/province/region must be 95 characters or fewer',
        },
        inclusion: (_value, attributes) => {
          if (
            attributes.address.country === countryDropdownOption.unitedStates
          ) {
            return {
              within: Object.values(STATE_CODE_MAPPING),
              message: `^${stateCodeErrorMessage}`,
            };
          }
          return false;
        },
      },
      'address.zipCode': {
        presence: {
          allowEmpty: false,
          message: '^Zip/postal code cannot be blank',
        },
        length: {
          maximum: 10,
          message: '^Zip/postal code must be 10 characters or fewer',
        },
      },
      'address.country': {
        presence: {
          allowEmpty: false,
          message: '^Country cannot be blank',
        },
        length: {
          maximum: 95,
          message: '^Country must be 95 characters or fewer',
        },
      },
    };

    // validatejs requires a scheme, which we don't necessarily need in the profile; rather than
    // forking their website regex, just ensure a scheme ahead of validation.
    const { professionalUrl } = validationData;
    const urlError = professionalUrl
      ? validate(
          { website: canonicalizeUrl(professionalUrl) },
          {
            website: {
              url: {
                message: `^Professional URL ${professionalUrl} is not a valid URL`,
              },
            },
          }
        )
      : undefined;

    const errors = {
      ...validate(validationData, validationCheck),
      ...urlError,
    };

    return fp.isEmpty(errors) ? undefined : errors;
  }

  render() {
    const {
      profile: {
        givenName,
        familyName,
        username,
        areaOfResearch,
        professionalUrl = '',
        address: {
          streetAddress1,
          streetAddress2,
          city,
          state,
          zipCode,
          country,
        },
      },
    } = this.state;
    const { gsuiteDomain } = serverConfigStore.get().config;

    const usernameLabelText = (
      <div>
        {formLabels.username}
        <TooltipTrigger
          side='top'
          content={
            <div>
              Usernames can contain only:{' '}
              <ul style={{ marginLeft: '0.15rem' }}>
                <li>letters (a-z)</li>
                <li>numbers (0-9)</li>
                <li>dashes (-)</li>
                <li>underscores (_)</li>
                {/* <li>apostrophes (')</li> temporarily disabled - see RW-7618 */}
                <li>periods (.)</li>
                <li>minimum of 3 characters</li>
                <li>maximum of 64 characters</li>
              </ul>
              <br />
              Usernames cannot begin or end with a period (.) and may not
              contain more than one period (.) in a row.
            </div>
          }
          style={{ marginLeft: '0.75rem' }}
        >
          <InfoIcon style={{ height: '16px', paddingLeft: '2px' }} />
        </TooltipTrigger>
      </div>
    );

    const errors = this.validate();

    return (
      <div
        id='account-creation'
        style={{
          paddingTop: '2.25rem',
          paddingRight: '4.5rem',
          paddingLeft: '1.5rem',
        }}
      >
        <div style={{ fontSize: 28, fontWeight: 400, color: colors.primary }}>
          Create your account
        </div>
        <FlexRow>
          <FlexColumn style={{ marginRight: '3rem' }}>
            <div style={{ ...styles.text, fontSize: 16, marginTop: '1.5rem' }}>
              Please complete Step 2 of 3
            </div>
            <div style={{ ...styles.text, fontSize: 12, marginTop: '0.45rem' }}>
              All fields required unless indicated as optional
            </div>
            <Section
              header={
                <div>
                  Create an <AoU /> username
                </div>
              }
              style={{ marginTop: '1.875rem' }}
            >
              <div>
                <FlexRow>
                  <FlexColumn>
                    <div style={styles.asideText}>
                      We create a 'username'@{gsuiteDomain} Google account which
                      you will use to login to the Workbench. This is a separate
                      account and not related to any personal or professional
                      Google accounts you may have.
                    </div>
                    <TextInputWithLabel
                      value={username}
                      inputId='username'
                      inputName='username'
                      placeholder={formLabels.username}
                      invalid={
                        this.state.usernameConflictError ||
                        this.usernameInvalidError()
                      }
                      containerStyle={{ width: '39rem', marginTop: '1.875rem' }}
                      labelText={usernameLabelText}
                      onChange={(v) => this.usernameChanged(v)}
                    >
                      <div style={inputStyles.iconArea}>
                        <ValidationIcon validSuccess={this.isUsernameValid()} />
                      </div>
                      <i style={{ ...styles.asideText, marginLeft: 4 }}>
                        @{gsuiteDomain}
                      </i>
                    </TextInputWithLabel>
                  </FlexColumn>
                </FlexRow>
                {this.state.usernameConflictError && (
                  <div style={{ height: '2.25rem' }}>
                    <FormValidationErrorMessage id='usernameConflictError'>
                      Username is already taken.
                    </FormValidationErrorMessage>
                  </div>
                )}
                {this.usernameInvalidError() && (
                  <div style={{ height: '2.25rem' }}>
                    <FormValidationErrorMessage id='usernameError'>
                      {username} is not a valid username.
                    </FormValidationErrorMessage>
                  </div>
                )}
              </div>
            </Section>
            <Section
              header={
                <FlexRow style={{ alignItems: 'center' }}>
                  <div>About you</div>
                  <PubliclyDisplayed style={{ marginLeft: '1.5rem' }} />
                </FlexRow>
              }
              style={{ marginTop: '3rem' }}
            >
              <FlexColumn>
                <FlexRow style={{ marginTop: '.375rem' }}>
                  <TextInputWithLabel
                    value={givenName}
                    inputId='givenName'
                    inputName='givenName'
                    placeholder={formLabels.givenName}
                    invalid={givenName.length > nameLength}
                    labelText={formLabels.givenName}
                    onChange={(value) =>
                      this.updateProfileObject('givenName', value)
                    }
                  />
                  {givenName.length > nameLength && (
                    <ErrorMessage id='givenNameError'>
                      First Name must be {nameLength} characters or less.
                    </ErrorMessage>
                  )}
                  <TextInputWithLabel
                    value={familyName}
                    inputId='familyName'
                    inputName='familyName'
                    placeholder={formLabels.familyName}
                    invalid={familyName.length > nameLength}
                    containerStyle={styles.multiInputSpacing}
                    onChange={(v) => this.updateProfileObject('familyName', v)}
                    labelText={formLabels.familyName}
                  />
                  {familyName.length > nameLength && (
                    <ErrorMessage id='familyNameError'>
                      Last Name must be {nameLength} character or less.
                    </ErrorMessage>
                  )}
                </FlexRow>
                <div style={{ marginTop: '1.5rem' }}>
                  <MultiSelectWithLabel
                    placeholder={'Select one or more'}
                    options={AccountCreationOptions.degree}
                    value={this.state.profile.degrees}
                    labelText={
                      <div>
                        Your degrees{' '}
                        <span style={styles.optionalText}>(optional)</span>
                      </div>
                    }
                    onChange={(e) =>
                      this.setState(fp.set(['profile', 'degrees'], e.value))
                    }
                  />
                </div>
              </FlexColumn>
            </Section>
            <Section
              header={
                <React.Fragment>
                  <div>Your institutional mailing address</div>
                  <div style={styles.asideText}>
                    We will use your address if we need to send correspondence
                    about the program.
                  </div>
                </React.Fragment>
              }
              style={{ marginTop: '3rem' }}
            >
              <FlexColumn style={{ lineHeight: '1.5rem' }}>
                <FlexRow style={{ marginTop: '1.5rem' }}>
                  <TextInputWithLabel
                    inputId='streetAddress'
                    dataTestId='streetAddress'
                    inputName='streetAddress'
                    placeholder={formLabels.streetAddress1}
                    value={streetAddress1}
                    labelText={formLabels.streetAddress1}
                    onChange={(value) =>
                      this.updateAddress('streetAddress1', value)
                    }
                  />
                  <TextInputWithLabel
                    inputId='streetAddress2'
                    dataTestId='streetAddress2'
                    inputName='streetAddress2'
                    placeholder={formLabels.streetAddress2}
                    value={streetAddress2}
                    labelText={formLabels.streetAddress2}
                    containerStyle={styles.multiInputSpacing}
                    onChange={(value) =>
                      this.updateAddress('streetAddress2', value)
                    }
                  />
                </FlexRow>
                <FlexRow style={{ marginTop: '1.5rem' }}>
                  <TextInputWithLabel
                    inputId='city'
                    dataTestId='city'
                    inputName='city'
                    placeholder={formLabels.city}
                    value={city}
                    labelText={formLabels.city}
                    onChange={(value) => this.updateAddress('city', value)}
                  />
                  <FlexColumn>
                    <TextInputWithLabel
                      inputId='state'
                      ariaLabel='State'
                      inputName='state'
                      placeholder={formLabels.state}
                      value={state}
                      labelText={formLabels.state}
                      containerStyle={styles.multiInputSpacing}
                      onChange={(value) => this.updateAddress('state', value)}
                    />
                    {this.stateInvalidError() && (
                      <div
                        style={{
                          height: '2.25rem',
                          ...styles.multiInputSpacing,
                        }}
                      >
                        <FormValidationErrorMessage id='stateError'>
                          {stateCodeErrorMessage}
                        </FormValidationErrorMessage>
                      </div>
                    )}
                  </FlexColumn>
                </FlexRow>
                <FlexRow style={{ marginTop: '1.5rem' }}>
                  <TextInputWithLabel
                    inputId='zip'
                    dataTestId='zip'
                    inputName='zip'
                    placeholder={formLabels.zipCode}
                    value={zipCode}
                    labelText={formLabels.zipCode}
                    onChange={(value) => this.updateAddress('zipCode', value)}
                  />
                  <FlexColumn
                    style={{ width: '18rem', ...styles.multiInputSpacing }}
                  >
                    <label style={{ fontWeight: 600, color: colors.primary }}>
                      {formLabels.country}
                    </label>
                    <Select
                      aria-label='Country dropdown'
                      value={this.state.countryDropdownSelection}
                      options={[
                        {
                          value: countryDropdownOption.unitedStates,
                          label: countryDropdownOption.unitedStates,
                        },
                        {
                          value: countryDropdownOption.other,
                          label: countryDropdownOption.other,
                        },
                      ]}
                      onChange={(value) =>
                        this.updateCountryDropdownSelection(value)
                      }
                    />
                    {this.state.countryDropdownSelection ===
                      countryDropdownOption.other && (
                      <div style={{ marginTop: '0.3rem' }}>
                        <TextInput
                          id='country'
                          name='country'
                          aria-label='Country input'
                          placeholder='Please specify'
                          value={country}
                          onChange={(value) =>
                            this.updateAddress('country', value)
                          }
                          style={{ ...commonStyles.sectionInput }}
                        />
                      </div>
                    )}
                  </FlexColumn>
                </FlexRow>
              </FlexColumn>
            </Section>
            <Section
              header={
                <React.Fragment>
                  <FlexRow style={{ alignItems: 'flex-start' }}>
                    <label
                      style={{ maxWidth: '60%' }}
                      htmlFor={areaOfResearchId}
                    >
                      Your research background, experience, and research
                      interests
                    </label>
                    <PubliclyDisplayed style={{ marginLeft: '1.5rem' }} />
                  </FlexRow>
                  <div style={{ ...styles.asideText, marginTop: '.125px' }}>
                    This information will be posted publicly on the <AoU />{' '}
                    Research Hub website to inform program participants.
                    <span style={{ marginLeft: 2, fontSize: 12 }}>
                      (2000 character limit)
                    </span>
                  </div>
                  <div style={{ marginTop: '1.125rem' }}>
                    <FlexRow
                      style={{ color: colors.accent, alignItems: 'center' }}
                    >
                      <div
                        style={{ cursor: 'pointer', fontSize: 14 }}
                        onClick={() =>
                          this.setState((previousState) => ({
                            showMostInterestedInKnowingBlurb:
                              !previousState.showMostInterestedInKnowingBlurb,
                          }))
                        }
                      >
                        <AoU /> participants are most interested in knowing:
                      </div>
                      <ClrIcon
                        shape='angle'
                        style={{
                          transform: this.state.showMostInterestedInKnowingBlurb
                            ? 'rotate(180deg)'
                            : 'rotate(90deg)',
                        }}
                      />
                    </FlexRow>
                    {this.state.showMostInterestedInKnowingBlurb && (
                      <ul
                        style={{ ...styles.asideList, marginLeft: '1.125rem' }}
                      >
                        {researchPurposeList.map((value, index) => (
                          <li key={index} style={styles.asideText}>
                            {value}
                          </li>
                        ))}
                      </ul>
                    )}
                  </div>
                </React.Fragment>
              }
              style={{ marginTop: '3rem' }}
              sectionHeaderStyles={{ borderBottom: null }}
            >
              <TextAreaWithLengthValidationMessage
                id={areaOfResearchId}
                initialText={areaOfResearch}
                maxCharacters={2000}
                onChange={(s: string) =>
                  this.updateProfileObject('areaOfResearch', s)
                }
                textBoxStyleOverrides={styles.textAreaStyleOverride}
                tooShortWarningCharacters={100}
                tooShortWarning={NOT_ENOUGH_CHARACTERS_RESEARCH_DESCRIPTION}
              />
            </Section>
            <Section
              header={
                <React.Fragment>
                  <FlexRow style={{ alignItems: 'flex-start' }}>
                    <div style={{ maxWidth: '60%' }}>
                      Your professional profile or bio page below, if available
                    </div>
                    <PubliclyDisplayed style={{ marginLeft: '1.5rem' }} />
                  </FlexRow>
                  <div
                    style={{
                      ...styles.asideText,
                      ...styles.optionalText,
                      marginTop: '.1875rem',
                    }}
                  >
                    (Optional)
                  </div>
                  <div
                    style={{
                      ...styles.asideText,
                      marginTop: '.75rem',
                      marginBottom: '.75rem',
                    }}
                  >
                    You could provide a link to your faculty bio page from your
                    institution's website, your LinkedIn profile page, or
                    another webpage featuring your work. This will allow <AoU />{' '}
                    researchers and participants to learn more about your work
                    and publications.
                  </div>
                </React.Fragment>
              }
              style={{ marginTop: '3rem' }}
            >
              <TextInputWithLabel
                dataTestId='professionalUrl'
                inputName='professionalUrl'
                placeholder='Professional Url'
                value={professionalUrl}
                labelText={<div>Paste Professional URL here</div>}
                containerStyle={{ width: '39rem', marginTop: '.375rem' }}
                onChange={(value) =>
                  this.updateProfileObject('professionalUrl', value)
                }
              />
            </Section>
            <Section style={{ marginTop: '3rem' }}>
              <MultipleChoiceQuestion
                question={
                  'How did you learn about the All of Us Researcher\n' +
                  '                      Workbench?'
                }
                options={[
                  {
                    label: 'Research All of Us Website',
                    value: GeneralDiscoverySource.RESEARCHALLOFUSWEBSITE,
                  },
                  {
                    label: 'Social Media',
                    value: GeneralDiscoverySource.SOCIALMEDIA,
                  },
                  {
                    label: 'Journal or News Article',
                    value: GeneralDiscoverySource.JOURNALORNEWSARTICLE,
                  },
                  {
                    label: 'Activity, Presentation, or Event',
                    value: GeneralDiscoverySource.ACTIVITYPRESENTATIONOREVENT,
                  },
                  {
                    label: 'Friends or Colleagues',
                    value: GeneralDiscoverySource.FRIENDSORCOLLEAGUES,
                  },
                  {
                    label: 'Other Website',
                    value: GeneralDiscoverySource.OTHERWEBSITE,
                  },
                  {
                    label: 'Other',
                    value: GeneralDiscoverySource.OTHER,
                    showInput: true,
                    otherText:
                      this.state.profile.generalDiscoverySourceOtherText,
                    otherTextMaxLength: 250,
                    otherTextPlaceholder: 'Please Describe',
                    onChange: (value) => {
                      this.updateProfileObject(
                        'generalDiscoverySourceOtherText',
                        value ? '' : null
                      );
                    },
                    onChangeOtherText: (value) =>
                      this.updateProfileObject(
                        'generalDiscoverySourceOtherText',
                        value
                      ),
                  },
                ]}
                multiple
                selected={this.state.profile.generalDiscoverySources}
                onChange={(value) =>
                  this.updateProfileObject('generalDiscoverySources', value)
                }
              />
            </Section>
            <Section style={{ marginTop: '3rem' }}>
              <MultipleChoiceQuestion
                question={
                  'Did you learn about the All of Us Researcher Workbench from any of these program partners?'
                }
                options={[
                  {
                    label:
                      'All of Us Evenings with Genetics Research Program, Baylor College \n' +
                      'of Medicine, Department of Molecular and Human Genetics',
                    value:
                      PartnerDiscoverySource.ALLOFUSEVENINGSWITHGENETICSRESEARCHPROGRAM,
                  },
                  {
                    label: 'All of Us Research Program Staff',
                    value: PartnerDiscoverySource.ALLOFUSRESEARCHPROGRAMSTAFF,
                  },
                  {
                    label: 'All of Us Researcher Academy/RTI International',
                    value:
                      PartnerDiscoverySource.ALLOFUSRESEARCHERACADEMYRTIINTERNATIONAL,
                  },
                  {
                    label:
                      'American Association on Health and Disability (AAHD)',
                    value:
                      PartnerDiscoverySource.AMERICANASSOCIATIONONHEALTHANDDISABILITYAAHD,
                  },
                  {
                    label: 'Asian Health Coalition',
                    value: PartnerDiscoverySource.ASIANHEALTHCOALITION,
                  },
                  {
                    label: 'CTSA/PACER Community Network (CPCN)',
                    value: PartnerDiscoverySource.CTSAPACERCOMMUNITYNETWORKCPCN,
                  },
                  {
                    label: 'Data and Research Center (DRC)',
                    value: PartnerDiscoverySource.DATAANDRESEARCHCENTERDRC,
                  },
                  {
                    label: 'Delta Research and Educational Foundation (DREF)',
                    value:
                      PartnerDiscoverySource.DELTARESEARCHANDEDUCATIONALFOUNDATIONDREF,
                  },
                  {
                    label: 'FiftyForward (Senior Citizens, Inc.)',
                    value: PartnerDiscoverySource.FIFTYFORWARDSENIORCITIZENSINC,
                  },
                  {
                    label:
                      'IGNITE Northwell Health, Feinstein Institute for Medical Research',
                    value:
                      PartnerDiscoverySource.IGNITENORTHWELLHEALTHFEINSTEININSTITUTEFORMEDICALRESEARCH,
                  },
                  {
                    label: 'National Alliance for Hispanic Health (NAHH)',
                    value:
                      PartnerDiscoverySource.NATIONALALLIANCEFORHISPANICHEALTHNAHH,
                  },
                  {
                    label: 'National Baptist Convention, USA, Inc.',
                    value:
                      PartnerDiscoverySource.NATIONALBAPTISTCONVENTIONUSAINC,
                  },
                  {
                    label: 'Network of the National Library of Medicine (NNLM)',
                    value:
                      PartnerDiscoverySource.NETWORKOFTHENATIONALLIBRARYOFMEDICINENNLM,
                  },
                  {
                    label: 'PRIDEnet/Stanford University',
                    value: PartnerDiscoverySource.PRIDENETSTANFORDUNIVERSITY,
                  },
                  {
                    label: 'Pyxis Partners',
                    value: PartnerDiscoverySource.PYXISPARTNERS,
                  },
                  {
                    label: 'Scripps Research Institute',
                    value: PartnerDiscoverySource.SCRIPPSRESEARCHINSTITUTE,
                  },
                  {
                    label: 'Other',
                    value: PartnerDiscoverySource.OTHER,
                    showInput: true,
                    otherText:
                      this.state.profile.partnerDiscoverySourceOtherText,
                    otherTextMaxLength: 250,
                    otherTextPlaceholder: 'Please Describe',
                    onChange: (value) => {
                      this.updateProfileObject(
                        'partnerDiscoverySourceOtherText',
                        value ? '' : null
                      );
                    },
                    onChangeOtherText: (value) =>
                      this.updateProfileObject(
                        'partnerDiscoverySourceOtherText',
                        value
                      ),
                  },
                  {
                    label: 'None of the Above',
                    value: PartnerDiscoverySource.NONEOFTHEABOVE,
                  },
                ]}
                multiple
                selected={this.state.profile.partnerDiscoverySources}
                onChange={(value) =>
                  this.updateProfileObject('partnerDiscoverySources', value)
                }
              />
            </Section>
            <FormSection style={{ marginTop: '6rem', paddingBottom: '1.5rem' }}>
              <Button
                aria-label='Previous'
                type='secondary'
                style={{ marginRight: '1.5rem' }}
                onClick={() => this.props.onPreviousClick(this.state.profile)}
              >
                Previous
              </Button>
              <TooltipTrigger
                content={
                  errors && (
                    <React.Fragment>
                      <div>Please review the following: </div>
                      <BulletAlignedUnorderedList>
                        {Object.keys(errors).map((key) => (
                          <li key={errors[key][0]}>{errors[key][0]}</li>
                        ))}
                      </BulletAlignedUnorderedList>
                    </React.Fragment>
                  )
                }
                disabled={!errors}
              >
                <Button
                  aria-label='Next'
                  disabled={
                    this.state.usernameCheckInProgress ||
                    this.isUsernameValidationError() ||
                    Boolean(errors)
                  }
                  style={{ height: '3rem', width: '15rem' }}
                  onClick={() => {
                    AnalyticsTracker.Registration.CreateAccountPage();
                    this.props.onComplete(this.state.profile);
                  }}
                >
                  Next
                </Button>
              </TooltipTrigger>
            </FormSection>
          </FlexColumn>
          <FlexColumn>
            <FlexColumn style={styles.asideContainer}>
              <WhyWillSomeInformationBePublic />
            </FlexColumn>
          </FlexColumn>
        </FlexRow>
      </div>
    );
  }
}
