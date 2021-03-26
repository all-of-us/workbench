import {Injectable} from '@angular/core';
import {Observable} from 'rxjs/Observable';
import {ReplaySubject} from 'rxjs/ReplaySubject';

import {configApi} from 'app/services/swagger-fetch-clients';
import {ConfigResponse} from 'generated/fetch';

@Injectable()
export class ServerConfigService {
  private configObs: Observable<ConfigResponse>;
  constructor() {}

  public getConfig(): Observable<ConfigResponse> {
    if (!this.configObs) {
      // Use of a replaySubject() caches the output of the API call across subscriptions.
      const subject = new ReplaySubject<ConfigResponse>();
      configApi().getConfig().then((config: ConfigResponse) => subject.next(config));
      this.configObs = subject.asObservable();
    }
    return this.configObs;
  }
}
