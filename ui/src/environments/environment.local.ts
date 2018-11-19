import {testEnvironmentBase} from 'environments/test-env-base';

export const environment = {
  displayTag: 'Local->Local',
  allOfUsApiUrl: 'http://localhost:8081',
  clientId: testEnvironmentBase.clientId,
  tcellappid: "AoUNonProd-WZFW2",
  tcellapikey: "AQEBBAEkx4iE2KxNyI7Wx08EwU1ycTM7E4FMSmaibbMUQxNU6uQvuAJt7fyABAtFYSYfgEE",
  leoApiUrl: 'https://leonardo.dsde-dev.broadinstitute.org',
  publicApiUrl: 'http://localhost:8083',
  publicUiUrl: 'http://localhost:4201',
  debug: true,
  gaId: 'UA-112406425-5',
  enableTemporal: true
};
