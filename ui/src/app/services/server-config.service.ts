import {Injectable} from '@angular/core';
import {Observable} from 'rxjs/Observable';
import {ReplaySubject} from 'rxjs/ReplaySubject';

import {ConfigResponse, ConfigService} from 'generated';

@Injectable()
export class ServerConfigService {
  private configObs: Observable<ConfigResponse>;
  constructor(
    private configService: ConfigService
  ) {}

  public getConfig(): Observable<ConfigResponse> {
    if (!this.configObs) {
      // Use of a replaySubject() caches the output of the API call across subscriptions.
      const subject = new ReplaySubject<ConfigResponse>();
      this.configService.getConfig().subscribe(subject);
      this.configObs = subject.asObservable();
    }
    return this.configObs;
  }
}
