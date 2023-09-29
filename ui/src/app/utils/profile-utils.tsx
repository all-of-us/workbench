import { Profile } from 'generated/fetch';

import { Country, INTL_USER_SIGN_IN_CHECK } from 'app/utils/constants';
import { authStore } from 'app/utils/stores';

export const getProfilePictureSrc = () => {
  return authStore.get().auth?.user?.profile.picture;
};

export const isUserFromUS = (profile: Profile) => {
  const userCountry = profile?.address?.country;
  const signIn = new Date(profile.firstSignInTime);
  const signed_in_after_nov = INTL_USER_SIGN_IN_CHECK <= signIn;
  return Country[userCountry] === Country.US || !signed_in_after_nov;
};
