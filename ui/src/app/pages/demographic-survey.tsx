import * as React from 'react';
import { useEffect, useState } from 'react';
import * as fp from 'lodash/fp';

import { Button } from 'app/components/buttons';
import DemographicSurveyV2 from 'app/components/demographic-survey-v2';
import { TooltipTrigger } from 'app/components/popups';
import { WithSpinnerOverlayProps } from 'app/components/with-spinner-overlay';
import { profileApi } from 'app/services/swagger-fetch-clients';
import { useNavigation } from 'app/utils/navigation';
import { profileStore } from 'app/utils/stores';

interface DemographicSurveyProps extends WithSpinnerOverlayProps {
  returnAddress: string;
}

export const DemographicSurvey = (props: DemographicSurveyProps) => {
  const [errors, setErrors] = useState(null);
  const [changed, setChanged] = useState(false);
  const [initialSurvey, setInitialSurvey] = useState(null);
  const [profile, setProfile] = useState(null);
  const [loading, setLoading] = useState(true);
  const [, navigateByUrl] = useNavigation();

  const { hideSpinner, showSpinner, returnAddress } = props;

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

  const handleSubmit = async () => {
    showSpinner();
    await profileApi().updateProfile(profile);
    hideSpinner();
    const returnAddressAdjusted = returnAddress ?? '/profile';
    navigateByUrl(returnAddressAdjusted);
  };

  const handleUpdate = (updatedProfile) => {
    setProfile((prevState) => {
      return updatedProfile(prevState);
    });
  };

  if (loading) {
    return <></>;
  }

  return profile ? (
    <div style={{ marginTop: '1rem', paddingLeft: '1rem', width: '32rem' }}>
      <DemographicSurveyV2
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
          disabled={!!errors || !changed}
          type='primary'
          data-test-id={'submit-button'}
          onClick={handleSubmit}
          style={{ margin: '1rem 0rem' }}
        >
          Submit
        </Button>
      </TooltipTrigger>
    </div>
  ) : (
    <div style={{ marginTop: '1rem' }}>
      Unfortunately, your profile did not load. Please try again later.
    </div>
  );
};
