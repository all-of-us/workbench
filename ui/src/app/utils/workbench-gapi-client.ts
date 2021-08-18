declare const gapi: any;

interface GoogleBillingAccountInfo {
  billingAccountName: string;
  billingEnabled: boolean;
}

export async function getBillingAccountInfo(googleProject: string) {
  return new Promise<GoogleBillingAccountInfo>((resolve) => {
    gapi.load('client', () => {
      gapi.client.load('cloudbilling', 'v1', () => {
        gapi.client.cloudbilling.projects.getBillingInfo({
          name: 'projects/' + googleProject
        }).then(response => resolve(JSON.parse(response.body)));
      });
    });
  });
}
