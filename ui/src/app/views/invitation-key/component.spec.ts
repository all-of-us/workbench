import {DebugElement} from '@angular/core';
import {ComponentFixture, fakeAsync, TestBed, tick} from '@angular/core/testing';
import {FormsModule} from '@angular/forms';
import {UrlSegment} from '@angular/router';
import {RouterTestingModule} from '@angular/router/testing';

import {ClarityModule} from '@clr/angular';

import {ProfileService} from 'generated';

import {ProfileServiceStub} from 'testing/stubs/profile-service-stub';

import {
    queryByCss, simulateClick,
    updateAndTick
} from '../../../testing/test-helpers';
import {SignInService} from '../../services/sign-in.service';

import {AccountCreationComponent} from '../account-creation/component';
import {AppComponent} from '../app/component';
import {InvitationKeyComponent} from '../invitation-key/component';
import {PageTemplateSignedOutComponent} from '../page-template-signed-out/component';
import {RoutingSpinnerComponent} from '../routing-spinner/component';

class InvitationKeyPage {
  fixture: ComponentFixture<InvitationKeyComponent>;
  route: UrlSegment[];
  nextButton: DebugElement;

  constructor(testBed: typeof TestBed) {
    this.fixture = testBed.createComponent(InvitationKeyComponent);
    this.readPageData();
  }

  readPageData() {
    updateAndTick(this.fixture);
    updateAndTick(this.fixture);
    this.nextButton = queryByCss(this.fixture, '#next');
  }
}

describe('InvitationKeyComponent', () => {
  let invitationKeyPage: InvitationKeyPage;

  beforeEach(fakeAsync(() => {
    TestBed.configureTestingModule({
      imports: [
        RouterTestingModule,
        FormsModule,
        ClarityModule.forRoot()
      ],
      declarations: [
        AppComponent,
        AccountCreationComponent,
        InvitationKeyComponent,
        PageTemplateSignedOutComponent,
        RoutingSpinnerComponent
      ],
      providers: [
        { provide: AppComponent, useValue: {}},
        { provide: SignInService, useValue: {}},
        { provide: ProfileService, useValue: new ProfileServiceStub() }
      ] }).compileComponents().then(() => {
        invitationKeyPage = new InvitationKeyPage(TestBed);
        });
        tick();
  }));

  it('should create the app', fakeAsync(() => {
    const fixture = TestBed.createComponent(InvitationKeyComponent);
    const app = fixture.debugElement.componentInstance;
    expect(app).toBeTruthy();
    expect(app.invitationKeyVerifed).toBeFalsy();
    expect(app.invitationKeyReq).toBeFalsy();
    expect(app.invitationKeyInvalid).toBeFalsy();
  }));


  it('should not accept blank invitation code', fakeAsync(() => {
    simulateClick(invitationKeyPage.fixture, invitationKeyPage.nextButton);
    const app = invitationKeyPage.fixture.debugElement.componentInstance;
    expect(app.invitationKeyVerifed).toBeFalsy();
    expect(app.invitationKeyReq).toBeTruthy();
    expect(app.invitationKeyInvalid).toBeFalsy();
  }));

  it('should throw an error with invalid invitation code', fakeAsync(() => {
    const app = invitationKeyPage.fixture.debugElement.componentInstance;
    app.invitationKey = 'invalid';
    simulateClick(invitationKeyPage.fixture, invitationKeyPage.nextButton);
    expect(app.invitationKeyVerifed).toBeFalsy();
    expect(app.invitationKeyReq).toBeFalsy();
    expect(app.invitationKeyInvalid).toBeTruthy();
  }));

  it('should continue to next page on entering correct invitation code', fakeAsync(() => {
    const app = invitationKeyPage.fixture.debugElement.componentInstance;
    app.invitationKey = 'dummy';
    simulateClick(invitationKeyPage.fixture, invitationKeyPage.nextButton);
    expect(app.invitationKeyVerifed).toBeTruthy();
    expect(app.invitationKeyReq).toBeFalsy();
    expect(app.invitationKeyInvalid).toBeFalsy();
  }));

});
