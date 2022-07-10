import * as React from 'react';
import { useEffect, useState } from 'react';
import { useLocation } from 'react-router-dom';

import {
  DEMOGRAPHIC_SURVEY_V2_NOTIFICATION_END_DATE,
  DEMOGRAPHIC_SURVEY_V2_PATH,
  DEMOGRAPHIC_SURVEY_V2_PATH_WITH_PARAM,
} from 'app/utils/constants';
import { profileStore, serverConfigStore, useStore } from 'app/utils/stores';

import { NotificationBanner } from './notification-banner';

const identifierIndex = ['days', 'hours', 'minutes', 'seconds'];

export const TakeDemographicSurveyV2BannerMaybe = () => {
  const { profile } = useStore(profileStore);
  const location = useLocation();

  let notificationText = `We have a new version of demographic survey question to better understand the diversity of the 
     research community. Please complete the survey by `;

  const calculateTimeLeft = () => {
    const difference =
      +new Date(DEMOGRAPHIC_SURVEY_V2_NOTIFICATION_END_DATE) - +new Date();
    let timeLeft = {};

    // Update the text as part of nudge story part 2
    if (difference <= 0) {
      notificationText = 'Please fill the demographic survey now!! ';
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

  // Loop through the intervals and display time
  // if its 1 day and just 3 seconds show: 1 day 0 hours 0 minutes and 3 seconds
  // However if its just 1 minute and  4 seconds show: 1 minutes and 4 seconds
  Object.keys(timeLeft).forEach((interval, index) => {
    if (!timeLeft[interval]) {
      let intervalIndex = index - 1;
      let removeInterval = true;
      while (intervalIndex >= 0) {
        if (timeLeft[identifierIndex[intervalIndex]]) {
          removeInterval = false;
          break;
        }
        intervalIndex = intervalIndex - 1;
      }
      if (removeInterval) {
        return;
      }
    }

    timeLeftDisplayStr =
      timeLeftDisplayStr + '  ' + timeLeft[interval] + ' ' + interval;
  });

  // Users will continue to see the banner, until they have taken the survey
  // If the time is up, update the text message rather than hide the banner
  if (timeLeftDisplayStr === '') {
    notificationText += ' This will only take few minutes';
  } else {
    notificationText =
      notificationText +
      DEMOGRAPHIC_SURVEY_V2_NOTIFICATION_END_DATE +
      `, this will only take 2 minutes. 
     Time Left to complete the survey : ` +
      timeLeftDisplayStr;
  }

  const featureFlag =
    serverConfigStore.get().config.enableUpdatedDemographicSurvey;

  return (
    featureFlag &&
    !profile.demographicSurveyV2 && (
      <NotificationBanner
        dataTestId={'take-survey-notification'}
        text={notificationText}
        boxStyle={{ width: '1980px' }}
        textStyle={{ width: '1000px' }}
        buttonStyle={{ width: '180px' }}
        buttonText='Take the Survey'
        buttonPath={DEMOGRAPHIC_SURVEY_V2_PATH_WITH_PARAM}
        buttonDisabled={location.pathname === DEMOGRAPHIC_SURVEY_V2_PATH}
      />
    )
  );
};
