declare const gapi: any;

interface GoogleBillingAccountInfo {
  billingAccountName: string;
  billingEnabled: boolean;
}

export async function getBillingAccountInfo(workspaceNamespace: string) {
  await ensureBillingScope()

  return new Promise<GoogleBillingAccountInfo>((resolve) => {
    gapi.load('client', () => {
      gapi.client.load('cloudbilling', 'v1', () => {
        gapi.client.cloudbilling.projects.getBillingInfo({
          name: 'projects/' + workspaceNamespace
        }).then(response => resolve(JSON.parse(response.body)));
      });
    });
  });
}

// The delay before continuing to avoid errors due to delays in applying the new scope grant
const BILLING_SCOPE_DELAY_MS = 2000;

const getAuthInstance = () => {
  return gapi.auth2.getAuthInstance()
}

export const hasBillingScope = () => {
  return getAuthInstance().currentUser.get().hasGrantedScopes('https://www.googleapis.com/auth/cloud-billing')
}

/*
 * Request Google Cloud Billing scope if necessary.
 *
 * NOTE: Requesting additional scopes may invoke a browser pop-up which the browser might block.
 * If you use ensureBillingScope during page load and the pop-up is blocked, a rejected promise will
 * be returned. In this case, you'll need to provide something for the user to deliberately click on
 * and retry ensureBillingScope in reaction to the click.
 */
export const ensureBillingScope = async () => {
  if (!hasBillingScope()) {
    const options = new gapi.auth2.SigninOptionsBuilder();
    options.setScope('https://www.googleapis.com/auth/cloud-billing');
    await getAuthInstance().currentUser.get().grant(options)
    // Wait 250ms before continuing to avoid errors due to delays in applying the new scope grant
    await delay(BILLING_SCOPE_DELAY_MS)
  }
}

export const delay = ms => {
  return new Promise(resolve => setTimeout(resolve, ms));
};
