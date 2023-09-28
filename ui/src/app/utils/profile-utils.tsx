import { Profile } from 'generated/fetch';

import { Country } from 'app/utils/constants';
import { authStore } from 'app/utils/stores';

export const getProfilePictureSrc = () => {
  return authStore.get().auth?.user?.profile.picture;
};

// TODO: ADD DATE CHECK
export const isUserFromUS = (profile: Profile) => {
  const userCountry = profile?.address?.country;
  return Country[userCountry] === Country.US;
};
