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
  simulateInput,
  updateAndTick
} from '../../../testing/test-helpers';
import {ServerConfigService} from '../../services/server-config.service';

import {AccountCreationSuccessComponent} from '../account-creation-success/component';
import {AccountCreationComponent} from '../account-creation/component';
import {InvitationKeyComponent} from '../invitation-key/component';
import {LoginComponent} from '../login/component';
import {PageTemplateSignedOutComponent} from '../page-template-signed-out/component';
import {RoutingSpinnerComponent} from '../routing-spinner/component';

class AccountCreationPage {
  fixture: ComponentFixture<AccountCreationComponent>;
  component: AccountCreationComponent;
  passwordField: DebugElement;
  passwordAgainField: DebugElement;
  usernameField: DebugElement;

  constructor(testBed: typeof TestBed) {
    this.fixture = testBed.createComponent(AccountCreationComponent);
    this.component = this.fixture.componentInstance;
    this.readPageData();
  }

  readPageData() {
    updateAndTick(this.fixture);
    this.passwordField = this.fixture.debugElement.query(By.css('#password'));
    this.passwordAgainField = this.fixture.debugElement.query(By.css('#passwordAgain'));
    this.usernameField = this.fixture.debugElement.query(By.css('#username'));
  }
}


describe('AccountCreationComponent', () => {
  let profileServiceStub: ProfileServiceStub;
  let page: AccountCreationPage;
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
        { provide: InvitationKeyComponent, useValue: {}},
        { provide: ProfileService, useValue: profileServiceStub },
        {
          provide: ServerConfigService,
          useValue: new ServerConfigServiceStub({
            gsuiteDomain: 'fake-research-aou.org'
          })
        }]
    }).compileComponents().then(() => {
      page = new AccountCreationPage(TestBed);
      tick();
    });
  }));

  it('handles selecting password', fakeAsync(() => {
    page.readPageData();
    expect(page.component.passwordOffFocus).toBeTruthy();
    simulateEvent(page.fixture, page.passwordField, 'focus');
    expect(page.component.passwordOffFocus).toBe(false);
    simulateEvent(page.fixture, page.passwordField, 'blur');
    expect(page.component.passwordOffFocus).toBe(true);
  }));

  it('handles password length requirements', fakeAsync(() => {
    page.readPageData();
    simulateEvent(page.fixture, page.passwordField, 'focus');
    simulateInput(page.fixture, page.passwordField, 'Passwor');
    simulateEvent(page.fixture, page.passwordField, 'blur');
    expect(page.fixture.debugElement.query(By.css('#password-length-error'))).toBeTruthy();
    expect(page.passwordField.classes.unsuccessfulInput).toBeTruthy();
    simulateInput(page.fixture, page.passwordField,
      'ThisissuchalongpasswordThisissuchalongpasswordThisissuchalongpasswordThisissuchalongpass \
       ThisissuchalongpasswordThisissuchalongpasswordThisissuchalongpasswordThisissuchalongpass \
       ThisissuchalongpasswordThisissuchalongpasswordThisissuchalongpasswordThisissuchalongpass');
    expect(page.fixture.debugElement.query(By.css('#password-length-error'))).toBeTruthy();
    expect(page.passwordField.classes.unsuccessfulInput).toBeTruthy();
    simulateInput(page.fixture, page.passwordField, 'Password');
    expect(page.fixture.debugElement.query(By.css('#password-length-error'))).toBeNull();
    expect(page.passwordField.classes.successfulInput).toBeTruthy();
  }));

  it('handles password casing requirements', fakeAsync(() => {
    page.readPageData();
    simulateEvent(page.fixture, page.passwordField, 'focus');
    simulateInput(page.fixture, page.passwordField, 'password');
    simulateEvent(page.fixture, page.passwordField, 'blur');
    expect(page.fixture.debugElement.query(By.css('#password-case-error'))).toBeTruthy();
    expect(page.passwordField.classes.unsuccessfulInput).toBeTruthy();
    expect(page.component.passwordIsNotValid).toBeTruthy();
    simulateInput(page.fixture, page.passwordField, 'Password');
    expect(page.fixture.debugElement.query(By.css('#password-case-error'))).toBeNull();
    expect(page.passwordField.classes.successfulInput).toBeTruthy();
  }));

  it('only shows password errors when off focus', fakeAsync(() => {
    page.readPageData();
    simulateEvent(page.fixture, page.passwordField, 'focus');
    simulateInput(page.fixture, page.passwordField, 'password');
    expect(page.passwordField.classes.unsuccessfulInput).toBeFalsy();
    simulateEvent(page.fixture, page.passwordField, 'blur');
    expect(page.passwordField.classes.unsuccessfulInput).toBeTruthy();
    simulateEvent(page.fixture, page.passwordField, 'focus');
    expect(page.passwordField.classes.unsuccessfulInput).toBeFalsy();
  }));

  it('handles selecting password again', fakeAsync(() => {
    page.readPageData();
    expect(page.component.passwordAgainOffFocus).toBeTruthy();
    simulateEvent(page.fixture, page.passwordAgainField, 'focus');
    expect(page.component.passwordAgainOffFocus).toBe(false);
    simulateEvent(page.fixture, page.passwordAgainField, 'blur');
    expect(page.component.passwordAgainOffFocus).toBe(true);
  }));

  it('handles each password again failure case', fakeAsync(() => {
    page.readPageData();
    simulateEvent(page.fixture, page.passwordField, 'focus');
    simulateInput(page.fixture, page.passwordField, 'Password');
    simulateEvent(page.fixture, page.passwordField, 'blur');

    simulateEvent(page.fixture, page.passwordAgainField, 'focus');
    expect(page.fixture.debugElement.query(By.css('#password-match-error'))).toBeFalsy();
    simulateInput(page.fixture, page.passwordAgainField, 'Passwor');
    simulateEvent(page.fixture, page.passwordAgainField, 'blur');
    expect(page.fixture.debugElement.query(By.css('#password-match-error'))).toBeTruthy();
    expect(page.passwordAgainField.classes.unsuccessfulInput).toBeTruthy();
    simulateInput(page.fixture, page.passwordAgainField, 'Password');
    expect(page.fixture.debugElement.query(By.css('#password-match-error'))).toBeFalsy();
    expect(page.passwordAgainField.classes.successfulInput).toBeTruthy();

  }));

  it('only shows password again errors when off focus', fakeAsync(() => {
    page.readPageData();
    simulateEvent(page.fixture, page.passwordField, 'focus');
    simulateInput(page.fixture, page.passwordField, 'Password');
    simulateEvent(page.fixture, page.passwordField, 'blur');

    simulateEvent(page.fixture, page.passwordAgainField, 'focus');
    simulateInput(page.fixture, page.passwordAgainField, 'Passwor');
    expect(page.fixture.debugElement.query(By.css('#password-match-error'))).toBeFalsy();

    simulateEvent(page.fixture, page.passwordAgainField, 'blur');
    expect(page.fixture.debugElement.query(By.css('#password-match-error'))).toBeTruthy();

    simulateEvent(page.fixture, page.passwordAgainField, 'focus');
    expect(page.fixture.debugElement.query(By.css('#password-match-error'))).toBeFalsy();
  }));

  it('handles selecting username', fakeAsync(() => {
    page.readPageData();
    expect(page.component.usernameOffFocus).toBeTruthy();
    simulateEvent(page.fixture, page.usernameField, 'focus');
    expect(page.component.usernameOffFocus).toBeFalsy();
    simulateEvent(page.fixture, page.usernameField, 'blur');
    expect(page.component.usernameOffFocus).toBeTruthy();
  }));

  it('handles each username invalidity', fakeAsync(() => {
    page.readPageData();
    // Begin with a period
    simulateEvent(page.fixture, page.usernameField, 'focus');
    simulateInput(page.fixture, page.usernameField, '.username');
    tick(300);
    tick();
    simulateEvent(page.fixture, page.usernameField, 'blur');
    expect(page.fixture.debugElement.query(By.css('#username-invalid-error'))).toBeTruthy();
    expect(page.usernameField.classes.unsuccessfulInput).toBeTruthy();

    // End with a period
    simulateInput(page.fixture, page.usernameField, 'username.');
    tick(300);
    tick();
    expect(page.fixture.debugElement.query(By.css('#username-invalid-error'))).toBeTruthy();
    expect(page.usernameField.classes.unsuccessfulInput).toBeTruthy();

    // Contains special characters
    simulateInput(page.fixture, page.usernameField, 'user@name');
    tick(300);
    tick();
    expect(page.fixture.debugElement.query(By.css('#username-invalid-error'))).toBeTruthy();
    expect(page.usernameField.classes.unsuccessfulInput).toBeTruthy();


    simulateInput(page.fixture, page.usernameField, 'blah');
    tick(300);
    tick();
    expect(page.fixture.debugElement.query(By.css('#username-invalid-error'))).toBeFalsy();
    expect(page.usernameField.classes.unsuccessfulInput).toBeFalsy();
  }));
});
