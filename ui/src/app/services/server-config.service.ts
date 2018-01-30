import {Injectable} from '@angular/core';
import {Observable} from 'rxjs/Observable';

import {ErrorHandlingService} from 'app/services/error-handling.service';

import {ConfigResponse, ConfigService} from 'generated';

@Injectable()
export class ServerConfigService {
  private configObs: Observable<ConfigResponse>;
  constructor(
    private configService: ConfigService, private errorHandlingService: ErrorHandlingService) {}

  public getConfig(): Observable<ConfigResponse> {
    if (!this.configObs) {
      // share() avoids reexecution of this call on each subscribe().
      this.configObs = this.errorHandlingService.retryApi(this.configService.getConfig()).share();
    }
    return this.configObs;
  }
}
