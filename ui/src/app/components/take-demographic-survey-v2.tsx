import * as React from 'react';
import { useEffect, useState } from 'react';
import { useLocation } from 'react-router-dom';

import { reactStyles } from 'app/utils';
import {
  DEMOGRAPHIC_SURVEY_V2_NOTIFICATION_END_DATE,
  DEMOGRAPHIC_SURVEY_V2_PATH,
} from 'app/utils/constants';
import { profileStore, serverConfigStore, useStore } from 'app/utils/stores';

import { NotificationBanner } from './notification-banner';

const identifierIndex = ['days', 'hours', 'minutes', 'seconds'];

const styles = reactStyles({
  bannerText: {
    width: '880px',
    lineHeight: '1',
    marginBottom: '0.5rem',
    alignItems: 'top',
  },
  bannerBox: {
    width: '45rem',
    height: '3rem',
  },
  bannerButton: {
    width: '120px',
    alignSelf: 'center',
  },
});

export const TakeDemographicSurveyV2BannerMaybe = () => {
  const { profile } = useStore(profileStore);
  const location = useLocation();

  let notificationText = `The All of Us Research Program is dedicated to cultivating a diverse 
  research community and building an inclusive platform.`;

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
      // If the interval value is 0, skip it only if there are no intervals before existing one:
      // E.g If hours are 0 , skip if there are 0 days.
      // Or if there are 0 minute, skip if there are no hours and no days left
      // Do not skip 0 minutes if interval is 1 day 0 hours 0 minutes 3 seconds
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
    notificationText +=
      ` Please complete the updated Researcher
    Demographic Survey before ` +
      DEMOGRAPHIC_SURVEY_V2_NOTIFICATION_END_DATE +
      `(` +
      timeLeftDisplayStr +
      ` remaining). The survey will
    take approximately 2 minutes to complete. 
    Your answers to these questions will help us learn more about who is using the platform.`;
  }

  const featureFlag =
    serverConfigStore.get().config.enableUpdatedDemographicSurvey;

  return (
    featureFlag &&
    !profile.demographicSurveyV2 && (
      <NotificationBanner
        dataTestId={'take-survey-notification'}
        text={notificationText}
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
