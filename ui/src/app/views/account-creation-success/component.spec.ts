import {ComponentFixture, fakeAsync, TestBed, tick} from '@angular/core/testing';
import {FormsModule} from '@angular/forms';
import {RouterTestingModule} from '@angular/router/testing';

import {ClarityModule} from '@clr/angular';

import {
  ProfileService,
} from 'generated';

import {ProfileServiceStub, ProfileStubVariables} from 'testing/stubs/profile-service-stub';
import {SignInServiceStub} from 'testing/stubs/sign-in-service-stub';

import {
  updateAndTick
} from '../../../testing/test-helpers';
import {SignInService} from '../../services/sign-in.service';

import {AccountCreationSuccessComponent} from '../account-creation-success/component';
import {AccountCreationComponent} from '../account-creation/component';
import {LoginComponent} from '../login/component';

describe('AccountCreationSuccessComponent', () => {
  let fixture: ComponentFixture<AccountCreationSuccessComponent>;
  beforeEach(fakeAsync(() => {
    TestBed.configureTestingModule({
      imports: [
        RouterTestingModule,
        FormsModule,
        ClarityModule.forRoot()
      ],
      declarations: [
        AccountCreationSuccessComponent
      ],
      providers: [
        { provide: AccountCreationComponent, useValue: {
            profile: ProfileStubVariables.PROFILE_STUB
          }
        },
        { provide: LoginComponent, useValue: {}},
        { provide: ProfileService, useValue: new ProfileServiceStub },
        { provide: SignInService, useValue: new SignInServiceStub()}
      ]
    }).compileComponents().then(() => {
      fixture = TestBed.createComponent(AccountCreationSuccessComponent);
      tick();
    });
  }));

  it('should render', fakeAsync(() => {
    updateAndTick(fixture);
    expect(fixture).toBeTruthy();
  }));
});
