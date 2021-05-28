import {AnalyticsTracker} from 'app/utils/analytics';
import {profileApi} from 'app/services/swagger-fetch-clients';
import {environment} from 'environments/environment';
import {withErrorModal} from 'app/components/modals';
import {queryParamsStore} from 'app/utils/navigation';
import {profileStore} from 'app/utils/stores';


export async function redirectToTraining() {
  AnalyticsTracker.Registration.EthicsTraining();
  await profileApi().updatePageVisits({page: 'moodle'});
  window.open(environment.trainingUrl + '/static/data-researcher.html?saml=on', '_blank');
}

export const wasRefferredFromRenewal = (): boolean => queryParamsStore.getValue().renewal === "1"

export const reloadProfile = withErrorModal({
    title: 'Could Not Load Profile', 
    message: 'Profile could not be reloaded. Please refresh the page to get your updated profile'
  }, 
  profileStore.get().reload
)