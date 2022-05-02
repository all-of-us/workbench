import * as React from 'react';
import { useEffect, useState } from 'react';
import * as fp from 'lodash/fp';

import { AccessModule, Profile } from 'generated/fetch';

import { environment } from 'environments/environment';
import { useQuery } from 'app/components/app-router';
import { Button } from 'app/components/buttons';
import { FadeBox } from 'app/components/containers';
import { FlexColumn, FlexRow } from 'app/components/flex';
import { Header } from 'app/components/headers';
import { withErrorModal } from 'app/components/modals';
import { SupportMailto } from 'app/components/support';
import { AoU } from 'app/components/text-wrappers';
import { withProfileErrorModal } from 'app/components/with-error-modal';
import { WithSpinnerOverlayProps } from 'app/components/with-spinner-overlay';
import { RenewalRequirementsText } from 'app/pages/access/access-renewal';
import { profileApi } from 'app/services/swagger-fetch-clients';
import colors, { colorWithWhiteness } from 'app/styles/colors';
import { reactStyles, switchCase } from 'app/utils';
import { AccessTierShortNames } from 'app/utils/access-tiers';
import {
  buildRasRedirectUrl,
  bypassAll,
  getAccessModuleConfig,
  getAccessModuleStatusByName,
  getAccessModuleStatusByNameOrEmpty,
  GetStartedButton,
  isCompliant,
  isEligibleModule,
  isRenewalCompleteForModule,
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

export enum DARPageMode {
  INITIAL_REGISTRATION = 'INITIAL_REGISTRATION',
  ANNUAL_RENEWAL = 'ANNUAL_RENEWAL',
}

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
  modules: AccessModule[] = allInitialModules
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

// the header(s) outside the Fadebox

const InitialOuterHeader = () => (
  <FlexColumn style={styles.initialRegistrationOuterHeader}>
    <Header style={styles.initialRegistrationHeaderRW}>
      Researcher Workbench
    </Header>
    <Header style={styles.initialRegistrationHeaderDAR}>
      Data Access Requirements
    </Header>
  </FlexColumn>
);

const OuterHeader = (props: { pageMode: DARPageMode }) =>
  props.pageMode === DARPageMode.INITIAL_REGISTRATION && <InitialOuterHeader />;

// the header(s) inside the Fadebox

const InitialInnerHeader = () => (
  <div data-test-id='initial-registration-header' style={styles.pleaseComplete}>
    Please complete the necessary steps to gain access to the <AoU /> datasets.
  </div>
);

const AnnualInnerHeader = () => (
  <FlexColumn>
    <div
      data-test-id='annual-renewal-header'
      style={styles.renewalHeaderYearly}
    >
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

const InnerHeader = (props: { pageMode: DARPageMode }) =>
  props.pageMode === DARPageMode.INITIAL_REGISTRATION ? (
    <InitialInnerHeader />
  ) : (
    <AnnualInnerHeader />
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
    const { profile, reload } = useStore(profileStore);
    const {
      config: { unsafeAllowSelfBypass },
    } = useStore(serverConfigStore);
    // handle the different page modes of Data Access Requirements
    const [pageMode, setPageMode] = useState(DARPageMode.INITIAL_REGISTRATION);

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

    const pageModeParam = query.get('pageMode');
    useEffect(() => {
      if (
        environment.mergedAccessRenewal &&
        pageModeParam &&
        Object.values(DARPageMode).includes(DARPageMode[pageModeParam])
      ) {
        setPageMode(DARPageMode[pageModeParam]);
      }
    }, [environment.mergedAccessRenewal, pageModeParam]);

    // At any given time, at most two modules will be clickable:
    //  1. The active module, which we visually direct the user to with a CTA
    //  2. The next required module, which may diverge when the active module is optional.
    // This configuration allows the user to skip the optional CT section.
    const [activeModule, setActiveModule] = useState(null);
    const [clickableModules, setClickableModules] = useState([]);

    const getNextActive = (modules: AccessModule[]) =>
      getActiveModule(getEligibleModules(modules, profile), profile, pageMode);
    const nextActive = getNextActive(allInitialModules);
    const nextRequired = getNextActive(initialRequiredModules);

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

    const isComplete = profile && !nextRequired;

    return (
      <FlexColumn style={styles.pageWrapper}>
        <OuterHeader {...{ pageMode }} />
        {isComplete && <Completed />}
        {unsafeAllowSelfBypass && clickableModules.length > 0 && (
          <SelfBypass onClick={async () => selfBypass(spinnerProps, reload)} />
        )}
        <FadeBox style={styles.fadeBox}>
          <InnerHeader {...{ pageMode }} />
          <React.Fragment>{cards}</React.Fragment>
        </FadeBox>
      </FlexColumn>
    );
  }
);
