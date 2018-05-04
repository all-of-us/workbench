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
    simulateClick,
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
  nextButton: DebugElement;

  constructor(testBed: typeof TestBed) {
    this.fixture = testBed.createComponent(InvitationKeyComponent);
    this.readPageData();
  }

  readPageData() {
    updateAndTick(this.fixture);
    updateAndTick(this.fixture);
    this.nextButton = this.fixture.debugElement.query(By.css('#next'));
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
    expect(app.invitationKeyVerifed).toBeFalsy();
    expect(app.invitationKeyReq).toBeFalsy();
    expect(app.invitationKeyInvalid).toBeFalsy();
  }));


  it('should not accept blank invitation code', fakeAsync(() => {
    simulateClick(invitationKeyPage.fixture, invitationKeyPage.nextButton);
    const app = invitationKeyPage.fixture.debugElement.componentInstance;
    updateAndTick(invitationKeyPage.fixture);
    expect(app.invitationKeyVerifed).toBeFalsy();
    expect(app.invitationKeyReq).toBeTruthy();
    expect(app.invitationKeyInvalid).toBeFalsy();
  }));

  it('should throw an error with invalid invitation code', fakeAsync(() => {
    const app = invitationKeyPage.fixture.debugElement.componentInstance;
    app.invitationKey = 'invalid';
    updateAndTick(invitationKeyPage.fixture);
    simulateClick(invitationKeyPage.fixture, invitationKeyPage.nextButton);
    expect(app.invitationKeyVerifed).toBeFalsy();
    expect(app.invitationKeyReq).toBeFalsy();
    expect(app.invitationKeyInvalid).toBeTruthy();
  }));

  it('should continue to next page on entering correct invitation code', fakeAsync(() => {
    const app = invitationKeyPage.fixture.debugElement.componentInstance;
    app.invitationKey = 'dummy';
    updateAndTick(invitationKeyPage.fixture);
    simulateClick(invitationKeyPage.fixture, invitationKeyPage.nextButton);
    expect(app.invitationKeyVerifed).toBeTruthy();
    expect(app.invitationKeyReq).toBeFalsy();
    expect(app.invitationKeyInvalid).toBeFalsy();
  }));

  it('should allow account creation', fakeAsync(() => {
    const fixture = invitationKeyPage.fixture;
    fixture.debugElement.componentInstance.invitationKey = 'dummy';
    updateAndTick(fixture);
    simulateClick(fixture, invitationKeyPage.nextButton);

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
