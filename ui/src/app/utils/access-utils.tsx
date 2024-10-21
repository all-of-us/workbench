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

const redirectToTraining = async () => {
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
  window.open(url.toString(), '_blank');
  await profileApi().updatePageVisits({ page: 'absorb' });
};

export async function redirectToRegisteredTraining() {
  AnalyticsTracker.Registration.RegisteredTraining();
  await redirectToTraining();
}

export async function redirectToControlledTraining() {
  AnalyticsTracker.Registration.ControlledTraining();
  await redirectToTraining();
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

interface AccessModuleUIConfig extends AccessModuleConfig {
  isEnabledInEnvironment: boolean; // either true or dependent on a feature flag
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
  const { enableRasLoginGovLinking, enableComplianceTraining, accessModules } =
    serverConfigStore.get().config;
  const apiConfig = accessModules.find((m) => m.name === moduleName);
  return switchCase<AccessModule, any>(
    moduleName,

    [
      AccessModule.TWO_FACTOR_AUTH,
      () => ({
        ...apiConfig,
        isEnabledInEnvironment: true,
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
        adminPageTitle: 'Verify your identity',
        refreshAction: () => redirectToRas(false),
      }),
    ],

    [
      AccessModule.COMPLIANCE_TRAINING,
      () => ({
        ...apiConfig,
        isEnabledInEnvironment: enableComplianceTraining,
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
        adminPageTitle: 'Sign Data User Code of Conduct',
        renewalTimeEstimate: 5,
      }),
    ],

    [
      AccessModule.PROFILE_CONFIRMATION,
      () => ({
        ...apiConfig,
        isEnabledInEnvironment: true,
        adminPageTitle: 'Update your profile',
        renewalTimeEstimate: 5,
      }),
    ],

    [
      AccessModule.PUBLICATION_CONFIRMATION,
      () => ({
        ...apiConfig,
        isEnabledInEnvironment: true,
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

export const acceptTermsOfService = (tosVersion: number) => {
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
      const { adminPageTitle, externalSyncAction } =
        getAccessModuleConfig(moduleName);
      if (externalSyncAction) {
        try {
          await externalSyncAction();
        } catch (exception) {
          return Promise.reject(`Failed to synchronize ${adminPageTitle}`);
        }
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
