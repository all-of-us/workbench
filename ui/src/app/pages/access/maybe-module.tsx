// Renders a module when it's enabled via feature flags.  Returns null if not.
import * as React from 'react';
import { useState } from 'react';

import { AccessModule, Profile } from 'generated/fetch';

import { WithSpinnerOverlayProps } from 'app/components/with-spinner-overlay';
import { switchCase } from 'app/utils';
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
  active: boolean;
  clickable: boolean;
  spinnerProps: WithSpinnerOverlayProps;
  style?;
}

export const MaybeModule = ({
  profile,
  moduleName,
  active,
  clickable,
  spinnerProps,
  style,
}: ModuleProps): JSX.Element => {
  // whether to show the Two Factor Auth Modal
  const [showTwoFactorAuthModal, setShowTwoFactorAuthModal] = useState(false);
  const [navigate] = useNavigation();

  // outside of the main getAccessModuleConfig() so that function doesn't have to deal with navigate
  const moduleAction: Function = switchCase(
    moduleName,
    [AccessModule.TWOFACTORAUTH, () => () => setShowTwoFactorAuthModal(true)],
    [AccessModule.RASLINKLOGINGOV, () => redirectToRas],
    [AccessModule.ERACOMMONS, () => redirectToNiH],
    [AccessModule.COMPLIANCETRAINING, () => redirectToRegisteredTraining],
    [AccessModule.CTCOMPLIANCETRAINING, () => redirectToControlledTraining],
    [
      AccessModule.DATAUSERCODEOFCONDUCT,
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
        active,
        clickable,
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
