import { getAccessToken } from 'app/utils/authentication';

export interface GoogleBillingAccountInfo {
  billingAccountName: string;
  billingEnabled: boolean;
}

export async function getBillingAccountInfo(googleProject: string) {
  const response = await fetch(
    `https://content-cloudbilling.googleapis.com/v1/projects/${googleProject}/billingInfo`,
    {
      headers: new Headers({
        Authorization: `Bearer ${getAccessToken()}`,
      }),
    }
  );
  return response.json();
}
