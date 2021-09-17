import * as fp from 'lodash/fp';
import * as React from 'react';

import {ControlledTierBadge} from 'app/components/icons';
import {AoU} from 'app/components/text-wrappers';
import {displayDateWithoutHours, lensOnProps} from 'app/utils';
import {getRegistrationTasksMap} from 'app/utils/access-utils';
import {serverConfigStore, useStore} from 'app/utils/stores';
import {Profile} from 'generated/fetch';
import {ProfileRegistrationStepStatus} from './profile-registration-step-status';
import {styles} from './profile-styles';
import { useState } from 'react';
import { RouteRedirect } from 'app/components/app-router';

enum RegistrationStepStatus {
    COMPLETED,
    BYPASSED,
    UNCOMPLETE
}

interface CompletionTime {
  completionTime: number;
  bypassTime: number;
}

const getRegistrationStatus = (completionTime: number, bypassTime: number) => {
  return completionTime !== null && completionTime !== undefined ? RegistrationStepStatus.COMPLETED :
        bypassTime !== null && completionTime !== undefined ? RegistrationStepStatus.BYPASSED : RegistrationStepStatus.UNCOMPLETE;
};

const bypassedText = (bypassTime: number): JSX.Element => {
  return <React.Fragment>
        <div>Bypassed on:</div>
        <div>{displayDateWithoutHours(bypassTime)}</div>
    </React.Fragment>;
};

const getCompleteOrBypassContent = ({bypassTime, completionTime}: CompletionTime): JSX.Element => {
  switch (getRegistrationStatus(completionTime, bypassTime)) {
    case RegistrationStepStatus.COMPLETED:
      return <React.Fragment>
                <div>Completed on:</div>
                <div>{displayDateWithoutHours(completionTime)}</div>
            </React.Fragment>;
    case RegistrationStepStatus.BYPASSED:
      return bypassedText(bypassTime);
    default:
      return;
  }
};

const getEraCommonsCardText = (profile: Profile) => {
  switch (getRegistrationStatus(profile.eraCommonsCompletionTime, profile.eraCommonsBypassTime)) {
    case RegistrationStepStatus.COMPLETED:
      return <div>
                {profile.eraCommonsLinkedNihUsername != null && <React.Fragment>
                    <div> Username:</div>
                    <div> {profile.eraCommonsLinkedNihUsername} </div>
                </React.Fragment>}
                {profile.eraCommonsLinkExpireTime != null &&
                //  Firecloud returns eraCommons link expiration as 0 if there is no linked account.
                profile.eraCommonsLinkExpireTime !== 0
                && <React.Fragment>
                    <div> Completed on:</div>
                    <div>
                        {displayDateWithoutHours(profile.eraCommonsCompletionTime)}
                    </div>
                </React.Fragment>}
            </div>;
    case RegistrationStepStatus.BYPASSED:
      return bypassedText(profile.twoFactorAuthBypassTime);
    default:
      return;
  }
};

const getComplianceTrainingText = (profile: Profile) => {
  switch (getRegistrationStatus(profile.complianceTrainingCompletionTime, profile.complianceTrainingBypassTime)) {
    case RegistrationStepStatus.COMPLETED:
      return <React.Fragment>
                <div>Training Completed</div>
                <div>{displayDateWithoutHours(profile.complianceTrainingCompletionTime)}</div>
            </React.Fragment>;
    case RegistrationStepStatus.BYPASSED:
      return bypassedText(profile.complianceTrainingBypassTime);
    default:
      return;
  }
};

const getDUCCText = (profile: Profile) => {
  const universalText = <a onClick={getRegistrationTasksMap()['dataUserCodeOfConduct'].onClick}>
        View code of conduct
    </a>;
  switch (getRegistrationStatus(profile.dataUseAgreementCompletionTime, profile.dataUseAgreementBypassTime)) {
    case RegistrationStepStatus.COMPLETED:
      return <React.Fragment>
              <div>Signed On:</div>
              <div>
                  {displayDateWithoutHours(profile.dataUseAgreementCompletionTime)}
              </div>
              {universalText}
          </React.Fragment>;
    case RegistrationStepStatus.BYPASSED:
      return <React.Fragment>
              {bypassedText(profile.dataUseAgreementBypassTime)}
              {universalText}
          </React.Fragment>;
    case RegistrationStepStatus.UNCOMPLETE:
      return universalText;
  }
};

const focusCompletionProps = lensOnProps(['completionTime', 'bypassTime']);

const getTwoFactorContent = fp.flow(
  focusCompletionProps(['twoFactorAuthCompletionTime', 'twoFactorAuthBypassTime']),
  getCompleteOrBypassContent
);

const getControlledTierContent = fp.flow(
  focusCompletionProps(['controlledTierCompletionTime', 'controlledTierBypassTime']),
  getCompleteOrBypassContent
);

export const ProfileAccessModules = (props: {profile: Profile}) => {
  const {profile} = props;
  const {config: {enableComplianceTraining, enableEraCommons}} = useStore(serverConfigStore);
  const [redirectToDUCC, setRedirectToDUCC] = useState(false);

    // these have never been enabled for the user profile, and the component will be deleted before this happens.
    // see https://precisionmedicineinitiative.atlassian.net/browse/RW-7256
  const controlledTierEnabled = false, controlledTierBypassTime = null, controlledTierCompletionTime = null;

  return redirectToDUCC
      ? <RouteRedirect path={'/data-code-of-conduct'}/>
      : <React.Fragment>
            <div style={styles.title}>
                Requirements for <AoU/> Workbench access
            </div>
            <hr style={{...styles.verticalLine}}/>
            <div style={{display: 'grid', gap: '10px', gridAutoRows: '225px', gridTemplateColumns: '220px 220px'}}>
                {controlledTierEnabled && <ProfileRegistrationStepStatus
                    title={<span><AoU/> Controlled Tier Data Training</span>}
                    wasBypassed={!!controlledTierBypassTime}
                    incompleteButtonText={'Get Started'}
                    completedButtonText={'Completed'}
                    isComplete={!!(controlledTierCompletionTime || controlledTierBypassTime)}
                    // TODO: link to the training modules once they are available
                    completeStep={() => null}
                    content={getControlledTierContent({controlledTierCompletionTime, controlledTierBypassTime})}
                >
                    <div>
                        {!(controlledTierCompletionTime || controlledTierBypassTime) && <div>To be completed</div>}
                        <ControlledTierBadge/>
                    </div>
                </ProfileRegistrationStepStatus>}
                <ProfileRegistrationStepStatus
                    title='Turn on Google 2-Step Verification'
                    wasBypassed={!!profile.twoFactorAuthBypassTime}
                    incompleteButtonText='Set Up'
                    completedButtonText={getRegistrationTasksMap()['twoFactorAuth'].completedText}
                    isComplete={!!(getRegistrationTasksMap()['twoFactorAuth'].completionTimestamp(profile))}
                    completeStep={getRegistrationTasksMap()['twoFactorAuth'].onClick}
                    content={getTwoFactorContent(profile)}
                >
                </ProfileRegistrationStepStatus>
                {enableEraCommons && <ProfileRegistrationStepStatus
                    title='Connect Your eRA Commons Account'
                    wasBypassed={!!profile.eraCommonsBypassTime}
                    incompleteButtonText='Link'
                    completedButtonText={getRegistrationTasksMap()['eraCommons'].completedText}
                    isComplete={!!(getRegistrationTasksMap()['eraCommons'].completionTimestamp(profile))}
                    completeStep={getRegistrationTasksMap()['eraCommons'].onClick}
                    content={getEraCommonsCardText(profile)}
                >
                </ProfileRegistrationStepStatus>}
                {enableComplianceTraining && <ProfileRegistrationStepStatus
                    title={<span><AoU/> Responsible Conduct of Research Training</span>}
                    wasBypassed={!!profile.complianceTrainingBypassTime}
                    incompleteButtonText='Access Training'
                    completedButtonText={getRegistrationTasksMap()['complianceTraining'].completedText}
                    isComplete={!!(getRegistrationTasksMap()['complianceTraining'].completionTimestamp(profile))}
                    completeStep={getRegistrationTasksMap()['complianceTraining'].onClick}
                    content={getComplianceTrainingText(profile)}
                >
                </ProfileRegistrationStepStatus>}
                <ProfileRegistrationStepStatus
                    title='Sign Data User Code Of Conduct'
                    wasBypassed={!!profile.dataUseAgreementBypassTime}
                    incompleteButtonText='Sign'
                    completedButtonText={getRegistrationTasksMap()['dataUserCodeOfConduct'].completedText}
                    isComplete={!!(getRegistrationTasksMap()['dataUserCodeOfConduct'].completionTimestamp(profile))}
                    completeStep={() => {
                      getRegistrationTasksMap()['dataUserCodeOfConduct'].onClick;
                      setRedirectToDUCC(true);
                    }}
                    childrenStyle={{marginLeft: 0}}
                    content={getDUCCText(profile)}
                >
                </ProfileRegistrationStepStatus>
            </div>
        </React.Fragment>;
};
