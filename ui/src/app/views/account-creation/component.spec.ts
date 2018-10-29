import {DebugElement} from '@angular/core';
import {ComponentFixture, fakeAsync, TestBed, tick} from '@angular/core/testing';
import {FormsModule} from '@angular/forms';
import {By} from '@angular/platform-browser';

import {ClarityModule} from '@clr/angular';

import {ProfileService} from 'generated';

import {ProfileServiceStub} from 'testing/stubs/profile-service-stub';
import {ServerConfigServiceStub} from 'testing/stubs/server-config-service-stub';
import {randomString} from '../../utils';

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

class AccountCreationPage {
  fixture: ComponentFixture<AccountCreationComponent>;
  component: AccountCreationComponent;
  usernameField: DebugElement;
  givenNameField: DebugElement;
  familyName: DebugElement;
  currentPosition: DebugElement;
  organization: DebugElement;

  constructor(testBed: typeof TestBed) {
    this.fixture = testBed.createComponent(AccountCreationComponent);
    this.component = this.fixture.componentInstance;
    this.readPageData();
  }

  readPageData() {
    updateAndTick(this.fixture);
    this.usernameField = this.fixture.debugElement.query(By.css('#username'));
    this.givenNameField = this.fixture.debugElement.query(By.css('#givenName'));
    this.familyName = this.fixture.debugElement.query(By.css('#familyName'));
    this.currentPosition = this.fixture.debugElement.query(By.css('#currentPosition'));
    this.organization = this.fixture.debugElement.query(By.css('#organization'));
  }
}


describe('AccountCreationComponent', () => {
  let page: AccountCreationPage;
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
      page = new AccountCreationPage(TestBed);
      tick();
    });
  }));

  it('handles selecting username', fakeAsync(() => {
    page.readPageData();
    expect(page.component.usernameFocused).toBeFalsy();
    simulateEvent(page.fixture, page.usernameField, 'focus');
    expect(page.component.usernameFocused).toBeTruthy();
    simulateEvent(page.fixture, page.usernameField, 'blur');
    expect(page.component.usernameFocused).toBeFalsy();
  }));

  it('handles each username invalidity', fakeAsync(() => {
    page.readPageData();
    // Begin with a period
    simulateEvent(page.fixture, page.usernameField, 'focus');
    simulateInput(page.fixture, page.usernameField, '.username');
    tick(300);
    updateAndTick(page.fixture);
    expect(page.fixture.debugElement.query(By.css('#username-invalid-error'))).toBeTruthy();
    expect(page.usernameField.classes.unsuccessfulInput).toBeTruthy();

    // End with a period
    simulateInput(page.fixture, page.usernameField, 'username.');
    tick(300);
    updateAndTick(page.fixture);
    expect(page.fixture.debugElement.query(By.css('#username-invalid-error'))).toBeTruthy();
    expect(page.usernameField.classes.unsuccessfulInput).toBeTruthy();

    // Contains special characters
    simulateInput(page.fixture, page.usernameField, 'user@name');
    tick(300);
    updateAndTick(page.fixture);
    expect(page.fixture.debugElement.query(By.css('#username-invalid-error'))).toBeTruthy();
    expect(page.usernameField.classes.unsuccessfulInput).toBeTruthy();


    simulateInput(page.fixture, page.usernameField, 'blah');
    tick(300);
    updateAndTick(page.fixture);
    expect(page.fixture.debugElement.query(By.css('#username-invalid-error'))).toBeFalsy();
    expect(page.usernameField.classes.unsuccessfulInput).toBeFalsy();
  }));

  it('handles long username with mismatch at end', fakeAsync(() => {
    page.readPageData();

    simulateInput(
      page.fixture, page.usernameField, 'thisisaverylongusernamewithnowspaceswillitwork t');
    tick(300);
    updateAndTick(page.fixture);
    expect(page.fixture.debugElement.query(By.css('#username-invalid-error'))).toBeTruthy();
    expect(page.usernameField.classes.unsuccessfulInput).toBeTruthy();
  }));

  it('handles long given name errors', fakeAsync(() => {
    simulateInput(
      page.fixture, page.givenNameField, randomString(81));
    tick(300);
    updateAndTick(page.fixture);
    expect(page.givenNameField.classes.unsuccessfulInput).toBeTruthy();
  }));

  it('handles long family name errors', fakeAsync(() => {
    simulateInput(
      page.fixture, page.familyName, randomString(81));
    tick(300);
    updateAndTick(page.fixture);
    expect(page.familyName.classes.unsuccessfulInput).toBeTruthy();
  }));

  it('handles long organization errors', fakeAsync(() => {
    simulateInput(
      page.fixture, page.organization, randomString(256));
    tick(300);
    updateAndTick(page.fixture);
    expect(page.organization.classes.unsuccessfulInput).toBeTruthy();
  }));

  it('handles long current position errors', fakeAsync(() => {
    simulateInput(
      page.fixture, page.currentPosition, randomString(256));
    tick(300);
    updateAndTick(page.fixture);
    expect(page.currentPosition.classes.unsuccessfulInput).toBeTruthy();
  }));

});
