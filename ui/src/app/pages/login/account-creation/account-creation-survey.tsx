import * as fp from 'lodash/fp';
import * as React from 'react';
import * as validate from 'validate.js';

import {Button} from 'app/components/buttons';
import {FlexColumn, FlexRow} from 'app/components/flex';
import {FormSection} from 'app/components/forms';
import {CheckBox, RadioButton} from 'app/components/inputs';
import {TooltipTrigger} from 'app/components/popups';
import {SpinnerOverlay} from 'app/components/spinners';
import {TextColumn} from 'app/components/text-column';
import {AouTitle} from 'app/components/text-wrappers';
import {profileApi} from 'app/services/swagger-fetch-clients';
import colors from 'app/styles/colors';
import {toggleIncludes} from 'app/utils';
import {serverConfigStore} from 'app/utils/navigation';
import {environment} from 'environments/environment';
import {Profile} from 'generated/fetch';
import ReCAPTCHA from 'react-google-recaptcha';
import {DropDownSection, Section, TextInputWithLabel} from './account-creation';
import {AccountCreationOptions} from './account-creation-options';

const styles = {
  checkbox: {height: 17, width: 17, marginTop: '0.15rem'},
  checkboxWrapper: {display: 'flex', alignItems: 'flex-start', width: '13rem', marginBottom: '0.5rem'},
  checkboxLabel: {
    color: colors.primary,
    fontFamily: 'Montserrat',
    fontSize: '14px',
    fontWeight: 400,
    paddingLeft: '0.25rem',
    paddingRight: '0.5rem'
  },
  checkboxAreaContainer: {
    justifyContent: 'flex-start',
    flexWrap: 'wrap',
    height: '9rem',
    width: '26rem'
  }
};

export interface AccountCreationSurveyProps {
  invitationKey: string;
  termsOfServiceVersion?: number;
  profile: Profile;
  onComplete: (profile: Profile) => void;
  onPreviousClick: (profile: Profile) => void;
}

export interface AccountCreationState {
  captcha: boolean;
  captchaToken: string;
  creatingAccount: boolean;
  profile: Profile;
}

export class AccountCreationSurvey extends React.Component<AccountCreationSurveyProps, AccountCreationState> {
  private captchaRef = React.createRef<ReCAPTCHA>();
  constructor(props: any) {
    super(props);
    this.state = {
      captcha: !environment.enableCaptcha,
      captchaToken: '',
      creatingAccount: false,
      profile: {...this.props.profile},
    };
  }

  // TODO: we should probably bump this logic out of the survey component and either into its own
  // component or into the top-level SignIn component. The fact that we're awkwardly passing the
  // invitation key and tos version into this component (for the sole purpose of relaying this data
  // to the backend) is a telltale sign that this should be refactored.
  createAccount(): void {
    const {invitationKey, termsOfServiceVersion, onComplete} = this.props;
    this.setState({creatingAccount: true});
    profileApi().createAccount({
      profile: this.state.profile,
      captchaVerificationToken: this.state.captchaToken,
      invitationKey: invitationKey,
      termsOfServiceVersion: termsOfServiceVersion
    })
      .then((savedProfile) => {
        this.setState({profile: savedProfile, creatingAccount: false});
        onComplete(savedProfile);
      }).catch(error => {
        // TODO: we need to show some user-facing error message when create account fails.
        console.log(error);
        if (environment.enableCaptcha) {
          // Reset captcha
          this.captchaRef.current.reset();
          this.setState({captcha: false});
        }
        this.setState({creatingAccount: false});

      });
  }

  captureCaptchaResponse(token) {
    this.setState({captchaToken: token, captcha: true});
  }

  updateList(key, value) {
    // Toggle Includes removes the element if it already exist and adds if not
    const attributeList = toggleIncludes(value, this.state.profile.demographicSurvey[key] || []);
    this.updateDemographicAttribute(key, attributeList);
  }

  updateDemographicAttribute(attribute, value) {
    this.setState(fp.set(['profile', 'demographicSurvey', attribute], value));
  }

  createOptionCheckbox(optionKey: string, optionObject: any) {
    const {profile: {demographicSurvey}} = this.state;
    const initialValue = demographicSurvey[optionKey] && demographicSurvey[optionKey].includes(optionObject.value);

    return <CheckBox label={optionObject.label} data-test-id={'checkbox-' + optionObject.value.toString()}
                     style={styles.checkbox} key={optionObject.value.toString()}
                     checked={initialValue}
                     wrapperStyle={styles.checkboxWrapper} labelStyle={styles.checkboxLabel}
                     onChange={(value) => this.updateList(optionKey, optionObject.value)}
    />;
  }

  render() {
    const {profile: {demographicSurvey}, creatingAccount, captcha} = this.state;
    const validationCheck = {
      lgbtqIdentity: {
        length: {
          maximum: 255,
          tooLong: 'is too long for our system. Please reduce to 255 or fewer characters.'
        }
      },
    };
    const errors = validate(demographicSurvey, validationCheck);
    const {requireInstitutionalVerification} = serverConfigStore.getValue();

    return <div style={{marginTop: '1rem', paddingLeft: '3rem', width: '26rem'}}>
      <TextColumn>
        <div style={{fontSize: 28, fontWeight: 400, marginBottom: '.8rem'}}>Optional Demographics Survey</div>
        <div style={{fontSize: 16, marginBottom: '.5rem'}}>
          Please complete Step {requireInstitutionalVerification ? '3 of 3' : '2 of 2'}
        </div>
        <div>
          <label style={{fontWeight: 600}}>Answering these questions is optional.</label> <AouTitle/> will
          use this information to measure our success at reaching diverse researchers.
          We will not share your individual answers.
        </div>
      </TextColumn>

      {/*Race section*/}
      <Section header='Race' subHeader='Select all that apply'>
        <FlexColumn style={styles.checkboxAreaContainer}>
          {AccountCreationOptions.race.map((race) => {
            return this.createOptionCheckbox('race', race);
          })}
        </FlexColumn>
      </Section>

      {/*Ethnicity section*/}
      <DropDownSection data-test-id='dropdown-ethnicity'
                       header='Ethnicity' options={AccountCreationOptions.ethnicity}
                       value={demographicSurvey.ethnicity}
                       onChange={(e) => this.updateDemographicAttribute('ethnicity', e)}/>

      {/*Gender Identity section*/}
      <Section header='Gender Identity' subHeader='Select all that apply'>
        <FlexColumn style={{...styles.checkboxAreaContainer, height: '5rem'}}>
          {AccountCreationOptions.genderIdentity.map((genderIdentity) => {
            return this.createOptionCheckbox('genderIdentityList', genderIdentity);
          })}
        </FlexColumn>
      </Section>

      <Section header='Do you identify as lesbian, gay, bisexual, transgender, queer (LGBTQ),
or another sexual and/or gender minority?'>
        <FlexColumn>
          <FlexRow style={{alignItems: 'baseline'}}>
            <RadioButton data-test-id='radio-lgbtq-yes' id='radio-lgbtq-yes' onChange={
              (e) => this.updateDemographicAttribute('identifiesAsLgbtq', true)}
                         checked={demographicSurvey.identifiesAsLgbtq === true}
                         style={{marginRight: '0.5rem'}}/>
            <label htmlFor='radio-lgbtq-yes' style={{paddingRight: '3rem', color: colors.primary}}>Yes</label>
          </FlexRow>
          <FlexRow style={{alignItems: 'baseline'}}>
            <RadioButton data-test-id='radio-lgbtq-no' id='radio-lgbtq-no' onChange={(e) => this.updateDemographicAttribute('identifiesAsLgbtq', false)}
                         checked={demographicSurvey.identifiesAsLgbtq === false}
                         style={{marginRight: '0.5rem'}}/>
            <label htmlFor='radio-lgbtq-no' style={{color: colors.primary}}>No</label>
          </FlexRow>
        </FlexColumn>
        <label></label>
        <TextInputWithLabel labelText='If yes, please tell us about your LGBTQ+ identity'
                            value={demographicSurvey.lgbtqIdentity} inputName='lgbtqIdentity'
                            containerStyle={{width: '26rem', marginTop: '0.5rem'}} inputStyle={{width: '26rem'}}
                            onChange={(value) => this.updateDemographicAttribute('lgbtqIdentity', value)}
                            disabled={!demographicSurvey.identifiesAsLgbtq}/>
      </Section>

      {/*Sex at birth section*/}
      <Section header='Sex at birth' subHeader='Select all that apply'>
        <FlexColumn style={{...styles.checkboxAreaContainer, height: '5rem'}}>
          {AccountCreationOptions.sexAtBirth.map((sexAtBirth) => {
            return this.createOptionCheckbox('sexAtBirth', sexAtBirth);
          })}
        </FlexColumn>
      </Section>

      {/*Year of birth section*/}
      <DropDownSection header='Year of Birth' options={AccountCreationOptions.Years}
                       value={demographicSurvey.yearOfBirth}
                       onChange={(e) => this.updateDemographicAttribute('yearOfBirth', e)}
      />
      {/*Disability section*/}
      <Section header='Do you have a physical or cognitive disability?'>
        <FlexColumn>
          <FlexRow style={{alignItems: 'baseline'}}>
            <RadioButton id='radio-disability-yes' onChange={
              (e) => this.updateDemographicAttribute('disability', true)}
                         checked={demographicSurvey.disability === true}
                         style={{marginRight: '0.5rem'}}/>
            <label htmlFor='radio-disability-yes' style={{paddingRight: '3rem', color: colors.primary}}>Yes</label>
          </FlexRow>
          <FlexRow style={{alignItems: 'baseline'}}>
            <RadioButton id='radio-disability-no' onChange={(e) => this.updateDemographicAttribute('disability', false)}
                         checked={demographicSurvey.disability === false}
                         style={{marginRight: '0.5rem'}}/>
            <label htmlFor='radio-disability-no' style={{color: colors.primary}}>No</label>
          </FlexRow>
        </FlexColumn>
      </Section>
      {/*Education section*/}
      <DropDownSection header='Highest Level of Education Completed'
                       options={AccountCreationOptions.levelOfEducation}
                       value={demographicSurvey.education}
                       onChange={
                         (e) => this.updateDemographicAttribute('education', e)}/>

      {environment.enableCaptcha && <div style={{paddingTop: '1rem'}}>
        <ReCAPTCHA sitekey={environment.captchaSiteKey}
                   ref = {this.captchaRef}
                   onChange={(value) => this.captureCaptchaResponse(value)}/>
      </div>}
      <FormSection style={{paddingBottom: '1rem'}}>
        <Button type='secondary' style={{marginRight: '1rem'}} disabled={creatingAccount}
                onClick={() => this.props.onPreviousClick(this.state.profile)}>
          Previous
        </Button>
        <TooltipTrigger content={errors && <React.Fragment>
            <div>Please review the following: </div>
            <ul>
              {Object.keys(errors).map((key) => <li key={errors[key][0]}>{errors[key][0]}</li>)}
            </ul>
        </React.Fragment>}>
          <Button type='primary' disabled={creatingAccount || creatingAccount || errors || !captcha}
                  onClick={() => this.createAccount()}>
            Submit
          </Button>
        </TooltipTrigger>
      </FormSection>
      {creatingAccount && <SpinnerOverlay />}
    </div>;
  }
}
