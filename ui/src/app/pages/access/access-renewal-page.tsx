import * as fp from 'lodash/fp';
import * as React from 'react';
import * as validate from 'validate.js';

import {Button} from 'app/components/buttons';
import {FadeBox} from 'app/components/containers';
import {FlexColumn, FlexRow} from 'app/components/flex';
import {ControlledTierBadge} from 'app/components/icons';
import {TextAreaWithLengthValidationMessage, TextInput, ValidationError} from 'app/components/inputs';
import {BulletAlignedUnorderedList} from 'app/components/lists';
import {Modal} from 'app/components/modals';
import {TooltipTrigger} from 'app/components/popups';
import {SpinnerOverlay} from 'app/components/spinners';
import {AoU} from 'app/components/text-wrappers';
import {withProfileErrorModal, WithProfileErrorModalProps} from 'app/components/with-error-modal';
import {getRegistrationTasksMap} from 'app/pages/homepage/registration-dashboard';
import {AccountCreationOptions} from 'app/pages/login/account-creation/account-creation-options';
import {DataAccessPanel} from 'app/pages/profile/data-access-panel';
import {DemographicSurvey} from 'app/pages/profile/demographic-survey';
import {ProfileRegistrationStepStatus} from 'app/pages/profile/profile-registration-step-status';
import {styles} from 'app/pages/profile/profile-styles';
import {profileApi} from 'app/services/swagger-fetch-clients';
import {institutionApi} from 'app/services/swagger-fetch-clients';
import colors, {colorWithWhiteness, addOpacity} from 'app/styles/colors';
import {
  displayDateWithoutHours,
  formatFreeCreditsUSD,
  lensOnProps,
  withUserProfile,
  withStyle,
  reactStyles
} from 'app/utils';
import {convertAPIError, reportError} from 'app/utils/errors';
import {serverConfigStore} from 'app/utils/stores';
import {environment} from 'environments/environment';
import {InstitutionalRole, Profile} from 'generated/fetch';
import {PublicInstitutionDetails} from 'generated/fetch';
import {Dropdown} from 'primereact/dropdown';
import {withRouteData} from 'app/components/app-router';
import {Arrow, Times, withCircleBackground} from 'app/components/icons'
import {baseStyles} from 'app/components/card'


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
  areaOfResearch: {...required, ...notTooLong(2000)},
  streetAddress1: {...required, ...notTooLong(95)},
  streetAddress2: notTooLong(95),
  zipCode: {...required, ...notTooLong(10)},
  city: {...required, ...notTooLong(95)},
  state: {...required, ...notTooLong(95)},
  country: {...required, ...notTooLong(95)}
};

enum RegistrationStepStatus {
  COMPLETED,
  BYPASSED,
  UNCOMPLETE
}

interface ProfilePageProps extends WithProfileErrorModalProps {
  profileState: {
    profile: Profile;
    reload: () => {};
  };
  controlledTierProfile: {
    controlledTierCompletionTime?: number
    controlledTierBypassTime?: number
    controlledTierEnabled?: boolean
  };
}

interface ProfilePageState {
  currentProfile: Profile;
  institutions: Array<PublicInstitutionDetails>;
  showDemographicSurveyModal: boolean;
  updating: boolean;
}

interface CompletionTime {
  completionTime: number;
  bypassTime: number;
}

const getRegistrationStatus = (completionTime: number, bypassTime: number) => {
  return completionTime !== null && completionTime !== undefined ? RegistrationStepStatus.COMPLETED :
  bypassTime !== null && completionTime !== undefined ? RegistrationStepStatus.BYPASSED : RegistrationStepStatus.UNCOMPLETE;
};


const focusCompletionProps = lensOnProps(['completionTime', 'bypassTime']);

// const getTwoFactorContent = fp.flow(
//   focusCompletionProps(['twoFactorAuthCompletionTime', 'twoFactorAuthBypassTime']),
//   getCompleteOrBypassContent
// );

// const getControlledTierContent = fp.flow(
//   focusCompletionProps(['controlledTierCompletionTime', 'controlledTierBypassTime']),
//   getCompleteOrBypassContent
// );


const style = {
  h1: {
    fontSize: '0.83rem', 
    fontWeight: 600, 
    color: colors.primary
  },
  h2: {
    fontSize: '0.75rem',
    fontWeight: 600
  },
  h3: {
    fontSize: '0.675rem',
    fontWeight: 600
  }
}

const BackArrow = withCircleBackground(() => <Arrow style={{height: 21, width: 18}}/>);
const borderColor = colorWithWhiteness(colors.dark, 0.8)
// Step
// Title
// Last | Next
// Description
// Actions
const RenewalCard = withStyle({...baseStyles.card, border: `1px solid ${borderColor}`, boxShadow: 'none'})(
  ({step = 1, title = 'test', last = 1, next = 2, description = 'Lorem ipsum', actions = 'Action', style}) => {
    return <div style={style}>
      <div style={{fontWeight: 600}}>STEP {step}</div>
      <div>{title}</div>
      <div>Last Updated On: {last}</div>
      <div>Next Review: {next}</div>
      <div>{description}</div>
      <div>{actions}</div>
    </div>
  }
)



export const AccessRenewalPage = fp.flow(
  withRouteData,
  withUserProfile(),
  withProfileErrorModal
)(() => {
  return <FadeBox style={{margin: '1rem auto 0', color: colors.primary}}>
    <div style={{display: 'grid', gridTemplateColumns: '1.5rem 1fr', alignItems: 'center', columnGap: '.675rem'}}>
      <BackArrow style={{height: '1.5rem', width: '1.5rem'}}/>
      <div style={style.h1}>Yearly Researcher Workbench access renewal</div>
      <div style={{gridColumnStart: 2}}>Researchers are required to complete a number of steps as part of the annual renewal to maintain access to All of Us data. Renewal of access will occur on a rolling basis annually (i.e. for each user, access renewal will be due 365 days after the date of authorization to access All of Us data.</div>
    </div>
    <div style={{...style.h2, marginTop: '1rem'}}>Please complete the following steps</div>
    <RenewalCard/>
  </FadeBox>;

    // <ProfileRegistrationStepStatus
    // title={<span><i>All of Us</i> Controlled Tier Data Training</span>}
    // wasBypassed={!!controlledTierBypassTime}
    // incompleteButtonText={'Get Started'}
    // completedButtonText={'Completed'}
    // isComplete={!!(controlledTierCompletionTime || controlledTierBypassTime)}
    // // TODO: link to the training modules once they are available
    // completeStep={() => null}
    // content={getControlledTierContent({controlledTierCompletionTime, controlledTierBypassTime})}
    // >
    //   <div>
    //     {!(controlledTierCompletionTime || controlledTierBypassTime) && <div>To be completed</div>}
    //     <ControlledTierBadge/>
    //   </div>
    // </ProfileRegistrationStepStatus>
  }) 