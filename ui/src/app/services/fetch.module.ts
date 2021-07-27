import { NgModule } from '@angular/core';
import {bindApiClients as notebooksBindApiClients} from 'app/services/notebooks-swagger-fetch-clients';
import {SignInService} from 'app/services/sign-in.service';
import {bindApiClients, getApiBaseUrl} from 'app/services/swagger-fetch-clients';

import {
  Configuration,
} from 'generated/fetch';

import {
  Configuration as LeoConfiguration
} from 'notebooks-generated/fetch';

import {environment} from 'environments/environment';


// TODO angular2react - I think these can be removed
// "Configuration" means Swagger API Client configuration.
export function getConfiguration(signInService: SignInService) {
  // return new Configuration({
  //   basePath: getApiBaseUrl(),
  //   accessToken: () => signInService.currentAccessToken
  // });
}

export function getLeoConfiguration(signInService: SignInService) {
  // return new LeoConfiguration({
  //   basePath: environment.leoApiUrl,
  //   accessToken: () => signInService.currentAccessToken
  // });
}

/**
 * This module requires a FETCH_API_REF and FetchConfiguration instance to be
 * provided. Unfortunately typescript-fetch does not provide this module by
 * default, so a new entry will need to be added below for each new API service
 * added to the Swagger interfaces.
 */
@NgModule({
  imports:      [],
  declarations: [],
  exports:      [],
  providers: [{
    provide: Configuration,
    deps: [SignInService],
    useFactory: getConfiguration
  }, {
    provide: LeoConfiguration,
    deps: [SignInService],
    useFactory: getLeoConfiguration
  }]
})
export class FetchModule {
  constructor(conf: Configuration,
    leoConf: LeoConfiguration) {
    bindApiClients(conf);
    notebooksBindApiClients(leoConf);
  }
}
