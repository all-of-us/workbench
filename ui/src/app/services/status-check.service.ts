import {Injectable} from '@angular/core';
import {Http} from '@angular/http';
import {Observable} from 'rxjs/Observable';

import {InterceptedHttp} from 'app/factory/InterceptedHttp';
import {ErrorHandlingService} from 'app/services/error-handling.service';

import {StatusService} from 'generated';

@Injectable()
export class StatusCheckService {
  public apiDown: boolean;
  public firecloudDown: boolean;
  public notebooksDown: boolean;

  constructor(
    private http: Http,
    private statusService: StatusService,
  ) {
    this.apiDown = false;
    this.firecloudDown = false;
    this.notebooksDown = false;
    (<InterceptedHttp> http).shouldCheckStatus$.subscribe(() => {
      this.getApiStatus();
    });
  }

  private getApiStatus(): void {
    (<InterceptedHttp> this.http).shouldPingStatus = false;
    this.statusService.getStatus().subscribe((resp) => {
      if (resp.firecloudStatus === false) {
        this.firecloudDown = true;
      }
      if (resp.notebooksStatus === false) {
        this.notebooksDown = true;
      }
      (<InterceptedHttp> this.http).shouldPingStatus = true;
      return;
    }, () => {
      this.apiDown = true;
      (<InterceptedHttp> this.http).shouldPingStatus = true;
      return;
    });
  }
}
