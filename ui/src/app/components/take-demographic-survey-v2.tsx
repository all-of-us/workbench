import * as React from 'react';
import { useLocation } from 'react-router-dom';

import { reactStyles } from 'app/utils';
import {
  DEMOGRAPHIC_SURVEY_V2_NOTIFICATION_END_DATE,
  DEMOGRAPHIC_SURVEY_V2_PATH,
} from 'app/utils/constants';
import { profileStore, serverConfigStore, useStore } from 'app/utils/stores';

import { NotificationBanner } from './notification-banner';
import { AoU } from './text-wrappers';

const styles = reactStyles({
  bannerText: {
    width: '26rem',
  },
  bannerBox: {
    width: '36rem',
  },
  bannerButton: {
    width: '120px',
    alignSelf: 'center',
  },
});

export const TakeDemographicSurveyV2BannerMaybe = () => {
  const { profile } = useStore(profileStore);
  const location = useLocation();

  // Delete this const once we reach the deadline DEMOGRAPHIC_SURVEY_V2_NOTIFICATION_END_DATE
  const notificationText = (
    <span>
      Please take 5 minutes to complete the updated Researcher Demographic
      Survey before {DEMOGRAPHIC_SURVEY_V2_NOTIFICATION_END_DATE}. Your response
      will help <AoU /> grow our platform and community.
    </span>
  );

  const notificationTextAfterDeadline = (
    <span>
      Please take 5 minutes to complete the updated Researcher Demographic
      Survey. Your response will help <AoU /> grow our platform and community.
    </span>
  );

  const deadlineReached =
    +new Date(DEMOGRAPHIC_SURVEY_V2_NOTIFICATION_END_DATE) < +new Date();

  const featureFlag =
    serverConfigStore.get().config.enableUpdatedDemographicSurvey;

  return (
    featureFlag &&
    !profile.demographicSurveyV2 && (
      <NotificationBanner
        dataTestId={'take-survey-notification'}
        text={
          deadlineReached ? notificationTextAfterDeadline : notificationText
        }
        boxStyle={styles.bannerBox}
        textStyle={styles.bannerText}
        useLocationLink={true}
        buttonStyle={styles.bannerButton}
        buttonText='Take Survey'
        buttonPath={DEMOGRAPHIC_SURVEY_V2_PATH}
        buttonDisabled={location.pathname === DEMOGRAPHIC_SURVEY_V2_PATH}
      />
    )
  );
};
