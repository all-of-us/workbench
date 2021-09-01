import * as fp from 'lodash/fp';
import * as React from 'react';
import {useEffect, useState} from 'react';

import {Button, Link} from 'app/components/buttons';
import {FadeBox} from 'app/components/containers';
import {FlexColumn, FlexRow} from 'app/components/flex';
import {Header} from 'app/components/headers';
import {
  ArrowRight,
  CheckCircle,
  DARIcons,
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
import {cond, displayDateWithoutHours, reactStyles} from 'app/utils';
import {
  buildRasRedirectUrl,
  bypassAll,
  getAccessModuleStatusByName,
  getRegistrationTask,
  GetStartedButton,
} from 'app/utils/access-utils';
import {queryParamsStore, useNavigation} from 'app/utils/navigation';
import {profileStore, serverConfigStore, useStore} from 'app/utils/stores';
import {AccessModule, AccessModuleStatus} from 'generated/fetch';
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

const duccModule = [
  AccessModule.DATAUSERCODEOFCONDUCT,
];

// in display order
// exported for test
export const allModules: AccessModule[] = [
  ...rtModules,
  ...ctModules,
  ...duccModule,
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

const handleTerraShibbolethCallback = (token: string, spinnerProps: WithSpinnerOverlayProps) => {
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
  });

  return handler();
};

const handleRasCallback = (code: string, spinnerProps: WithSpinnerOverlayProps) => {
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
  });

  return handler();
};

interface ModuleProps {
  module: AccessModule;
  active: boolean;    // is this the currently-active module that the user should complete

  // TODO RW-7059
  // eligible: boolean;  // is the user eligible to complete this module (does the inst. allow it)
}
const MaybeModule = (props: ModuleProps): JSX.Element => {
  const {profile} = useStore(profileStore);
  const [navigate, ] = useNavigation();

  const {module, active} = props;
  const statusTextMaybe = bypassedOrCompletedText(getAccessModuleStatusByName(profile, module));

  // whether to show the refresh button: this module has been clicked
  const [showRefresh, setShowRefresh] = useState(false);

  // whether to show the Two Factor Auth Modal
  const [showTwoFactorAuthModal, setShowTwoFactorAuthModal] = useState(false);

  const registrationTask = getRegistrationTask(navigate, module);

  // kluge until we have fully migrated from the Registration Dashboard:
  // getRegistrationTask() has onClick() functions for every module, which is generally what we want
  // but we pop up a modal for Two Factor Auth instead of using the standard task
  const moduleAction = registrationTask && (module === AccessModule.TWOFACTORAUTH ?
      () => setShowTwoFactorAuthModal(true) :
      registrationTask.onClick);

  const Refresh = () => <Button
      type='primary'
      style={styles.refreshButton}
      onClick={() => location.reload()} >
    <Repeat style={styles.refreshIcon}/> Refresh
  </Button>;

  const Next = () => <FlexRow style={styles.nextElement}>
    <span style={styles.nextText}>NEXT</span> <ArrowRight style={styles.nextIcon}/>
  </FlexRow>;

  const eligible = true; // TODO RW-7059
  const ModuleIcon = () => <div style={styles.moduleIcon}>
    {cond(
      // not eligible to complete module
      [!eligible, () => <MinusCircle data-test-id={`module-${module}-ineligible`} style={{color: colors.disabled}}/>],
      // eligible and (completed or bypassed)
      [eligible && !!statusTextMaybe, () => <CheckCircle data-test-id={`module-${module}-complete`} style={{color: colors.success}}/>],
      // eligible and incomplete and unbypassed
      [eligible && !statusTextMaybe, () => <CheckCircle data-test-id={`module-${module}-incomplete`} style={{color: colors.disabled}}/>])}
  </div>;

  const ModuleBox = ({children}) => {
    return active ?
        <Link onClick={() => {
          setShowRefresh(true);
          moduleAction();
        }}>
          <FlexRow style={styles.activeModuleBox}>{children}</FlexRow>
        </Link> :
        <FlexRow style={styles.inactiveModuleBox}>{children}</FlexRow>;
  };

  const Module = () => <FlexRow data-test-id={`module-${module}`}>
    <FlexRow style={styles.moduleCTA}>
      {active && (showRefresh ? <Refresh/> : <Next/>)}
    </FlexRow>
    <ModuleBox>
      <ModuleIcon/>
      <FlexColumn>
        <div style={active ? styles.activeModuleText : styles.inactiveModuleText}>
          {moduleLabels.get(module)}
        </div>
        {statusTextMaybe && <div style={styles.moduleDate}>{statusTextMaybe}</div>}
      </FlexColumn>
    </ModuleBox>
    {showTwoFactorAuthModal && <TwoFactorAuthModal
        onClick={() => setShowTwoFactorAuthModal(false)}
        onCancel={() => setShowTwoFactorAuthModal(false)}/>}
  </FlexRow>;

  const moduleEnabled = !!registrationTask;
  return moduleEnabled ? <Module/> : null;
};

export const DataAccessRequirements = fp.flow(withProfileErrorModal)((spinnerProps: WithSpinnerOverlayProps) => {
  const {profile, reload} = useStore(profileStore);

  const syncExternalModulesAndReloadProfile = async() => {
    const aborter = new AbortController();
    spinnerProps.showSpinner();
    await Promise.all([
      profileApi().syncTwoFactorAuthStatus({signal: aborter.signal}),
      profileApi().syncComplianceTrainingStatus({signal: aborter.signal}),
      profileApi().syncEraCommonsStatus({signal: aborter.signal}),
    ]);
    spinnerProps.hideSpinner();
    reload();

    // cleanup on unmount
    return aborter.abort;
  };

  useEffect(() => {
    syncExternalModulesAndReloadProfile();
  }, []);

  // handle the route /nih-callback?token=<token>
  // handle the route /ras-callback?code=<code>
  const {token, code} = queryParamsStore.getValue();
  useEffect(() => {
    if (token) {
      handleTerraShibbolethCallback(token, spinnerProps);
    }
  }, [token] );
  useEffect(() => {
    if (code) {
      handleRasCallback(code, spinnerProps);
    }
  }, [code] );

  // which module are we currently guiding the user to complete?
  const [activeModule, setActiveModule] = useState(null);

  const [navigate, ] = useNavigation();
  const enabledModules = allModules.map(module => {
    const enabledTaskMaybe = getRegistrationTask(navigate, module);
    return enabledTaskMaybe && enabledTaskMaybe.module;
  });

  // whenever the profile changes, setActiveModule(the first incomplete enabled module)
  useEffect(() => {
    fp.flow(
      fp.find<AccessModule>(module => {
        const status = getAccessModuleStatusByName(profile, module);
        return !bypassedOrCompletedText(status);
      }),
      setActiveModule
    )
    (enabledModules);
  }, [profile]);

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

  const SelfBypass = () => {
    const selfBypass = async() => {
      spinnerProps.showSpinner();
      await bypassAll(allModules, true);
      spinnerProps.hideSpinner();
      reload();
    };

    return <FlexRow data-test-id='self-bypass' style={styles.selfBypass}>
      <div style={styles.selfBypassText}>[Test environment] Self-service bypass is enabled</div>
      <Button style={{marginLeft: '0.5rem'}} onClick={() => selfBypass()}>Bypass all</Button>
    </FlexRow>;
  };

  const ModulesForCard = (props: {modules: AccessModule[]}) => {
    const {modules} = props;
    return <FlexColumn style={styles.modulesContainer}>
      {modules.map(module =>
          <MaybeModule key={module} module={module} active={module === activeModule}/>
      )}
    </FlexColumn>;
  };

  const RegisteredTierCard = () => {
    return <FlexRow style={styles.card}>
      <FlexColumn>
        <div style={styles.cardStep}>Step 1</div>
        <div style={styles.cardHeader}>Complete Registration</div>
        <FlexRow style={styles.rtData}><RegisteredTierBadge/> Registered Tier data</FlexRow>
        <div style={styles.rtDataDetails}>Once registered, you’ll have access to:</div>
        <FlexRow style={styles.rtDataDetails}><DARIcons.individual/> Individual (not aggregated) data</FlexRow>
        <FlexRow style={styles.rtDataDetails}><DARIcons.identifying/> Identifying information removed</FlexRow>
        <FlexRow style={styles.rtDataDetails}><DARIcons.electronic/> Electronic health records</FlexRow>
        <FlexRow style={styles.rtDataDetails}><DARIcons.survey/> Survey responses</FlexRow>
        <FlexRow style={styles.rtDataDetails}><DARIcons.physical/> Physical measurements</FlexRow>
        <FlexRow style={styles.rtDataDetails}><DARIcons.wearable/> Wearable devices</FlexRow>
      </FlexColumn>
      <ModulesForCard modules={rtModules}/>
    </FlexRow>;
  };

  const DuccCard = () => <FlexRow style={{...styles.card, height: '125px'}}>
    <FlexColumn>
      {/* This will be Step 3 when CT becomes the new Step 2 */}
      <div style={styles.cardStep}>Step 2</div>
      <div style={styles.cardHeader}>Sign the code of conduct</div>
    </FlexColumn>
    <ModulesForCard modules={duccModule}/>
  </FlexRow>;

  const {config: {unsafeAllowSelfBypass}} = useStore(serverConfigStore);

  return <FlexColumn style={styles.pageWrapper}>
    <DARHeader/>
    {profile && !activeModule && <Completed/>}
    {unsafeAllowSelfBypass && activeModule && <SelfBypass/>}
    <FadeBox style={styles.fadeBox}>
      <div style={styles.pleaseComplete}>
        Please complete the necessary steps to gain access to the <AoU/> datasets.
      </div>
      <RegisteredTierCard/>
      {/* TODO RW-7059 - Step 2 ControlledTierCard */}
      <DuccCard/>
    </FadeBox>
    </FlexColumn>;
});
