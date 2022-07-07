import * as React from 'react';
import { useEffect, useState } from 'react';

import { profileStore, serverConfigStore, useStore } from 'app/utils/stores';

import { NotificationBanner } from './notification-banner';

export const TakeDemographicSurveyV2BannerMaybe = () => {
  const { profile } = useStore(profileStore);
  const [showBanner, setShowBanner] = useState(true);
  const demographicSurveyV2Path = '/demographic-survey';

  const surveyEndDate = '7/26/2022';

  const calculateTimeLeft = () => {
    const difference = +new Date(surveyEndDate) - +new Date();
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
    timeLeftDisplayStr = timeLeftDisplayStr + '  ' + timeLeft[interval] + ' ' + interval;
  });

  const notificationText =
    `We have a new version of demographic survey question to better understand the diversity of the 
     research community. Please complete the survey by `+ surveyEndDate +`, this will only take 2 minutes. 
     Time Left to complete the survey : ` + timeLeftDisplayStr;

  const demographicV2Submitted =
    serverConfigStore.get().config.enableUpdatedDemographicSurvey &&
    !!profile.demographicSurveyV2;

  return (
    !demographicV2Submitted &&
    showBanner && (
      <NotificationBanner
        dataTestId={'take-survey-notification'}
        text={notificationText}
        boxStyle={{ width: '1980px' }}
        textStyle={{ width: '1000px' }}
        buttonStyle={{ width: '140px' }}
        buttonText='Take the Survey'
        buttonPath={demographicSurveyV2Path}
        buttonDisabled={false}
      />
    )
  );
};
