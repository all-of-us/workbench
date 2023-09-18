// Renders a module when it's enabled via feature flags.  Returns null if not.
import * as React from 'react';
import { useState } from 'react';

import { AccessModule, Profile } from 'generated/fetch';

import { switchCase } from '@terra-ui-packages/core-utils';
import { WithSpinnerOverlayProps } from 'app/components/with-spinner-overlay';
import {
  getAccessModuleStatusByName,
  isEligibleModule,
  redirectToControlledTraining,
  redirectToNiH,
  redirectToRas,
  redirectToRegisteredTraining,
} from 'app/utils/access-utils';
import { AnalyticsTracker } from 'app/utils/analytics';
import { useNavigation } from 'app/utils/navigation';

import { Module } from './module';
import { TwoFactorAuthModal } from './two-factor-auth-modal';

interface ModuleProps {
  profile: Profile;
  moduleName: AccessModule;
  focused: boolean;
  active: boolean;
  spinnerProps: WithSpinnerOverlayProps;
  style?;
}

export const MaybeModule = ({
  profile,
  moduleName,
  focused,
  active,
  spinnerProps,
  style,
}: ModuleProps): JSX.Element => {
  // whether to show the Two Factor Auth Modal
  const [showTwoFactorAuthModal, setShowTwoFactorAuthModal] = useState(false);
  const [navigate] = useNavigation();

  // outside of the main getAccessModuleConfig() so that function doesn't have to deal with navigate
  const moduleAction: Function = switchCase(
    moduleName,
    [AccessModule.TWO_FACTOR_AUTH, () => () => setShowTwoFactorAuthModal(true)],
    [AccessModule.RASLINKLOGINGOV, () => redirectToRas],
    [AccessModule.IDENTITY, () => redirectToRas],
    [AccessModule.ERA_COMMONS, () => redirectToNiH],
    [AccessModule.COMPLIANCE_TRAINING, () => redirectToRegisteredTraining],
    [AccessModule.CT_COMPLIANCE_TRAINING, () => redirectToControlledTraining],
    [
      AccessModule.DATA_USER_CODE_OF_CONDUCT,
      () => () => {
        AnalyticsTracker.Registration.EnterDUCC();
        navigate(['data-code-of-conduct']);
      },
    ]
  );

  const eligible = isEligibleModule(moduleName, profile);
  const status = getAccessModuleStatusByName(profile, moduleName);
  return (
    <Module
      {...{
        focused,
        active,
        eligible,
        moduleAction,
        moduleName,
        profile,
        spinnerProps,
        status,
        style,
      }}
    >
      {showTwoFactorAuthModal && (
        <TwoFactorAuthModal
          onClick={() => setShowTwoFactorAuthModal(false)}
          onCancel={() => setShowTwoFactorAuthModal(false)}
        />
      )}
    </Module>
  );
};
