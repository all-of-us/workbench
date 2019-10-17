import {ListPageHeader} from 'app/components/headers';
import * as fp from 'lodash/fp';
import {Dropdown} from 'primereact/dropdown';
import * as React from 'react';

import {Button} from 'app/components/buttons';
import {FlexRow} from 'app/components/flex';
import {CheckBox, RadioButton} from 'app/components/inputs';
import colors from 'app/styles/colors';
import {
  DataAccessLevel,
  Gender,
  Profile,
  Race
} from 'generated/fetch';
import {Section} from './account-creation';

import {
  profileApi
} from 'app/services/swagger-fetch-clients';
import {toggleIncludes} from 'app/utils';
import {AccountCreationOptions} from './account-creation-options';


const styles = {
  questionLabel: {
    width: 76,
    color: colors.primary,
    fontFamily: 'Montserrat',
    fontSize: '14px',
    fontWeight: 400,
  }
};

export const DropDownSection = (props) => {
  return <Section header={props.header}>
    <Dropdown placeholder='Select' options={props.options} style={{width: '50%'}}
              value={props.value}
              onChange={(e) => props.onChange(e.value)}/>
  </Section>;
};

export const CheckBoxWithLabel = (props) => {
  return <div style={{display: 'flex', width: '9rem', marginBottom: '0.5rem'}}>
    <CheckBox style={{marginTop: '0.3rem'}}
              checked={props.checked} onChange={(value) => props.onChange(value)}/>
    <div style={{paddingLeft: '0.5rem', paddingRight: '0.5rem'}}>
      <label style={{...styles.questionLabel, ...props.style}}>{props.label}</label>
    </div>
  </div>;
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
          race: [] as Race[],
          ethnicity: undefined,
          gender: [] as Gender[],
          yearOfBirth: 0,
          education: undefined,
          disability: false
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
        setProfile(savedProfile, 'accountCreationSuccess');
      }).catch(error => {
        console.log(error);
        this.setState({creatingAccount: false});
      });
  }

  updateList(attribute, value) {
    // Toggle Includes removes the element if it already exist and adds if not
    const attributeList = toggleIncludes(value, this.state.profile.demographicSurvey[attribute]);
    this.setState(fp.set(['profile', 'demographicSurvey', attribute], attributeList));
  }

  toggleDisability(value) {
    this.setState(fp.set(['profile', 'demographicSurvey', 'disability'], value));
  }

  updateDemographicAttribute(attribute, value) {
    this.setState(fp.set(['profile', 'demographicSurvey', attribute], value));
  }

  render() {
    const {profile: {demographicSurvey}} = this.state;
    return <div style={{marginTop: '1rem', paddingLeft: '3rem'}}>
      <label style={{color: colors.primary, fontSize: 16}}>
        Please complete Step 2 of 2
      </label>
      <ListPageHeader>
        Demographics Survey <label style={{fontSize: '12px', fontWeight: 400}}>
        (All Survey Fields are optional)</label>
      </ListPageHeader>

      {/*Race section*/}
      <Section header='1. Race'>
        <div style={{display: 'flex', justifyContent: 'flex-start', flexWrap: 'wrap'}}>
          {AccountCreationOptions.Race.map((race) => {
            return <CheckBoxWithLabel attribute='race' label={race.label}
                                      onChange={(value) => this.updateList('race', race.value)}
                                      value={race.value}/>; })
          }
        </div>
      </Section>

      {/*Ethnicity section*/}
      <DropDownSection header='2. Ethnicity' options={AccountCreationOptions.Ethnicity}
                       value={demographicSurvey.ethnicity}
                       onChange={(e) => this.updateDemographicAttribute('ethnicity', e)}/>

      {/*Gender section*/}
      <Section header='3. Gender'>
        <FlexRow style={{flexWrap: 'wrap'}}>
          {AccountCreationOptions.Gender.map((gender) => {
            return <CheckBoxWithLabel attribute='gender'
                                      onChange={(value) => this.updateList('gender', gender.value)}
                                      label={gender.label} value={gender.value}/>;
          })
          }
        </FlexRow>
      </Section>

      {/*Year of birth section*/}
      <DropDownSection header='4. Year of Birth' options={AccountCreationOptions.Years}
                       value={demographicSurvey.yearOfBirth}
                       onChange={(e) => this.updateDemographicAttribute('yearOfBirth', e)}
      />

      {/*Education section*/}
      <DropDownSection header='5. Highest Level of Education Completed'
                       options={AccountCreationOptions.LevelOfEducation}
                       value={demographicSurvey.education}
                       onChange={
                         (e) => this.updateDemographicAttribute('education', e)}/>

      {/*Disability section*/}
      <Section header='6. Do you have a Physical or Cognitive disability?'>
        <div style={{paddingTop: '0.5rem'}}>
          <RadioButton onChange={
            (e) => this.toggleDisability(true)}
                       checked={this.state.profile.demographicSurvey.disability}
                       style={{marginRight: '0.5rem'}}/>
          <label style={{paddingRight: '3rem', color: colors.primary}}>
            Yes
          </label>
          <RadioButton onChange={(e) => this.toggleDisability(false)}
                       checked={!this.state.profile.demographicSurvey.disability}
                       style={{marginRight: '0.5rem'}}/>
          <label style={{color: colors.primary}}>No</label>
        </div>
      </Section>

      <div style={{display: 'flex', paddingTop: '2rem'}}>
        <Button type='secondary' style={{marginRight: '1rem'}} disabled={this.state.creatingAccount}
                onClick={() => this.props.setProfile(this.profileObj, 'accountCreation')}>
          Previous
        </Button>
        <Button type='primary' disabled={this.state.creatingAccount || this.state.creatingAccount}
                onClick={() => this.createAccount()}>
          Submit
        </Button>
      </div>
    </div>;
  }
}
