import { inject, TestBed } from '@angular/core/testing';

import { DbConstantsService } from './db-constants.service';

describe('DbConstantsService', () => {
  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [DbConstantsService]
    });
  });

  it('should be created', inject([DbConstantsService], (service: DbConstantsService) => {
    expect(service).toBeTruthy();
  }));
});
