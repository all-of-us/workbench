import {ComponentFixture, fakeAsync, TestBed, tick} from '@angular/core/testing';
import {FormsModule} from '@angular/forms';
import {RouterTestingModule} from '@angular/router/testing';

import {ClarityModule} from '@clr/angular';

import {SignInServiceStub} from 'testing/stubs/sign-in-service-stub';

import {
  setupModals,
  updateAndTick
} from 'testing/test-helpers';

import {SignInService} from 'app/services/sign-in.service';
import {registerApiClient} from 'app/services/swagger-fetch-clients';

import {FooterComponent} from 'app/components/footer';
import {RoutingSpinnerComponent} from 'app/components/routing-spinner/component';
import {TextModalComponent} from 'app/components/text-modal';
import {SignedInComponent} from 'app/pages/signed-in/component';
import {NavBarComponent} from 'app/pages/signed-in/nav-bar';
import {CdrVersionsApi, ProfileApi, StatusAlertApi} from 'generated/fetch';
import {CdrVersionsApiStub} from 'testing/stubs/cdr-versions-api-stub';
import {ProfileApiStub} from 'testing/stubs/profile-api-stub';
import {StatusAlertApiStub} from 'testing/stubs/status-alert-api-stub';
import {ZendeskWidgetComponent} from 'app/components/zendesk-widget';

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
        SignedInComponent,
        RoutingSpinnerComponent,
        TextModalComponent,
        NavBarComponent,
        FooterComponent,
        ZendeskWidgetComponent
      ],
      providers: [
        {provide: SignInService, useValue: new SignInServiceStub()},
      ]
    }).compileComponents().then(() => {
      fixture = TestBed.createComponent(SignedInComponent);
      tick();
      setupModals(fixture);
    });
  }));

  beforeEach(() => {
    registerApiClient(ProfileApi, new ProfileApiStub());
    registerApiClient(CdrVersionsApi, new CdrVersionsApiStub());
    registerApiClient(StatusAlertApi, new StatusAlertApiStub());
  });

  it('should render', fakeAsync(() => {
    updateAndTick(fixture);
    expect(fixture).toBeTruthy();
    fixture.destroy();
  }));
});
