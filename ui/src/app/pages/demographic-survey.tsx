import * as React from 'react';
import { useEffect, useState } from 'react';

import { Profile } from 'generated/fetch';

import { Button } from 'app/components/buttons';
import DemographicSurveyV2 from 'app/components/demographic-survey-v2';
import { TooltipTrigger } from 'app/components/popups';
import { profileApi } from 'app/services/swagger-fetch-clients';
import { profileStore } from 'app/utils/stores';

export const DemographicSurvey = (props) => {
  const [errors, setErrors] = useState(null);
  const [profile, setProfile] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    setProfile(profileStore.get().profile);
    props.hideSpinner();
    setLoading(false);
  }, []);

  const handleSubmit = async () => {
    props.showSpinner();
    await profileApi().updateProfile(profile);
    props.hideSpinner();
  };

  const handleUpdate = (updatedProfile: Profile, updatedErrors: any) => {
    setProfile(updatedProfile);
    setErrors(updatedErrors);
  };

  if (loading) {
    return <></>;
  }

  return profile ? (
    <>
      <DemographicSurveyV2 profile={profile} onUpdate={handleUpdate} />
      <TooltipTrigger
        content={
          errors && (
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
              </ul>
            </>
          )
        }
      >
        <Button
          disabled={!!errors}
          type='primary'
          data-test-id={'submit-button'}
          onClick={handleSubmit}
          style={{ margin: '1rem 0rem' }}
        >
          Submit
        </Button>
      </TooltipTrigger>
    </>
  ) : (
    <div style={{ marginTop: '1rem' }}>
      Unfortunately, your profile did not load. Please try again later.
    </div>
  );
};
