declare const gapi: any;

export async function getBillingAccountName(workspaceNamespace: string) {
  return new Promise((resolve) => {
    gapi.load('client', () => {
      gapi.client.load('cloudbilling', 'v1', () => {
        gapi.client.cloudbilling.projects.getBillingInfo({
          name: 'projects/' + workspaceNamespace
        }).then(response => resolve(JSON.parse(response.body).billingAccountName));
      });
    });
  });
}
