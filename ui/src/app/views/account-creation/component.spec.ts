import {ComponentFixture, fakeAsync, TestBed, tick} from '@angular/core/testing';
import {FormsModule} from '@angular/forms';
import {By} from '@angular/platform-browser';

import {ClarityModule} from '@clr/angular';

import {randomString} from 'app/utils/index';
import {ProfileService} from 'generated';
import {ProfileServiceStub} from 'testing/stubs/profile-service-stub';
import {ServerConfigServiceStub} from 'testing/stubs/server-config-service-stub';

import {
  simulateEvent,
  simulateInput,
  updateAndTick
} from '../../../testing/test-helpers';
import {ServerConfigService} from '../../services/server-config.service';

import {AccountCreationModalsComponent} from '../account-creation-modals/component';
import {AccountCreationSuccessComponent} from '../account-creation-success/component';
import {AccountCreationComponent} from '../account-creation/component';
import {InvitationKeyComponent} from '../invitation-key/component';
import {LoginComponent} from '../login/component';
import {PageTemplateSignedOutComponent} from '../page-template-signed-out/component';

describe('AccountCreationComponent', () => {
  let fixture: ComponentFixture<AccountCreationComponent>;
  beforeEach(fakeAsync(() => {
    TestBed.configureTestingModule({
      imports: [
        FormsModule,
        ClarityModule.forRoot()
      ],
      declarations: [
        LoginComponent,
        AccountCreationComponent,
        AccountCreationModalsComponent,
        AccountCreationSuccessComponent,
        InvitationKeyComponent,
        PageTemplateSignedOutComponent
      ],
      providers: [
        { provide: LoginComponent, useValue: {}},
        { provide: InvitationKeyComponent, useValue: {}},
        { provide: ProfileService, useValue: new ProfileServiceStub() },
        {
          provide: ServerConfigService,
          useValue: new ServerConfigServiceStub({
            gsuiteDomain: 'fake-research-aou.org'
          })
        }]
    }).compileComponents().then(() => {
      fixture = TestBed.createComponent(AccountCreationComponent);
      tick();
      updateAndTick(fixture);
    });
  }));

  it('handles selecting username', fakeAsync(() => {
    expect(fixture.componentInstance.usernameFocused).toBeFalsy();
    simulateEvent(fixture, fixture.debugElement.query(By.css('#username')), 'focus');
    expect(fixture.componentInstance.usernameFocused).toBeTruthy();
    simulateEvent(fixture, fixture.debugElement.query(By.css('#username')), 'blur');
    expect(fixture.componentInstance.usernameFocused).toBeFalsy();
  }));

  it('handles each username invalidity', fakeAsync(() => {
    // Begin with a period
    const usernameField = fixture.debugElement.query(By.css('#username'));
    simulateEvent(fixture, usernameField, 'focus');
    simulateInput(fixture, usernameField, '.username');
    tick(300);
    updateAndTick(fixture);
    let usernameInvalidError =
      fixture.debugElement.queryAll(By.css('#username-invalid-error')).length;
    expect(usernameInvalidError).toBeTruthy();
    expect(usernameField.classes.unsuccessfulInput).toBeTruthy();

    // End with a period
    simulateInput(fixture, usernameField, 'username.');
    tick(300);
    updateAndTick(fixture);
    usernameInvalidError = fixture.debugElement.queryAll(By.css('#username-invalid-error')).length;
    expect(usernameInvalidError).toBeTruthy();
    expect(usernameField.classes.unsuccessfulInput).toBeTruthy();

    // Contains special characters
    simulateInput(fixture, usernameField, 'user@name');
    tick(300);
    updateAndTick(fixture);
    usernameInvalidError = fixture.debugElement.queryAll(By.css('#username-invalid-error')).length;
    expect(usernameInvalidError).toBeTruthy();
    expect(usernameField.classes.unsuccessfulInput).toBeTruthy();


    simulateInput(fixture, usernameField, 'blah');
    tick(300);
    updateAndTick(fixture);
    usernameInvalidError = fixture.debugElement.queryAll(By.css('#username-invalid-error')).length;
    expect(usernameInvalidError).toBeFalsy();
    expect(usernameField.classes.unsuccessfulInput).toBeFalsy();
  }));

  it('handles long username with mismatch at end', fakeAsync(() => {
    const usernameField = fixture.debugElement.query(By.css('#username'));
    simulateInput(fixture, usernameField, 'thisisaverylongusernamewithnowspaceswillitwork t');
    tick(300);
    updateAndTick(fixture);
    const usernameInvalidError =
      fixture.debugElement.queryAll(By.css('#username-invalid-error')).length;
    expect(usernameInvalidError).toBeTruthy();
    expect(usernameField.classes.unsuccessfulInput).toBeTruthy();
  }));

  it('handles long given name errors', fakeAsync(() => {
    const givenNameField = fixture.debugElement.query(By.css('#givenName'));
    simulateInput(fixture, givenNameField, randomString(81));
    tick(300);
    updateAndTick(fixture);
    expect(givenNameField.classes.unsuccessfulInput).toBeTruthy();
  }));

  it('handles long family name errors', fakeAsync(() => {
    const familyNameField = fixture.debugElement.query(By.css('#familyName'));
    simulateInput(fixture, familyNameField, randomString(81));
    tick(300);
    updateAndTick(fixture);
    expect(familyNameField.classes.unsuccessfulInput).toBeTruthy();
  }));

  it('handles long organization errors', fakeAsync(() => {
    const organizationField = fixture.debugElement.query(By.css('#organization'));
    simulateInput(fixture, organizationField, randomString(256));
    tick(300);
    updateAndTick(fixture);
    expect(organizationField.classes.unsuccessfulInput).toBeTruthy();
  }));

  it('handles long current position errors', fakeAsync(() => {
    const currentPositionField = fixture.debugElement.query(By.css('#currentPosition'));
    simulateInput(fixture, currentPositionField, randomString(256));
    tick(300);
    updateAndTick(fixture);
    expect(currentPositionField.classes.unsuccessfulInput).toBeTruthy();
  }));

});
