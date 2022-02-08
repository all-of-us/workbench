import * as fp from 'lodash/fp';
import * as React from 'react';
import { Redirect } from 'react-router-dom';

import { Button } from 'app/components/buttons';
import { AoU } from 'app/components/text-wrappers';
import { profileApi, userAdminApi } from 'app/services/swagger-fetch-clients';
import { AnalyticsTracker } from 'app/utils/analytics';
import { convertAPIError } from 'app/utils/errors';
import { encodeURIComponentStrict } from 'app/utils/navigation';
import {
  authStore,
  profileStore,
  serverConfigStore,
  useStore,
} from 'app/utils/stores';
import {
  AccessModule,
  AccessModuleStatus,
  AccessModuleConfig,
  ErrorCode,
  Profile,
} from 'generated/fetch';
import { parseQueryParams } from 'app/components/app-router';
import { cond, switchCase } from './index';
import { TooltipTrigger } from 'app/components/popups';
import { InfoIcon } from 'app/components/icons';
import {
  getWholeDaysFromNow,
  displayDateWithoutHours,
  MILLIS_PER_DAY,
} from './dates';
import {
  hasExpired,
  isExpiringNotBypassed,
} from 'app/pages/access/access-renewal';

export enum AccessModulesStatus {
  NEVER_EXPIRES = 'Complete (Never Expires)',
  CURRENT = 'Current',
  EXPIRING_SOON = 'Expiring Soon',
  EXPIRED = 'Expired',
  BYPASSED = 'Bypassed',
  INCOMPLETE = 'Incomplete',
}

const { useState, useEffect } = React;

export async function redirectToRegisteredTraining() {
  AnalyticsTracker.Registration.RegisteredTraining();
  await profileApi().updatePageVisits({ page: 'moodle' });
  const {
    config: { complianceTrainingHost },
  } = serverConfigStore.get();
  const url = `https://${complianceTrainingHost}/static/data-researcher.html?saml=on'`;
  window.open(url, '_blank');
}

export async function redirectToControlledTraining() {
  AnalyticsTracker.Registration.ControlledTraining();
  await profileApi().updatePageVisits({ page: 'moodle' });
  const {
    config: { complianceTrainingHost },
  } = serverConfigStore.get();
  const url = `https://${complianceTrainingHost}/static/data-researcher-controlled.html?saml=on'`;
  window.open(url, '_blank');
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

export const ACCESS_RENEWAL_PATH = '/access-renewal';
export const DATA_ACCESS_REQUIREMENTS_PATH = '/data-access-requirements';

interface AccessModuleUIConfig extends AccessModuleConfig {
  isEnabledInEnvironment: boolean; // either true or dependent on a feature flag
  isRequiredByRT: boolean;
  isRequiredByCT: boolean;
  AARTitleComponent: () => JSX.Element;
  DARTitleComponent: () => JSX.Element;
  adminPageTitle: string;
  externalSyncAction?: Function;
  refreshAction?: Function;
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
    enableRasLoginGovLinking,
    enforceRasLoginGovLinking,
    enableEraCommons,
    enableComplianceTraining,
    accessModules,
  } = serverConfigStore.get().config;
  const apiConfig = accessModules.find((m) => m.name === moduleName);
  return switchCase(
    moduleName,

    [
      AccessModule.TWOFACTORAUTH,
      () => ({
        ...apiConfig,
        isEnabledInEnvironment: true,
        DARTitleComponent: () => <div>Turn on Google 2-Step Verification</div>,
        adminPageTitle: 'Google 2-Step Verification',
        externalSyncAction: async () =>
          await profileApi().syncTwoFactorAuthStatus(),
        refreshAction: async () => await profileApi().syncTwoFactorAuthStatus(),
        isRequiredByRT: true,
        isRequiredByCT: true,
      }),
    ],

    [
      AccessModule.RASLINKLOGINGOV,
      () => ({
        ...apiConfig,
        isEnabledInEnvironment:
          enableRasLoginGovLinking || enforceRasLoginGovLinking,
        DARTitleComponent: () => (
          <div>
            Verify your identity with Login.gov{' '}
            <TooltipTrigger
              content={
                'For additional security, we require you to verify your identity by uploading a photo of your ID.'
              }
            >
              <InfoIcon style={{ margin: '0 0.3rem' }} />
            </TooltipTrigger>
          </div>
        ),
        adminPageTitle: 'Verify your identity with Login.gov',
        refreshAction: () => redirectToRas(false),
        isRequiredByRT: enforceRasLoginGovLinking,
        isRequiredByCT: enforceRasLoginGovLinking,
      }),
    ],

    [
      AccessModule.ERACOMMONS,
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
      AccessModule.COMPLIANCETRAINING,
      () => ({
        ...apiConfig,
        isEnabledInEnvironment: enableComplianceTraining,
        AARTitleComponent: () => (
          <div>
            <AoU /> Responsible Conduct of Research Training
          </div>
        ),
        DARTitleComponent: () => (
          <div>
            Complete <AoU /> research Registered Tier training
          </div>
        ),
        adminPageTitle: 'Registered Tier training',
        externalSyncAction: async () =>
          await profileApi().syncComplianceTrainingStatus(),
        refreshAction: async () =>
          await profileApi().syncComplianceTrainingStatus(),
        isRequiredByRT: true,
        isRequiredByCT: true,
      }),
    ],

    [
      AccessModule.CTCOMPLIANCETRAINING,
      () => ({
        ...apiConfig,
        isEnabledInEnvironment: enableComplianceTraining,
        DARTitleComponent: () => (
          <div>
            Complete <AoU /> research Controlled Tier training
          </div>
        ),
        adminPageTitle: 'Controlled Tier training',
        externalSyncAction: async () =>
          await profileApi().syncComplianceTrainingStatus(),
        refreshAction: async () =>
          await profileApi().syncComplianceTrainingStatus(),
        isRequiredByRT: false,
        isRequiredByCT: true,
      }),
    ],

    [
      AccessModule.DATAUSERCODEOFCONDUCT,
      () => ({
        ...apiConfig,
        isEnabledInEnvironment: true,
        AARTitleComponent: () => 'Sign Data User Code of Conduct',
        DARTitleComponent: () => <div>Sign Data User Code of Conduct</div>,
        adminPageTitle: 'Sign Data User Code of Conduct',
        isRequiredByRT: true,
        isRequiredByCT: true,
      }),
    ],

    [
      AccessModule.PROFILECONFIRMATION,
      () => ({
        ...apiConfig,
        isEnabledInEnvironment: true,
        AARTitleComponent: () => 'Update your profile',
        adminPageTitle: 'Update your profile',
        isRequiredByRT: true,
        isRequiredByCT: true,
      }),
    ],

    [
      AccessModule.PUBLICATIONCONFIRMATION,
      () => ({
        ...apiConfig,
        isEnabledInEnvironment: true,
        AARTitleComponent: () =>
          'Report any publications or presentations based on your research using the Researcher Workbench',
        adminPageTitle: 'Report any publications',
        isRequiredByRT: true,
        isRequiredByCT: true,
      }),
    ]
  );
};

// the modules subject to Annual Access Renewal (AAR), in the order shown on the AAR page.
export const accessRenewalModules = [
  AccessModule.PROFILECONFIRMATION,
  AccessModule.PUBLICATIONCONFIRMATION,
  AccessModule.COMPLIANCETRAINING,
  AccessModule.DATAUSERCODEOFCONDUCT,
];

export const wasReferredFromRenewal = (queryParams): boolean => {
  const renewal = parseQueryParams(queryParams).get('renewal');
  return renewal === '1';
};
export const NOTIFICATION_THRESHOLD_DAYS = 30;

// return the number of full days remaining to expiration in the soonest-to-expire module,
// but only if it is within the threshold.
// if it is not, or no expiration dates are present in the profile: return undefined.
export const maybeDaysRemaining = (profile: Profile): number | undefined => {
  const earliestExpiration: number = fp.flow(
    fp.get(['accessModules', 'modules']),
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
          if (errorResponse.errorCode === ErrorCode.USERDISABLED && mounted) {
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

export const getAccessModuleStatusByName = (
  profile: Profile,
  moduleName: AccessModule
): AccessModuleStatus => {
  return profile.accessModules.modules.find((a) => a.moduleName === moduleName);
};

export const bypassAll = async (
  accessModules: AccessModule[],
  isBypassed: boolean
) => {
  for (const module of accessModules) {
    await userAdminApi().unsafeSelfBypassAccessRequirement({
      moduleName: module,
      isBypassed: isBypassed,
    });
  }
};

export const GetStartedButton = ({ style = { marginLeft: '0.5rem' } }) => (
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

export const computeRenewalDisplayDates = ({
  completionEpochMillis,
  expirationEpochMillis,
  bypassEpochMillis,
}: AccessModuleStatus) => {
  const userCompletedModule = !!completionEpochMillis;
  const userBypassedModule = !!bypassEpochMillis;
  const lastConfirmedDate = withInvalidDateHandling(completionEpochMillis);
  const nextReviewDate = withInvalidDateHandling(expirationEpochMillis);
  const bypassDate = withInvalidDateHandling(bypassEpochMillis);

  function getCompleteOrExpireModuleStatus(): AccessModulesStatus {
    return cond(
      [!expirationEpochMillis, () => AccessModulesStatus.NEVER_EXPIRES],
      [hasExpired(expirationEpochMillis), () => AccessModulesStatus.EXPIRED],
      [
        isExpiringNotBypassed({ expirationEpochMillis }),
        () => AccessModulesStatus.EXPIRING_SOON,
      ],
      [!!expirationEpochMillis, () => AccessModulesStatus.CURRENT]
    );
  }

  return cond(
    // User has bypassed module
    [
      userBypassedModule,
      () => ({
        lastConfirmedDate: `${bypassDate}`,
        nextReviewDate: 'Unavailable (bypassed)',
        moduleStatus: AccessModulesStatus.BYPASSED,
      }),
    ],
    // User never completed training
    [
      !userCompletedModule && !userBypassedModule,
      () => ({
        lastConfirmedDate: 'Unavailable (not completed)',
        nextReviewDate: 'Unavailable (not completed)',
        moduleStatus: AccessModulesStatus.INCOMPLETE,
      }),
    ],
    // User completed training; covers expired, within-lookback, and after-lookback cases.
    [
      userCompletedModule && !userBypassedModule,
      () => {
        const daysRemaining = getWholeDaysFromNow(expirationEpochMillis);
        const daysRemainingDisplay =
          daysRemaining >= 0
            ? `(${daysRemaining} day${daysRemaining !== 1 ? 's' : ''})`
            : '(expired)';
        return {
          lastConfirmedDate,
          nextReviewDate: `${nextReviewDate} ${daysRemainingDisplay}`,
          moduleStatus: getCompleteOrExpireModuleStatus(),
        };
      },
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
  return Promise.all(
    moduleNames.map(async (moduleName) => {
      const { externalSyncAction } = getAccessModuleConfig(moduleName);
      if (externalSyncAction) {
        await externalSyncAction();
      }
    })
  );
};
