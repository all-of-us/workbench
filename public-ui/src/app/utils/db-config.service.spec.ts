import { inject, TestBed } from '@angular/core/testing';

import { DbConfigService } from './db-config.service';

describe('DbConstantsService', () => {
  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [DbConfigService]
    });
  });

  it('should be created', inject([DbConfigService], (service: DbConfigService) => {
    expect(service).toBeTruthy();
  }));
});
