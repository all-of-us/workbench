import { isTestAccessTokenActive } from 'app/utils/authentication';
import {LOCAL_STORAGE_KEY_TEST_ACCESS_TOKEN} from './cookies';

declare const gapi: any;

interface GoogleBillingAccountInfo {
  billingAccountName: string;
  billingEnabled: boolean;
}

export async function getBillingAccountInfo(googleProject: string) {
  return new Promise<GoogleBillingAccountInfo>((resolve) => {
    gapi.load('client', () => {
      if (isTestAccessTokenActive()) {
        gapi.client.load('cloudbilling', 'v1', () => {
          gapi.client.setToken({
            access_token: window.localStorage.getItem(LOCAL_STORAGE_KEY_TEST_ACCESS_TOKEN)
          });
          gapi.client.cloudbilling.projects.getBillingInfo({
            name: 'projects/' + googleProject
          }).then(response => resolve(JSON.parse(response.body)));
        });
      } else {
        gapi.client.load('cloudbilling', 'v1', () => {
          gapi.client.cloudbilling.projects.getBillingInfo({
            name: 'projects/' + googleProject
          }).then(response => resolve(JSON.parse(response.body)));
        });
      }
    });
  });
}
