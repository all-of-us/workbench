import {Button} from 'app/components/buttons';
import {FlexColumn, FlexRow} from 'app/components/flex';
import {FormSection} from 'app/components/forms';
import {CheckBox, RadioButton, TextInputWithLabel} from 'app/components/inputs';
import {TooltipTrigger} from 'app/components/popups';
import {SpinnerOverlay} from 'app/components/spinners';
import {TextColumn} from 'app/components/text-column';
import {AouTitle} from 'app/components/text-wrappers';
import {withProfileErrorModal, WithProfileErrorModalProps} from 'app/components/with-error-modal';
import {AccountCreationOptions} from 'app/pages/login/account-creation/account-creation-options';
import {
  DropDownSection,
  Section,
} from 'app/pages/login/account-creation/common';
import colors, {colorWithWhiteness} from 'app/styles/colors';
import {reactStyles, toggleIncludes} from 'app/utils';

import {convertAPIError, reportError} from 'app/utils/errors';
import {environment} from 'environments/environment';
import {Disability, GenderIdentity, Profile, Race, SexAtBirth} from 'generated/fetch';
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

export interface Props extends WithProfileErrorModalProps {
  profile: Profile;
  // Required if enablePrevious is true.
  onPreviousClick?: (profile: Profile) => void;
  onCancelClick?: (profile: Profile) => void;
  saveProfile: (profile: Profile, captchaToken: string) => Promise<Profile>;
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

const isChecked = (demographicSurvey, optionKey, value) =>
  demographicSurvey && demographicSurvey[optionKey] && demographicSurvey[optionKey].includes(value);

export const DemographicSurvey = fp.flow(withProfileErrorModal)(
  class DemographicSurveyComponent extends React.Component<Props, State> {
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

  // Clicking this checkbox will remove other answers within the 'checkbox group'
    createNoAnswerCheckbox({value, label}, optionKey: string) {
      const {profile: {demographicSurvey}} = this.state;

      return <CheckBox label={label}
                     data-test-id={`checkbox-${optionKey}-${value}`}
                     style={styles.checkbox}
                     key={value.toString()}
                     checked={isChecked(demographicSurvey, optionKey, value)}
                     wrapperStyle={styles.checkboxWrapper} labelStyle={styles.checkboxLabel}
                     manageOwnState={false}
                     onChange={nextValue => this.setState(fp.set(['profile', 'demographicSurvey', optionKey], nextValue ? [value] : []))}
    />;
    }

  // Clicking one of these options will remove the prefer not to answer selection, if selected
    createOptionCheckbox(optionKey: string, optionObject: any, preferNoAnswerValue: any) {
      const {profile: {demographicSurvey}} = this.state;
      const initialValue = demographicSurvey && demographicSurvey[optionKey] && demographicSurvey[optionKey].includes(optionObject.value);

      return <CheckBox label={optionObject.label}
                     data-test-id={'checkbox-' + optionObject.value.toString()}
                     style={styles.checkbox} key={optionObject.value.toString()}
                     checked={initialValue}
                     manageOwnState={false}
                     wrapperStyle={styles.checkboxWrapper} labelStyle={styles.checkboxLabel}
                     onChange={(value) => this.updateList(optionKey, optionObject.value, preferNoAnswerValue)}
    />;
    }

    captureCaptchaResponse(token) {
      this.setState({captchaToken: token, captcha: true});
    }

    checkboxArea(optionKey, pntaOption, allOptions) {
      return fp.flow(
        fp.remove({value: pntaOption}), // Remove the PNTA checkbox from the list, return list w/o PNTA
        fp.map(item => this.createOptionCheckbox(optionKey, item, pntaOption)), // Create checkboxes sans PNTA
        v => [...v, this.createNoAnswerCheckbox(fp.find({value: pntaOption}, allOptions), optionKey )] // Append PNTA checkbox to list
    )(allOptions);
    }

    updateList(key, value, preferNoAnswerValue) {
    // Toggle Includes removes the element if it already exists and adds if not
      const attributeList = fp.flow(
        toggleIncludes(value),
        fp.remove(v => v === preferNoAnswerValue)
    )(this.state.profile.demographicSurvey[key] || []);
      this.updateDemographicAttribute(key, attributeList);
    }

    updateDemographicAttribute(attribute, value) {
      this.setState(fp.set(['profile', 'demographicSurvey', attribute], value));
    }

    validateDemographicSurvey(demographicSurvey) {
      validate.validators.nullBoolean = v => (v === true || v === false || v === null) ? undefined : 'value must be selected';
      const validationCheck = {
        race: { presence: { allowEmpty: false},  },
        ethnicity: { presence: { allowEmpty: false } },
        genderIdentityList: { presence: { allowEmpty: false } },
        identifiesAsLgbtq: { nullBoolean: {} },
        sexAtBirth: { presence: { allowEmpty: false } },
        yearOfBirth: { presence: { allowEmpty: false } },
        disability: { presence: { allowEmpty: false } },
        education: { presence: { allowEmpty: false } },
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

    async saveSurvey() {
      const {captchaToken} = this.state;
      this.setState({loading: true});

      try {
        const savedProfile = await this.props.saveProfile(this.state.profile, captchaToken);
        this.setState(prevState => ({profile: savedProfile || prevState.profile, loading: false}));
      } catch (error) {
        reportError(error);
        const {message} = await convertAPIError(error);
        this.props.showProfileErrorModal(message);
        if (environment.enableCaptcha && this.props.enableCaptcha) {
          // Reset captcha
          this.captchaRef.current.reset();
          this.setState({captcha: false});
        }
        this.setState({loading: false});
      }
    }

    render() {
      const {profile: {demographicSurvey = {}}, captcha, loading} = this.state;

      const errors = this.validateDemographicSurvey(demographicSurvey);

      return <div style={{marginTop: '1rem', paddingLeft: '1rem', width: '32rem'}}>
      <TextColumn>
        <div style={{fontSize: 28, fontWeight: 400, marginBottom: '.8rem'}}>Demographics Survey</div>
        {this.props.showStepCount &&
          <div style={{fontSize: 16, marginBottom: '.5rem'}}>
            Please complete Step 3 of 3
          </div>
        }
        <div style={{
          backgroundColor: colorWithWhiteness(colors.accent, .75),
          padding: '1rem',
          borderRadius: '5px'
        }}>
          The <AouTitle/> will
          use this information to measure our success at reaching diverse researchers.
          We will not share your individual answers.
        </div>
      </TextColumn>

      {/*Race section*/}
      <Section header='Race'>
        <SelectAllText/>
        <FlexColumn style={styles.checkboxAreaContainer}>
          { this.checkboxArea('race', Race.PREFERNOANSWER, AccountCreationOptions.race) }
        </FlexColumn>
      </Section>

      {/*Ethnicity section*/}
      <DropDownSection data-test-id='dropdown-ethnicity'
                       header='Ethnicity'
                       options={AccountCreationOptions.ethnicity}
                       value={!!demographicSurvey ? demographicSurvey.ethnicity : null}
                       onChange={(e) => this.updateDemographicAttribute('ethnicity', e)}/>

      {/*Gender Identity section*/}
      <Section header='Gender Identity'>
        <SelectAllText/>
        <FlexColumn style={{...styles.checkboxAreaContainer, height: '5rem'}}>
          { this.checkboxArea('genderIdentityList', GenderIdentity.PREFERNOANSWER, AccountCreationOptions.genderIdentity) }
        </FlexColumn>
      </Section>

      <Section header='Do you identify as lesbian, gay, bisexual, transgender, queer (LGBTQ),
or another sexual and/or gender minority?'>
        <FlexColumn>
          <FlexRow style={{alignItems: 'baseline'}}>
            <RadioButton data-test-id='radio-lgbtq-yes'
                         id='radio-lgbtq-yes'
                         onChange={() => this.updateDemographicAttribute('identifiesAsLgbtq', true)}
                         checked={!!demographicSurvey ? demographicSurvey.identifiesAsLgbtq === true : false}
                         style={{marginRight: '0.5rem'}}/>
            <label htmlFor='radio-lgbtq-yes' style={{paddingRight: '3rem', color: colors.primary}}>Yes</label>
          </FlexRow>
          <FlexRow style={{alignItems: 'baseline'}}>
            <RadioButton data-test-id='radio-lgbtq-no'
                         id='radio-lgbtq-no'
                         onChange={() => this.updateDemographicAttribute('identifiesAsLgbtq', false)}
                         checked={!!demographicSurvey ? demographicSurvey.identifiesAsLgbtq === false : false}
                         style={{marginRight: '0.5rem'}}/>
            <label htmlFor='radio-lgbtq-no' style={{color: colors.primary}}>No</label>
          </FlexRow>
          <FlexRow style={{alignItems: 'baseline'}}>
            <RadioButton data-test-id='radio-lgbtq-pnta'
                         id='radio-lgbtq-pnta'
                         onChange={() => this.updateDemographicAttribute('identifiesAsLgbtq', null)}
                         checked={!!demographicSurvey ? demographicSurvey.identifiesAsLgbtq === null : false}
                         style={{marginRight: '0.5rem'}}/>
            <label htmlFor='radio-lgbtq-pnta' style={{color: colors.primary}}>Prefer not to answer</label>
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
      <Section header='Sex at birth'>
        <SelectAllText/>
        <FlexColumn style={{...styles.checkboxAreaContainer, height: '5rem'}}>
          { this.checkboxArea('sexAtBirth', SexAtBirth.PREFERNOANSWER, AccountCreationOptions.sexAtBirth) }
        </FlexColumn>
      </Section>

      {/*Year of birth section*/}
      <DropDownSection data-test-id='year-of-birth'
                       header='Year of Birth'
                       options={AccountCreationOptions.Years}
                       value={!!demographicSurvey ? demographicSurvey.yearOfBirth : null}
                       onChange={(e) => this.updateDemographicAttribute('yearOfBirth', e)}
      />
      {/*Disability section*/}
      <Section header='Do you have a physical or cognitive disability?'>
        <FlexColumn>
          <FlexRow style={{alignItems: 'baseline'}}>
            <RadioButton id='radio-disability-yes'
                         onChange={() => this.updateDemographicAttribute('disability', Disability.True)}
                         checked={!!demographicSurvey ? demographicSurvey.disability === Disability.True : false}
                         style={{marginRight: '0.5rem'}}/>
            <label htmlFor='radio-disability-yes' style={{paddingRight: '3rem', color: colors.primary}}>Yes</label>
          </FlexRow>
          <FlexRow style={{alignItems: 'baseline'}}>
            <RadioButton id='radio-disability-no'
                         onChange={() => this.updateDemographicAttribute('disability', Disability.False)}
                         checked={!!demographicSurvey ? demographicSurvey.disability === Disability.False : false}
                         style={{marginRight: '0.5rem'}}/>
            <label htmlFor='radio-disability-no' style={{color: colors.primary}}>No</label>
          </FlexRow>
          <FlexRow style={{alignItems: 'baseline'}}>
            <RadioButton id='radio-disability-pnta'
                         onChange={() => this.updateDemographicAttribute('disability', Disability.PREFERNOANSWER)}
                         checked={!!demographicSurvey ? demographicSurvey.disability === Disability.PREFERNOANSWER : false}
                         style={{marginRight: '0.5rem'}}/>
            <label htmlFor='radio-disability-pnta' style={{color: colors.primary}}>Prefer not to answer</label>
          </FlexRow>
        </FlexColumn>
      </Section>
      {/*Education section*/}
      <DropDownSection data-test-id='highest-education-level'
                       header='Highest Level of Education Completed'
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
            <li>You may select "Prefer not to answer" for each unfilled item to continue</li>
          </ul>
        </React.Fragment>}>
          <Button type='primary'
                  disabled={
                    loading
                    || (errors && Object.keys(errors).length > 0)
                    || (!environment.enableCaptcha && !this.props.enableCaptcha && !captcha)
                  }
                  onClick={_ => this.saveSurvey()}
                  data-test-id={'submit-button'}
          >
            Submit
          </Button>
        </TooltipTrigger>
      </FormSection>
      {loading && <SpinnerOverlay overrideStylesOverlay={{position: 'fixed'}}/>}
    </div>;
    }
  });
