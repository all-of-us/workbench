import {ComponentFixture, fakeAsync, TestBed, tick} from '@angular/core/testing';
import {FormsModule} from '@angular/forms';
import {RouterTestingModule} from '@angular/router/testing';

import {ClarityModule} from '@clr/angular';

import {
  DataAccessLevel,
  ProfileService,
} from 'generated';

import {ProfileServiceStub} from 'testing/stubs/profile-service-stub';
import {ServerConfigServiceStub} from 'testing/stubs/server-config-service-stub';
import {SignInServiceStub} from 'testing/stubs/sign-in-service-stub';

import {
  updateAndTick
} from '../../../testing/test-helpers';
import {ServerConfigService} from '../../services/server-config.service';
import {SignInService} from '../../services/sign-in.service';

import {AccountCreationModalsComponent} from '../account-creation-modals/component';
import {AccountCreationSuccessComponent} from '../account-creation-success/component';
import {AccountCreationComponent} from '../account-creation/component';
import {LoginComponent} from '../login/component';

class AccountCreationSuccessPage {
  fixture: ComponentFixture<AccountCreationSuccessComponent>;
  component: AccountCreationSuccessComponent;

  constructor(testBed: typeof TestBed) {
    this.fixture = testBed.createComponent(AccountCreationSuccessComponent);
    this.component = this.fixture.componentInstance;
    this.readPageData();
  }

  readPageData() {
    updateAndTick(this.fixture);
  }
}


describe('AccountCreationSuccessComponent', () => {
  let page: AccountCreationSuccessPage;
  beforeEach(fakeAsync(() => {
    TestBed.configureTestingModule({
      imports: [
        RouterTestingModule,
        FormsModule,
        ClarityModule.forRoot()
      ],
      declarations: [
        AccountCreationModalsComponent,
        AccountCreationSuccessComponent
      ],
      providers: [
        { provide: AccountCreationComponent, useValue: {profile: {
          username: '',
          enabledInFireCloud: false,
          dataAccessLevel: DataAccessLevel.Unregistered,
          givenName: '',
          familyName: '',
          contactEmail: ''
        }}},
        { provide: LoginComponent, useValue: {}},
        { provide: ProfileService, useValue: new ProfileServiceStub },
        {
          provide: ServerConfigService,
          useValue: new ServerConfigServiceStub({
            gsuiteDomain: 'fake-research-aou.org'
          })
        },
        { provide: SignInService, useValue: new SignInServiceStub()}
      ]
    }).compileComponents().then(() => {
      page = new AccountCreationSuccessPage(TestBed);
      tick();
    });
  }));

  it('should render', fakeAsync(() => {
    page.readPageData();
    expect(page.fixture).toBeTruthy();
  }));
});
