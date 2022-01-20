declare const gapi: any;

export const getProfilePictureSrc = () => {
  return gapi?.auth2
    ? gapi.auth2
        .getAuthInstance()
        .currentUser.get()
        .getBasicProfile()
        .getImageUrl()
    : null;
};
