import {Button} from 'app/components/buttons';
import {FlexColumn, FlexRow} from 'app/components/flex';
import {FormSection} from 'app/components/forms';
import {CheckBox, RadioButton} from 'app/components/inputs';
import {TooltipTrigger} from 'app/components/popups';
import {SpinnerOverlay} from 'app/components/spinners';
import {TextColumn} from 'app/components/text-column';
import {AouTitle} from 'app/components/text-wrappers';
import {AccountCreationOptions} from 'app/pages/login/account-creation/account-creation-options';
import {DropDownSection, Section, TextInputWithLabel} from 'app/pages/login/account-creation/common';
import colors, {colorWithWhiteness} from 'app/styles/colors';
import {reactStyles, toggleIncludes} from 'app/utils';
import {serverConfigStore} from 'app/utils/navigation';

import {environment} from 'environments/environment';
import {Profile} from 'generated/fetch';
import * as fp from 'lodash/fp';
import * as React from 'react';
import ReCAPTCHA from 'react-google-recaptcha';
import * as validate from 'validate.js';

const styles = reactStyles({
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
});

const SelectAllText = () => {
  return <div style={{color: colors.primary, fontSize: '12px', }}>Select all that apply.</div>;
};

export interface Props {
  profile: Profile;
  // Required if enablePrevious is true.
  onPreviousClick?: (profile: Profile) => void;
  onCancelClick?: (profile: Profile) => void;
  onSubmit: (profile: Profile, captchaToken: string) => Promise<Profile>;
  enableCaptcha: boolean;
  enablePrevious: boolean;
  showStepCount: boolean;
}

interface State {
  captcha: boolean;
  captchaToken: string;
  loading: boolean;
  profile: Profile;
}

export class DemographicSurvey extends React.Component<Props, State> {
  private captchaRef = React.createRef<ReCAPTCHA>();
  constructor(props: any) {
    super(props);
    this.state = {
      captcha: false,
      captchaToken: '',
      loading: false,
      profile: {...this.props.profile},
    };
  }

  createOptionCheckbox(optionKey: string, optionObject: any) {
    const {profile: {demographicSurvey}} = this.props;
    const initialValue = demographicSurvey && demographicSurvey[optionKey] && demographicSurvey[optionKey].includes(optionObject.value);

    return <CheckBox label={optionObject.label} data-test-id={'checkbox-' + optionObject.value.toString()}
                     style={styles.checkbox} key={optionObject.value.toString()}
                     checked={initialValue}
                     wrapperStyle={styles.checkboxWrapper} labelStyle={styles.checkboxLabel}
                     onChange={(value) => this.updateList(optionKey, optionObject.value)}
    />;
  }

  captureCaptchaResponse(token) {
    this.setState({captchaToken: token, captcha: true});
  }

  updateList(key, value) {
    // Toggle Includes removes the element if it already exists and adds if not
    const attributeList = toggleIncludes(value, this.props.profile.demographicSurvey[key] || []);
    this.updateDemographicAttribute(key, attributeList);
  }

  updateDemographicAttribute(attribute, value) {
    this.setState(fp.set(['profile', 'demographicSurvey', attribute], value));
  }

  validateDemographicSurvey(demographicSurvey) {
    const validationCheck = {
      lgbtqIdentity: {
        length: {
          maximum: 255,
          tooLong: '^LGBTQ identity description is too long for our system. ' +
              'Please reduce to 255 or fewer characters.'
        }
      },
    };
    return validate(demographicSurvey, validationCheck);
  }

  render() {
    const {profile: {demographicSurvey}, captcha, captchaToken, loading} = this.state;
    const {requireInstitutionalVerification} = serverConfigStore.getValue();

    const errors = this.validateDemographicSurvey(demographicSurvey);

    return <div style={{marginTop: '1rem', paddingLeft: '1rem', width: '32rem'}}>
      <TextColumn>
        <div style={{fontSize: 28, fontWeight: 400, marginBottom: '.8rem'}}>Optional Demographics Survey</div>
        {this.props.showStepCount &&
          <div style={{fontSize: 16, marginBottom: '.5rem'}}>
            Please complete Step {requireInstitutionalVerification ? '3 of 3' : '2 of 2'}
          </div>
        }
        <div style={{
          backgroundColor: colorWithWhiteness(colors.accent, .85),
          padding: '1rem',
          borderRadius: '5px'
        }}>
          <label style={{fontWeight: 600}}>Answering these questions is optional.</label> <AouTitle/> will
          use this information to measure our success at reaching diverse researchers.
          We will not share your individual answers.
        </div>
      </TextColumn>

      {/*Race section*/}
      <Section header='Race' subHeader='(Optional)' subHeaderStyle={{fontStyle: 'italic'}}>
        <SelectAllText/>
        <FlexColumn style={styles.checkboxAreaContainer}>
          {AccountCreationOptions.race.map((race) => {
            return this.createOptionCheckbox('race', race);
          })}
        </FlexColumn>
      </Section>

      {/*Ethnicity section*/}
      <DropDownSection data-test-id='dropdown-ethnicity'
                       header='Ethnicity'
                       subHeader='(Optional)'
                       options={AccountCreationOptions.ethnicity}
                       value={!!demographicSurvey ? demographicSurvey.ethnicity : null}
                       onChange={(e) => this.updateDemographicAttribute('ethnicity', e)}/>

      {/*Gender Identity section*/}
      <Section header='Gender Identity' subHeader='(Optional)'>
        <SelectAllText/>
        <FlexColumn style={{...styles.checkboxAreaContainer, height: '5rem'}}>
          {AccountCreationOptions.genderIdentity.map((genderIdentity) => {
            return this.createOptionCheckbox('genderIdentityList', genderIdentity);
          })}
        </FlexColumn>
      </Section>

      <Section header='Do you identify as lesbian, gay, bisexual, transgender, queer (LGBTQ),
or another sexual and/or gender minority?' subheader='(Optional)'>
        <FlexColumn>
          <FlexRow style={{alignItems: 'baseline'}}>
            <RadioButton data-test-id='radio-lgbtq-yes' id='radio-lgbtq-yes' onChange={
              (e) => this.updateDemographicAttribute('identifiesAsLgbtq', true)}
                         checked={!!demographicSurvey ? demographicSurvey.identifiesAsLgbtq === true : false}
                         style={{marginRight: '0.5rem'}}/>
            <label htmlFor='radio-lgbtq-yes' style={{paddingRight: '3rem', color: colors.primary}}>Yes</label>
          </FlexRow>
          <FlexRow style={{alignItems: 'baseline'}}>
            <RadioButton data-test-id='radio-lgbtq-no' id='radio-lgbtq-no' onChange={(e) => this.updateDemographicAttribute('identifiesAsLgbtq', false)}
                         checked={!!demographicSurvey ? demographicSurvey.identifiesAsLgbtq === false : false}
                         style={{marginRight: '0.5rem'}}/>
            <label htmlFor='radio-lgbtq-no' style={{color: colors.primary}}>No</label>
          </FlexRow>
        </FlexColumn>
        <label></label>
        <TextInputWithLabel labelText='If yes, please tell us about your LGBTQ+ identity'
                            value={!!demographicSurvey ? demographicSurvey.lgbtqIdentity : ''} inputName='lgbtqIdentity'
                            containerStyle={{width: '26rem', marginTop: '0.5rem'}} inputStyle={{width: '26rem'}}
                            onChange={(value) => this.updateDemographicAttribute('lgbtqIdentity', value)}
                            disabled={!!demographicSurvey ? !demographicSurvey.identifiesAsLgbtq : true}/>
      </Section>

      {/*Sex at birth section*/}
      <Section header='Sex at birth' subHeader='(Optional)'>
        <SelectAllText/>
        <FlexColumn style={{...styles.checkboxAreaContainer, height: '5rem'}}>
          {AccountCreationOptions.sexAtBirth.map((sexAtBirth) => {
            return this.createOptionCheckbox('sexAtBirth', sexAtBirth);
          })}
        </FlexColumn>
      </Section>

      {/*Year of birth section*/}
      <DropDownSection header='Year of Birth'
                       subHeader='(Optional)'
                       options={AccountCreationOptions.Years}
                       value={!!demographicSurvey ? demographicSurvey.yearOfBirth : null}
                       onChange={(e) => this.updateDemographicAttribute('yearOfBirth', e)}
      />
      {/*Disability section*/}
      <Section header='Do you have a physical or cognitive disability?' subHeader='(Optional)'>
        <FlexColumn>
          <FlexRow style={{alignItems: 'baseline'}}>
            <RadioButton id='radio-disability-yes' onChange={
              (e) => this.updateDemographicAttribute('disability', true)}
                         checked={!!demographicSurvey ? demographicSurvey.disability === true : false}
                         style={{marginRight: '0.5rem'}}/>
            <label htmlFor='radio-disability-yes' style={{paddingRight: '3rem', color: colors.primary}}>Yes</label>
          </FlexRow>
          <FlexRow style={{alignItems: 'baseline'}}>
            <RadioButton id='radio-disability-no' onChange={(e) => this.updateDemographicAttribute('disability', false)}
                         checked={!!demographicSurvey ? demographicSurvey.disability === false : false}
                         style={{marginRight: '0.5rem'}}/>
            <label htmlFor='radio-disability-no' style={{color: colors.primary}}>No</label>
          </FlexRow>
        </FlexColumn>
      </Section>
      {/*Education section*/}
      <DropDownSection header='Highest Level of Education Completed'
                       subHeader='(Optional)'
                       options={AccountCreationOptions.levelOfEducation}
                       value={!!demographicSurvey ? demographicSurvey.education : null}
                       onChange={(e) => this.updateDemographicAttribute('education', e)}/>
      {environment.enableCaptcha && this.props.enableCaptcha && <div style={{paddingTop: '1rem'}}>
        <ReCAPTCHA sitekey={environment.captchaSiteKey}
                   ref = {this.captchaRef}
                   onChange={(value) => this.captureCaptchaResponse(value)}/>
      </div>}
      <FormSection style={{paddingBottom: '1rem'}}>
        {this.props.enablePrevious && <Button type='secondary' style={{marginRight: '1rem'}} disabled={loading}
                onClick={() => this.props.onPreviousClick(this.state.profile)}>
          Previous
        </Button>}
        {!this.props.enablePrevious && <Button
            type={'secondary'}
            style={{marginRight: '1rem'}}
            disabled={loading}
            onClick={() => this.props.onCancelClick(this.state.profile)}>
          Cancel
        </Button>}
        <TooltipTrigger content={errors && <React.Fragment>
          <div>Please review the following: </div>
          <ul>
            {Object.keys(errors).map((key) => <li key={errors[key][0]}>{errors[key][0]}</li>)}
          </ul>
        </React.Fragment>}>
          <Button type='primary'
                  disabled={
                    loading
                    || (errors && Object.keys(errors).length > 0)
                    || (!environment.enableCaptcha && !this.props.enableCaptcha && !captcha)
                  }
                  onClick={async() => {
                    this.setState({loading: true});
                    try {
                      const savedProfile = await this.props.onSubmit(this.state.profile, captchaToken);
                      // If the submit fails, then profile is null, and the try will apparently not
                      // always break in time to prevent these next lines from executing. so we
                      // null-check and don't null out the profile if it doesn't exist.
                      if (!!savedProfile) {
                        this.setState({profile: savedProfile, loading: false});
                      } else {
                        this.setState({loading: false});
                      }
                    } catch (error) {
                      // TODO: we need to show some user-facing error message when create account fails.
                      console.log(error);
                      if (environment.enableCaptcha && this.props.enableCaptcha) {
                        // Reset captcha
                        this.captchaRef.current.reset();
                        this.setState({captcha: false});
                      }
                      this.setState({loading: false});
                    }
                  }}
                  data-test-id={'submit-button'}
          >
            Submit
          </Button>
        </TooltipTrigger>
      </FormSection>
      {loading && <SpinnerOverlay overrideStylesOverlay={{position: 'fixed'}}/>}
    </div>;
  }
}
