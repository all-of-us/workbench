import {ComponentFixture, fakeAsync, TestBed, tick} from '@angular/core/testing';
import {FormsModule} from '@angular/forms';
import {RouterTestingModule} from '@angular/router/testing';

import {ClarityModule} from '@clr/angular';

import {ProfileService} from 'generated';

import {ProfileServiceStub} from 'testing/stubs/profile-service-stub';
import {ServerConfigServiceStub} from 'testing/stubs/server-config-service-stub';

import {
  updateAndTick
} from '../../../testing/test-helpers';
import {ServerConfigService} from '../../services/server-config.service';

import {AccountCreationModalsComponent} from '../account-creation-modals/component';

class AccountCreationModalsPage {
  fixture: ComponentFixture<AccountCreationModalsComponent>;
  component: AccountCreationModalsComponent;

  constructor(testBed: typeof TestBed) {
    this.fixture = testBed.createComponent(AccountCreationModalsComponent);
    this.component = this.fixture.componentInstance;
    this.readPageData();
  }

  readPageData() {
    updateAndTick(this.fixture);
  }
}


describe('AccountCreationModalsComponent', () => {
  let page: AccountCreationModalsPage;
  beforeEach(fakeAsync(() => {
    TestBed.configureTestingModule({
      imports: [
        RouterTestingModule,
        FormsModule,
        ClarityModule.forRoot()
      ],
      declarations: [
        AccountCreationModalsComponent
      ],
      providers: [
        { provide: ProfileService, useValue: new ProfileServiceStub() },
        {
          provide: ServerConfigService,
          useValue: new ServerConfigServiceStub({
            gsuiteDomain: 'fake-research-aou.org'
          })
        }]
    }).compileComponents().then(() => {
      page = new AccountCreationModalsPage(TestBed);
      tick();
    });
  }));

  it('should render', fakeAsync(() => {
    page.readPageData();
    expect(page.fixture).toBeTruthy();
  }));
});
