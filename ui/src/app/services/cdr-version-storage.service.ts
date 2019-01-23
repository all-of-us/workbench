import 'rxjs/Rx';

import {Injectable} from '@angular/core';
import {ReplaySubject} from 'rxjs/ReplaySubject';

import {
  CdrVersionListResponse,
  CdrVersionsService
} from 'generated';


/**
 * Request and cache CDR versions indefinitely. We expect this data to change
 * ~quarterly, so there is no need to re-request this within a single client
 * session.
 */
@Injectable()
export class CdrVersionStorageService {
  private cdrVersions = new ReplaySubject<CdrVersionListResponse>(1);
  public cdrVersions$ = this.cdrVersions.asObservable();

  constructor(private cdrVersionsService: CdrVersionsService) {
    this.cdrVersionsService.getCdrVersions()
      .subscribe((resp: CdrVersionListResponse) => {
        this.cdrVersions.next(resp);
      });
  }
}
