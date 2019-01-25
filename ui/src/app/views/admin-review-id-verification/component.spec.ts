import {ComponentFixture, fakeAsync, TestBed, tick} from '@angular/core/testing';

import {ClarityModule} from '@clr/angular';

import {
  ProfileService,
} from 'generated';

import {ProfileServiceStub} from 'testing/stubs/profile-service-stub';
import {ServerConfigServiceStub} from 'testing/stubs/server-config-service-stub';

import {ServerConfigService} from 'app/services/server-config.service';
import {
  updateAndTick
} from 'testing/test-helpers';

import {AdminReviewIdVerificationComponent} from 'app/views/admin-review-id-verification/component';

describe('AdminReviewIdVerificationComponent', () => {
  let fixture: ComponentFixture<AdminReviewIdVerificationComponent>;
  beforeEach(fakeAsync(() => {
    TestBed.configureTestingModule({
      imports: [
        ClarityModule.forRoot()
      ],
      declarations: [
        AdminReviewIdVerificationComponent
      ],
      providers: [
        { provide: ProfileService, useValue: new ProfileServiceStub() },
        {
          provide: ServerConfigService,
          useValue: new ServerConfigServiceStub({
            gsuiteDomain: 'fake-research-aou.org'
          })
        }
      ]
    }).compileComponents().then(() => {
      fixture = TestBed.createComponent(AdminReviewIdVerificationComponent);
      tick();
    });
  }));

  it('should render', fakeAsync(() => {
    updateAndTick(fixture);
    expect(fixture).toBeTruthy();
  }));
});
