import * as React from 'react';
import { useState } from 'react';

import { NewUserSatisfactionSurveyModal } from 'app/components/new-user-satisfaction-survey-modal';
import { NotificationBanner } from 'app/components/notification-banner';
import { profileStore, useStore } from 'app/utils/stores';
import moment from 'moment';

export const NewUserSatisfactionSurveyBannerMaybe = () => {
  const { profile, reload } = useStore(profileStore);
  const [showModal, setShowModal] = useState(false);

  const eligibilityEnd = moment(
    profile.newUserSatisfactionSurveyEligibilityEndTime
  ).format('M/D/YY');

  return (
    <>
      {profile.newUserSatisfactionSurveyEligibility && (
        <NotificationBanner
          dataTestId='new-user-satisfaction-survey-notification'
          text={
            <span>
              Please take 2 minutes to rate your satisfaction with the{' '}
              <i>All of Us</i> Researcher Workbench. Please complete the survey
              by {eligibilityEnd}.
            </span>
          }
          buttonText='Take Survey'
          buttonOnClick={() => setShowModal(true)}
          bannerTextWidth='20rem'
          buttonAriaLabel='take satisfaction survey'
        />
      )}
      {showModal && (
        <NewUserSatisfactionSurveyModal
          onCancel={() => setShowModal(false)}
          onSubmitSuccess={() => {
            setShowModal(false);
            reload();
          }}
        />
      )}
    </>
  );
};
