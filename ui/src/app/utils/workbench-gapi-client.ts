declare const gapi: any;

interface BillingAccountInfo {
  billingAccountName: string
}

export async function getBillingAccountInfo(workspaceNamespace: string) {
  return new Promise<BillingAccountInfo>((resolve) => {
    gapi.load('client', () => {
      gapi.client.load('cloudbilling', 'v1', () => {
        gapi.client.cloudbilling.projects.getBillingInfo({
          name: 'projects/' + workspaceNamespace
        }).then(response => resolve(JSON.parse(response.body)));
      });
    });
  });
}
