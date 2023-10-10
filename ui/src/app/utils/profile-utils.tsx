import { Profile } from 'generated/fetch';

import { Country, INTL_USER_SIGN_IN_CHECK } from 'app/utils/constants';
import { authStore } from 'app/utils/stores';

export const getProfilePictureSrc = () => {
  return authStore.get().auth?.user?.profile.picture;
};

export const showDemographicSurvey = (country, date) => {
  return country === Country.US || INTL_USER_SIGN_IN_CHECK > date;
};

export const shouldShowDemographicSurvey = (profile: Profile) => {
  const userCountry = profile?.address?.country;
  const signIn = new Date(profile.firstSignInTime);
  return showDemographicSurvey(userCountry, signIn);
};
