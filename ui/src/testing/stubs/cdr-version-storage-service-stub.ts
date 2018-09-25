import {ReplaySubject} from 'rxjs/ReplaySubject';

import {
  CdrVersion,
  CdrVersionListResponse
} from 'generated';

export class CdrVersionStorageServiceStub {
  private cdrVersions = new ReplaySubject<CdrVersionListResponse>(1);
  public cdrVersions$ = this.cdrVersions.asObservable();

  constructor(value: CdrVersionListResponse) {
    this.cdrVersions.next(value);
  }
}
