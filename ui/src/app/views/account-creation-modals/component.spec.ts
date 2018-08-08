import {DebugElement} from '@angular/core';
import {ComponentFixture, fakeAsync, TestBed, tick} from '@angular/core/testing';
import {FormsModule} from '@angular/forms';
import {By} from '@angular/platform-browser';
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
import {RoutingSpinnerComponent} from '../routing-spinner/component';

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
  let profileServiceStub: ProfileServiceStub;
  let page: AccountCreationModalsPage;
  beforeEach(fakeAsync(() => {
    profileServiceStub = new ProfileServiceStub();
    TestBed.configureTestingModule({
      imports: [
        RouterTestingModule,
        FormsModule,
        ClarityModule.forRoot()
      ],
      declarations: [
        AccountCreationModalsComponent,
        RoutingSpinnerComponent
      ],
      providers: [
        { provide: ProfileService, useValue: profileServiceStub },
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
