import * as React from 'react';
import { useEffect, useState } from 'react';
import * as fp from 'lodash/fp';

import { AccessModule, AccessModuleStatus, Profile } from 'generated/fetch';

import { environment } from 'environments/environment';
import { useQuery } from 'app/components/app-router';
import { Button, Clickable } from 'app/components/buttons';
import { FadeBox } from 'app/components/containers';
import { FlexColumn, FlexRow } from 'app/components/flex';
import { Header } from 'app/components/headers';
import {
  ArrowRight,
  CheckCircle,
  ControlledTierBadge,
  MinusCircle,
  RegisteredTierBadge,
  Repeat,
} from 'app/components/icons';
import { withErrorModal } from 'app/components/modals';
import { SupportButton, SupportMailto } from 'app/components/support';
import { AoU } from 'app/components/text-wrappers';
import { withProfileErrorModal } from 'app/components/with-error-modal';
import { WithSpinnerOverlayProps } from 'app/components/with-spinner-overlay';
import { RenewalRequirementsText } from 'app/pages/access/access-renewal';
import { profileApi } from 'app/services/swagger-fetch-clients';
import colors, { colorWithWhiteness } from 'app/styles/colors';
import { cond, reactStyles, switchCase } from 'app/utils';
import {
  AccessTierDisplayNames,
  AccessTierShortNames,
} from 'app/utils/access-tiers';
import {
  buildRasRedirectUrl,
  bypassAll,
  getAccessModuleConfig,
  getAccessModuleStatusByName,
  GetStartedButton,
  redirectToControlledTraining,
  redirectToNiH,
  redirectToRas,
  redirectToRegisteredTraining,
  syncModulesExternal,
} from 'app/utils/access-utils';
import { AnalyticsTracker } from 'app/utils/analytics';
import { displayDateWithoutHours } from 'app/utils/dates';
import { useNavigation } from 'app/utils/navigation';
import { profileStore, serverConfigStore, useStore } from 'app/utils/stores';
import { openZendeskWidget } from 'app/utils/zendesk';
import { ReactComponent as additional } from 'assets/icons/DAR/additional.svg';
import { ReactComponent as electronic } from 'assets/icons/DAR/electronic.svg';
import { ReactComponent as genomic } from 'assets/icons/DAR/genomic.svg';
import { ReactComponent as identifying } from 'assets/icons/DAR/identifying.svg';
import { ReactComponent as individual } from 'assets/icons/DAR/individual.svg';
import { ReactComponent as physical } from 'assets/icons/DAR/physical.svg';
import { ReactComponent as survey } from 'assets/icons/DAR/survey.svg';
import { ReactComponent as wearable } from 'assets/icons/DAR/wearable.svg';

import { TwoFactorAuthModal } from './two-factor-auth-modal';

const styles = reactStyles({
  registrationOuterHeader: {
    marginLeft: '3%',
    width: '50%',
  },
  regHeaderRW: {
    textTransform: 'uppercase',
    margin: '1em 0 0 0',
  },
  regHeaderDAR: {
    height: '30px',
    width: '302px',
    fontFamily: 'Montserrat',
    fontSize: '22px',
    fontWeight: 500,
    letterSpacing: 0,
    margin: '0.5em 0 0 0',
  },
  renewalHeaderYearly: {
    color: colors.primary,
    fontSize: 20,
    fontWeight: 600,
    marginBottom: '1em',
  },
  renewalHeaderRequirements: {
    color: colors.primary,
    fontSize: 14,
    marginBottom: '1em',
  },
  completed: {
    height: '87px',
    padding: '1em',
    marginLeft: '3%',
    marginRight: '3%',
    borderRadius: '5px',
    color: colors.primary,
    backgroundColor: colorWithWhiteness(colors.success, 0.82),
  },
  completedHeader: {
    fontSize: '18px',
    fontWeight: 600,
  },
  completedText: {
    fontSize: '14px',
  },
  selfBypass: {
    height: '87px',
    padding: '1em',
    marginLeft: '3%',
    marginRight: '3%',
    borderRadius: '5px',
    borderColor: colors.primary,
    justifyContent: 'center',
  },
  selfBypassText: {
    alignSelf: 'center',
    color: colors.primary,
    fontSize: '18px',
    fontWeight: 600,
  },
  pageWrapper: {
    marginLeft: '-1rem',
    marginRight: '-0.6rem',
    justifyContent: 'space-between',
    fontSize: '1.2em',
  },
  fadeBox: {
    margin: '0.5rem 0 0 3%',
    width: '95%',
    padding: '0 0.1rem',
  },
  pleaseComplete: {
    color: colors.primary,
    fontSize: 16,
    fontWeight: 600,
  },
  card: {
    height: '375px',
    width: '1195px',
    borderRadius: '0.4rem',
    marginTop: '0.7rem',
    marginBottom: '1.7rem',
    color: colors.primary,
    backgroundColor: colorWithWhiteness(colors.accent, 0.9),
    padding: '1em',
    fontWeight: 500,
  },
  cardStep: {
    height: '19px',
    marginBottom: '0.5em',
  },
  cardHeader: {
    fontSize: '24px',
    fontWeight: 600,
    letterSpacing: 0,
    lineHeight: '22px',
    marginBottom: '0.5em',
  },
  dataHeader: {
    fontSize: '16px',
    fontWeight: 600,
    marginBottom: '0.5em',
    marginLeft: '0.5em',
  },
  ctDataOptional: {
    fontSize: '16px',
    fontStyle: 'italic',
    fontWeight: 'normal',
    marginBottom: '0.5em',
  },
  dataDetailsIcon: {
    marginRight: '0.5em',
  },
  dataDetails: {
    fontSize: '14px',
    fontWeight: 100,
    marginBottom: '0.5em',
  },
  requestAccess: {
    marginTop: '0.5rem',
    marginBottom: '0.5rem',
  },
  modulesContainer: {
    marginLeft: 'auto',
  },
  moduleCTA: {
    fontSize: '10px',
    width: '100px',
    alignSelf: 'center',
    paddingRight: '0.5em',
  },
  clickableModuleBox: {
    padding: '0.5em',
    margin: '0.2em',
    width: '593px',
    borderRadius: '0.2rem',
    backgroundColor: colors.white,
    border: '1px solid',
    borderColor: colors.accent,
  },
  backgroundModuleBox: {
    padding: '0.5em',
    margin: '0.2em',
    width: '593px',
    borderRadius: '0.2rem',
    backgroundColor: colorWithWhiteness(colors.accent, 0.95),
  },
  moduleIcon: {
    marginLeft: '0.2em',
    marginRight: '1em',
  },
  clickableModuleText: {
    color: colors.primary,
  },
  backgroundModuleText: {
    opacity: '0.5',
  },
  moduleDate: {
    opacity: '0.5',
    fontSize: '12px',
  },
  nextElement: {
    marginLeft: 'auto',
  },
  nextText: {
    marginLeft: 'auto',
    alignSelf: 'center',
    paddingRight: '0.5em',
  },
  nextIcon: {
    fontSize: '18px',
    color: colors.white,
    background: colors.success,
    paddingRight: '0.2em',
    paddingLeft: '0.2em',
    alignSelf: 'center',
  },
  refreshButton: {
    height: '25px',
    width: '81px',
    fontSize: '10px',
    borderRadius: '3px',
    marginLeft: 'auto',
  },
  refreshIcon: {
    fontSize: '18px',
    paddingRight: '4px',
  },
  link: {
    color: colors.accent,
    cursor: 'pointer',
    textDecoration: 'underline',
  },
  loginGovHelp: {
    opacity: '0.5',
    fontSize: '12px',
    lineHeight: '22px',
  },
});

// in display order
const rtModules = [
  AccessModule.TWOFACTORAUTH,
  AccessModule.RASLINKLOGINGOV,
  AccessModule.ERACOMMONS,
  AccessModule.COMPLIANCETRAINING,
];
const ctModule = AccessModule.CTCOMPLIANCETRAINING;
const duccModule = AccessModule.DATAUSERCODEOFCONDUCT;

// in display order
// exported for test
export const requiredModules: AccessModule[] = [...rtModules, duccModule];

export const allModules: AccessModule[] = [...rtModules, ctModule, duccModule];

enum PageMode {
  INITIAL_REGISTRATION = 'INITIAL_REGISTRATION',
  ANNUAL_RENEWAL = 'ANNUAL_RENEWAL',
}

const isCompleted = (status: AccessModuleStatus): boolean =>
  !!status?.completionEpochMillis;
const isBypassed = (status: AccessModuleStatus): boolean =>
  !!status?.bypassEpochMillis;
const isCompliant = (status: AccessModuleStatus) =>
  isCompleted(status) || isBypassed(status);

const getStatusText = (status: AccessModuleStatus) => {
  console.assert(
    isCompliant(status),
    'Cannot provide status text for incomplete module'
  );
  const { completionEpochMillis, bypassEpochMillis }: AccessModuleStatus =
    status || {};
  return isCompleted(status)
    ? `Completed on: ${displayDateWithoutHours(completionEpochMillis)}`
    : `Bypassed on: ${displayDateWithoutHours(bypassEpochMillis)}`;
};

const handleTerraShibbolethCallback = (
  token: string,
  spinnerProps: WithSpinnerOverlayProps,
  reloadProfile: Function
) => {
  const handler = withErrorModal({
    title: 'Error saving NIH Authentication status.',
    message:
      'An error occurred trying to save your NIH Authentication status. Please try again.',
    onDismiss: () => {
      spinnerProps.hideSpinner();
    },
  })(async () => {
    spinnerProps.showSpinner();
    await profileApi().updateNihToken({ jwt: token });
    spinnerProps.hideSpinner();
    reloadProfile();
  });

  return handler();
};

const handleRasCallback = (
  code: string,
  spinnerProps: WithSpinnerOverlayProps,
  reloadProfile: Function
) => {
  const handler = withErrorModal({
    title: 'Error saving RAS Login.Gov linkage status.',
    message:
      'An error occurred trying to save your RAS Login.Gov linkage status. Please try again.',
    onDismiss: () => {
      spinnerProps.hideSpinner();
    },
  })(async () => {
    spinnerProps.showSpinner();
    await profileApi().linkRasAccount({
      authCode: code,
      redirectUrl: buildRasRedirectUrl(),
    });
    spinnerProps.hideSpinner();
    reloadProfile();

    // Cleanup parameter from URL after linking.
    window.history.replaceState({}, '', '/');
  });

  return handler();
};

const selfBypass = async (
  spinnerProps: WithSpinnerOverlayProps,
  reloadProfile: Function,
  modules: AccessModule[] = allModules
) => {
  spinnerProps.showSpinner();
  await bypassAll(modules, true);
  spinnerProps.hideSpinner();
  reloadProfile();
};

const isEraCommonsModuleRequiredByInstitution = (
  profile: Profile,
  moduleNames: AccessModule
): boolean => {
  // Remove the eRA Commons module when the flag to enable RAS is set and the user's
  // institution does not require eRA Commons for RT.

  if (moduleNames !== AccessModule.ERACOMMONS) {
    return true;
  }
  const { enableRasLoginGovLinking } = serverConfigStore.get().config;
  if (!enableRasLoginGovLinking) {
    return true;
  }

  return fp.flow(
    fp.filter({ accessTierShortName: AccessTierShortNames.Registered }),
    fp.some('eraRequired')
  )(profile.tierEligibilities);
};

const isEligibleModule = (module: AccessModule, profile: Profile) => {
  if (module !== AccessModule.CTCOMPLIANCETRAINING) {
    // Currently a user can only be ineligible for CT modules.
    // Note: eRA Commons is an edge case which is handled elsewhere. It is
    // technically also possible for CT eRA commons to be ineligible.
    return true;
  }
  const controlledTierEligibility = profile.tierEligibilities.find(
    (tier) => tier.accessTierShortName === AccessTierShortNames.Controlled
  );
  return !!controlledTierEligibility?.eligible;
};

// exported for test
export const getEligibleModules = (
  modules: AccessModule[],
  profile: Profile
): AccessModule[] =>
  fp.flow(
    fp.filter((module: AccessModule) => isEligibleModule(module, profile)),
    fp.map(getAccessModuleConfig),
    fp.filter((moduleConfig) => moduleConfig.isEnabledInEnvironment),
    fp.filter((moduleConfig) =>
      isEraCommonsModuleRequiredByInstitution(profile, moduleConfig.name)
    ),
    fp.map((moduleConfig) => moduleConfig.name)
  )(modules);

const incompleteModules = (
  modules: AccessModule[],
  profile: Profile
): AccessModule[] =>
  modules.filter(
    (moduleName) =>
      !isCompliant(getAccessModuleStatusByName(profile, moduleName))
  );

// exported for test
export const getActiveModule = (
  modules: AccessModule[],
  profile: Profile
): AccessModule => incompleteModules(modules, profile)[0];

const Refresh = (props: { showSpinner: Function; refreshAction: Function }) => {
  const { showSpinner, refreshAction } = props;
  return (
    <Button
      type='primary'
      style={styles.refreshButton}
      onClick={async () => {
        showSpinner();
        await refreshAction();
        location.reload(); // also hides spinner
      }}
    >
      <Repeat style={styles.refreshIcon} /> Refresh
    </Button>
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

const ModuleIcon = (props: {
  moduleName: AccessModule;
  completedOrBypassed: boolean;
  eligible?: boolean;
}) => {
  const { moduleName, completedOrBypassed, eligible = true } = props;

  return (
    <div style={styles.moduleIcon}>
      {cond(
        [
          !eligible,
          () => (
            <MinusCircle
              data-test-id={`module-${moduleName}-ineligible`}
              style={{ color: colors.disabled }}
            />
          ),
        ],
        [
          eligible && completedOrBypassed,
          () => (
            <CheckCircle
              data-test-id={`module-${moduleName}-complete`}
              style={{ color: colors.success }}
            />
          ),
        ],
        [
          eligible && !completedOrBypassed,
          () => (
            <CheckCircle
              data-test-id={`module-${moduleName}-incomplete`}
              style={{ color: colors.disabled }}
            />
          ),
        ]
      )}
    </div>
  );
};

// Sep 16 hack while we work out some RAS bugs
const TemporaryRASModule = () => {
  const moduleName = AccessModule.RASLINKLOGINGOV;
  const { DARTitleComponent } = getAccessModuleConfig(moduleName);
  return (
    <FlexRow data-test-id={`module-${moduleName}`}>
      <FlexRow style={styles.moduleCTA} />
      <FlexRow style={styles.backgroundModuleBox}>
        <ModuleIcon
          moduleName={moduleName}
          completedOrBypassed={false}
          eligible={false}
        />
        <FlexColumn style={styles.backgroundModuleText}>
          <DARTitleComponent />
          <div style={{ fontSize: '14px', marginTop: '0.5em' }}>
            <b>Temporarily disabled.</b> Due to technical difficulties, this
            step is disabled. In the future, you'll be prompted to complete
            identity verification to continue using the workbench.
          </div>
        </FlexColumn>
      </FlexRow>
    </FlexRow>
  );
};

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

const ModuleBox = (props: {
  clickable: boolean;
  action: Function;
  children;
}) => {
  const { clickable, action, children } = props;
  return clickable ? (
    <Clickable onClick={() => action()}>
      <FlexRow style={styles.clickableModuleBox}>{children}</FlexRow>
    </Clickable>
  ) : (
    <FlexRow style={styles.backgroundModuleBox}>{children}</FlexRow>
  );
};

interface ModuleProps {
  profile: Profile;
  moduleName: AccessModule;
  active: boolean;
  clickable: boolean;
  spinnerProps: WithSpinnerOverlayProps;
}

// Renders a module when it's enabled via feature flags.  Returns null if not.
const MaybeModule = ({
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

const ControlledTierEraModule = (props: {
  profile: Profile;
  eligible: boolean;
  spinnerProps: WithSpinnerOverlayProps;
}): JSX.Element => {
  const { profile, eligible, spinnerProps } = props;
  // whether to show the refresh button: this module has been clicked
  const [showRefresh, setShowRefresh] = useState(false);
  const moduleName = AccessModule.ERACOMMONS;
  const { DARTitleComponent, refreshAction, isEnabledInEnvironment } =
    getAccessModuleConfig(moduleName);
  const status = getAccessModuleStatusByName(profile, moduleName);

  // module is not clickable if (user is ineligible for CT) or (user has completed/bypassed module already)
  const clickable = eligible && !isCompliant(status);

  const Module = () => {
    return (
      <FlexRow data-test-id={`module-${moduleName}`}>
        <FlexRow style={styles.moduleCTA}>
          {showRefresh && refreshAction && (
            <Refresh
              refreshAction={refreshAction}
              showSpinner={spinnerProps.showSpinner}
            />
          )}
        </FlexRow>
        <ModuleBox
          clickable={clickable}
          action={() => {
            setShowRefresh(true);
            redirectToNiH();
          }}
        >
          <ModuleIcon
            moduleName={moduleName}
            eligible={eligible}
            completedOrBypassed={isCompliant(status)}
          />
          <FlexColumn>
            <div
              style={
                clickable
                  ? styles.clickableModuleText
                  : styles.backgroundModuleText
              }
            >
              <DARTitleComponent />
            </div>
            {isCompliant(status) && (
              <div style={styles.moduleDate}>{getStatusText(status)}</div>
            )}
          </FlexColumn>
        </ModuleBox>
      </FlexRow>
    );
  };

  return isEnabledInEnvironment ? (
    <Module data-test-id={`module-${moduleName}`} />
  ) : null;
};

// the header outside the Fadebox
const OuterHeader = (props: { pageMode: PageMode }) =>
  props.pageMode === PageMode.INITIAL_REGISTRATION && (
    <FlexColumn style={styles.registrationOuterHeader}>
      <Header style={styles.regHeaderRW}>Researcher Workbench</Header>
      <Header style={styles.regHeaderDAR}>Data Access Requirements</Header>
    </FlexColumn>
  );

// the header inside the Fadebox
const InnerHeader = (props: { pageMode: PageMode }) =>
  props.pageMode === PageMode.INITIAL_REGISTRATION ? (
    <div style={styles.pleaseComplete}>
      Please complete the necessary steps to gain access to the <AoU />{' '}
      datasets.
    </div>
  ) : (
    <FlexColumn>
      <div style={styles.renewalHeaderYearly}>
        Yearly Researcher Workbench access renewal
      </div>
      <div style={styles.renewalHeaderRequirements}>
        <RenewalRequirementsText /> For any questions, please contact{' '}
        <SupportMailto />.
      </div>
      <div style={styles.pleaseComplete}>
        Please complete the following steps.
      </div>
    </FlexColumn>
  );

const SelfBypass = (props: { onClick: () => void }) => (
  <FlexRow data-test-id='self-bypass' style={styles.selfBypass}>
    <div style={styles.selfBypassText}>
      [Test environment] Self-service bypass is enabled
    </div>
    <Button style={{ marginLeft: '0.5rem' }} onClick={() => props.onClick()}>
      Bypass all
    </Button>
  </FlexRow>
);

const Completed = () => (
  <FlexRow data-test-id='dar-completed' style={styles.completed}>
    <FlexColumn>
      <div style={styles.completedHeader}>
        Thank you for completing all the necessary steps
      </div>
      <div style={styles.completedText}>
        Researcher Workbench data access is complete.
      </div>
    </FlexColumn>
    <GetStartedButton style={{ marginLeft: 'auto' }} />
  </FlexRow>
);

interface CardProps {
  profile: Profile;
  modules: AccessModule[];
  activeModule: AccessModule;
  clickableModules: AccessModule[];
  spinnerProps: WithSpinnerOverlayProps;
  children?: string | React.ReactNode;
}

const ModulesForCard = (props: CardProps) => {
  const {
    profile,
    modules,
    activeModule,
    clickableModules,
    spinnerProps,
    children,
  } = props;

  return (
    <FlexColumn style={styles.modulesContainer}>
      {modules.map((moduleName) => (
        <MaybeModule
          key={moduleName}
          moduleName={moduleName}
          profile={profile}
          active={activeModule === moduleName}
          clickable={clickableModules.includes(moduleName)}
          spinnerProps={spinnerProps}
        />
      ))}
      {children}
    </FlexColumn>
  );
};

// TODO is there a better way?

const Additional = additional;
const Electronic = electronic;
const Genomic = genomic;
const Identifying = identifying;
const Individual = individual;
const Physical = physical;
const Survey = survey;
const Wearable = wearable;

const renderIcon = (iconName: string) =>
  switchCase(
    iconName,
    ['additional', () => <Additional style={styles.dataDetailsIcon} />],
    ['electronic', () => <Electronic style={styles.dataDetailsIcon} />],
    ['genomic', () => <Genomic style={styles.dataDetailsIcon} />],
    ['identifying', () => <Identifying style={styles.dataDetailsIcon} />],
    ['individual', () => <Individual style={styles.dataDetailsIcon} />],
    ['physical', () => <Physical style={styles.dataDetailsIcon} />],
    ['survey', () => <Survey style={styles.dataDetailsIcon} />],
    ['wearable', () => <Wearable style={styles.dataDetailsIcon} />]
  );

const DataDetail = (props: { icon: string; text: string }) => {
  const { icon, text } = props;
  return (
    <FlexRow>
      {renderIcon(icon)}
      <div style={styles.dataDetails}>{text}</div>
    </FlexRow>
  );
};

const RegisteredTierCard = (props: {
  profile: Profile;
  activeModule: AccessModule;
  clickableModules: AccessModule[];
  spinnerProps: WithSpinnerOverlayProps;
  pageMode: PageMode;
}) => {
  const { profile, activeModule, clickableModules, spinnerProps, pageMode } =
    props;
  const rtDisplayName = AccessTierDisplayNames.Registered;
  const { enableRasLoginGovLinking } = serverConfigStore.get().config;

  const accessCondition = () =>
    switchCase(
      pageMode,
      [PageMode.INITIAL_REGISTRATION, () => 'Once registered'],
      [PageMode.ANNUAL_RENEWAL, () => 'Once renewed']
    );

  return (
    <FlexRow style={styles.card}>
      <FlexColumn>
        <div style={styles.cardStep}>Step 1</div>
        <div style={styles.cardHeader}>Complete Registration</div>
        <FlexRow>
          <RegisteredTierBadge />
          <div style={styles.dataHeader}>{rtDisplayName} data</div>
        </FlexRow>
        <div style={styles.dataDetails}>
          {accessCondition()}, you’ll have access to:
        </div>
        <DataDetail icon='individual' text='Individual (not aggregated) data' />
        <DataDetail icon='identifying' text='Identifying information removed' />
        <DataDetail icon='electronic' text='Electronic health records' />
        <DataDetail icon='survey' text='Survey responses' />
        <DataDetail icon='physical' text='Physical measurements' />
        <DataDetail icon='wearable' text='Wearable devices' />
      </FlexColumn>
      <ModulesForCard
        profile={profile}
        modules={getEligibleModules(rtModules, profile)}
        activeModule={activeModule}
        clickableModules={clickableModules}
        spinnerProps={spinnerProps}
      >
        {!enableRasLoginGovLinking && <TemporaryRASModule />}
      </ModulesForCard>
    </FlexRow>
  );
};

const ControlledTierStep = (props: { enabled: boolean; text: String }) => {
  return (
    <FlexRow>
      <FlexRow style={styles.moduleCTA} />
      {/* Since Institution access steps does not require user interaction, will display them as inactive*/}
      <FlexRow style={styles.backgroundModuleBox}>
        <div style={styles.moduleIcon}>
          {props.enabled ? (
            <CheckCircle
              data-test-id='eligible'
              style={{ color: colors.success }}
            />
          ) : (
            <MinusCircle
              data-test-id='ineligible'
              style={{ color: colors.disabled }}
            />
          )}
        </div>
        <FlexColumn style={styles.backgroundModuleText}>
          <div>{props.text}</div>
        </FlexColumn>
      </FlexRow>
    </FlexRow>
  );
};

const ControlledTierCard = (props: {
  profile: Profile;
  activeModule: AccessModule;
  clickableModules: AccessModule[];
  reload: Function;
  spinnerProps: WithSpinnerOverlayProps;
}) => {
  const { profile, activeModule, clickableModules, spinnerProps } = props;
  const controlledTierEligibility = profile.tierEligibilities.find(
    (tier) => tier.accessTierShortName === AccessTierShortNames.Controlled
  );
  const registeredTierEligibility = profile.tierEligibilities.find(
    (tier) => tier.accessTierShortName === AccessTierShortNames.Registered
  );
  const isSigned = !!controlledTierEligibility;
  const isEligible = isSigned && controlledTierEligibility.eligible;
  const {
    verifiedInstitutionalAffiliation: { institutionDisplayName },
  } = profile;
  // Display era in CT if:
  // 1) Institution has signed the CT institution agreement,
  // 2) Registered Tier DOES NOT require era
  // 3) CT Requirement DOES require era
  const displayEraCommons =
    isSigned &&
    !registeredTierEligibility?.eraRequired &&
    controlledTierEligibility.eraRequired;
  const rtDisplayName = AccessTierDisplayNames.Registered;
  const ctDisplayName = AccessTierDisplayNames.Controlled;

  return (
    <FlexRow data-test-id='controlled-card' style={styles.card}>
      <FlexColumn>
        <div style={styles.cardStep}>Step 2</div>
        <div style={styles.cardHeader}>Additional Data Access</div>
        <FlexRow>
          <ControlledTierBadge />
          <div style={styles.dataHeader}>{ctDisplayName} data - </div>
          <div style={styles.ctDataOptional}>&nbsp;Optional</div>
        </FlexRow>
        {isEligible ? (
          <div data-test-id='eligible-text' style={styles.dataDetails}>
            You are eligible to access {ctDisplayName} data.
          </div>
        ) : (
          <div>
            <div data-test-id='ineligible-text' style={styles.dataDetails}>
              You are not currently eligible; action by {institutionDisplayName}{' '}
              is required.
            </div>
            <div style={styles.requestAccess}>
              <SupportButton label='Request Access' />
            </div>
          </div>
        )}
        <div style={styles.dataDetails}>
          In addition to {rtDisplayName} data, the {ctDisplayName} curated
          dataset contains:
        </div>
        <DataDetail icon='genomic' text='Genomic data' />
        <DataDetail icon='additional' text='Additional demographic details' />
      </FlexColumn>
      <FlexColumn style={styles.modulesContainer}>
        <ControlledTierStep
          data-test-id='controlled-signed'
          enabled={isSigned}
          text={`${institutionDisplayName} must sign an institutional agreement`}
        />
        <ControlledTierStep
          data-test-id='controlled-user-email'
          enabled={isEligible}
          text={`${institutionDisplayName} must allow you to access ${ctDisplayName} data`}
        />
        {displayEraCommons && (
          <ControlledTierEraModule
            profile={profile}
            eligible={isEligible}
            spinnerProps={spinnerProps}
          />
        )}
        <ModulesForCard
          profile={profile}
          modules={[ctModule]}
          activeModule={activeModule}
          clickableModules={clickableModules}
          spinnerProps={spinnerProps}
        />
      </FlexColumn>
    </FlexRow>
  );
};

const DuccCard = (props: {
  profile: Profile;
  activeModule: AccessModule;
  clickableModules: AccessModule[];
  spinnerProps: WithSpinnerOverlayProps;
  stepNumber: number;
}) => {
  const { profile, activeModule, clickableModules, spinnerProps, stepNumber } =
    props;
  return (
    <FlexRow style={{ ...styles.card, height: '125px' }}>
      <FlexColumn>
        <div style={styles.cardStep}>Step {stepNumber}</div>
        <div style={styles.cardHeader}>Sign the code of conduct</div>
      </FlexColumn>
      <ModulesForCard
        profile={profile}
        modules={[duccModule]}
        activeModule={activeModule}
        clickableModules={clickableModules}
        spinnerProps={spinnerProps}
      />
    </FlexRow>
  );
};

export const DataAccessRequirements = fp.flow(withProfileErrorModal)(
  (spinnerProps: WithSpinnerOverlayProps) => {
    const { profile, reload } = useStore(profileStore);
    const {
      config: { unsafeAllowSelfBypass, accessTiersVisibleToUsers },
    } = useStore(serverConfigStore);

    useEffect(() => {
      const onMount = async () => {
        await syncModulesExternal(
          incompleteModules(getEligibleModules(allModules, profile), profile)
        );
        await reload();
        spinnerProps.hideSpinner();
      };

      onMount();
    }, []);

    const query = useQuery();

    // handle the route /nih-callback?token=<token>
    const token = query.get('token');
    useEffect(() => {
      if (token) {
        handleTerraShibbolethCallback(token, spinnerProps, reload);
      }
    }, [token]);

    // handle the route /ras-callback?code=<code>
    const code = query.get('code');
    useEffect(() => {
      if (code) {
        handleRasCallback(code, spinnerProps, reload);
      }
    }, [code]);

    // handle the different page modes of Data Access Requirements
    const [pageMode, setPageMode] = useState(PageMode.INITIAL_REGISTRATION);
    const pageModeParam = query.get('pageMode');
    useEffect(() => {
      if (
        environment.mergedAccessRenewal &&
        pageModeParam &&
        Object.values(PageMode).includes(pageModeParam as unknown as PageMode)
      ) {
        setPageMode(pageModeParam as unknown as PageMode);
      }
    }, [environment.mergedAccessRenewal, pageModeParam]);

    // At any given time, at most two modules will be clickable:
    //  1. The active module, which we visually direct the user to with a CTA
    //  2. The next required module, which may diverge when the active module is optional.
    // This configuration allows the user to skip the optional CT section.
    const [activeModule, setActiveModule] = useState(null);
    const [clickableModules, setClickableModules] = useState([]);

    const getNextActive = (modules: AccessModule[]) =>
      getActiveModule(getEligibleModules(modules, profile), profile);
    const nextActive = getNextActive(allModules);
    const nextRequired = getNextActive(requiredModules);

    // whenever the profile changes, update the next modules to complete
    useEffect(() => {
      setActiveModule(nextActive);
      setClickableModules(
        fp.flow(
          fp.filter((m) => !!m),
          fp.uniq
        )([nextActive, nextRequired])
      );
    }, [nextActive, nextRequired]);

    const showCtCard = accessTiersVisibleToUsers.includes(
      AccessTierShortNames.Controlled
    );

    const rtCard = (
      <RegisteredTierCard
        key='rt'
        profile={profile}
        activeModule={activeModule}
        clickableModules={clickableModules}
        spinnerProps={spinnerProps}
        pageMode={pageMode}
      />
    );
    const ctCard = showCtCard ? (
      <ControlledTierCard
        key='ct'
        profile={profile}
        activeModule={activeModule}
        clickableModules={clickableModules}
        reload={reload}
        spinnerProps={spinnerProps}
      />
    ) : null;
    const dCard = (
      <DuccCard
        key='dt'
        profile={profile}
        activeModule={activeModule}
        clickableModules={clickableModules}
        spinnerProps={spinnerProps}
        stepNumber={showCtCard ? 3 : 2}
      />
    );

    const cards = showCtCard ? [rtCard, ctCard, dCard] : [rtCard, dCard];

    return (
      <FlexColumn style={styles.pageWrapper}>
        <OuterHeader pageMode={pageMode} />
        {profile && !nextRequired && <Completed />}
        {unsafeAllowSelfBypass && clickableModules.length > 0 && (
          <SelfBypass onClick={async () => selfBypass(spinnerProps, reload)} />
        )}
        <FadeBox style={styles.fadeBox}>
          <InnerHeader pageMode={pageMode} />
          <React.Fragment>{cards}</React.Fragment>
        </FadeBox>
      </FlexColumn>
    );
  }
);
