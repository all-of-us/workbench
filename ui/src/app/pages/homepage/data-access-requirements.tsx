import * as fp from 'lodash/fp';
import * as React from 'react';
import {useEffect, useState} from 'react';

import { useQuery } from 'app/components/app-router';
import {Button, Clickable} from 'app/components/buttons';
import {FadeBox} from 'app/components/containers';
import {FlexColumn, FlexRow} from 'app/components/flex';
import {Header} from 'app/components/headers';
import {
  ArrowRight,
  CheckCircle,
  InfoIcon,
  MinusCircle,
  RegisteredTierBadge,
  Repeat
} from 'app/components/icons';
import {withErrorModal} from 'app/components/modals';
import {TooltipTrigger} from 'app/components/popups';
import {AoU} from 'app/components/text-wrappers';
import {withProfileErrorModal} from 'app/components/with-error-modal';
import {WithSpinnerOverlayProps} from 'app/components/with-spinner-overlay';
import {profileApi} from 'app/services/swagger-fetch-clients';
import colors, {colorWithWhiteness} from 'app/styles/colors';
import {cond, displayDateWithoutHours, reactStyles, switchCase} from 'app/utils';
import {
  buildRasRedirectUrl,
  bypassAll,
  getAccessModuleStatusByName,
  getRegistrationTask,
  GetStartedButton,
  redirectToRas,
} from 'app/utils/access-utils';
import {isAbortError} from 'app/utils/errors';
import {useNavigation} from 'app/utils/navigation';
import {profileStore, serverConfigStore, useStore} from 'app/utils/stores';
import {AccessModule, AccessModuleStatus, Profile} from 'generated/fetch';
import {TwoFactorAuthModal} from './two-factor-auth-modal';

const styles = reactStyles({
  headerFlexColumn: {
    marginLeft: '3%',
    width: '50%',
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
  headerRW: {
    textTransform: 'uppercase',
    margin: '1em 0 0 0',
  },
  headerDAR: {
    height: '30px',
    width: '302px',
    fontFamily: 'Montserrat',
    fontSize: '22px',
    fontWeight: 500,
    letterSpacing: 0,
    margin: '0.5em 0 0 0',
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
    fontSize: '14px',
    color: colors.primary,
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
  rtData: {
    fontSize: '16px',
    fontWeight: 600,
    marginBottom: '0.5em',
    marginLeft: '0.5em',
  },
  rtDetailsIcon: {
    marginRight: '0.5em',
  },
  rtDataDetails: {
    fontSize: '14px',
    fontWeight: 100,
    marginBottom: '0.5em',
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
  activeModuleBox: {
    padding: '0.5em',
    margin: '0.2em',
    width: '593px',
    borderRadius: '0.2rem',
    backgroundColor: colors.white,
    border: '1px solid',
    borderColor: colors.accent,
  },
  inactiveModuleBox: {
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
  activeModuleText: {
    color: colors.primary,
  },
  inactiveModuleText: {
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
});

// in display order
const rtModules = [
  AccessModule.TWOFACTORAUTH,
  AccessModule.RASLINKLOGINGOV,
  AccessModule.ERACOMMONS,
  AccessModule.COMPLIANCETRAINING,
];

// TODO RW-7059
const ctModules = [];

const duccModule = AccessModule.DATAUSERCODEOFCONDUCT;

// in display order
// exported for test
export const allModules: AccessModule[] = [
  ...rtModules,
  ...ctModules,
  duccModule,
];

const LoginGovTooltip = () => <TooltipTrigger
    content={'For additional security, we require you to verify your identity by uploading a photo of your ID.'}>
  <InfoIcon style={{margin: '0 0.3rem'}}/>
</TooltipTrigger>;

// TODO merge with RegistrationTasks after we remove RegistrationDashboard
const moduleLabels: Map<AccessModule, JSX.Element> = new Map([
  [AccessModule.TWOFACTORAUTH, <div>Turn on Google 2-Step Verification</div>],
  [AccessModule.RASLINKLOGINGOV, <div>Verify your identity with Login.gov <LoginGovTooltip/></div>],
  [AccessModule.ERACOMMONS, <div>Connect your eRA Commons account</div>],
  [AccessModule.COMPLIANCETRAINING, <div>Complete <AoU/> research Registered Tier training</div>],
  [AccessModule.DATAUSERCODEOFCONDUCT, <div>Sign Data User Code of Conduct</div>],
]);

const withAborter = async(abortableFn: (AbortController) => void) => {
  const aborter = new AbortController();
  try {
    await abortableFn(aborter);
  } catch (e) {
    if (!isAbortError(e)) { throw e; }
  } finally {
    aborter.abort();
  }
};

const moduleRefreshActions: Map<AccessModule, Function> = new Map([
  [AccessModule.TWOFACTORAUTH, async() => {
    await withAborter(aborter => profileApi().syncTwoFactorAuthStatus({signal: aborter.signal}));
  }],
  [AccessModule.RASLINKLOGINGOV, () => redirectToRas(false)],
  [AccessModule.ERACOMMONS, async() => {
    await withAborter(aborter => profileApi().syncEraCommonsStatus({signal: aborter.signal}));
  }],
  [AccessModule.COMPLIANCETRAINING, async() => {
    await withAborter(aborter => profileApi().syncComplianceTrainingStatus({signal: aborter.signal}))
  }],
  [AccessModule.DATAUSERCODEOFCONDUCT, () => {}],
]);

// this function does double duty:
// - returns appropriate text for completed and bypassed modules and null for incomplete modules
// - because of this, truthy return values indicate that a module is either complete or bypassed
const bypassedOrCompletedText = (status: AccessModuleStatus) => {
  const {completionEpochMillis, bypassEpochMillis}: AccessModuleStatus = status || {};
  const userCompletedModule = !!completionEpochMillis;
  const userBypassedModule = !!bypassEpochMillis;

  return cond(
      [userCompletedModule, () => `Completed on: ${displayDateWithoutHours(completionEpochMillis)}`],
      [userBypassedModule, () => `Bypassed on: ${displayDateWithoutHours(bypassEpochMillis)}`],
      // return nothing if there's no text
    () => null
  );
};

const handleTerraShibbolethCallback = (token: string, spinnerProps: WithSpinnerOverlayProps, reloadProfile: Function) => {
  const handler = withErrorModal({
    title: 'Error saving NIH Authentication status.',
    message: 'An error occurred trying to save your NIH Authentication status. Please try again.',
    onDismiss: () => {
      spinnerProps.hideSpinner();
    }
  })(async() => {
    spinnerProps.showSpinner();
    await profileApi().updateNihToken({jwt: token});
    spinnerProps.hideSpinner();
    reloadProfile();
  });

  return handler();
};

const handleRasCallback = (code: string, spinnerProps: WithSpinnerOverlayProps, reloadProfile: Function) => {
  const handler = withErrorModal({
    title: 'Error saving RAS Login.Gov linkage status.',
    message: 'An error occurred trying to save your RAS Login.Gov linkage status. Please try again.',
    onDismiss: () => {
      spinnerProps.hideSpinner();
    }
  })(async() => {
    spinnerProps.showSpinner();
    await profileApi().linkRasAccount({ authCode: code, redirectUrl: buildRasRedirectUrl() });
    spinnerProps.hideSpinner();
    reloadProfile();

    // Cleanup parameter from URL after linking.
    window.history.replaceState({}, '', '/');
  });

  return handler();
};

const selfBypass = async(spinnerProps: WithSpinnerOverlayProps, reloadProfile: Function) => {
  spinnerProps.showSpinner();
  await bypassAll(allModules, true);
  spinnerProps.hideSpinner();
  reloadProfile();
};

// exported for test
export const getEnabledModules = (modules: AccessModule[], navigate): AccessModule[] => fp.flatMap(moduleName => {
  const enabledTaskMaybe = getRegistrationTask(navigate, moduleName);
  return enabledTaskMaybe ? [enabledTaskMaybe.module] : [];
}, modules);

// exported for test
export const getActiveModule = (modules: AccessModule[], profile: Profile): AccessModule => modules.find(moduleName => {
  const status = getAccessModuleStatusByName(profile, moduleName);
  return !bypassedOrCompletedText(status);
});

const Refresh = (props: { showSpinner: Function; refreshAction: Function }) => {
  const {showSpinner, refreshAction} = props;
  return <Button
      type='primary'
      style={styles.refreshButton}
      onClick={async() => {
        showSpinner();
        await refreshAction();
        location.reload(); // also hides spinner
      }} >
    <Repeat style={styles.refreshIcon}/> Refresh
  </Button>;
}

const Next = () => <FlexRow style={styles.nextElement}>
  <span style={styles.nextText}>NEXT</span> <ArrowRight style={styles.nextIcon}/>
</FlexRow>;

const ModuleIcon = (props: {moduleName: AccessModule, completedOrBypassed: boolean, eligible?: boolean}) => {
  const {moduleName, completedOrBypassed, eligible = true} = props;

  return <div style={styles.moduleIcon}>
    {cond(
      [!eligible,
        () => <MinusCircle data-test-id={`module-${moduleName}-ineligible`} style={{color: colors.disabled}}/>],
      [eligible && completedOrBypassed,
        () => <CheckCircle data-test-id={`module-${moduleName}-complete`} style={{color: colors.success}}/>],
      [eligible && !completedOrBypassed,
        () => <CheckCircle data-test-id={`module-${moduleName}-incomplete`} style={{color: colors.disabled}}/>])}
  </div>;
};

// Sep 16 hack while we work out some RAS bugs
const TemporaryRASModule = () => {
  const moduleName = AccessModule.RASLINKLOGINGOV;
  return <FlexRow data-test-id={`module-${moduleName}`}>
    <FlexRow style={styles.moduleCTA}/>
    <FlexRow style={styles.inactiveModuleBox}>
      <ModuleIcon moduleName={moduleName} completedOrBypassed={false} eligible={false}/>
      <FlexColumn style={styles.inactiveModuleText}>
        <div>
          {moduleLabels.get(moduleName)}
        </div>
        <div style={{fontSize: '14px', marginTop: '0.5em'}}>
          <b>Temporarily disabled.</b> Due to technical difficulties, this step is disabled.
          In the future, you'll be prompted to complete identity verification to continue using the workbench.
        </div>
     </FlexColumn>
    </FlexRow>
  </FlexRow>;
};

interface ModuleProps {
  moduleName: AccessModule;
  active: boolean;    // is this the currently-active module that the user should complete

  // TODO RW-7059
  // eligible: boolean;  // is the user eligible to complete this module (does the inst. allow it)
  spinnerProps: WithSpinnerOverlayProps;
}
// Renders a module when it's enabled via feature flags.  Returns null if not.
const MaybeModule = ({moduleName, active, spinnerProps}: ModuleProps): JSX.Element => {
  // whether to show the refresh button: this module has been clicked
  const [showRefresh, setShowRefresh] = useState(false);

  // whether to show the Two Factor Auth Modal
  const [showTwoFactorAuthModal, setShowTwoFactorAuthModal] = useState(false);

  const [navigate, ] = useNavigation();
  const registrationTask = getRegistrationTask(navigate, moduleName);

  const ModuleBox = ({children}) => {
    // kluge until we have fully migrated from the Registration Dashboard:
    // getRegistrationTask() has onClick() functions for every module, which is generally what we want
    // but we pop up a modal for Two Factor Auth instead of using the standard task
    const moduleAction = registrationTask && (moduleName === AccessModule.TWOFACTORAUTH ?
        () => setShowTwoFactorAuthModal(true) :
        registrationTask.onClick);

    return active ?
        <Clickable onClick={() => {
          setShowRefresh(true);
          moduleAction();
        }}>
          <FlexRow style={styles.activeModuleBox}>{children}</FlexRow>
        </Clickable> :
        <FlexRow style={styles.inactiveModuleBox}>{children}</FlexRow>;
  };

  const Module = () => {
    const {profile} = useStore(profileStore);
    const statusTextMaybe = bypassedOrCompletedText(getAccessModuleStatusByName(profile, moduleName));

    return <FlexRow data-test-id={`module-${moduleName}`}>
      <FlexRow style={styles.moduleCTA}>
        {active && (showRefresh
            ? <Refresh
                refreshAction={moduleRefreshActions.get(moduleName)}
                showSpinner={spinnerProps.showSpinner}/>
            : <Next/>)}
      </FlexRow>
      <ModuleBox>
        <ModuleIcon moduleName={moduleName} completedOrBypassed={!!statusTextMaybe}/>
        <FlexColumn>
          <div style={active ? styles.activeModuleText : styles.inactiveModuleText}>
            {moduleLabels.get(moduleName)}
          </div>
          {statusTextMaybe && <div style={styles.moduleDate}>{statusTextMaybe}</div>}
        </FlexColumn>
      </ModuleBox>
      {showTwoFactorAuthModal && <TwoFactorAuthModal
          onClick={() => setShowTwoFactorAuthModal(false)}
          onCancel={() => setShowTwoFactorAuthModal(false)}/>}
    </FlexRow>;
  };

  // temp hack Sep 16: render a special temporary RAS module if disabled
  if (moduleName === AccessModule.RASLINKLOGINGOV) {
    const {enableRasLoginGovLinking} = serverConfigStore.get().config;
    if (!enableRasLoginGovLinking) {
      return <TemporaryRASModule/>;
    }
  }
  const moduleEnabled = !!registrationTask;
  return moduleEnabled ? <Module/> : null;
};

const DARHeader = () => <FlexColumn style={styles.headerFlexColumn}>
  <Header style={styles.headerRW}>Researcher Workbench</Header>
  <Header style={styles.headerDAR}>Data Access Requirements</Header>
</FlexColumn>;

const Completed = () => <FlexRow data-test-id='dar-completed' style={styles.completed}>
  <FlexColumn>
    <div style={styles.completedHeader}>Thank you for completing all the necessary steps</div>
    <div style={styles.completedText}>Researcher Workbench data access is complete.</div>
  </FlexColumn>
  <GetStartedButton style={{marginLeft: 'auto'}}/>
</FlexRow>;

const ModulesForCard = (props: {modules: AccessModule[], activeModule: AccessModule, spinnerProps: WithSpinnerOverlayProps}) => {
  const {modules, activeModule, spinnerProps} = props;
  return <FlexColumn style={styles.modulesContainer}>
    {modules.map(moduleName =>
        <MaybeModule key={moduleName} moduleName={moduleName} active={moduleName === activeModule} spinnerProps={spinnerProps}/>
    )}
  </FlexColumn>;
};

// TODO is there a better way?

import {ReactComponent as individual} from 'assets/icons/DAR/individual.svg';
import {ReactComponent as identifying} from 'assets/icons/DAR/identifying.svg';
import {ReactComponent as electronic} from 'assets/icons/DAR/electronic.svg';
import {ReactComponent as survey} from 'assets/icons/DAR/survey.svg';
import {ReactComponent as physical} from 'assets/icons/DAR/physical.svg';
import {ReactComponent as wearable} from 'assets/icons/DAR/wearable.svg';

const Individual = individual;
const Identifying = identifying;
const Electronic = electronic;
const Survey = survey;
const Physical = physical;
const Wearable = wearable;

const renderIcon = (iconName: string) => switchCase(iconName,
    ['individual', () => <Individual style={styles.rtDetailsIcon}/>],
    ['identifying', () => <Identifying style={styles.rtDetailsIcon}/>],
    ['electronic', () => <Electronic style={styles.rtDetailsIcon}/>],
    ['survey', () => <Survey style={styles.rtDetailsIcon}/>],
    ['physical', () => <Physical style={styles.rtDetailsIcon}/>],
    ['wearable', () => <Wearable style={styles.rtDetailsIcon}/>]
);

const DataDetail = (props: {icon: string, text: string}) => {
  const {icon, text} = props;
  return <FlexRow>
    {renderIcon(icon)}
    <div style={styles.rtDataDetails}>{text}</div>
  </FlexRow>;
};

const RegisteredTierCard = (props: {activeModule: AccessModule, spinnerProps: WithSpinnerOverlayProps}) => {
  const {activeModule, spinnerProps} = props;
  return <FlexRow style={styles.card}>
    <FlexColumn>
      <div style={styles.cardStep}>Step 1</div>
      <div style={styles.cardHeader}>Complete Registration</div>
      <FlexRow>
        <RegisteredTierBadge/>
        <div style={styles.rtData}>Registered Tier data</div>
      </FlexRow>
      <div style={styles.rtDataDetails}>Once registered, you’ll have access to:</div>
      <DataDetail icon='individual' text='Individual (not aggregated) data'/>
      <DataDetail icon='identifying' text='Identifying information removed'/>
      <DataDetail icon='electronic' text='Electronic health records'/>
      <DataDetail icon='survey' text='Survey responses'/>
      <DataDetail icon='physical' text='Physical measurements'/>
      <DataDetail icon='wearable' text='Wearable devices'/>
    </FlexColumn>
    <ModulesForCard modules={rtModules} activeModule={activeModule} spinnerProps={spinnerProps}/>
  </FlexRow>;
};

const DuccCard = (props: {activeModule: AccessModule, spinnerProps: WithSpinnerOverlayProps}) => {
  const {activeModule, spinnerProps} = props;
  return <FlexRow style={{...styles.card, height: '125px'}}>
    <FlexColumn>
      {/* This will be Step 3 when CT becomes the new Step 2 */}
      <div style={styles.cardStep}>Step 2</div>
      <div style={styles.cardHeader}>Sign the code of conduct</div>
    </FlexColumn>
    <ModulesForCard modules={[duccModule]} activeModule={activeModule} spinnerProps={spinnerProps}/>
  </FlexRow>;
};

export const DataAccessRequirements = fp.flow(withProfileErrorModal)((spinnerProps: WithSpinnerOverlayProps) => {
  const {profile, reload} = useStore(profileStore);

  useEffect(() => {
    spinnerProps.hideSpinner();
  }, []);

  // handle the route /nih-callback?token=<token>
  // handle the route /ras-callback?code=<code>
  const query = useQuery();
  const token = query.get('token');
  const code = query.get('code');
  useEffect(() => {
    if (token) {
      handleTerraShibbolethCallback(token, spinnerProps, reload);
    }
  }, [token] );
  useEffect(() => {
    if (code) {
      handleRasCallback(code, spinnerProps, reload);
    }
  }, [code]);

  // which module are we currently guiding the user to complete?
  const [activeModule, setActiveModule] = useState(null);

  const [navigate, ] = useNavigation();
  const enabledModules = getEnabledModules(allModules, navigate);

  // whenever the profile changes, setActiveModule(the first incomplete enabled module)
  useEffect(() => {
    const activeModule = getActiveModule(enabledModules, profile);
    if (activeModule) {
      setActiveModule(activeModule);
    }
  }, [profile]);

  const {config: {unsafeAllowSelfBypass}} = useStore(serverConfigStore);

  return <FlexColumn style={styles.pageWrapper}>
      <DARHeader/>
      {profile && !activeModule && <Completed/>}
      {unsafeAllowSelfBypass && activeModule && <FlexRow data-test-id='self-bypass' style={styles.selfBypass}>
        <div style={styles.selfBypassText}>[Test environment] Self-service bypass is enabled</div>
        <Button
            style={{marginLeft: '0.5rem'}}
            onClick={async() => await selfBypass(spinnerProps, reload)}>Bypass all</Button>
      </FlexRow>}
      <FadeBox style={styles.fadeBox}>
        <div style={styles.pleaseComplete}>
          Please complete the necessary steps to gain access to the <AoU/> datasets.
        </div>
        <RegisteredTierCard activeModule={activeModule} spinnerProps={spinnerProps}/>
        {/* TODO RW-7059 - Step 2 ControlledTierCard */}
        <DuccCard activeModule={activeModule} spinnerProps={spinnerProps}/>
      </FadeBox>
    </FlexColumn>;
});
