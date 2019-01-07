import {Injectable} from '@angular/core';
import {BehaviorSubject} from 'rxjs/BehaviorSubject';
import {Observable} from 'rxjs/Observable';

import {ConfigResponse} from 'publicGenerated';

export class ServerConfigServiceStub {
  constructor(public config: ConfigResponse) {}

  public getConfig(): Observable<ConfigResponse> {
    return new BehaviorSubject<ConfigResponse>(this.config).asObservable();
  }
}
