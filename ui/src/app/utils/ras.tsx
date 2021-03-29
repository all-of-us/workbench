import {encodeURIComponentStrict} from "app/utils/navigation";

/** Build the RAS OAuth redirect URL. It should be AoU hostname/ras-callback. */
export const buildRasRedirectUrl = (): string => {
  return encodeURIComponentStrict(window.location.origin.toString() + '/ras-callback');
};

