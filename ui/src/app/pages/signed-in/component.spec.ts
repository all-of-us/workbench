import {ComponentFixture, fakeAsync, TestBed, tick} from '@angular/core/testing';
import {FormsModule} from '@angular/forms';
import {RouterTestingModule} from '@angular/router/testing';

import {ClarityModule} from '@clr/angular';

import {ErrorHandlingServiceStub} from 'testing/stubs/error-handling-service-stub';
import {ProfileStorageServiceStub} from 'testing/stubs/profile-storage-service-stub';
import {ServerConfigServiceStub} from 'testing/stubs/server-config-service-stub';
import {SignInServiceStub} from 'testing/stubs/sign-in-service-stub';

import {
  setupModals,
  updateAndTick
} from 'testing/test-helpers';

import {ErrorHandlingService} from 'app/services/error-handling.service';
import {ProfileStorageService} from 'app/services/profile-storage.service';
import {ServerConfigService} from 'app/services/server-config.service';
import {SignInService} from 'app/services/sign-in.service';
import {registerApiClient} from 'app/services/swagger-fetch-clients';

import {BugReportComponent} from 'app/components/bug-report';
import {RoutingSpinnerComponent} from 'app/components/routing-spinner/component';
import {TextModalComponent} from 'app/components/text-modal';
import {SignedInComponent} from 'app/pages/signed-in/component';
import {NavBarComponent} from 'app/pages/signed-in/nav-bar';
import {CdrVersionsApi} from 'generated/fetch';
import {CdrVersionsApiStub} from 'testing/stubs/cdr-versions-api-stub';

describe('SignedInComponent', () => {
  let fixture: ComponentFixture<SignedInComponent>;
  beforeEach(fakeAsync(() => {
    TestBed.configureTestingModule({
      imports: [
        FormsModule,
        RouterTestingModule,
        ClarityModule.forRoot()
      ],
      declarations: [
        BugReportComponent,
        SignedInComponent,
        RoutingSpinnerComponent,
        TextModalComponent,
        NavBarComponent
      ],
      providers: [
        {provide: ErrorHandlingService, useValue: new ErrorHandlingServiceStub()},
        {provide: ProfileStorageService, useValue: new ProfileStorageServiceStub()},
        {
          provide: ServerConfigService,
          useValue: new ServerConfigServiceStub({
            gsuiteDomain: 'fake-research-aou.org'
          })
        },
        {provide: SignInService, useValue: new SignInServiceStub()},
      ]
    }).compileComponents().then(() => {
      fixture = TestBed.createComponent(SignedInComponent);
      tick();
      setupModals(fixture);
    });
  }));

  beforeEach(() => {
    registerApiClient(CdrVersionsApi, new CdrVersionsApiStub());
  });

  it('should render', fakeAsync(() => {
    updateAndTick(fixture);
    expect(fixture).toBeTruthy();
    fixture.destroy();
  }));
});
