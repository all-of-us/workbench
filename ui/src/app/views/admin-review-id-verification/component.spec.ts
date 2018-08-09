import {DebugElement} from '@angular/core';
import {ComponentFixture, fakeAsync, TestBed, tick} from '@angular/core/testing';
import {FormsModule} from '@angular/forms';
import {By} from '@angular/platform-browser';
import {RouterTestingModule} from '@angular/router/testing';

import {ClarityModule} from '@clr/angular';

import {
  ProfileService,
} from 'generated';

import {ProfileServiceStub} from 'testing/stubs/profile-service-stub';
import {ServerConfigServiceStub} from 'testing/stubs/server-config-service-stub';

import {
  updateAndTick
} from '../../../testing/test-helpers';
import {ServerConfigService} from '../../services/server-config.service';

import {AdminReviewIdVerificationComponent} from '../admin-review-id-verification/component';

class AdminReviewIdVerificationPage {
  fixture: ComponentFixture<AdminReviewIdVerificationComponent>;
  component: AdminReviewIdVerificationComponent;

  constructor(testBed: typeof TestBed) {
    this.fixture = testBed.createComponent(AdminReviewIdVerificationComponent);
    this.component = this.fixture.componentInstance;
    this.readPageData();
  }

  readPageData() {
    updateAndTick(this.fixture);
  }
}


describe('AdminReviewIdVerificationComponent', () => {
  let page: AdminReviewIdVerificationPage;
  beforeEach(fakeAsync(() => {
    TestBed.configureTestingModule({
      imports: [
        RouterTestingModule,
        FormsModule,
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
      page = new AdminReviewIdVerificationPage(TestBed);
      tick();
    });
  }));

  it('should render', fakeAsync(() => {
    page.readPageData();
    expect(page.fixture).toBeTruthy();
  }));
});
