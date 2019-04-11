import {Environment} from 'environments/environment-type';

export const environment: Environment = {
  displayTag: '',
  shouldShowDisplayTag: false,
  allOfUsApiUrl: 'https://api.workbench.researchallofus.org',
  clientId: '684273740878-d7i68in5d9hqr6n9mfvrdh53snekp79f.apps.googleusercontent.com',
  tcellappid: 'AoUProd-35j28',
  tcellapikey: 'AQEBBAEGP2gTM2pIdJAQOIeNrm8dcTM7E4FMSmaibbMUQxNU6qy6nLPOBK8QfSvPFSsX8PQ',
  leoApiUrl: 'https://notebooks.firecloud.org',
  publicApiUrl: 'https://public.api.researchallofus.org',
  publicUiUrl: 'https://databrowser.researchallofus.org',
  debug: false,
  gaId: 'UA-112406425-4',
  gaUserAgentDimension: 'dimension1',
  zendeskHelpCenterUrl: 'http://aousupporthelp.zendesk.com/hc',
  shibbolethUrl: 'https://shibboleth.dsde-prod.broadinstitute.org',
  trainingUrl: 'https://aou.nnlm.gov',
  // Use care when changing these flags in prod!
  //
  // See environment-type.ts for more details on transient flags, including
  // exit criteria and Jira ticket links.
  enableJupyterLab: false,
  enableDatasetBuilder: false,
  enableCBListSearch: false,
};
