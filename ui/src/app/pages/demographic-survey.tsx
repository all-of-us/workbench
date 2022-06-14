import * as React from 'react';
import { useEffect, useState } from 'react';

import DemographicSurveyV2 from 'app/components/demographic-survey-v2';
import { profileStore } from 'app/utils/stores';

export const DemographicSurvey = (props) => {
  const [profile, setProfile] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    setProfile(profileStore.get().profile);
    props.hideSpinner();
    setLoading(false);
  }, []);

  if (loading) {
    return <></>;
  }
  return profile ? (
    <DemographicSurveyV2 profile={profile} />
  ) : (
    <div style={{ marginTop: '1rem' }}>
      Unfortunately, your profile did not load. Please try again later.
    </div>
  );
};
