import * as React from 'react';
import { Redirect } from 'react-router-dom';
import * as fp from 'lodash/fp';

import {
  AccessModule,
  AccessModuleConfig,
  AccessModuleStatus,
  ErrorCode,
  Profile,
} from 'generated/fetch';

import { cond, DEFAULT, switchCase } from '@terra-ui-packages/core-utils';
import { parseQueryParams } from 'app/components/app-router';
import { Button } from 'app/components/buttons';
import { InfoIcon } from 'app/components/icons';
import { TooltipTrigger } from 'app/components/popups';
import { ComplianceTrainingModuleCardTitle } from 'app/pages/access/compliance-training-module-card-title';
import { IdentityHelpText } from 'app/pages/access/identity-help-text';
import { LoginGovHelpText } from 'app/pages/access/login-gov-help-text';
import { userIsDisabled } from 'app/routing/guards';
import { profileApi } from 'app/services/swagger-fetch-clients';
import { AnalyticsTracker } from 'app/utils/analytics';
import { convertAPIError } from 'app/utils/errors';
import { encodeURIComponentStrict } from 'app/utils/navigation';
import {
  authStore,
  profileStore,
  serverConfigStore,
  useStore,
} from 'app/utils/stores';

import { AccessTierShortNames } from './access-tiers';
import { isCurrentDUCCVersion } from './code-of-conduct';
import {
  displayDateWithoutHours,
  getWholeDaysFromNow,
  MILLIS_PER_DAY,
} from './dates';

export enum AccessRenewalStatus {
  NEVER_EXPIRES = 'Complete (Never Expires)',
  CURRENT = 'Current',
  EXPIRING_SOON = 'Expiring Soon',
  EXPIRED = 'Expired',
  BYPASSED = 'Bypassed',
  INCOMPLETE = 'Incomplete',
}

export enum DARPageMode {
  INITIAL_REGISTRATION = 'INITIAL_REGISTRATION',
  ANNUAL_RENEWAL = 'ANNUAL_RENEWAL',
}

const { useState, useEffect } = React;

const redirectToRegisteredTrainingMoodle = async () => {
  await profileApi().updatePageVisits({ page: 'moodle' });
  const {
    config: { complianceTrainingHost },
  } = serverConfigStore.get();
  const url = `https://${complianceTrainingHost}/static/data-researcher.html?saml=on`;
  window.open(url, '_blank');
};

const redirectToTrainingAbsorb = async () => {
  await profileApi().updatePageVisits({ page: 'absorb' });
  const {
    config: {
      absorbSamlIdentityProviderId,
      absorbSamlServiceProviderId,
      gsuiteDomain,
    },
  } = serverConfigStore.get();
  const url = new URL('https://accounts.google.com/o/saml2/initsso');
  url.searchParams.set('idpid', absorbSamlIdentityProviderId);
  url.searchParams.set('spid', absorbSamlServiceProviderId);
  url.searchParams.set('forceauthn', 'false');
  url.searchParams.set('hd', gsuiteDomain);
  url.searchParams.set('RelayState', "https://aoudev.myabsorb.com/#/online-courses/3765dc64-cc64-4efa-bfc0-9a4dc2e9d09d");
  console.warn(url.toString())
  window.open(url.toString(), '_blank');
};

export async function redirectToRegisteredTraining() {
  AnalyticsTracker.Registration.RegisteredTraining();

  const useAbsorb = await profileApi().useAbsorb();

  if (useAbsorb) {
    await redirectToTrainingAbsorb();
  } else {
    await redirectToRegisteredTrainingMoodle();
  }
}

const redirectToControlledTrainingMoodle = async () => {
  await profileApi().updatePageVisits({ page: 'moodle' });
  const {
    config: { complianceTrainingHost },
  } = serverConfigStore.get();
  const url = `https://${complianceTrainingHost}/static/data-researcher-controlled.html?saml=on`;
  window.open(url, '_blank');
};

export async function redirectToControlledTraining() {
  AnalyticsTracker.Registration.ControlledTraining();

  const useAbsorb = await profileApi().useAbsorb();

  if (useAbsorb) {
    await redirectToTrainingAbsorb();
  } else {
    await redirectToControlledTrainingMoodle();
  }
}

export const getTwoFactorSetupUrl = (): string => {
  const accountChooserBase = 'https://accounts.google.com/AccountChooser';
  const url = new URL(accountChooserBase);
  // If available, set the 'Email' param to give Google a hint that we want to access the
  // target URL as this specific G Suite user. This helps guide users when multi-login is in use.
  if (profileStore.get().profile) {
    url.searchParams.set('Email', profileStore.get().profile.username);
  }
  url.searchParams.set(
    'continue',
    'https://myaccount.google.com/signinoptions/two-step-verification/enroll'
  );
  return url.toString();
};

export const redirectToTwoFactorSetup = (): void => {
  AnalyticsTracker.Registration.TwoFactorAuth();
  window.open(getTwoFactorSetupUrl(), '_blank');
};

export const NIH_CALLBACK_PATH = '/nih-callback';

export const redirectToNiH = (): void => {
  AnalyticsTracker.Registration.ERACommons();

  const url =
    serverConfigStore.get().config.rasLogoutUrl +
    serverConfigStore.get().config.shibbolethUiBaseUrl +
    '/login?return-url=' +
    encodeURIComponent(
      window.location.origin.toString() + NIH_CALLBACK_PATH + '?token={token}'
    );
  window.open(url, '_blank');
};

export const RAS_CALLBACK_PATH = '/ras-callback';

/** Build the RAS OAuth redirect URL. It should be AoU hostname/ras-callback. */
export const buildRasRedirectUrl = (): string => {
  return encodeURIComponentStrict(
    window.location.origin.toString() + RAS_CALLBACK_PATH
  );
};

export const redirectToRas = (openInNewTab: boolean = true): void => {
  AnalyticsTracker.Registration.RasLoginGov();
  // The scopes are also used in backend for fetching user info.
  const url =
    serverConfigStore.get().config.rasHost +
    '/auth/oauth/v2/authorize?client_id=' +
    serverConfigStore.get().config.rasClientId +
    '&prompt=login+consent&redirect_uri=' +
    buildRasRedirectUrl() +
    '&response_type=code&scope=openid+profile+email+federated_identities';

  openInNewTab ? window.open(url, '_blank') : <Redirect to={url} />;
};

export const DATA_ACCESS_REQUIREMENTS_PATH = '/data-access-requirements';
export const ACCESS_RENEWAL_PATH =
  DATA_ACCESS_REQUIREMENTS_PATH + '?pageMode=' + DARPageMode.ANNUAL_RENEWAL;

interface DARTitleComponentConfig {
  profile: Profile;
  afterInitialClick?: boolean;
  onClick?: Function;
}

interface AARTitleComponentConfig {
  profile: Profile;
}

interface AccessModuleUIConfig extends AccessModuleConfig {
  isEnabledInEnvironment: boolean; // either true or dependent on a feature flag
  AARTitleComponent: (config: AARTitleComponentConfig) => JSX.Element;
  DARTitleComponent: (config: DARTitleComponentConfig) => JSX.Element;
  adminPageTitle: string;
  externalSyncAction?: Function;
  refreshAction?: Function;
  renewalTimeEstimate?: number;
}

// This needs to be a function, because we want it to evaluate at call time,
// not at compile time, to ensure that we make use of the server config store.
// This is important so that we can use feature flags.
//
// Important: The completion criteria here needs to be kept synchronized with
// the server-side logic, else users can get stuck on the DAR
// without a next step:
// https://github.com/all-of-us/workbench/blob/main/api/src/main/java/org/pmiops/workbench/db/dao/UserServiceImpl.java#L240-L272
export const getAccessModuleConfig = (
  moduleName: AccessModule
): AccessModuleUIConfig => {
  const {
    enableRasIdMeLinking,
    enableRasLoginGovLinking,
    enableEraCommons,
    enableComplianceTraining,
    accessModules,
  } = serverConfigStore.get().config;
  const apiConfig = accessModules.find((m) => m.name === moduleName);
  return switchCase<AccessModule, any>(
    moduleName,

    [
      AccessModule.TWO_FACTOR_AUTH,
      () => ({
        ...apiConfig,
        isEnabledInEnvironment: true,
        DARTitleComponent: () => <div>Turn on Google 2-Step Verification</div>,
        adminPageTitle: 'Google 2-Step Verification',
        externalSyncAction: async () =>
          await profileApi().syncTwoFactorAuthStatus(),
        refreshAction: async () => await profileApi().syncTwoFactorAuthStatus(),
      }),
    ],
    [
      AccessModule.IDENTITY,
      () => ({
        ...apiConfig,
        isEnabledInEnvironment: enableRasLoginGovLinking,
        DARTitleComponent: (props: DARTitleComponentConfig) => {
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
        adminPageTitle: 'Verify your identity with Login.gov',
        refreshAction: () => redirectToRas(false),
      }),
    ],

    [
      AccessModule.ERA_COMMONS,
      () => ({
        ...apiConfig,
        isEnabledInEnvironment: enableEraCommons,
        DARTitleComponent: () => <div>Connect your eRA Commons account</div>,
        adminPageTitle: 'Connect your eRA Commons* account',
        externalSyncAction: async () =>
          await profileApi().syncEraCommonsStatus(),
        refreshAction: async () => await profileApi().syncEraCommonsStatus(),
      }),
    ],

    [
      AccessModule.COMPLIANCE_TRAINING,
      () => ({
        ...apiConfig,
        isEnabledInEnvironment: enableComplianceTraining,
        AARTitleComponent: (props: AARTitleComponentConfig) => (
          <ComplianceTrainingModuleCardTitle
            tier={AccessTierShortNames.Registered}
            profile={props.profile}
          />
        ),
        DARTitleComponent: (props: DARTitleComponentConfig) => (
          <ComplianceTrainingModuleCardTitle
            tier={AccessTierShortNames.Registered}
            profile={props.profile}
          />
        ),
        adminPageTitle: 'Registered Tier training',
        externalSyncAction: async () =>
          await profileApi().syncComplianceTrainingStatus(),
        refreshAction: async () =>
          await profileApi().syncComplianceTrainingStatus(),
        renewalTimeEstimate: 60,
      }),
    ],

    [
      AccessModule.CT_COMPLIANCE_TRAINING,
      () => ({
        ...apiConfig,
        isEnabledInEnvironment: enableComplianceTraining,
        AARTitleComponent: (props: AARTitleComponentConfig) => (
          <ComplianceTrainingModuleCardTitle
            tier={AccessTierShortNames.Controlled}
            profile={props.profile}
          />
        ),
        DARTitleComponent: (props: DARTitleComponentConfig) => (
          <ComplianceTrainingModuleCardTitle
            tier={AccessTierShortNames.Controlled}
            profile={props.profile}
          />
        ),
        adminPageTitle: 'Controlled Tier training',
        externalSyncAction: async () =>
          await profileApi().syncComplianceTrainingStatus(),
        refreshAction: async () =>
          await profileApi().syncComplianceTrainingStatus(),
        renewalTimeEstimate: 60,
      }),
    ],

    [
      AccessModule.DATA_USER_CODE_OF_CONDUCT,
      () => ({
        ...apiConfig,
        isEnabledInEnvironment: true,
        AARTitleComponent: () => 'Sign Data User Code of Conduct',
        DARTitleComponent: () => <div>Sign Data User Code of Conduct</div>,
        adminPageTitle: 'Sign Data User Code of Conduct',
        renewalTimeEstimate: 5,
      }),
    ],

    [
      AccessModule.PROFILE_CONFIRMATION,
      () => ({
        ...apiConfig,
        isEnabledInEnvironment: true,
        AARTitleComponent: () => 'Update your profile',
        adminPageTitle: 'Update your profile',
        renewalTimeEstimate: 5,
      }),
    ],

    [
      AccessModule.PUBLICATION_CONFIRMATION,
      () => ({
        ...apiConfig,
        isEnabledInEnvironment: true,
        AARTitleComponent: () =>
          'Report any publications or presentations based on your research using the Researcher Workbench',
        adminPageTitle: 'Report any publications',
        renewalTimeEstimate: 5,
      }),
    ],
    [
      DEFAULT,
      () => ({
        ...apiConfig,
      }),
    ]
  );
};

// the modules subject to Registered Tier Annual Access Renewal (AAR), in the order shown on the AAR page.
export const rtAccessRenewalModules = [
  AccessModule.PROFILE_CONFIRMATION,
  AccessModule.PUBLICATION_CONFIRMATION,
  AccessModule.COMPLIANCE_TRAINING,
  AccessModule.DATA_USER_CODE_OF_CONDUCT,
];

export const wasReferredFromRenewal = (queryParams): boolean => {
  const renewal = parseQueryParams(queryParams).get('renewal');
  return renewal === '1';
};

export const NOTIFICATION_THRESHOLD_DAYS = 30;

// return the number of full days remaining to expiration in the soonest-to-expire module for a given tier,
// but only if it is within the threshold.
// if it is not, or no expiration dates are present in the profile for this tier: return undefined.
export const maybeDaysRemaining = (
  profile: Profile,
  accessTier: AccessTierShortNames = AccessTierShortNames.Registered
): number | undefined => {
  const tierFilter = (status: AccessModuleStatus): boolean =>
    accessTier === AccessTierShortNames.Registered
      ? getAccessModuleConfig(status.moduleName).requiredForRTAccess
      : getAccessModuleConfig(status.moduleName).requiredForCTAccess;

  const earliestExpiration: number = fp.flow(
    fp.get(['accessModules', 'modules']),
    fp.filter(tierFilter),
    fp.map<AccessModuleStatus, number>((m) => m.expirationEpochMillis),
    // remove the undefined expirationEpochMillis
    fp.compact,
    fp.min
  )(profile);

  if (earliestExpiration) {
    // show the number of full remaining days, e.g. 30 if 30.7 remain or 0 if only a partial day remains
    const daysRemaining = Math.floor(
      (earliestExpiration - Date.now()) / MILLIS_PER_DAY
    );
    if (daysRemaining <= NOTIFICATION_THRESHOLD_DAYS) {
      return daysRemaining;
    }
  }
};

// A hook to determine whether the current user is signed in and disabled.
// Returns undefined if the status is unknown.
export const useIsUserDisabled = () => {
  const { authLoaded, isSignedIn } = useStore(authStore);
  const [disabled, setDisabled] = useState<boolean | undefined>(undefined);
  useEffect(() => {
    if (!authLoaded) {
      return;
    }

    let mounted = true;
    if (!isSignedIn) {
      setDisabled(false);
    } else {
      (async () => {
        try {
          await profileStore.get().load();
          if (mounted) {
            setDisabled(false);
          }
        } catch (e) {
          const errorResponse = await convertAPIError(e);
          if (errorResponse.errorCode === ErrorCode.USER_DISABLED && mounted) {
            setDisabled(true);
          }
        }
      })();
    }
    return () => {
      mounted = false;
    };
  }, [authLoaded, isSignedIn]);
  return disabled;
};

export const useShowTOS = () => {
  const { authLoaded, isSignedIn } = useStore(authStore);
  const profile = profileStore.get().profile;
  const [userRequiredToAcceptTOS, setUserRequiredToAcceptTOS] =
    useState<boolean>(false);
  useEffect(() => {
    if (!authLoaded || !isSignedIn) {
      setUserRequiredToAcceptTOS(false);
    } else if (profile) {
      // wait for profile to load, to  ensure user initialization happens
      // before checking term of service status
      (async () => {
        try {
          // Do not show terms of service page, if the user is disabled or not eligible for RT
          if (userIsDisabled(profile.disabled)) {
            setUserRequiredToAcceptTOS(false);
          } else {
            const userHasAcceptedLatestTOS =
              await profileApi().getUserTermsOfServiceStatus();
            setUserRequiredToAcceptTOS(!userHasAcceptedLatestTOS);
          }
        } catch (e) {
          console.log('Error while getting user terms of service status');
        }
      })();
    }
    return () => {};
  }, [authLoaded, isSignedIn, profile]);
  return userRequiredToAcceptTOS;
};

export const acceptTermsOfService = (tosVersion) => {
  (async () => {
    try {
      await profileApi().acceptTermsOfService(tosVersion);
      window.location.reload();
    } catch (ex) {
      console.log('Error while accepting TOS');
    }
  })();
};

export const getAccessModuleStatusByNameOrEmpty = (
  modules: AccessModuleStatus[],
  moduleName: AccessModule
): AccessModuleStatus => {
  return (
    modules.find((status) => status.moduleName === moduleName) || {
      moduleName,
    }
  );
};

export const getAccessModuleStatusByName = (
  profile: Profile,
  moduleName: AccessModule
): AccessModuleStatus => {
  return getAccessModuleStatusByNameOrEmpty(
    profile.accessModules.modules,
    moduleName
  );
};

export const GetStartedButton = ({ style = { marginLeft: '0.75rem' } }) => (
  <Button
    style={style}
    onClick={() => {
      // After a registration status change, to be safe, we reload the application. This results in
      // rerendering of the homepage, but also reruns some application bootstrapping / caching which may
      // have been dependent on the user's registration status, e.g. CDR config information.
      location.replace('/');
    }}
  >
    Get Started
  </Button>
);

const withInvalidDateHandling = (date) => {
  if (!date) {
    return 'Unavailable';
  } else {
    return displayDateWithoutHours(date);
  }
};

// The module has already expired
export const hasExpired = (expiration: number): boolean =>
  !!expiration && getWholeDaysFromNow(expiration) < 0;

export const isExpiringOrExpired = (
  expiration: number,
  module: AccessModule
): boolean => {
  const trainingModules: Array<AccessModule> = [
    AccessModule.COMPLIANCE_TRAINING,
    AccessModule.CT_COMPLIANCE_TRAINING,
  ];
  const lookback = trainingModules.includes(module)
    ? serverConfigStore.get().config.complianceTrainingRenewalLookback
    : serverConfigStore.get().config.accessRenewalLookback;
  return !!expiration && getWholeDaysFromNow(expiration) <= lookback;
};

export const isCompleted = (
  status: AccessModuleStatus,
  duccSignedVersion: number
): boolean =>
  status?.moduleName === AccessModule.DATA_USER_CODE_OF_CONDUCT
    ? // special case for DUCC: considered incomplete if the signed version is missing or old
      isCurrentDUCCVersion(duccSignedVersion) && !!status?.completionEpochMillis
    : !!status?.completionEpochMillis;

export const isBypassed = (status: AccessModuleStatus): boolean =>
  !!status?.bypassEpochMillis;

export const isCompliant = (
  status: AccessModuleStatus,
  duccSignedVersion: number
): boolean => isCompleted(status, duccSignedVersion) || isBypassed(status);

// is the module "renewal complete" ?
// meaning (bypassed || (complete and not expiring))
export const isRenewalCompleteForModule = (
  status: AccessModuleStatus,
  duccSignedVersion: number
) => {
  return (
    isBypassed(status) ||
    (isCompleted(status, duccSignedVersion) &&
      !isExpiringOrExpired(status?.expirationEpochMillis, status.moduleName))
  );
};

interface RenewalDisplayDates {
  lastConfirmedDate: string;
  nextReviewDate: string;
  moduleStatus: AccessRenewalStatus;
}
export const computeRenewalDisplayDates = (
  status: AccessModuleStatus,
  duccSignedVersion: number
): RenewalDisplayDates => {
  const {
    completionEpochMillis,
    expirationEpochMillis,
    bypassEpochMillis,
    moduleName,
  } = status || {};
  const userExpiredModule = !!expirationEpochMillis;
  const lastConfirmedDate = withInvalidDateHandling(completionEpochMillis);
  const nextReviewDate = withInvalidDateHandling(expirationEpochMillis);
  const bypassDate = withInvalidDateHandling(bypassEpochMillis);

  const daysRemainingDisplay = () => {
    const daysRemaining = getWholeDaysFromNow(expirationEpochMillis);
    return `(${daysRemaining} day${daysRemaining !== 1 ? 's' : ''})`;
  };

  return cond<RenewalDisplayDates>(
    // User has bypassed module
    [
      isBypassed(status),
      () => ({
        lastConfirmedDate: `${bypassDate}`,
        nextReviewDate: 'Unavailable (bypassed)',
        moduleStatus: AccessRenewalStatus.BYPASSED,
      }),
    ],
    // Module is incomplete
    [
      !isCompleted(status, duccSignedVersion) && !isBypassed(status),
      () => ({
        lastConfirmedDate: 'Unavailable (not completed)',
        nextReviewDate: 'Unavailable (not completed)',
        moduleStatus: AccessRenewalStatus.INCOMPLETE,
      }),
    ],
    // After this point, we know the module is complete and not bypassed.  The remaining checks determine whether the
    // completion is expired, never expires, expires soon (within-lookback) or expires later (after lookback).
    [
      !userExpiredModule,
      () => ({
        lastConfirmedDate,
        nextReviewDate: `Never Expires`,
        moduleStatus: AccessRenewalStatus.NEVER_EXPIRES,
      }),
    ],
    [
      hasExpired(expirationEpochMillis),
      () => ({
        lastConfirmedDate,
        nextReviewDate: `${nextReviewDate} (expired)`,
        moduleStatus: AccessRenewalStatus.EXPIRED,
      }),
    ],
    [
      isExpiringOrExpired(expirationEpochMillis, moduleName),
      () => ({
        lastConfirmedDate,
        nextReviewDate: `${nextReviewDate} ${daysRemainingDisplay()}`,
        moduleStatus: AccessRenewalStatus.EXPIRING_SOON,
      }),
    ],
    [
      isCompleted(status, duccSignedVersion),
      () => ({
        lastConfirmedDate,
        nextReviewDate: `${nextReviewDate} ${daysRemainingDisplay()}`,
        moduleStatus: AccessRenewalStatus.CURRENT,
      }),
    ]
  );
};

// return true if user is eligible for registered tier.
// A user loses tier eligibility when they are removed from institution tier requirement
export const eligibleForTier = (
  profile: Profile,
  accessTierShortName: string
): boolean => {
  const rtEligiblity = profile.tierEligibilities.find(
    (t) => t.accessTierShortName === accessTierShortName
  );
  return rtEligiblity?.eligible;
};

export const syncModulesExternal = async (moduleNames: AccessModule[]) => {
  // RT and CT compliance training have the same external sync action.
  // Calling both can cause conflicts, so we need to remove one.
  // We choose to remove CT arbitrarily.
  const filteredModuleNames = moduleNames.includes(
    AccessModule.COMPLIANCE_TRAINING
  )
    ? moduleNames.filter((m) => m !== AccessModule.CT_COMPLIANCE_TRAINING)
    : moduleNames;

  return Promise.all(
    filteredModuleNames.map(async (moduleName) => {
      const { externalSyncAction } = getAccessModuleConfig(moduleName);
      if (externalSyncAction) {
        await externalSyncAction();
      }
    })
  );
};

export const isEligibleModule = (module: AccessModule, profile: Profile) => {
  if (module !== AccessModule.CT_COMPLIANCE_TRAINING) {
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

export const getStatusText = (
  status: AccessModuleStatus,
  duccSignedVersion: number
) => {
  console.assert(
    isCompliant(status, duccSignedVersion),
    'Cannot provide status text for incomplete module'
  );
  const { completionEpochMillis, bypassEpochMillis } = status;
  return isCompleted(status, duccSignedVersion)
    ? `Completed on: ${displayDateWithoutHours(completionEpochMillis)}`
    : `Bypassed on: ${displayDateWithoutHours(bypassEpochMillis)}`;
};

export const hasRtExpired = (profile: Profile): boolean => {
  return rtAccessRenewalModules
    .filter(
      (moduleName) => getAccessModuleConfig(moduleName).isEnabledInEnvironment
    )
    .map((module: AccessModule) => getAccessModuleStatusByName(profile, module))
    .some(
      (status: AccessModuleStatus) =>
        hasExpired(status.expirationEpochMillis) && !status?.bypassEpochMillis
    );
};
