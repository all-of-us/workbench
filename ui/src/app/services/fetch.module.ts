import { Inject, NgModule } from '@angular/core';
import {bindApiClients as notebooksBindApiClients} from 'app/services/notebooks-swagger-fetch-clients';
import {bindApiClients} from 'app/services/swagger-fetch-clients';
import * as portableFetch from 'portable-fetch';

import {
  Configuration as FetchConfiguration,
  FetchAPI,
} from 'generated/fetch';

import {
  Configuration as LeoConfiguration
} from 'notebooks-generated';


const FETCH_API_REF = 'fetchApi';

/**
 * This module requires a FETCH_API_REF and FetchConfiguration instance to be
 * provided. Unfortunately typescript-fetch does not provide this module by
 * default, so a new entry will need to be added below for each new API service
 * added to the Swagger interfaces.
 *
 * This module is transitional for the Angular -> React conversion. Once routing
 * switches off Angular, we should generate these API stubs dynamically.
 */
@NgModule({
  imports:      [],
  declarations: [],
  exports:      [],
  providers: [{
    provide: FETCH_API_REF,
    useValue: portableFetch
  }]
})
export class FetchModule {
  constructor(conf: FetchConfiguration,
    leoConf: LeoConfiguration,
    @Inject(FETCH_API_REF) fetchApi: FetchAPI) {
    bindApiClients(conf, fetchApi);
    notebooksBindApiClients(leoConf, fetchApi);
  }
}
