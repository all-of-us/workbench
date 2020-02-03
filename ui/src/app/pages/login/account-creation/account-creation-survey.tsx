import {ListPageHeader} from 'app/components/headers';
import * as fp from 'lodash/fp';
import {Dropdown} from 'primereact/dropdown';
import * as React from 'react';

import {Button} from 'app/components/buttons';
import {FlexColumn, FlexRow} from 'app/components/flex';
import {CheckBox, RadioButton} from 'app/components/inputs';
import colors from 'app/styles/colors';
import {
  DataAccessLevel,
  Profile,
  Race,
  SexAtBirth,
} from 'generated/fetch';
import {Section, TextInputWithLabel} from './account-creation';

import {TooltipTrigger} from 'app/components/popups';
import {signedOutImages} from 'app/pages/login/signed-out-images';
import {
  profileApi
} from 'app/services/swagger-fetch-clients';
import {toggleIncludes} from 'app/utils';
import * as validate from 'validate.js';
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

export const DropDownSection = (props) => {
  return <Section header={props.header}>
    <Dropdown placeholder='Select' options={props.options} style={{width: '50%'}}
              value={props.value}
              onChange={(e) => props.onChange(e.value)}/>
  </Section>;
};

export interface AccountCreationSurveyProps {
  invitationKey: string;
  profile: Profile;
  setProfile: Function;
}

export interface AccountCreationState {
  creatingAccount: boolean;
  profile: Profile;
}

export class AccountCreationSurvey extends React.Component<AccountCreationSurveyProps, AccountCreationState> {
  constructor(props: any) {
    super(props);
    this.state = {
      creatingAccount: false,
      profile: {
        username: '',
        dataAccessLevel: DataAccessLevel.Protected,
        demographicSurvey: {
          disability: false,
          education: undefined,
          ethnicity: undefined,
          identifiesAsLgbtq: false,
          lgbtqIdentity: '',
          race: [] as Race[],
          sexAtBirth: [] as SexAtBirth[],
          yearOfBirth: 0
        }
      }
    };
  }

  componentDidMount() {
    this.setState({profile: this.profileObj});
  }

  get profileObj() {
    const demographicSurvey = this.state.profile.demographicSurvey;
    return {
      ...this.props.profile,
      demographicSurvey
    };
  }

  createAccount(): void {
    const {invitationKey, setProfile} = this.props;
    this.setState({creatingAccount: true});
    profileApi().createAccount({profile: this.profileObj, invitationKey: invitationKey})
      .then((savedProfile) => {
        this.setState({profile: savedProfile, creatingAccount: false});
        setProfile(savedProfile, {stepName: 'accountCreationSuccess', backgroundImages: signedOutImages.login});
      }).catch(error => {
        console.log(error);
        this.setState({creatingAccount: false});
      });
  }

  updateList(attribute, value) {
    // Toggle Includes removes the element if it already exist and adds if not
    const attributeList = toggleIncludes(value, this.state.profile.demographicSurvey[attribute]);
    this.updateDemographicAttribute(attribute, attributeList);
  }

  updateDemographicAttribute(attribute, value) {
    this.setState(fp.set(['profile', 'demographicSurvey', attribute], value));
  }

  createOptionCheckbox(checkboxLabel: string, optionKey: string, optionValue: any, key: string) {
    return <CheckBox label={checkboxLabel}
                     style={styles.checkbox} key={key}
                     wrapperStyle={styles.checkboxWrapper} labelStyle={styles.checkboxLabel}
                     onChange={(value) => this.updateList(optionKey, optionValue)}
    />;
  }

  render() {
    const {profile: {demographicSurvey}} = this.state;
    const validationCheck = {
      lgbtqIdentity: {
        length: {
          maximum: 255,
          tooLong: 'is too long for our system. Please reduce to 255 or fewer characters.'
        }
      },
    };
    const errors = validate(demographicSurvey, validationCheck);
    console.log(errors);
    return <div style={{marginTop: '1rem', paddingLeft: '3rem', width: '26rem'}}>
      <label style={{color: colors.primary, fontSize: 16}}>
        Please complete Step 2 of 2
      </label>
      <ListPageHeader>
        Demographics Survey <label style={{fontSize: '12px', fontWeight: 400}}>
        (All Survey Fields are optional)</label>
      </ListPageHeader>

      {/*Race section*/}
      <Section header='Race'>
        <FlexColumn style={styles.checkboxAreaContainer}>
          {AccountCreationOptions.race.map((race) => {
            return this.createOptionCheckbox(race.label, 'race', race.value, race.value.toString());
          })}
        </FlexColumn>
      </Section>

      {/*Ethnicity section*/}
      <DropDownSection header='Ethnicity' options={AccountCreationOptions.ethnicity}
                       value={demographicSurvey.ethnicity}
                       onChange={(e) => this.updateDemographicAttribute('ethnicity', e)}/>
      <Section header='Do you identify as lesbian, gay, bisexual, transgender, queer (LGBTQ),
or another sexual and/or gender minority?'>
        <FlexColumn>
          <FlexRow style={{alignItems: 'baseline'}}>
            <RadioButton onChange={
              (e) => this.updateDemographicAttribute('identifiesAsLgbtq', true)}
                         checked={demographicSurvey.identifiesAsLgbtq}
                         style={{marginRight: '0.5rem'}}/>
            <label style={{paddingRight: '3rem', color: colors.primary}}>Yes</label>
          </FlexRow>
          <FlexRow style={{alignItems: 'baseline'}}>
            <RadioButton onChange={(e) => this.updateDemographicAttribute('identifiesAsLgbtq', false)}
                         checked={!demographicSurvey.identifiesAsLgbtq}
                         style={{marginRight: '0.5rem'}}/>
            <label style={{color: colors.primary}}>No</label>
          </FlexRow>
        </FlexColumn>
        <label></label>
        <TextInputWithLabel labelText='If yes, please tell us about your LGBTQ+ identity'
                            value={demographicSurvey.lgbtqIdentity} inputName='lgbtqIdentity'
                            containerStyle={{width: '26rem', marginTop: '0.5rem'}} inputStyle={{width: '26rem'}}
                            onChange={(value) => this.updateDemographicAttribute('lgbtqIdentity', value)}
                            disabled={!demographicSurvey.identifiesAsLgbtq}/>
      </Section>

      {/*Gender Identity section*/}
      <Section header='Gender Identity'>
        <FlexColumn style={{...styles.checkboxAreaContainer, height: '5rem'}}>
          {AccountCreationOptions.gender.map((gender) => {
            return this.createOptionCheckbox(gender.label, 'gender',
                gender.value, gender.value.toString());
          })}
        </FlexColumn>
      </Section>

      {/*Sex at birth section*/}
      <Section header='Sex at birth'>
        <FlexColumn style={{...styles.checkboxAreaContainer, height: '5rem'}}>
          {AccountCreationOptions.sexAtBirth.map((sexAtBirth) => {
            return this.createOptionCheckbox(sexAtBirth.label, 'sexAtBirth',
              sexAtBirth.value, sexAtBirth.value.toString());
          })}
        </FlexColumn>
      </Section>

      {/*Year of birth section*/}
      <DropDownSection header='Year of Birth' options={AccountCreationOptions.Years}
                       value={demographicSurvey.yearOfBirth}
                       onChange={(e) => this.updateDemographicAttribute('yearOfBirth', e)}
      />
      {/*Disability section*/}
      <Section header='Do you have a Physical or Cognitive disability?'>
        <FlexColumn>
          <FlexRow style={{alignItems: 'baseline'}}>
            <RadioButton onChange={
              (e) => this.updateDemographicAttribute('disability', true)}
                         checked={demographicSurvey.disability}
                         style={{marginRight: '0.5rem'}}/>
            <label style={{paddingRight: '3rem', color: colors.primary}}>Yes</label>
          </FlexRow>
          <FlexRow style={{alignItems: 'baseline'}}>
            <RadioButton onChange={(e) => this.updateDemographicAttribute('disability', false)}
                         checked={!demographicSurvey.disability}
                         style={{marginRight: '0.5rem'}}/>
            <label style={{color: colors.primary}}>No</label>
          </FlexRow>
        </FlexColumn>
      </Section>
      {/*Education section*/}
      <DropDownSection header='Highest Level of Education Completed'
                       options={AccountCreationOptions.levelOfEducation}
                       value={demographicSurvey.education}
                       onChange={
                         (e) => this.updateDemographicAttribute('education', e)}/>
      <div style={{display: 'flex', paddingTop: '2rem'}}>
        <Button type='secondary' style={{marginRight: '1rem'}} disabled={this.state.creatingAccount}
                onClick={() => this.props.setProfile(this.profileObj, {stepName: 'accountCreation'})}>
          Previous
        </Button>
        <TooltipTrigger content={errors && <React.Fragment>
            <div>Please review the following: </div>
            <ul>
              {Object.keys(errors).map((key) => <li key={errors[key][0]}>{errors[key][0]}</li>)}
            </ul>
        </React.Fragment>}>
          <Button type='primary' disabled={this.state.creatingAccount || this.state.creatingAccount || errors}
                  onClick={() => this.createAccount()}>
            Submit
          </Button>
        </TooltipTrigger>
      </div>
    </div>;
  }
}
