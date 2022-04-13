// Renders a module when it's enabled via feature flags.  Returns null if not.
import * as React from 'react';
import { useState } from 'react';

import { AccessModule, Profile } from 'generated/fetch';

import { FlexColumn, FlexRow } from 'app/components/flex';
import { ArrowRight } from 'app/components/icons';
import { WithSpinnerOverlayProps } from 'app/components/with-spinner-overlay';
import { cond, switchCase } from 'app/utils';
import {
  getAccessModuleConfig,
  getAccessModuleStatusByName,
  getStatusText,
  isCompliant,
  isEligibleModule,
  redirectToControlledTraining,
  redirectToNiH,
  redirectToRas,
  redirectToRegisteredTraining,
} from 'app/utils/access-utils';
import { AnalyticsTracker } from 'app/utils/analytics';
import { useNavigation } from 'app/utils/navigation';
import { openZendeskWidget } from 'app/utils/zendesk';

import { styles } from './data-access-requirements';
import { ModuleBox } from './module-box';
import { ModuleIcon } from './module-icon';
import { Refresh } from './refresh';
import { TwoFactorAuthModal } from './two-factor-auth-modal';

const ContactUs = (props: { profile: Profile }) => {
  const {
    profile: { givenName, familyName, username, contactEmail },
  } = props;
  return (
    <div data-test-id='contact-us'>
      <span
        style={styles.link}
        onClick={(e) => {
          openZendeskWidget(givenName, familyName, username, contactEmail);
          // prevents the enclosing Clickable's onClick() from triggering instead
          e.stopPropagation();
        }}
      >
        Contact us
      </span>{' '}
      if you’re having trouble completing this step.
    </div>
  );
};

const Next = () => (
  <FlexRow style={styles.nextElement}>
    <span data-test-id='next-module-cta' style={styles.nextText}>
      NEXT
    </span>{' '}
    <ArrowRight style={styles.nextIcon} />
  </FlexRow>
);

const LoginGovHelpText = (props: {
  profile: Profile;
  afterInitialClick: boolean;
}) => {
  const { profile, afterInitialClick } = props;

  // don't return help text if complete or bypassed
  const needsHelp = !isCompliant(
    getAccessModuleStatusByName(profile, AccessModule.RASLINKLOGINGOV)
  );

  return (
    needsHelp &&
    (afterInitialClick ? (
      <div style={styles.loginGovHelp}>
        <div>
          Looks like you still need to complete this action, please try again.
        </div>
        <ContactUs profile={profile} />
      </div>
    ) : (
      <div style={styles.loginGovHelp}>
        <div>
          Verifying your identity helps us keep participant data safe. You’ll
          need to provide your state ID, social security number, and phone
          number.
        </div>
        <ContactUs profile={profile} />
      </div>
    ))
  );
};

interface ModuleProps {
  profile: Profile;
  moduleName: AccessModule;
  active: boolean;
  clickable: boolean;
  spinnerProps: WithSpinnerOverlayProps;
}

export const MaybeModule = ({
  profile,
  moduleName,
  active,
  clickable,
  spinnerProps,
}: ModuleProps): JSX.Element => {
  // whether to show the refresh button: this module has been clicked
  const [showRefresh, setShowRefresh] = useState(false);

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

  const { DARTitleComponent, refreshAction, isEnabledInEnvironment } =
    getAccessModuleConfig(moduleName);
  const eligible = isEligibleModule(moduleName, profile);
  const Module = () => {
    const status = getAccessModuleStatusByName(profile, moduleName);
    return (
      <FlexRow data-test-id={`module-${moduleName}`}>
        <FlexRow style={styles.moduleCTA}>
          {cond(
            [
              clickable && showRefresh && !!refreshAction,
              () => (
                <Refresh
                  refreshAction={refreshAction}
                  showSpinner={spinnerProps.showSpinner}
                />
              ),
            ],
            [active, () => <Next />]
          )}
        </FlexRow>
        <ModuleBox
          clickable={clickable}
          action={() => {
            setShowRefresh(true);
            moduleAction();
          }}
        >
          <ModuleIcon
            moduleName={moduleName}
            eligible={eligible}
            completedOrBypassed={isCompliant(status)}
          />
          <FlexColumn>
            <div
              data-test-id={`module-${moduleName}-${
                clickable ? 'clickable' : 'unclickable'
              }-text`}
              style={
                clickable
                  ? styles.clickableModuleText
                  : styles.backgroundModuleText
              }
            >
              <DARTitleComponent />
              {moduleName === AccessModule.RASLINKLOGINGOV && (
                <LoginGovHelpText
                  profile={profile}
                  afterInitialClick={showRefresh}
                />
              )}
            </div>
            {isCompliant(status) && (
              <div style={styles.moduleDate}>{getStatusText(status)}</div>
            )}
          </FlexColumn>
        </ModuleBox>
        {showTwoFactorAuthModal && (
          <TwoFactorAuthModal
            onClick={() => setShowTwoFactorAuthModal(false)}
            onCancel={() => setShowTwoFactorAuthModal(false)}
          />
        )}
      </FlexRow>
    );
  };

  return isEnabledInEnvironment ? <Module /> : null;
};
