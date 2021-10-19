import * as fp from 'lodash/fp';
import * as React from 'react';
import {useEffect, useState} from 'react';
import assert from "assert";

import {useQuery} from 'app/components/app-router';
import {Button, Clickable} from 'app/components/buttons';
import {FadeBox} from 'app/components/containers';
import {FlexColumn, FlexRow} from 'app/components/flex';
import {Header} from 'app/components/headers';
import {
  ArrowRight,
  CheckCircle,
  ControlledTierBadge,
  MinusCircle,
  RegisteredTierBadge,
  Repeat
} from 'app/components/icons';
import {withErrorModal} from 'app/components/modals';
import {AoU} from 'app/components/text-wrappers';
import {withProfileErrorModal} from 'app/components/with-error-modal';
import {WithSpinnerOverlayProps} from 'app/components/with-spinner-overlay';
import {profileApi} from 'app/services/swagger-fetch-clients';
import colors, {colorWithWhiteness} from 'app/styles/colors';
import {cond, displayDateWithoutHours, reactStyles, switchCase} from 'app/utils';
import {
  buildRasRedirectUrl,
  bypassAll,
  getAccessModuleConfig,
  getAccessModuleStatusByName,
  GetStartedButton,
  redirectToNiH,
  redirectToRas,
  redirectToTraining,
} from 'app/utils/access-utils';
import {useNavigation} from 'app/utils/navigation';
import {profileStore, serverConfigStore, useStore} from 'app/utils/stores';
import {AccessModule, AccessModuleStatus, Profile} from 'generated/fetch';
import {TwoFactorAuthModal} from './two-factor-auth-modal';
import {AnalyticsTracker} from 'app/utils/analytics';
import {ReactComponent as additional} from 'assets/icons/DAR/additional.svg';
import {ReactComponent as electronic} from 'assets/icons/DAR/electronic.svg';
import {ReactComponent as genomic} from 'assets/icons/DAR/genomic.svg';
import {ReactComponent as identifying} from 'assets/icons/DAR/identifying.svg';
import {ReactComponent as individual} from 'assets/icons/DAR/individual.svg';
import {ReactComponent as physical} from 'assets/icons/DAR/physical.svg';
import {ReactComponent as survey} from 'assets/icons/DAR/survey.svg';
import {ReactComponent as wearable} from 'assets/icons/DAR/wearable.svg';
import {AccessTierShortNames} from 'app/utils/access-tiers';
import {environment} from 'environments/environment';

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

// TODO RW-7059 *
const ctModules = [AccessModule.ERACOMMONS];

const duccModule = AccessModule.DATAUSERCODEOFCONDUCT;

// in display order
// exported for test
export const allModules: AccessModule[] = [
  ...rtModules,
  ...ctModules,
  duccModule,
];

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
const getVisibleRTModules = (profile: Profile): AccessModule[] => {
  return fp.filter(module=> isEraCommonsModuleRequiredByInstitution(profile, module),rtModules);
}

const isEraCommonsModuleRequiredByInstitution = (profile: Profile, moduleNames: AccessModule): boolean => {
  // Remove the eRA Commons module when the flag to enable RAS is set and the user's
  // institution does not require eRA Commons for RT.

  if (moduleNames !== AccessModule.ERACOMMONS) { return true;}
  const {enableRasLoginGovLinking} = serverConfigStore.get().config;
  if (!enableRasLoginGovLinking) { return true; }

  return fp.flow(
      fp.filter({accessTierShortName: AccessTierShortNames.Registered}),
      fp.some('eraRequired')
  )(profile.tierEligibilities);
}

// exported for test
export const getVisibleModules = (modules: AccessModule[], profile: Profile): AccessModule[] => fp.flow(
    fp.map(getAccessModuleConfig),
    fp.filter(moduleConfig => moduleConfig.isEnabledInEnvironment),
    fp.filter(moduleConfig => isEraCommonsModuleRequiredByInstitution(profile, moduleConfig.moduleName)),
    fp.map(moduleConfig => moduleConfig.moduleName)
)(modules);

const incompleteModules = (modules: AccessModule[], profile: Profile): AccessModule[] => modules.filter(moduleName => {
  const status = getAccessModuleStatusByName(profile, moduleName);
  return !bypassedOrCompletedText(status);
});

const syncIncompleteModules = (modules: AccessModule[], profile: Profile, reloadProfile: Function) => {
  incompleteModules(modules, profile).map(async moduleName => {
    const {externalSyncAction} = getAccessModuleConfig(moduleName);
    if (externalSyncAction) {
      await externalSyncAction();
    }
  });
  reloadProfile();
}

// exported for test
export const getActiveModule = (modules: AccessModule[], profile: Profile): AccessModule => incompleteModules(modules, profile)[0];

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
  const {DARTitleComponent} = getAccessModuleConfig(moduleName);
  return <FlexRow data-test-id={`module-${moduleName}`}>
    <FlexRow style={styles.moduleCTA}/>
    <FlexRow style={styles.inactiveModuleBox}>
      <ModuleIcon moduleName={moduleName} completedOrBypassed={false} eligible={false}/>
      <FlexColumn style={styles.inactiveModuleText}>
        <DARTitleComponent/>
        <div style={{fontSize: '14px', marginTop: '0.5em'}}>
          <b>Temporarily disabled.</b> Due to technical difficulties, this step is disabled.
          In the future, you'll be prompted to complete identity verification to continue using the workbench.
        </div>
     </FlexColumn>
    </FlexRow>
  </FlexRow>;
};

interface ModuleProps {
  profile: Profile,
  moduleName: AccessModule;
  active: boolean;    // is this the currently-active module that the user should complete

  // TODO RW-7059
  // eligible: boolean;  // is the user eligible to complete this module (does the inst. allow it)
  spinnerProps: WithSpinnerOverlayProps;
}

// Renders a module when it's enabled via feature flags.  Returns null if not.
const MaybeModule = ({profile, moduleName, active, spinnerProps}: ModuleProps): JSX.Element => {
  // whether to show the refresh button: this module has been clicked
  const [showRefresh, setShowRefresh] = useState(false);

  // whether to show the Two Factor Auth Modal
  const [showTwoFactorAuthModal, setShowTwoFactorAuthModal] = useState(false);
  const [navigate, ] = useNavigation();

  // outside of the main getAccessModuleConfig() so that function doesn't have to deal with navigate
  const moduleAction: Function = switchCase(moduleName,
    [AccessModule.TWOFACTORAUTH, () => () => setShowTwoFactorAuthModal(true)],
    [AccessModule.RASLINKLOGINGOV, () => redirectToRas],
    [AccessModule.ERACOMMONS, () => redirectToNiH],
    [AccessModule.COMPLIANCETRAINING, () => redirectToTraining],
    [AccessModule.DATAUSERCODEOFCONDUCT, () => () => {
      AnalyticsTracker.Registration.EnterDUCC();
      navigate(['data-code-of-conduct']);
    }]);

  const {DARTitleComponent, refreshAction, isEnabledInEnvironment} = getAccessModuleConfig(moduleName);

  const ModuleBox = ({children}) => {
    return active
        ? <Clickable onClick={() => { setShowRefresh(true); moduleAction(); }}>
            <FlexRow style={styles.activeModuleBox}>{children}</FlexRow>
          </Clickable>
        : <FlexRow style={styles.inactiveModuleBox}>{children}</FlexRow>;
  };

  const Module = ({profile}) => {
    const statusTextMaybe = bypassedOrCompletedText(getAccessModuleStatusByName(profile, moduleName));

    return <FlexRow data-test-id={`module-${moduleName}`}>
      <FlexRow style={styles.moduleCTA}>
        {active && ((showRefresh && refreshAction)
            ? <Refresh
                refreshAction={refreshAction}
                showSpinner={spinnerProps.showSpinner}/>
            : <Next/>)}
      </FlexRow>
      <ModuleBox>
        <ModuleIcon moduleName={moduleName} completedOrBypassed={!!statusTextMaybe}/>
        <FlexColumn>
          <div style={active ? styles.activeModuleText : styles.inactiveModuleText}>
            <DARTitleComponent/>
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
  return isEnabledInEnvironment ? <Module profile={profile}/> : null;
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

interface CardProps {
  profile: Profile,
  modules: Array<AccessModule>,
  activeModule: AccessModule,
  spinnerProps: WithSpinnerOverlayProps
}
const ModulesForCard = (props: CardProps) => {
  const {profile, modules, activeModule, spinnerProps} = props;
  return <FlexColumn style={styles.modulesContainer}>
    {modules.map(moduleName =>
        <MaybeModule
            key={moduleName}
            moduleName={moduleName}
            profile={profile}
            active={moduleName === activeModule}
            spinnerProps={spinnerProps}/>
    )}
  </FlexColumn>;
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

const renderIcon = (iconName: string) => switchCase(iconName,
    ['additional', () => <Additional style={styles.rtDetailsIcon}/>],
    ['electronic', () => <Electronic style={styles.rtDetailsIcon}/>],
    ['genomic', () => <Genomic style={styles.rtDetailsIcon}/>],
    ['identifying', () => <Identifying style={styles.rtDetailsIcon}/>],
    ['individual', () => <Individual style={styles.rtDetailsIcon}/>],
    ['physical', () => <Physical style={styles.rtDetailsIcon}/>],
    ['survey', () => <Survey style={styles.rtDetailsIcon}/>],
    ['wearable', () => <Wearable style={styles.rtDetailsIcon}/>],
);

const DataDetail = (props: {icon: string, text: string}) => {
  const {icon, text} = props;
  return <FlexRow>
    {renderIcon(icon)}
    <div style={styles.rtDataDetails}>{text}</div>
  </FlexRow>;
};

const RegisteredTierCard = (props: {profile: Profile, activeModule: AccessModule, spinnerProps: WithSpinnerOverlayProps}) => {
  const {profile, activeModule, spinnerProps} = props;
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
    <ModulesForCard profile={profile} modules={getVisibleRTModules(profile)} activeModule={activeModule} spinnerProps={spinnerProps}/>
  </FlexRow>;
};

const ControlledTierCard = (props: {profile: Profile}) => {
  const {profile} = props;
  const controlledTierEligibility = profile.tierEligibilities.find(tier=> tier.accessTierShortName === AccessTierShortNames.Controlled);
  const isSigned = !!controlledTierEligibility;
  const hasAccess = isSigned && controlledTierEligibility.eligible;
  return <FlexRow style={{...styles.card, height: 300}}>
    <FlexColumn>
      <div style={styles.cardStep}>Step 2</div>
      <div style={styles.cardHeader}>Additional Data Access</div>
      <FlexRow>
        <ControlledTierBadge/>
        <div style={styles.rtData}>Controlled Tier data</div>
      </FlexRow>
      <div style={styles.rtDataDetails}>You are eligible to access Controlled Tier Data</div>
      <div style={styles.rtDataDetails}>In addition to Registered Tier data, the Controlled Tier curated dataset contains:</div>
      <DataDetail icon='genomic' text='Genomic data'/>
      <DataDetail icon='additional' text='Additional demographic details'/>
    </FlexColumn>
    <FlexColumn style={styles.modulesContainer}>
      <FlexRow>
        <FlexRow style={styles.moduleCTA}/>
        <FlexRow style={styles.inactiveModuleBox}>
          <div style={styles.moduleIcon}>
            {isSigned ? <CheckCircle style={{color: colors.success}}/>
                : <MinusCircle style={{color: colors.disabled}}/>}
          </div>
          <FlexColumn style={styles.inactiveModuleText}>
            <div>{profile.verifiedInstitutionalAffiliation.institutionDisplayName} must
              sign an institutional agreement
            </div>
          </FlexColumn>
        </FlexRow>
      </FlexRow>
      {/*  Has access*/}
      <FlexRow>
        <FlexRow style={styles.moduleCTA}/>
        <FlexRow style={styles.inactiveModuleBox}>
          <div style={styles.moduleIcon}>
            {hasAccess ? <CheckCircle style={{color: colors.success}}/>
                : <MinusCircle style={{color: colors.disabled}}/>}
          </div>
          <FlexColumn style={styles.inactiveModuleText}>
            <div>{profile.verifiedInstitutionalAffiliation.institutionDisplayName} must
              allow you to access controlled tier data
            </div>
          </FlexColumn>
        </FlexRow>
      </FlexRow>
    </FlexColumn>
  </FlexRow>
};

const DuccCard = (props: {profile: Profile, activeModule: AccessModule, spinnerProps: WithSpinnerOverlayProps, stepNumber: Number}) => {
  const {profile, activeModule, spinnerProps, stepNumber} = props;
  return <FlexRow style={{...styles.card, height: '125px'}}>
    <FlexColumn>
      <div style={styles.cardStep}>Step {stepNumber}</div>
      <div style={styles.cardHeader}>Sign the code of conduct</div>
    </FlexColumn>
    <ModulesForCard profile={profile} modules={[duccModule]} activeModule={activeModule} spinnerProps={spinnerProps}/>
  </FlexRow>;
};

export const DataAccessRequirements = fp.flow(withProfileErrorModal)((spinnerProps: WithSpinnerOverlayProps) => {
  const {profile, reload} = useStore(profileStore);
  const {config: {unsafeAllowSelfBypass}} = useStore(serverConfigStore);
  const visibleModules = getVisibleModules(allModules, profile);

  useEffect(() => {
    syncIncompleteModules(visibleModules, profile, reload);
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

  // whenever the profile changes, setActiveModule(the first incomplete enabled module)
  useEffect(() => {
    setActiveModule(getActiveModule(visibleModules, profile));
  }, [profile]);

  const enableCt = environment.accessTiersVisibleToUsers.includes(AccessTierShortNames.Controlled)

  const rtCard = <RegisteredTierCard key='rt' profile={profile} activeModule={activeModule} spinnerProps={spinnerProps}/>
  const ctCard = enableCt ? <ControlledTierCard key='ct' profile={profile}/> : null
  const dCard = <DuccCard key='dt' profile={profile} activeModule={activeModule} spinnerProps={spinnerProps} stepNumber={enableCt ? 3 : 2}/>

  const cards = enableCt ? [rtCard, ctCard, dCard] : [rtCard, dCard];

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
        <React.Fragment>{cards}</React.Fragment>
      </FadeBox>
    </FlexColumn>;
});
