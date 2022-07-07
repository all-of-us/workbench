import * as React from 'react';
import { useEffect, useState } from 'react';

import {
  DEMOGRAPHIC_SURVEY_V2_NOTIFICATION_END_DATE,
  DEMOGRAPHIC_SURVEY_V2_PATH,
} from 'app/utils/constants';
import { profileStore, serverConfigStore, useStore } from 'app/utils/stores';

import { NotificationBanner } from './notification-banner';

export const TakeDemographicSurveyV2BannerMaybe = () => {
  const { profile } = useStore(profileStore);
  const [showBanner, setShowBanner] = useState(true);

  const calculateTimeLeft = () => {
    const difference =
      +new Date(DEMOGRAPHIC_SURVEY_V2_NOTIFICATION_END_DATE) - +new Date();
    let timeLeft = {};

    if (showBanner && difference <= 0) {
      setShowBanner(false);
    }
    if (difference > 0) {
      timeLeft = {
        days: Math.floor(difference / (1000 * 60 * 60 * 24)),
        hours: Math.floor((difference / (1000 * 60 * 60)) % 24),
        minutes: Math.floor((difference / 1000 / 60) % 60),
        seconds: Math.floor((difference / 1000) % 60),
      };
    }

    return timeLeft;
  };

  const [timeLeft, setTimeLeft] = useState(calculateTimeLeft());

  useEffect(() => {
    const timer = setTimeout(() => {
      setTimeLeft(calculateTimeLeft());
    }, 1000);
    return () => clearTimeout(timer);
  });

  let timeLeftDisplayStr = '';

  // Loop through the keys: days, hours, minutes and seconds and append them
  Object.keys(timeLeft).forEach((interval) => {
    if (!timeLeft[interval]) {
      return;
    }
    timeLeftDisplayStr =
      timeLeftDisplayStr + '  ' + timeLeft[interval] + ' ' + interval;
  });

  const notificationText =
    `We have a new version of demographic survey question to better understand the diversity of the 
     research community. Please complete the survey by ` +
    DEMOGRAPHIC_SURVEY_V2_NOTIFICATION_END_DATE +
    `, this will only take 2 minutes. 
     Time Left to complete the survey : ` +
    timeLeftDisplayStr;

  const showTakeDemographicV2Banner =
    !!profile.demographicSurveyV2 && showBanner;

  return (
    serverConfigStore.get().config.enableUpdatedDemographicSurvey &&
    showTakeDemographicV2Banner && (
      <NotificationBanner
        dataTestId={'take-survey-notification'}
        text={notificationText}
        boxStyle={{ width: '1980px' }}
        textStyle={{ width: '1000px' }}
        buttonStyle={{ width: '140px' }}
        buttonText='Take the Survey'
        buttonPath={DEMOGRAPHIC_SURVEY_V2_PATH}
        buttonDisabled={false}
      />
    )
  );
};
