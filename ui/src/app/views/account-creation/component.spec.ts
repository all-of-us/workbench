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


describe('AccountCreationComponent', () => {
  let profileServiceStub: ProfileServiceStub;
  let fixture: ComponentFixture<AccountCreationComponent>;
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
      fixture = TestBed.createComponent(AccountCreationComponent);
      tick();
    });
    tick();
  }));

  it('handles selecting password', fakeAsync(() => {
    updateAndTick(fixture);
    expect(fixture.componentInstance.passwordOffFocus).toBeUndefined();
    simulateEvent(fixture, fixture.debugElement.query(By.css('#password')), 'focus');
    expect(fixture.componentInstance.passwordOffFocus).toBe(false);
    simulateEvent(fixture, fixture.debugElement.query(By.css('#password')), 'blur');
    expect(fixture.componentInstance.passwordOffFocus).toBe(true);
  }));

  it('handles each password failure case', fakeAsync(() => {
    updateAndTick(fixture);
    simulateEvent(fixture, fixture.debugElement.query(By.css('#password')), 'focus');
    simulateInput(fixture, fixture.debugElement.query(By.css('#password')), 'password');
    simulateEvent(fixture, fixture.debugElement.query(By.css('#password')), 'blur');
    expect(fixture.componentInstance.containsLowerAndUpperError).toBeTruthy();
    expect(fixture.componentInstance.isPasswordValidationError).toBeTruthy();
    simulateInput(fixture, fixture.debugElement.query(By.css('#password')), 'Passwor');
    expect(fixture.componentInstance.showPasswordLengthError).toBeTruthy();
    expect(fixture.componentInstance.isPasswordValidationError).toBeTruthy();
    simulateInput(fixture, fixture.debugElement.query(By.css('#password')),
      'ThisissuchalongpasswordThisissuchalongpasswordThisissuchalongpasswordThisissuchalongpass \
       ThisissuchalongpasswordThisissuchalongpasswordThisissuchalongpasswordThisissuchalongpass \
       ThisissuchalongpasswordThisissuchalongpasswordThisissuchalongpasswordThisissuchalongpass');
    expect(fixture.componentInstance.showPasswordLengthError).toBeTruthy();
    expect(fixture.componentInstance.isPasswordValidationError).toBeTruthy();
    simulateInput(fixture, fixture.debugElement.query(By.css('#password')), 'Password');
    expect(fixture.componentInstance.showPasswordLengthError).toBeFalsy();
    expect(fixture.componentInstance.containsLowerAndUpperError).toBeFalsy();
    expect(fixture.componentInstance.isPasswordValidationError).toBeFalsy();
  }));

  it('only shows password errors when off focus', fakeAsync(() => {
    updateAndTick(fixture);
    simulateEvent(fixture, fixture.debugElement.query(By.css('#password')), 'focus');
    simulateInput(fixture, fixture.debugElement.query(By.css('#password')), 'password');
    expect(fixture.componentInstance.showPasswordValidationError).toBeFalsy();
    simulateEvent(fixture, fixture.debugElement.query(By.css('#password')), 'blur');
    expect(fixture.componentInstance.showPasswordValidationError).toBeTruthy();
    simulateEvent(fixture, fixture.debugElement.query(By.css('#password')), 'focus');
    expect(fixture.componentInstance.showPasswordValidationError).toBeFalsy();
  }));


  it('handles selecting password again', fakeAsync(() => {
    updateAndTick(fixture);
    expect(fixture.componentInstance.passwordAgainOffFocus).toBeUndefined();
    simulateEvent(fixture, fixture.debugElement.query(By.css('#passwordAgain')), 'focus');
    expect(fixture.componentInstance.passwordAgainOffFocus).toBe(false);
    simulateEvent(fixture, fixture.debugElement.query(By.css('#passwordAgain')), 'blur');
    expect(fixture.componentInstance.passwordAgainOffFocus).toBe(true);
  }));

  it('handles each password again failure case', fakeAsync(() => {
    updateAndTick(fixture);
    simulateEvent(fixture, fixture.debugElement.query(By.css('#password')), 'focus');
    simulateInput(fixture, fixture.debugElement.query(By.css('#password')), 'Password');
    simulateEvent(fixture, fixture.debugElement.query(By.css('#password')), 'blur');

    simulateEvent(fixture, fixture.debugElement.query(By.css('#passwordAgain')), 'focus');
    expect(fixture.componentInstance.isPasswordAgainValidationError).toBeFalsy();
    expect(fixture.componentInstance.showPasswordsDoNotMatchError).toBeFalsy();
    simulateInput(fixture, fixture.debugElement.query(By.css('#passwordAgain')), 'Passwor');
    simulateEvent(fixture, fixture.debugElement.query(By.css('#passwordAgain')), 'blur');
    expect(fixture.componentInstance.isPasswordAgainValidationError).toBeTruthy();
    expect(fixture.componentInstance.showPasswordsDoNotMatchError).toBeTruthy();
    simulateInput(fixture, fixture.debugElement.query(By.css('#passwordAgain')), 'Password');
    expect(fixture.componentInstance.isPasswordAgainValidationError).toBeFalsy();
    expect(fixture.componentInstance.showPasswordsDoNotMatchError).toBeFalsy();
  }));

  it('only shows password again errors when off focus', fakeAsync(() => {
    updateAndTick(fixture);
    updateAndTick(fixture);
    simulateEvent(fixture, fixture.debugElement.query(By.css('#password')), 'focus');
    simulateInput(fixture, fixture.debugElement.query(By.css('#password')), 'Password');
    simulateEvent(fixture, fixture.debugElement.query(By.css('#password')), 'blur');

    simulateEvent(fixture, fixture.debugElement.query(By.css('#passwordAgain')), 'focus');
    simulateInput(fixture, fixture.debugElement.query(By.css('#passwordAgain')), 'Passwor');
    expect(fixture.componentInstance.showPasswordAgainValidationError).toBeFalsy();

    simulateEvent(fixture, fixture.debugElement.query(By.css('#passwordAgain')), 'blur');
    expect(fixture.componentInstance.showPasswordAgainValidationError).toBeTruthy();

    simulateEvent(fixture, fixture.debugElement.query(By.css('#passwordAgain')), 'focus');
    expect(fixture.componentInstance.showPasswordAgainValidationError).toBeFalsy();
  }));

  it('handles selecting username', fakeAsync(() => {
    updateAndTick(fixture);
    expect(fixture.componentInstance.usernameOffFocus).toBeUndefined();
    simulateEvent(fixture, fixture.debugElement.query(By.css('#username')), 'focus');
    expect(fixture.componentInstance.usernameOffFocus).toBe(false);
    simulateEvent(fixture, fixture.debugElement.query(By.css('#username')), 'blur');
    expect(fixture.componentInstance.usernameOffFocus).toBe(true);
  }));

  it('handles each username failure case', fakeAsync(() => {
    updateAndTick(fixture);
    // Begin with a period
    simulateEvent(fixture, fixture.debugElement.query(By.css('#username')), 'focus');
    simulateInput(fixture, fixture.debugElement.query(By.css('#username')), '.username');
    tick(300);
    tick();
    simulateEvent(fixture, fixture.debugElement.query(By.css('#username')), 'blur');
    expect(fixture.componentInstance.usernameInvalidError).toBeTruthy();
    expect(fixture.componentInstance.isUsernameValidationError).toBeTruthy();

    // End with a period
    simulateInput(fixture, fixture.debugElement.query(By.css('#username')), 'username.');
    tick(300);
    tick();
    expect(fixture.componentInstance.usernameInvalidError).toBeTruthy();
    expect(fixture.componentInstance.isUsernameValidationError).toBeTruthy();

    // Contains special characters
    simulateInput(fixture, fixture.debugElement.query(By.css('#username')), 'user@name');
    tick(300);
    tick();
    expect(fixture.componentInstance.usernameInvalidError).toBeTruthy();
    expect(fixture.componentInstance.isUsernameValidationError).toBeTruthy();


    simulateInput(fixture, fixture.debugElement.query(By.css('#username')), 'blah');
    tick(300);
    tick();
    expect(fixture.componentInstance.isUsernameValidationError).toBeFalsy();
  }));
});
