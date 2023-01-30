import {
  getAccessToken,
  isTestAccessTokenActive,
} from 'app/utils/authentication';

import { LOCAL_STORAGE_KEY_TEST_ACCESS_TOKEN } from './cookies';

export interface GoogleBillingAccountInfo {
  billingAccountName: string;
  billingEnabled: boolean;
}

export async function getBillingAccountInfo(googleProject: string) {
  const accessToken = isTestAccessTokenActive()
    ? window.localStorage.getItem(LOCAL_STORAGE_KEY_TEST_ACCESS_TOKEN)
    : getAccessToken();
  const response = await fetch(
    `https://content-cloudbilling.googleapis.com/v1/projects/${googleProject}/billingInfo`,
    {
      headers: new Headers({
        Authorization: `Bearer ${accessToken}`,
      }),
    }
  );
  return response.json();
}
