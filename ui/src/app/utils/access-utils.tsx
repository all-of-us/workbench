import * as fp from 'lodash/fp';
import * as React from 'react';
import {Redirect} from "react-router-dom";

import {Button} from 'app/components/buttons';
import {AoU} from 'app/components/text-wrappers';
import {profileApi} from 'app/services/swagger-fetch-clients';
import {AnalyticsTracker} from 'app/utils/analytics';
import {convertAPIError} from 'app/utils/errors';
import {encodeURIComponentStrict} from 'app/utils/navigation';
import {authStore, profileStore, serverConfigStore, useStore} from 'app/utils/stores';
import {environment} from 'environments/environment';
import {AccessModule, AccessModuleStatus, ErrorCode, Profile, UserTierEligibility} from 'generated/fetch';
import {parseQueryParams} from "app/components/app-router";
import {cond, daysFromNow, displayDateWithoutHours} from "./index";
import {AccessTierShortNames} from 'app/utils/access-tiers';

const {useState, useEffect} = React;

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

  const url = serverConfigStore.get().config.rasLogoutUrl + serverConfigStore.get().config.shibbolethUiBaseUrl + '/login?return-url=' +
    encodeURIComponent(
      window.location.origin.toString() + NIH_CALLBACK_PATH + '?token={token}');
  window.open(url, '_blank');
};

export const RAS_CALLBACK_PATH = '/ras-callback';

/** Build the RAS OAuth redirect URL. It should be AoU hostname/ras-callback. */
export const buildRasRedirectUrl = (): string => {
  return encodeURIComponentStrict(window.location.origin.toString() + RAS_CALLBACK_PATH);
};

export const redirectToRas = (openInNewTab: boolean = true): void => {
  AnalyticsTracker.Registration.RasLoginGov();
  // The scopes are also used in backend for fetching user info.
  const url = serverConfigStore.get().config.rasHost + '/auth/oauth/v2/authorize?client_id=' + serverConfigStore.get().config.rasClientId
      + '&prompt=login+consent&redirect_uri=' + buildRasRedirectUrl()
      + '&response_type=code&scope=openid+profile+email+ga4gh_passport_v1+federated_identities';

  openInNewTab ? window.open(url, '_blank') : <Redirect to={url}/>;
};

export const wasReferredFromRenewal = (queryParams): boolean => {
  const renewal = parseQueryParams(queryParams).get('renewal');
  return renewal === '1';
};
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

// the modules subject to Annual Access Renewal (AAR), in the order shown on the AAR page.
export const accessRenewalTitles = new Map<AccessModule, () => JSX.Element | string>([
  [AccessModule.PROFILECONFIRMATION, () => 'Update your profile'],
  [AccessModule.PUBLICATIONCONFIRMATION,
    () => 'Report any publications or presentations based on your research using the Researcher Workbench'],
  [AccessModule.COMPLIANCETRAINING, () => <div><AoU/> Responsible Conduct of Research Training</div>],
  [AccessModule.DATAUSERCODEOFCONDUCT, () => 'Sign Data User Code of Conduct'],
]) as Map<AccessModule, () => JSX.Element>;

export const isExpiring = (expiration: number): boolean => daysFromNow(expiration) <= serverConfigStore.get().config.accessRenewalLookback;

const withInvalidDateHandling = date => {
  if (!date) {
    return 'Unavailable';
  } else {
    return displayDateWithoutHours(date);
  }
};

export const computeDisplayDates = ({completionEpochMillis, expirationEpochMillis, bypassEpochMillis}: AccessModuleStatus) => {
  const userCompletedModule = !!completionEpochMillis;
  const userBypassedModule = !!bypassEpochMillis;
  const lastConfirmedDate = withInvalidDateHandling(completionEpochMillis);
  const nextReviewDate = withInvalidDateHandling(expirationEpochMillis);
  const bypassDate = withInvalidDateHandling(bypassEpochMillis);

  return cond(
      // User has bypassed module
      [userBypassedModule, () => ({lastConfirmedDate: `${bypassDate}`, nextReviewDate: 'Unavailable (bypassed)'})],
      // User never completed training
      [!userCompletedModule && !userBypassedModule, () =>
          ({lastConfirmedDate: 'Unavailable (not completed)', nextReviewDate: 'Unavailable (not completed)'})],
      // User completed training, but is in the lookback window
      [userCompletedModule && isExpiring(expirationEpochMillis), () => {
        const daysRemaining = daysFromNow(expirationEpochMillis);
        const daysRemainingDisplay = daysRemaining >= 0 ? `(${daysRemaining} day${daysRemaining !== 1 ? 's' : ''})` : '(expired)';
        return {
          lastConfirmedDate,
          nextReviewDate: `${nextReviewDate} ${daysRemainingDisplay}`
        };
      }],
      // User completed training and is up to date
      [userCompletedModule && !isExpiring(expirationEpochMillis), () => {
        const daysRemaining = daysFromNow(expirationEpochMillis);
        return {lastConfirmedDate, nextReviewDate: `${nextReviewDate} (${daysRemaining} day${daysRemaining !== 1 ? 's' : ''})`};
      }]
  );
};

// return true if user is egligible for registered tier.
// A user loses tier eligiblity when they are removed from institution tier requirement
export const eligibleForRegisteredForTier = (tierEligiblities: Array<UserTierEligibility>): boolean => {
  const rtEligiblity = tierEligiblities.find(t => t.accessTierShortName === AccessTierShortNames.Registered)
  return !!rtEligiblity && rtEligiblity.eligible
};
