import {AnalyticsTracker} from 'app/utils/analytics';
import {profileApi} from 'app/services/swagger-fetch-clients';
import {environment} from 'environments/environment';

export async function redirectToTraining() {
  AnalyticsTracker.Registration.EthicsTraining();
  await profileApi().updatePageVisits({page: 'moodle'});
  window.open(environment.trainingUrl + '/static/data-researcher.html?saml=on', '_blank');
}