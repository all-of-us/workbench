import { authStore } from 'app/utils/stores';

export const getProfilePictureSrc = () => {
  return authStore.get().auth?.user?.profile.picture;
};
