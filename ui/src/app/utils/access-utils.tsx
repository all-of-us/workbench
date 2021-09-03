import * as fp from 'lodash/fp';
import * as React from 'react';

import {Button} from 'app/components/buttons';
import {AoU} from 'app/components/text-wrappers';
import {profileApi} from 'app/services/swagger-fetch-clients';
import {AnalyticsTracker} from 'app/utils/analytics';
import {convertAPIError} from 'app/utils/errors';
import {encodeURIComponentStrict, queryParamsStore} from 'app/utils/navigation';
import {authStore, profileStore, serverConfigStore, useStore} from 'app/utils/stores';
import {environment} from 'environments/environment';
import {
  AccessModule,
  AccessModuleStatus,
  CdrVersionTiersResponse,
  ErrorCode,
  Profile
} from 'generated/fetch';
import {getCdrVersionTier} from './cdr-versions';
import {getLiveDUCCVersion} from './code-of-conduct';

const {useState, useEffect} = React;

interface RegistrationTask {
  key: string;    // legacy accessor text
  module: AccessModule;
  completionPropsKey: string;
  loadingPropsKey?: string;
  title: React.ReactNode;
  description: React.ReactNode;
  buttonText: string;
  completedText: string;
  completionTimestamp: (profile: Profile) => number;
  onClick: Function;
  featureFlag?: boolean;
}

export async function redirectToTraining() {
  AnalyticsTracker.Registration.EthicsTraining();
  await profileApi().updatePageVisits({page: 'moodle'});
  window.open(environment.trainingUrl + '/static/data-researcher.html?saml=on', '_blank');
}

export const getTwoFactorSetupUrl = (): string => {
  const accountChooserBase = 'https://accounts.google.com/AccountChooser';
  const url = new URL(accountChooserBase);
  // If available, set the 'Email' param to give Google a hint that we want to access the
  // target URL as this specific G Suite user. This helps guide users when multi-login is in use.
  if (profileStore.get().profile) {
    url.searchParams.set('Email', profileStore.get().profile.username);
  }
  url.searchParams.set('continue', 'https://myaccount.google.com/signinoptions/two-step-verification/enroll');
  return url.toString();
};

export const redirectToTwoFactorSetup = (): void => {
  AnalyticsTracker.Registration.TwoFactorAuth();
  window.open(getTwoFactorSetupUrl(), '_blank');
};

export const NIH_CALLBACK_PATH = '/nih-callback';

export const redirectToNiH = (): void => {
  AnalyticsTracker.Registration.ERACommons();
  const url = serverConfigStore.get().config.shibbolethUiBaseUrl + '/login?return-url=' +
      encodeURIComponent(
        window.location.origin.toString() + NIH_CALLBACK_PATH + '?token=<token>');
  window.open(url, '_blank');
};

export const RAS_CALLBACK_PATH = '/ras-callback';

/** Build the RAS OAuth redirect URL. It should be AoU hostname/ras-callback. */
export const buildRasRedirectUrl = (): string => {
  return encodeURIComponentStrict(window.location.origin.toString() + RAS_CALLBACK_PATH);
};

export const redirectToRas = (): void => {
  AnalyticsTracker.Registration.RasLoginGov();
  // The scopes are also used in backend for fetching user info.
  const url = serverConfigStore.get().config.rasHost + '/auth/oauth/v2/authorize?client_id=' + serverConfigStore.get().config.rasClientId
      + '&prompt=login+consent&redirect_uri=' + buildRasRedirectUrl()
      + '&response_type=code&scope=openid+profile+email+ga4gh_passport_v1+federated_identities';
  window.open(url, '_blank');
};

// This needs to be a function, because we want it to evaluate at call time,
// not at compile time, to ensure that we make use of the server config store.
// This is important so that we can feature flag off registration tasks.
//
// Important: The completion criteria here needs to be kept synchronized with
// the server-side logic, else users can get stuck on the registration dashboard
// without a next step:
// https://github.com/all-of-us/workbench/blob/master/api/src/main/java/org/pmiops/workbench/db/dao/UserServiceImpl.java#L240-L272
//
// Needing to pass navigate in here is a bit odd but necessary to access the navigate function which
// can only be accessed through a hook/HOC from a component.
export const getRegistrationTasks = (navigate): RegistrationTask[] => serverConfigStore.get().config ? ([
  {
    key: 'twoFactorAuth',
    module: AccessModule.TWOFACTORAUTH,
    completionPropsKey: 'twoFactorAuthCompleted',
    title: 'Turn on Google 2-Step Verification',
    description: 'Add an extra layer of security to your account by providing your phone number' +
        'in addition to your password to verify your identity upon login.',
    buttonText: 'Get Started',
    completedText: 'Completed',
    completionTimestamp: (profile: Profile) => {
      return profile.twoFactorAuthCompletionTime || profile.twoFactorAuthBypassTime;
    },
    onClick: redirectToTwoFactorSetup
  }, {
    key: 'rasLoginGov',
    module: AccessModule.RASLINKLOGINGOV,
    completionPropsKey: 'rasLoginGovLinked',
    loadingPropsKey: 'rasLoginGovLoading',
    title: 'Connect Your Login.Gov Account',
    featureFlag: serverConfigStore.get().config.enableRasLoginGovLinking,
    description: 'Connect your Researcher Workbench account to your login.gov account. ',
    buttonText: 'Connect',
    completedText: 'Linked',
    completionTimestamp: (profile: Profile) => {
      return profile.rasLinkLoginGovCompletionTime || profile.rasLinkLoginGovBypassTime;
    },
    onClick: redirectToRas
  },
  {
    key: 'eraCommons',
    module: AccessModule.ERACOMMONS,
    completionPropsKey: 'eraCommonsLinked',
    loadingPropsKey: 'eraCommonsLoading',
    title: 'Connect Your eRA Commons Account',
    description: 'Connect your Researcher Workbench account to your eRA Commons account. ' +
        'There is no exchange of personal data in this step.',
    featureFlag: serverConfigStore.get().config.enableEraCommons,
    buttonText: 'Connect',
    completedText: 'Linked',
    completionTimestamp: (profile: Profile) => {
      return profile.eraCommonsCompletionTime || profile.eraCommonsBypassTime;
    },
    onClick: redirectToNiH
  }, {
    key: 'complianceTraining',
    module: AccessModule.COMPLIANCETRAINING,
    completionPropsKey: 'trainingCompleted',
    title: <span><AoU/> Responsible Conduct of Research Training</span>,
    description: <div>Complete ethics training courses to understand the privacy safeguards and the
      compliance requirements for using the <AoU/> dataset.</div>,
    buttonText: 'Complete training',
    featureFlag: serverConfigStore.get().config.enableComplianceTraining,
    completedText: 'Completed',
    completionTimestamp: (profile: Profile) => {
      return profile.complianceTrainingCompletionTime || profile.complianceTrainingBypassTime;
    },
    onClick: redirectToTraining
  }, {
    key: 'dataUserCodeOfConduct',
    module: AccessModule.DATAUSERCODEOFCONDUCT,
    completionPropsKey: 'dataUserCodeOfConductCompleted',
    title: 'Data User Code of Conduct',
    description: <span>Sign the Data User Code of Conduct consenting to the <AoU/> data use policy.</span>,
    buttonText: 'View & Sign',
    completedText: 'Signed',
    completionTimestamp: (profile: Profile) => {
      if (profile.dataUseAgreementBypassTime) {
        return profile.dataUseAgreementBypassTime;
      }
      // The DUCC completion time field tracks the most recent DUCC completion
      // timestamp, but doesn't specify whether that DUCC is currently active.
      const requiredDuccVersion = getLiveDUCCVersion();
      if (profile.dataUseAgreementSignedVersion === requiredDuccVersion) {
        return profile.dataUseAgreementCompletionTime;
      }
      return null;
    },
    onClick: () => {
      AnalyticsTracker.Registration.EnterDUCC();
      navigate(['data-code-of-conduct']);
    }
  }
]).filter(registrationTask => registrationTask.featureFlag === undefined
    || registrationTask.featureFlag) : (() => {
      throw new Error('Cannot load registration tasks before config loaded');
    })();

export const getRegistrationTasksMap = (navigate) => getRegistrationTasks(navigate).reduce((acc, curr) => {
  acc[curr.key] = curr;
  return acc;
}, {});

export const getRegistrationTask = (navigate, module: AccessModule): RegistrationTask => {
  return getRegistrationTasks(navigate).find(task => task.module === module);
};

export const wasReferredFromRenewal = (): boolean => queryParamsStore.getValue().renewal === '1';
export const MILLIS_PER_DAY = 24 * 60 * 60 * 1000;
export const NOTIFICATION_THRESHOLD_DAYS = 30;

// return the number of full days remaining to expiration in the soonest-to-expire module,
// but only if it is within the threshold.
// if it is not, or no expiration dates are present in the profile: return undefined.
export const maybeDaysRemaining = (profile: Profile): number | undefined => {
  const earliestExpiration: number = fp.flow(
    fp.get(['accessModules', 'modules']),
    fp.map<AccessModuleStatus, number>(m => m.expirationEpochMillis),
    // remove the undefined expirationEpochMillis
    fp.compact,
    fp.min)(profile);

  if (earliestExpiration) {
    // show the number of full remaining days, e.g. 30 if 30.7 remain or 0 if only a partial day remains
    const daysRemaining = Math.floor((earliestExpiration - Date.now()) / MILLIS_PER_DAY);
    if (daysRemaining <= NOTIFICATION_THRESHOLD_DAYS) {
      return daysRemaining;
    }
  }
};

// A hook to determine whether the current user is signed in and disabled.
// Returns undefined if the status is unknown.
export const useIsUserDisabled = () => {
  const {authLoaded, isSignedIn} = useStore(authStore);
  const [disabled, setDisabled] = useState<boolean|undefined>(undefined);
  useEffect(() => {
    if (!authLoaded) {
      return;
    }

    let mounted = true;
    if (!isSignedIn) {
      setDisabled(false);
    } else {
      (async() => {
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
    return () => {mounted = false;}
  }, [authLoaded, isSignedIn]);
  return disabled;
};

export const getAccessModuleStatusByName = (profile: Profile, moduleName: AccessModule): AccessModuleStatus => {
  return profile.accessModules.modules.find(a => a.moduleName === moduleName);
};

export const getAccessModuleCompletionTime = (accessModules: Array<AccessModuleStatus>, moduleName: AccessModule): number => {
  const module = accessModules.find(a => a.moduleName === moduleName);
  return module ? module.completionEpochMillis : null;
};

export const getAccessModuleBypassTime = (accessModules: Array<AccessModuleStatus>, moduleName: AccessModule): number => {
  const module = accessModules.find(a => a.moduleName === moduleName);
  return module ? module.bypassEpochMillis : null;
};

export const bypassAll = async(accessModules: AccessModule[], isBypassed: boolean) => {
  for (const module of accessModules) {
    await profileApi().unsafeSelfBypassAccessRequirement({
      moduleName: module,
      isBypassed: isBypassed
    });
  }
};

export const GetStartedButton = ({style = {marginLeft: '0.5rem'}}) => <Button
    style={style}
    onClick={() => {
      // After a registration status change, to be safe, we reload the application. This results in
      // rerendering of the homepage, but also reruns some application bootstrapping / caching which may
      // have been dependent on the user's registration status, e.g. CDR config information.
      location.replace('/');
    }}>Get Started</Button>;

export const isTierPresentInEnvironment = (accessTierShortName: string, cdrTiers: CdrVersionTiersResponse): boolean => {
  return !!getCdrVersionTier(accessTierShortName, cdrTiers);
};
