import {Component} from '@angular/core';
import * as fp from 'lodash/fp';
import * as moment from 'moment';
import * as React from 'react';
import * as validate from 'validate.js';

import {Button, Clickable} from 'app/components/buttons';
import { FlexColumn, FlexRow } from 'app/components/flex';
import {ClrIcon} from 'app/components/icons';
import {TextArea, TextInput, ValidationError} from 'app/components/inputs';
import {Modal} from 'app/components/modals';
import {TooltipTrigger} from 'app/components/popups';
import {SpinnerOverlay} from 'app/components/spinners';
import {getRegistrationTasksMap} from 'app/pages/homepage/registration-dashboard';
import {DemographicSurvey} from 'app/pages/profile/demographics-survey';
import {ProfileRegistrationStepStatus} from 'app/pages/profile/profile-registration-step-status';
import {profileApi} from 'app/services/swagger-fetch-clients';
import colors, {colorWithWhiteness} from 'app/styles/colors';
import {reactStyles, ReactWrapperBase, withUserProfile} from 'app/utils';
import {serverConfigStore} from 'app/utils/navigation';
import {environment} from 'environments/environment';
import {InstitutionalAffiliation, InstitutionalRole, Profile} from 'generated/fetch';

const styles = reactStyles({
  h1: {
    color: colors.primary,
    fontSize: 20,
    fontWeight: 500,
    lineHeight: '24px'
  },
  inputLabel: {
    color: colors.primary,
    fontSize: 14, lineHeight: '18px',
    marginBottom: 6
  },
  inputStyle: {
    width: 250,
    marginRight: 20
  },
  longInputStyle: {
    height: 175, width: 420,
    resize: 'both'
  },
  box: {
    backgroundColor: colors.white,
    borderRadius: 8,
    padding: 21
  },
  title: {
    color: colors.primary,
    fontSize: 16,
    fontWeight: 600
  },
  uneditableProfileElement: {
    paddingLeft: '0.5rem',
    marginRight: 20,
    marginBottom: 20,
    height: '1.5rem',
    color: colors.primary
  }
});

// validators for validate.js
const required = {presence: {allowEmpty: false}};
const notTooLong = maxLength => ({
  length: {
    maximum: maxLength,
    tooLong: 'must be %{count} characters or less'
  }
});
const validators = {
  givenName: {...required, ...notTooLong(80)},
  familyName: {...required, ...notTooLong(80)},
  currentPosition: {...required, ...notTooLong(255)},
  organization: {...required, ...notTooLong(255)},
  areaOfResearch: required,
};

interface ProfilePageProps {
  profileState: {
    profile: Profile;
    reload: Function;
  };
}

interface ProfilePageState {
  profileEdits: Profile;
  updating: boolean;
  updatingSurvey: boolean;
}

export const ProfilePage = withUserProfile()(class extends React.Component<
    ProfilePageProps,
    ProfilePageState
> {
  static displayName = 'ProfilePage';

  constructor(props) {
    super(props);

    this.state = {
      profileEdits: props.profileState.profile || {},
      updating: false,
      updatingSurvey: false
    };
  }

  navigateToTraining(): void {
    window.location.assign(
      environment.trainingUrl + '/static/data-researcher.html?saml=on');
  }

  componentDidUpdate(prevProps) {
    const {profileState: {profile}} = this.props;

    if (!fp.isEqual(prevProps.profileState.profile, profile)) {
      this.setState({profileEdits: profile}); // for when profile loads after component load
    }
  }

  async saveProfile() {
    const {profileState: {reload}} = this.props;

    this.setState({updating: true});

    try {
      await profileApi().updateProfile(this.state.profileEdits);
      await reload();
    } catch (e) {
      console.error(e);
    } finally {
      this.setState({updating: false});
    }
  }

  async saveDemographicSurvey(profile) {
    const {profileState: {reload}} = this.props;
    this.setState({updating: true});

    try {
      await profileApi().updateProfile(profile);
      await reload();
    } catch (e) {
      // TODO: We should display some sort of user facing error if update fails.
      console.error(e);
    } finally {
      this.setState({updating: false});
    }
  }

  render() {
    const {profileState: {profile}} = this.props;
    const {profileEdits, updating, updatingSurvey} = this.state;
    const {enableComplianceTraining, enableEraCommons, enableDataUseAgreement, requireInstitutionalVerification} =
      serverConfigStore.getValue();
    const {
      givenName, familyName, currentPosition, organization, areaOfResearch,
      institutionalAffiliations = []
    } = profileEdits;
    const errors = validate({
      givenName, familyName, currentPosition, organization, areaOfResearch
    }, validators, {
      prettify: v => ({
        givenName: 'First Name',
        familyName: 'Last Name',
        areaOfResearch: 'Current Research'
      }[v] || validate.prettify(v))
    });

    // render a float value as US currency, rounded to cents: 255.372793 -> $255.37
    const usdElement = (value: number) => {
      value = value || 0.0;
      if (value < 0.0) {
        return <div style={{fontWeight: 600}}>-${(-value).toFixed(2)}</div>;
      } else {
        return <div style={{fontWeight: 600}}>${(value).toFixed(2)}</div>;
      }
    };

    const makeProfileInput = ({title, valueKey, isLong = false, ...props}) => {
      const errorText = profile && errors && errors[valueKey];

      const inputProps = {
        value: fp.get(valueKey, profileEdits) || '',
        onChange: v => this.setState(fp.set(['profileEdits', ...valueKey], v)),
        invalid: !!errorText,
        ...props
      };

      return <div style={{marginBottom: 40}}>
        <div style={styles.inputLabel}>{title}</div>
        {isLong ?
          <TextArea
            style={styles.longInputStyle}
            {...inputProps}
          /> :
          <TextInput
            style={styles.inputStyle}
            {...inputProps}
          />}
        <ValidationError>{errorText}</ValidationError>
      </div>;
    };

    const removeOldInstitutionalAffiliation = (affiliation: InstitutionalAffiliation) => {
      this.setState(fp.update(
        ['profileEdits', 'institutionalAffiliations'],
        fp.pull(affiliation)
      ));
    };

    const addEmptyOldInstitutionalAffiliation = () => {
      this.setState(fp.update(
        ['profileEdits', 'institutionalAffiliations'],
        affiliation => fp.concat(affiliation, {institution: '', role: ''})));
    };

    const renderVerifiedInstitutionalAffiliationComponents = () => {
      const {verifiedInstitutionalAffiliation} = profile;
      if (!verifiedInstitutionalAffiliation) {
        return;
      }

      const {institutionDisplayName, institutionalRoleEnum, institutionalRoleOtherText} = verifiedInstitutionalAffiliation;
      return <React.Fragment>
        <div style={{...styles.h1, marginBottom: 24}}>
          Verified Institutional Affiliation
        </div>
        <FlexRow>
          <FlexColumn>
            <div style={styles.inputLabel}>
              Institution
            </div>
            <div style={styles.uneditableProfileElement}>{institutionDisplayName}</div>
          </FlexColumn>
          <FlexColumn>
            <div style={styles.inputLabel}>
              Role
            </div>
            <div style={styles.uneditableProfileElement}>
            {institutionalRoleEnum === InstitutionalRole.OTHER ? institutionalRoleOtherText : institutionalRoleEnum}
            </div>
          </FlexColumn>
        </FlexRow>
      </React.Fragment>;
    };

    const renderOldInstitutionalAffiliationComponents = () => {
      return <React.Fragment>
        <div style={{...styles.h1, marginBottom: 24}}>
          Institution Affiliations
        </div>
        {institutionalAffiliations.map((v, i) =>
          <div style={{display: 'flex'}} key={`institution${i}`}>
            {makeProfileInput({
              title: 'Institution',
              valueKey: ['institutionalAffiliations', i, 'institution']
            })}
            {makeProfileInput({
              title: 'Role',
              valueKey: ['institutionalAffiliations', i, 'role']
            })}
            <Clickable
              style={{alignSelf: 'center'}}
              onClick={() => removeOldInstitutionalAffiliation(v)}
            >
              <ClrIcon
                shape='times'
                size='24'
                style={{color: colors.accent, marginBottom: 17}}
              />
            </Clickable>
          </div>
        )}
        <div style={{display: 'flex', width: 520, alignItems: 'center'}}>
          <div style={{border: `1px solid ${colorWithWhiteness(colors.dark, 0.5)}`, flex: 1}}/>
          <Clickable onClick={() => addEmptyOldInstitutionalAffiliation()}>
            <ClrIcon
              shape='plus-circle'
              size='19'
              style={{
                color: colors.accent,
                margin: '0 14px',
                flex: 'none', verticalAlign: 'text-bottom' // text-bottom makes it centered...?
              }}
            />
          </Clickable>
          <div style={{border: `1px solid ${colorWithWhiteness(colors.dark, 0.5)}`, flex: 1}}/>
        </div>
      </React.Fragment>;
    };

    const renderInstitutionalAffiliationComponents = () => {
      if (requireInstitutionalVerification) {
        return renderVerifiedInstitutionalAffiliationComponents();
      } else {
        return renderOldInstitutionalAffiliationComponents();
      }
    };

    return <div style={{margin: '35px 35px 100px 45px'}}>
      {(!profile || updating) && <SpinnerOverlay/>}
      <div style={{...styles.h1, marginBottom: 30}}>Profile</div>
      <div style={{display: 'flex'}}>
        <div style={{flex: '1 0 520px', paddingRight: 26}}>
          <div style={{display: 'flex'}}>
            {makeProfileInput({
              title: 'First Name',
              valueKey: 'givenName'
            })}
            {makeProfileInput({
              title: 'Last Name',
              valueKey: 'familyName'
            })}
          </div>
          {makeProfileInput({
            title: 'Contact Email',
            valueKey: 'contactEmail',
            disabled: true
          })}
          <div style={styles.inputLabel}>Username</div>
          <div style={styles.uneditableProfileElement}>
            {profile && profile.username}
          </div>
          {makeProfileInput({
            title: 'Your Current Position',
            valueKey: 'currentPosition'
          })}
          {makeProfileInput({
            title: 'Your Organization',
            valueKey: 'organization'
          })}
          {makeProfileInput({
            title: <React.Fragment>
              Current Research Work
              <TooltipTrigger
                side='right'
                content={<span>You are required to describe your current research in order to help
                  <i>All of Us</i> improve the Researcher Workbench.</span>}
              >
                <ClrIcon
                  shape='info-standard'
                  className='is-solid'
                  style={{marginLeft: 10, verticalAlign: 'middle', color: colors.accent}}
                />
              </TooltipTrigger>
            </React.Fragment>,
            valueKey: 'areaOfResearch',
            isLong: true
          })}
          {makeProfileInput({
            title: 'About You',
            valueKey: 'aboutYou',
            isLong: true
          })}
          {renderInstitutionalAffiliationComponents()}
          <div style={{marginTop: 100, display: 'flex'}}>
            <Button type='link'
              onClick={() => this.setState({profileEdits: profile})}
            >
              Discard Changes
            </Button>
            <TooltipTrigger
              side='top'
              content={!!errors && 'You must correct errors before saving.'}
            >
              <Button
                data-test-id='save profile'
                type='purplePrimary'
                style={{marginLeft: 40}}
                onClick={() => this.saveProfile()}
                disabled={!!errors || fp.isEqual(profile, profileEdits)}
              >
                Save Profile
              </Button>
            </TooltipTrigger>
          </div>
        </div>
       <div>
          {profile && <FlexRow style={{
            color: colors.primary, paddingRight: '0.5rem', justifyContent: 'flex-end'
          }}>
            <FlexColumn style={{alignItems: 'flex-end'}}>
              <div><i>All of Us</i> FREE credits used:</div>
              <div>Remaining <i>All of Us</i> FREE credits:</div>
            </FlexColumn>
            <FlexColumn style={{alignItems: 'flex-end', marginLeft: '1.0rem'}}>
              {usdElement(profile.freeTierUsage)}
              {usdElement(profile.freeTierDollarQuota - profile.freeTierUsage)}
            </FlexColumn>
          </FlexRow>}
          <div>
            <div style={styles.title}>Optional Demographics Survey</div>
            <Button
                type={'link'}
                onClick={async () => {
                  if (!profileEdits.demographicSurvey) {
                    await this.setState(fp.set(['profileEdits', 'demographicSurvey'], {}))
                  }
                  this.setState({updatingSurvey: true})
                }}
            >Update Survey</Button>
          </div>
          <ProfileRegistrationStepStatus
            title='Google 2-Step Verification'
            wasBypassed={!!profile.twoFactorAuthBypassTime}
            incompleteButtonText='Set Up'
            completedButtonText={getRegistrationTasksMap()['twoFactorAuth'].completedText}
            completionTimestamp={getRegistrationTasksMap()['twoFactorAuth'].completionTimestamp(profile)}
            isComplete={!!(getRegistrationTasksMap()['twoFactorAuth'].completionTimestamp(profile))}
            completeStep={getRegistrationTasksMap()['twoFactorAuth'].onClick  } />

          {enableComplianceTraining && <ProfileRegistrationStepStatus
            title='Access Training'
            wasBypassed={!!profile.complianceTrainingBypassTime}
            incompleteButtonText='Access Training'
            completedButtonText={getRegistrationTasksMap()['complianceTraining'].completedText}
            completionTimestamp={getRegistrationTasksMap()['complianceTraining'].completionTimestamp(profile)}
            isComplete={!!(getRegistrationTasksMap()['complianceTraining'].completionTimestamp(profile))}
            completeStep={getRegistrationTasksMap()['complianceTraining'].onClick} />}

          {enableEraCommons && <ProfileRegistrationStepStatus
            title='eRA Commons Account'
            wasBypassed={!!profile.eraCommonsBypassTime}
            incompleteButtonText='Link'
            completedButtonText={getRegistrationTasksMap()['eraCommons'].completedText}
            completionTimestamp={getRegistrationTasksMap()['eraCommons'].completionTimestamp(profile)}
            isComplete={!!(getRegistrationTasksMap()['eraCommons'].completionTimestamp(profile))}
            completeStep={getRegistrationTasksMap()['eraCommons'].onClick} >
            <div>
              {profile.eraCommonsLinkedNihUsername != null && <React.Fragment>
                <div> Username: </div>
                <div> { profile.eraCommonsLinkedNihUsername } </div>
              </React.Fragment>}
              {profile.eraCommonsLinkExpireTime != null &&
              //  Firecloud returns eraCommons link expiration as 0 if there is no linked account.
              profile.eraCommonsLinkExpireTime !== 0
              && <React.Fragment>
                <div> Link Expiration: </div>
                <div>
                  { moment.unix(profile.eraCommonsLinkExpireTime)
                    .format('MMMM Do, YYYY, h:mm:ss A') }
                </div>
              </React.Fragment>}
            </div>
          </ProfileRegistrationStepStatus>}

          {enableDataUseAgreement && <ProfileRegistrationStepStatus
            title='Data User Code Of Conduct'
            wasBypassed={!!profile.dataUseAgreementBypassTime}
            incompleteButtonText='Sign'
            completedButtonText={getRegistrationTasksMap()['dataUserCodeOfConduct'].completedText}
            completionTimestamp={getRegistrationTasksMap()['dataUserCodeOfConduct'].completionTimestamp(profile)}
            isComplete={!!(getRegistrationTasksMap()['dataUserCodeOfConduct'].completionTimestamp(profile))}
            completeStep={getRegistrationTasksMap()['dataUserCodeOfConduct'].onClick} >
            {profile.dataUseAgreementCompletionTime != null && <React.Fragment>
              <div> Agreement Renewal: </div>
              <div>
                { moment.unix(profile.dataUseAgreementCompletionTime / 1000)
                    .add(1, 'year')
                    .format('MMMM Do, YYYY') }
              </div>
            </React.Fragment>}
            <a
              onClick={getRegistrationTasksMap()['dataUserCodeOfConduct'].onClick}>
              View current agreement
            </a>
          </ProfileRegistrationStepStatus>}
        </div>
      </div>
      {updatingSurvey && <Modal width={850}>
        <DemographicSurvey
            profile={profileEdits}
            onCancelClick={() => {
              this.setState({updatingSurvey: false});
            }}
            onSubmit={async (profileWithUpdatedDemographicSurvey, captchaToken) => {
              this.setState({updatingSurvey: false});
              await this.saveDemographicSurvey(profileWithUpdatedDemographicSurvey);
            }}
            enableCaptcha={false}
            enablePrevious={false}
        />
      </Modal>}
    </div>;
  }
});

@Component({
  template: '<div #root></div>'
})
export class ProfilePageComponent extends ReactWrapperBase {
  constructor() {
    super(ProfilePage, []);
  }
}
