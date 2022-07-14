import * as React from 'react';
import { useEffect, useState } from 'react';
import { useLocation } from 'react-router';
import * as fp from 'lodash/fp';

import { Button, LinkLocationState } from 'app/components/buttons';
import { DemographicSurvey as DemographicSurveyComponent } from 'app/components/demographic-survey-v2';
import { TooltipTrigger } from 'app/components/popups';
import { WithSpinnerOverlayProps } from 'app/components/with-spinner-overlay';
import { profileApi } from 'app/services/swagger-fetch-clients';
import {
  DEMOGRAPHIC_SURVEY_SESSION_KEY,
  DEMOGRAPHIC_SURVEY_V2_PATH,
} from 'app/utils/constants';
import { useNavigation } from 'app/utils/navigation';
import { profileStore } from 'app/utils/stores';

export const DemographicSurvey = (props: WithSpinnerOverlayProps) => {
  const [errors, setErrors] = useState(null);
  const [changed, setChanged] = useState(false);
  const [initialSurvey, setInitialSurvey] = useState(null);
  const [profile, setProfile] = useState(null);
  const [loading, setLoading] = useState(true);
  const location = useLocation();
  const [submitting, setSubmitting] = useState(false);
  const [, navigateByUrl] = useNavigation();

  const { hideSpinner, showSpinner } = props;

  useEffect(() => {
    const profileStoreProfile = profileStore.get().profile;
    setInitialSurvey(profileStoreProfile.demographicSurveyV2);
    const currentSurvey = profileStoreProfile.demographicSurveyV2 || {
      education: null,
      ethnicityAiAnOtherText: null,
      ethnicityAsianOtherText: null,
      ethnicCategories: [],
      ethnicityOtherText: null,
      disabilityConcentrating: null,
      disabilityDressing: null,
      disabilityErrands: null,
      disabilityHearing: null,
      disabilityOtherText: null,
      disabilitySeeing: null,
      disabilityWalking: null,
      disadvantaged: null,
      genderIdentities: [],
      genderOtherText: null,
      orientationOtherText: null,
      sexAtBirth: null,
      sexAtBirthOtherText: null,
      sexualOrientations: [],
      yearOfBirth: null,
      yearOfBirthPreferNot: false,
    };
    setProfile({ ...profileStoreProfile, demographicSurveyV2: currentSurvey });
    hideSpinner();
    setLoading(false);
  }, []);

  useEffect(() => {
    setChanged(!fp.isEqual(initialSurvey, profile?.demographicSurveyV2));
  }, [profile]);

  // Users, who still have not filled survey v2 after 30days, will be shown the survey page after every sign in
  // In such scenarios the location pathname will be different from DEMOGRAPHIC_SURVEY_V2_PATH
  // In All other scenarios, i.e From Demographic Survey Banner or Profile page, the location page name will be
  // DEMOGRAPHIC_SURVEY_V2_PATH
  const redirectedFromSignIn = location.pathname !== DEMOGRAPHIC_SURVEY_V2_PATH;

  const handleSubmit = async () => {
    setSubmitting(true);
    showSpinner();
    await profileApi().updateProfile(profile);
    await profileStore.get().reload();
    hideSpinner();
    setSubmitting(false);

    // This logic should be cleaned up sometime in future, when all existing users have submitted
    // the latest demographic survey version
    if (redirectedFromSignIn) {
      navigateByUrl(location.pathname);
      return;
    } else {
      const prevLocationState = location.state as LinkLocationState;
      if (prevLocationState?.pathname) {
        navigateByUrl(prevLocationState.pathname);
      } else {
        navigateByUrl('profile');
      }
    }
  };

  const handleMaybeLater = async () => {
    sessionStorage.setItem(
      DEMOGRAPHIC_SURVEY_SESSION_KEY,
      new Date().toDateString()
    );
    navigateByUrl(location.pathname);
  };

  const handleUpdate = (updatedProfile) => {
    setProfile((prevState) => {
      return updatedProfile(prevState);
    });
  };

  if (loading) {
    return null;
  }

  return profile ? (
    <div style={{ marginTop: '1rem', paddingLeft: '1rem', width: '32rem' }}>
      <DemographicSurveyComponent
        profile={profile}
        onUpdate={(prop, value) =>
          handleUpdate(fp.set(['demographicSurveyV2', prop], value))
        }
        onError={setErrors}
      />
      <TooltipTrigger
        content={
          (errors || !changed) && (
            <>
              <div>Please review the following:</div>
              <ul>
                {errors && (
                  <>
                    {Object.keys(errors).map((key) => (
                      <li key={errors[key][0]}>{errors[key][0]}</li>
                    ))}
                    <li>
                      You may select "Prefer not to answer" for each unfilled
                      item listed above to continue
                    </li>
                  </>
                )}
                {!changed && (
                  <li>
                    Your survey has not changed since your last submission.
                  </li>
                )}
              </ul>
            </>
          )
        }
      >
        <Button
          disabled={!!errors || !changed || submitting}
          type='primary'
          onClick={handleSubmit}
          style={{ margin: '1rem 0rem' }}
        >
          Submit
        </Button>
      </TooltipTrigger>
      {redirectedFromSignIn && (
        <Button
          type='secondary'
          onClick={handleMaybeLater}
          style={{ marginLeft: '2rem' }}
        >
          Maybe Later
        </Button>
      )}
    </div>
  ) : (
    <div style={{ marginTop: '1rem' }}>Profile failed to load.</div>
  );
};
