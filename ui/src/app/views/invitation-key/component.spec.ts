import {DebugElement} from '@angular/core';
import {ComponentFixture, fakeAsync, TestBed, tick} from '@angular/core/testing';
import {FormsModule} from '@angular/forms';
import {By} from '@angular/platform-browser';
import {UrlSegment} from '@angular/router';
import {RouterTestingModule} from '@angular/router/testing';

import {ClarityModule} from '@clr/angular';

import {ProfileService} from 'generated';

import {ProfileServiceStub} from 'testing/stubs/profile-service-stub';
import {ServerConfigServiceStub} from 'testing/stubs/server-config-service-stub';

import {
  simulateEvent,
  updateAndTick
} from '../../../testing/test-helpers';

import {ServerConfigService} from '../../services/server-config.service';
import {SignInService} from '../../services/sign-in.service';

import {AccountCreationSuccessComponent} from '../account-creation-success/component';
import {AccountCreationComponent} from '../account-creation/component';
import {InvitationKeyComponent} from '../invitation-key/component';
import {LoginComponent} from '../login/component';
import {PageTemplateSignedOutComponent} from '../page-template-signed-out/component';
import {RoutingSpinnerComponent} from '../routing-spinner/component';

class InvitationKeyPage {
  fixture: ComponentFixture<InvitationKeyComponent>;
  route: UrlSegment[];
  form: DebugElement;

  constructor(testBed: typeof TestBed) {
    this.fixture = testBed.createComponent(InvitationKeyComponent);
    this.readPageData();
  }

  readPageData() {
    updateAndTick(this.fixture);
    updateAndTick(this.fixture);
    this.form = this.fixture.debugElement.query(By.css('form'));
  }
}

describe('InvitationKeyComponent', () => {
  let invitationKeyPage: InvitationKeyPage;
  let profileServiceStub: ProfileServiceStub;

  beforeEach(fakeAsync(() => {
    profileServiceStub = new ProfileServiceStub();
    TestBed.configureTestingModule({
      imports: [
        RouterTestingModule,
        FormsModule,
        ClarityModule.forRoot()
      ],
      declarations: [
        LoginComponent,
        AccountCreationComponent,
        AccountCreationSuccessComponent,
        InvitationKeyComponent,
        PageTemplateSignedOutComponent,
        RoutingSpinnerComponent
      ],
      providers: [
        { provide: LoginComponent, useValue: {}},
        { provide: SignInService, useValue: {}},
        { provide: ProfileService, useValue: profileServiceStub },
        {
          provide: ServerConfigService,
          useValue: new ServerConfigServiceStub({
            gsuiteDomain: 'fake-research-aou.org'
          })
        }
      ] }).compileComponents().then(() => {
        invitationKeyPage = new InvitationKeyPage(TestBed);
        });
        tick();
  }));

  it('should create the app', fakeAsync(() => {
    const fixture = TestBed.createComponent(InvitationKeyComponent);
    const app = fixture.debugElement.componentInstance;
    updateAndTick(fixture);
    expect(app).toBeTruthy();
    expect(app.invitationKeyVerified).toBeFalsy();
    expect(app.invitationKeyReq).toBeFalsy();
    expect(app.invitationKeyInvalid).toBeFalsy();
    const invitationInput = invitationKeyPage.fixture.debugElement.query(
        By.css('input'));
    expect(invitationInput.nativeElement.autofocus).toBeTruthy();
  }));


  it('should not accept blank invitation code', fakeAsync(() => {
    const app = invitationKeyPage.fixture.debugElement.componentInstance;
    simulateEvent(invitationKeyPage.fixture, invitationKeyPage.form, 'submit');
    updateAndTick(invitationKeyPage.fixture);
    updateAndTick(invitationKeyPage.fixture);
    expect(app.invitationKeyVerified).toBeFalsy();
    expect(app.invitationKeyReq).toBeTruthy();
    expect(app.invitationKeyInvalid).toBeFalsy();
    const focusedElement = document.activeElement;
    expect(focusedElement.id).toBe(app.invitationInput.first.nativeElement.id);
  }));

  it('should throw an error with invalid invitation code', fakeAsync(() => {
    const app = invitationKeyPage.fixture.debugElement.componentInstance;
    app.invitationKey = 'invalid';
    spyOn(app.invitationInput.first.nativeElement, 'focus');
    updateAndTick(invitationKeyPage.fixture);
    simulateEvent(invitationKeyPage.fixture, invitationKeyPage.form, 'submit');
    expect(app.invitationKeyVerified).toBeFalsy();
    expect(app.invitationKeyReq).toBeFalsy();
    expect(app.invitationKeyInvalid).toBeTruthy();
    expect(app.invitationInput.first.nativeElement.focus).toHaveBeenCalledTimes(1);
  }));

  it('should continue to next page on entering correct invitation code', fakeAsync(() => {
    const app = invitationKeyPage.fixture.debugElement.componentInstance;
    app.invitationKey = 'dummy';
    updateAndTick(invitationKeyPage.fixture);
    simulateEvent(invitationKeyPage.fixture, invitationKeyPage.form, 'submit');
    expect(app.invitationKeyVerified).toBeTruthy();
    expect(app.invitationKeyReq).toBeFalsy();
    expect(app.invitationKeyInvalid).toBeFalsy();
  }));

  it('should allow account creation', fakeAsync(() => {
    const fixture = invitationKeyPage.fixture;
    fixture.debugElement.componentInstance.invitationKey = 'dummy';
    updateAndTick(fixture);
    simulateEvent(fixture, invitationKeyPage.form, 'submit');
    const createDebugEl = fixture.debugElement.query(
      By.css('app-account-creation'));
    const createComponent = createDebugEl.componentInstance;
    createComponent.profile.username = 'researcher';
    createComponent.profile.givenName = 'Falco';
    createComponent.profile.familyName = 'Lombardi';
    createComponent.profile.contactEmail = 'fake@asdf.com';
    createComponent.password = 'passworD';
    createComponent.passwordAgain = 'passworD';
    updateAndTick(fixture);
    createDebugEl.query(By.css('button[type=submit]')).nativeElement.click();
    updateAndTick(fixture);
    expect(profileServiceStub.accountCreates).toEqual(1);
  }));
});
