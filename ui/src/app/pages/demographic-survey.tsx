import * as React from 'react';
import { useEffect, useState } from 'react';
import * as fp from 'lodash/fp';

import { Profile } from 'generated/fetch';

import { Button } from 'app/components/buttons';
import DemographicSurveyV2 from 'app/components/demographic-survey-v2';
import { TooltipTrigger } from 'app/components/popups';
import { profileApi } from 'app/services/swagger-fetch-clients';
import { useNavigation } from 'app/utils/navigation';
import { profileStore } from 'app/utils/stores';

export const DemographicSurvey = (props) => {
  const [errors, setErrors] = useState(null);
  const [changed, setChanged] = useState(false);
  const [initialSurvey, setInitialSurvey] = useState(null);
  const [profile, setProfile] = useState(null);
  const [loading, setLoading] = useState(true);
  const [, navigateByUrl] = useNavigation();

  useEffect(() => {
    const profileStoreProfile = profileStore.get().profile;
    setInitialSurvey(profileStoreProfile.demographicSurveyV2);
    setProfile(profileStoreProfile);
    props.hideSpinner();
    setLoading(false);
  }, []);

  const handleSubmit = async () => {
    props.showSpinner();
    await profileApi().updateProfile(profile);
    props.hideSpinner();
    const returnAddress = props.returnAddress ?? '/profile';
    navigateByUrl(returnAddress);
  };

  const handleUpdate = (updatedProfile: Profile, updatedErrors: any) => {
    setProfile(updatedProfile);
    setErrors(updatedErrors);
    setChanged(!fp.isEqual(initialSurvey, updatedProfile.demographicSurveyV2));
  };

  if (loading) {
    return <></>;
  }

  return profile ? (
    <div style={{ marginTop: '1rem', paddingLeft: '1rem', width: '32rem' }}>
      <DemographicSurveyV2 profile={profile} onUpdate={handleUpdate} />
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
