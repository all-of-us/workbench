import {profileApi} from 'app/services/swagger-fetch-clients';
import {AnalyticsTracker} from 'app/utils/analytics';
import {queryParamsStore} from 'app/utils/navigation';
import {environment} from 'environments/environment';
import {Profile, RenewableAccessModuleStatus} from 'generated/fetch';
import * as fp from 'lodash/fp';

export async function redirectToTraining() {
  AnalyticsTracker.Registration.EthicsTraining();
  await profileApi().updatePageVisits({page: 'moodle'});
  window.open(environment.trainingUrl + '/static/data-researcher.html?saml=on', '_blank');
}

export const wasRefferredFromRenewal = (): boolean => queryParamsStore.getValue().renewal === '1';
export const MILLIS_PER_DAY = 24 * 60 * 60 * 1000;
export const NOTIFICATION_THRESHOLD_DAYS = 30;

// return the number of full days remaining to expiration in the soonest-to-expire module,
// but only if it is within the threshold.
// if it is not, or no expiration dates are present in the profile: return undefined.
export const maybeDaysRemaining = (profile: Profile): number | undefined => {
  const earliestExpiration: number = fp.flow(
    fp.get(['renewableAccessModules', 'modules']),
    fp.map<RenewableAccessModuleStatus, number>(m => m.expirationEpochMillis),
    // remove the undefined expirationEpochMillis
    fp.compact,
    fp.min)(profile);

  if (earliestExpiration) {
    const daysRemaining = (earliestExpiration - Date.now()) / MILLIS_PER_DAY;
    if (daysRemaining < NOTIFICATION_THRESHOLD_DAYS) {
      // note that we will show 0 days remaining if the expiration is later today
      return Math.floor(daysRemaining);
    }
  }
};
