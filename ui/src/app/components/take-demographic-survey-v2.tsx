import * as React from 'react';
import { useLocation } from 'react-router-dom';

import { NotificationBanner } from 'app/components/notification-banner';
import { AoU } from 'app/components/text-wrappers';
import { reactStyles } from 'app/utils';
import { DEMOGRAPHIC_SURVEY_V2_PATH } from 'app/utils/constants';
import { isUserFromUSAOrSignedInBeforeNov } from 'app/utils/profile-utils';
import { profileStore, useStore } from 'app/utils/stores';

const styles = reactStyles({
  bannerButton: {
    width: '120px',
    alignSelf: 'center',
  },
});

export const TakeDemographicSurveyV2BannerMaybe = () => {
  const { profile } = useStore(profileStore);
  const location = useLocation();

  const notificationBannerText = (
    <span>
      Please take 5 minutes to complete the updated Researcher Demographic
      Survey. Your response will help <AoU /> grow our platform and community.
    </span>
  );

  return (
    isUserFromUSAOrSignedInBeforeNov(profile) &&
    !profile.demographicSurveyV2 && (
      <NotificationBanner
        dataTestId={'take-survey-notification'}
        text={notificationBannerText}
        useLocationLink={true}
        buttonStyle={styles.bannerButton}
        buttonText='Take Survey'
        buttonPath={DEMOGRAPHIC_SURVEY_V2_PATH}
        buttonDisabled={location.pathname === DEMOGRAPHIC_SURVEY_V2_PATH}
        bannerTextWidth='39rem'
      />
    )
  );
};
