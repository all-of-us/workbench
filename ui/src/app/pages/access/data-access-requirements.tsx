import * as React from 'react';
import { useEffect, useState } from 'react';
import * as fp from 'lodash/fp';

import { AccessModule, Profile } from 'generated/fetch';

import { useQuery } from 'app/components/app-router';
import { Button, HashLinkButton } from 'app/components/buttons';
import { FadeBox } from 'app/components/containers';
import { FlexColumn, FlexRow } from 'app/components/flex';
import { Header } from 'app/components/headers';
import { ExclamationTriangle } from 'app/components/icons';
import { withErrorModal } from 'app/components/modals';
import { SupportMailto } from 'app/components/support';
import { AoU } from 'app/components/text-wrappers';
import { withProfileErrorModal } from 'app/components/with-error-modal-wrapper';
import { WithSpinnerOverlayProps } from 'app/components/with-spinner-overlay';
import { profileApi, userAdminApi } from 'app/services/swagger-fetch-clients';
import colors, { colorWithWhiteness } from 'app/styles/colors';
import { reactStyles, switchCase } from 'app/utils';
import { AccessTierShortNames } from 'app/utils/access-tiers';
import {
  buildRasRedirectUrl,
  DARPageMode,
  getAccessModuleConfig,
  getAccessModuleStatusByName,
  getAccessModuleStatusByNameOrEmpty,
  GetStartedButton,
  isCompliant,
  isEligibleModule,
  isRenewalCompleteForModule,
  maybeDaysRemaining,
  syncModulesExternal,
} from 'app/utils/access-utils';
import { profileStore, serverConfigStore, useStore } from 'app/utils/stores';
import { ReactComponent as additional } from 'assets/icons/DAR/additional.svg';
import { ReactComponent as electronic } from 'assets/icons/DAR/electronic.svg';
import { ReactComponent as genomic } from 'assets/icons/DAR/genomic.svg';
import { ReactComponent as identifying } from 'assets/icons/DAR/identifying.svg';
import { ReactComponent as individual } from 'assets/icons/DAR/individual.svg';
import { ReactComponent as physical } from 'assets/icons/DAR/physical.svg';
import { ReactComponent as survey } from 'assets/icons/DAR/survey.svg';
import { ReactComponent as wearable } from 'assets/icons/DAR/wearable.svg';

import { ControlledTierCard } from './controlled-tier-card';
import { DuccCard } from './ducc-card';
import { RegisteredTierCard } from './registered-tier-card';

export const styles = reactStyles({
  initialRegistrationOuterHeader: {
    marginLeft: '3%',
    width: '50%',
  },
  initialRegistrationHeaderRW: {
    textTransform: 'uppercase',
    margin: '1em 0 0 0',
  },
  initialRegistrationHeaderDAR: {
    height: 30,
    width: 302,
    fontFamily: 'Montserrat',
    fontSize: 22,
    fontWeight: 500,
    letterSpacing: 0,
    margin: '0.5em 0 0 0',
  },
  renewalHeaderYearly: {
    color: colors.primary,
    fontSize: 20,
    fontWeight: 600,
    gridArea: 'header',
    alignSelf: 'center',
    marginBottom: '20px',
  },
  renewalHeaderYearlyExpired: {
    lineHeight: '40px',
    marginBottom: '0px',
  },
  renewalHeaderRequirements: {
    color: colors.primary,
    fontSize: 14,
    marginBottom: '1em',
    gridArea: 'explanation',
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
  controlledRenewal: {
    height: '87px',
    padding: '1em',
    marginLeft: '3%',
    marginRight: '3%',
    borderRadius: '5px',
    color: colors.primary,
    backgroundColor: colorWithWhiteness(colors.primary, 0.82),
  },
  controlledRenewalHeader: {
    fontSize: '18px',
    fontWeight: 600,
  },
  controlledRenewalText: {
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
    marginLeft: '-1.5rem',
    marginRight: '-0.9rem',
    justifyContent: 'space-between',
    fontSize: '1.2em',
  },
  fadeBox: {
    margin: '0.75rem 0 0 3%',
    width: '95%',
    padding: '0 0.15rem',
  },
  pleaseComplete: {
    color: colors.primary,
    fontSize: 16,
    fontWeight: 600,
    gridArea: 'instructions',
  },
  card: {
    width: '1195px',
    borderRadius: '0.6rem',
    marginTop: '1.05rem',
    marginBottom: '2.55rem',
    color: colors.primary,
    backgroundColor: colorWithWhiteness(colors.accent, 0.9),
    padding: '1em',
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
    marginTop: '0.75rem',
    marginBottom: '0.75rem',
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
    padding: '1.4em',
    margin: '0.0em 0.2em',
    width: '593px',
    borderRadius: '0.3rem',
    backgroundColor: colors.white,
    border: '1px solid',
    borderColor: colors.accent,
  },
  backgroundModuleBox: {
    padding: '1.4em',
    margin: '0.0em 0.2em',
    width: '593px',
    borderRadius: '0.3rem',
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
  renewalStatus: {
    marginLeft: '0.2em',
    marginRight: '1em',
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
  renewalInnerHeaderContainer: {
    display: 'grid',
    width: '1195px',
    gridTemplateAreas: `'icon header'
      '. explanation'
      'instructions instructions'
    `,
  },
  renewalInnerHeaderIcon: {
    gridArea: 'icon',
    marginRight: '0.75rem',
  },
});

const RenewalRequirementsText = () => (
  <span>
    Researchers are required to complete a number of steps as part of the annual
    renewal to maintain access to <AoU /> data. Renewal of access will occur on
    a rolling basis annually (i.e. for each user, access renewal will be due 365
    days after the date of authorization to access <AoU /> data).
  </span>
);

// in display order
export const initialRtModules = [
  AccessModule.TWOFACTORAUTH,
  AccessModule.RASLINKLOGINGOV,
  AccessModule.ERACOMMONS,
  AccessModule.COMPLIANCETRAINING,
];
export const renewalRtModules = [
  AccessModule.PROFILECONFIRMATION,
  AccessModule.PUBLICATIONCONFIRMATION,
  AccessModule.COMPLIANCETRAINING,
];
const ctModule = AccessModule.CTCOMPLIANCETRAINING;
const duccModule = AccessModule.DATAUSERCODEOFCONDUCT;

// in display order
// exported for test
export const initialRequiredModules: AccessModule[] = [
  ...initialRtModules,
  duccModule,
];

export const allInitialModules: AccessModule[] = [
  ...initialRtModules,
  ctModule,
  duccModule,
];

export const renewalRequiredModules: AccessModule[] = [
  ...renewalRtModules,
  duccModule,
];

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
  reloadProfile: Function
) => {
  spinnerProps.showSpinner();
  await userAdminApi().unsafeSelfBypassAccessRequirements();
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
  profile: Profile,
  pageMode: DARPageMode
): AccessModule[] =>
  modules.filter(
    (moduleName) =>
      !isCompliant(getAccessModuleStatusByName(profile, moduleName)) ||
      (pageMode === DARPageMode.ANNUAL_RENEWAL &&
        !isRenewalCompleteForModule(
          getAccessModuleStatusByNameOrEmpty(
            profile.accessModules.modules,
            moduleName
          )
        ))
  );

// exported for test
export const getActiveModule = (
  modules: AccessModule[],
  profile: Profile,
  pageMode: DARPageMode
): AccessModule => incompleteModules(modules, profile, pageMode)[0];

const getNextActive = (
  modules: AccessModule[],
  profile: Profile,
  pageMode: DARPageMode
) => getActiveModule(getEligibleModules(modules, profile), profile, pageMode);

// the header(s) outside the Fadebox

const OuterHeader = (props: { pageMode: DARPageMode }) => (
  <FlexColumn style={styles.initialRegistrationOuterHeader}>
    <Header style={styles.initialRegistrationHeaderRW}>
      Researcher Workbench
    </Header>
    <Header style={styles.initialRegistrationHeaderDAR}>
      {fp.cond([
        [
          (pm) => pm === DARPageMode.INITIAL_REGISTRATION,
          () => 'Data Access Requirements',
        ],
        [(pm) => pm === DARPageMode.ANNUAL_RENEWAL, () => 'Annual Renewal'],
      ])(props.pageMode)}
    </Header>
  </FlexColumn>
);

// the header(s) inside the Fadebox

const InitialInnerHeader = () => (
  <div data-test-id='initial-registration-header' style={styles.pleaseComplete}>
    Please complete the necessary steps to gain access to the <AoU /> datasets.
  </div>
);

const AnnualInnerHeader = (props: { hasExpired: boolean }) => {
  const { hasExpired } = props;
  return (
    <div style={styles.renewalInnerHeaderContainer}>
      {hasExpired && (
        <ExclamationTriangle
          size={40}
          color={colors.warning}
          style={styles.renewalInnerHeaderIcon}
        />
      )}
      <div
        data-test-id='annual-renewal-header'
        style={{
          ...{ ...styles.renewalHeaderYearly },
          ...(hasExpired && styles.renewalHeaderYearlyExpired),
        }}
      >
        {hasExpired
          ? 'Researcher workbench access has expired'
          : 'Yearly Researcher Workbench access renewal'}
      </div>
      <div style={styles.renewalHeaderRequirements}>
        <RenewalRequirementsText />
        <div style={{ marginTop: '0.75rem' }}>
          For any questions, please contact <SupportMailto />.
        </div>
      </div>
      <div style={styles.pleaseComplete}>
        Please complete the following steps.
      </div>
    </div>
  );
};

const InnerHeader = (props: { pageMode: DARPageMode; hasExpired: boolean }) =>
  props.pageMode === DARPageMode.INITIAL_REGISTRATION ? (
    <InitialInnerHeader />
  ) : (
    <AnnualInnerHeader hasExpired={props.hasExpired} />
  );

const SelfBypass = (props: { onClick: () => void }) => (
  <FlexRow data-test-id='self-bypass' style={styles.selfBypass}>
    <div style={styles.selfBypassText}>
      [Test environment] Self-service bypass is enabled
    </div>
    <Button style={{ marginLeft: '0.75rem' }} onClick={() => props.onClick()}>
      Bypass all
    </Button>
  </FlexRow>
);

const ControlledTierRenewalBanner = () => (
  <FlexRow
    data-test-id='controlled-tier-renewal-banner'
    style={styles.controlledRenewal}
  >
    <FlexColumn>
      <div style={styles.controlledRenewalHeader}>
        Controlled Tier Access Renewal
      </div>
      <div style={styles.controlledRenewalText}>
        Please update your modules below.
      </div>
    </FlexColumn>
    <HashLinkButton
      path='?pageMode=ANNUAL_RENEWAL#controlled-card'
      style={{ marginLeft: 'auto' }}
    >
      Get Started
    </HashLinkButton>
  </FlexRow>
);

const CompletionBanner = () => (
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

export const DataDetail = (props: { icon: string; text: string }) => {
  const { icon, text } = props;
  return (
    <FlexRow>
      {renderIcon(icon)}
      <div style={styles.dataDetails}>{text}</div>
    </FlexRow>
  );
};

export const DataAccessRequirements = fp.flow(withProfileErrorModal)(
  (spinnerProps: WithSpinnerOverlayProps) => {
    // Local State
    // At any given time, at most two modules will be clickable during initial registration:
    //  1. The active module, which we visually direct the user to with a CTA
    //  2. The next required module, which may diverge when the active module is optional.
    // This configuration allows the user to skip the optional CT section.
    const [activeModule, setActiveModule] = useState(null);
    const [clickableModules, setClickableModules] = useState([]);

    // Local Variables
    const { profile, reload } = useStore(profileStore);
    const {
      config: { unsafeAllowSelfBypass },
    } = useStore(serverConfigStore);

    const query = useQuery();
    const code = query.get('code');
    const token = query.get('token');

    const pageModeParam = query.get('pageMode');
    const pageMode =
      pageModeParam &&
      Object.values(DARPageMode).includes(DARPageMode[pageModeParam])
        ? DARPageMode[pageModeParam]
        : DARPageMode.INITIAL_REGISTRATION;

    const nextActive = getNextActive(allInitialModules, profile, pageMode);
    const nextRequired = getNextActive(
      pageMode === DARPageMode.INITIAL_REGISTRATION
        ? initialRequiredModules
        : renewalRequiredModules,
      profile,
      pageMode
    );

    const showCtRenewalBanner =
      pageMode === DARPageMode.ANNUAL_RENEWAL &&
      isCompliant(getAccessModuleStatusByName(profile, ctModule)) &&
      !isRenewalCompleteForModule(
        getAccessModuleStatusByName(profile, ctModule)
      );
    const showCompletionBanner =
      profile && !nextRequired && !showCtRenewalBanner;

    const rtCard = (
      <RegisteredTierCard
        {...{ profile, activeModule, clickableModules, spinnerProps, pageMode }}
        key='rt'
      />
    );
    const ctCard = (
      <ControlledTierCard
        {...{
          profile,
          activeModule,
          clickableModules,
          reload,
          spinnerProps,
          pageMode,
        }}
        key='ct'
      />
    );
    const dCard = (
      <DuccCard
        {...{ profile, activeModule, clickableModules, spinnerProps, pageMode }}
        key='dt'
        stepNumber={3}
      />
    );

    const cards = [rtCard, ctCard, dCard];

    // Effects
    useEffect(() => {
      const onMount = async () => {
        await syncModulesExternal(
          incompleteModules(
            getEligibleModules(allInitialModules, profile),
            profile,
            pageMode
          )
        );
        await reload();
        spinnerProps.hideSpinner();
      };

      onMount();
    }, []);

    /*
        TODO Move these into the effect with an empty dependency array.
         I suspect that these are only called when the component is reloaded,
          so the initial effect should be run again. My goal here is that effects should
          only depend on props or local state. When this is the case, we can them above the local
          variables.
       */
    // handle the route /nih-callback?token=<token>
    useEffect(() => {
      if (token) {
        handleTerraShibbolethCallback(token, spinnerProps, reload);
      }
    }, [token]);

    // handle the route /ras-callback?code=<code>
    useEffect(() => {
      if (code) {
        handleRasCallback(code, spinnerProps, reload);
      }
    }, [code]);

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

    const daysRemaining = maybeDaysRemaining(profile);
    const hasExpired = daysRemaining && daysRemaining <= 0;

    return (
      <FlexColumn style={styles.pageWrapper}>
        <OuterHeader {...{ pageMode }} />
        {showCtRenewalBanner && <ControlledTierRenewalBanner />}
        {showCompletionBanner && <CompletionBanner />}
        {unsafeAllowSelfBypass && clickableModules.length > 0 && (
          <SelfBypass onClick={async () => selfBypass(spinnerProps, reload)} />
        )}
        <FadeBox style={styles.fadeBox}>
          <InnerHeader {...{ pageMode, hasExpired }} />
          <React.Fragment>{cards}</React.Fragment>
        </FadeBox>
      </FlexColumn>
    );
  }
);
