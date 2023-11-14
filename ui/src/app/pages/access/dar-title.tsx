import * as React from 'react';

import { AccessModule, Profile } from 'generated/fetch';

import { DEFAULT, switchCase } from '@terra-ui-packages/core-utils';
import { InfoIcon } from 'app/components/icons';
import { TooltipTrigger } from 'app/components/popups';
import { ComplianceTrainingModuleCardTitle } from 'app/pages/access/compliance-training-module-card-title';
import { IdentityHelpText } from 'app/pages/access/identity-help-text';
import { LoginGovHelpText } from 'app/pages/access/login-gov-help-text';
import { AccessTierShortNames } from 'app/utils/access-tiers';
import { serverConfigStore } from 'app/utils/stores';

export interface DARTitleProps {
  moduleName: AccessModule;
  profile: Profile;
  afterInitialClick?: boolean;
  onClick?: Function;
}

export const DARTitle = (props: DARTitleProps) => {
  const { enableRasIdMeLinking } = serverConfigStore.get().config;

  return switchCase<AccessModule, React.ReactElement>(
    props.moduleName,
    [
      AccessModule.TWO_FACTOR_AUTH,
      () => <div>Turn on Google 2-Step Verification</div>,
    ],
    [
      AccessModule.IDENTITY,
      () => {
        return enableRasIdMeLinking ? (
          <>
            <div>Verify your identity</div>
            <IdentityHelpText {...props} />
          </>
        ) : (
          <>
            <div>
              Verify your identity with Login.gov{' '}
              <TooltipTrigger
                content={
                  'For additional security, we require you to verify your identity by uploading a photo of your ID.'
                }
              >
                <InfoIcon style={{ margin: '0 0.45rem' }} />
              </TooltipTrigger>
            </div>
            <LoginGovHelpText {...props} />
          </>
        );
      },
    ],
    [
      AccessModule.ERA_COMMONS,
      () => <div>Connect your eRA Commons account</div>,
    ],
    [
      AccessModule.COMPLIANCE_TRAINING,
      () => (
        <ComplianceTrainingModuleCardTitle
          tier={AccessTierShortNames.Registered}
          profile={props.profile}
        />
      ),
    ],
    [
      AccessModule.CT_COMPLIANCE_TRAINING,
      () => (
        <ComplianceTrainingModuleCardTitle
          tier={AccessTierShortNames.Controlled}
          profile={props.profile}
        />
      ),
    ],
    [
      AccessModule.DATA_USER_CODE_OF_CONDUCT,
      () => <div>Sign Data User Code of Conduct</div>,
    ],
    [AccessModule.PROFILE_CONFIRMATION, () => null],
    [AccessModule.PUBLICATION_CONFIRMATION, () => null],
    [DEFAULT, () => null]
  );
};
