import {async, TestBed} from '@angular/core/testing';
import {FormsModule} from '@angular/forms';
import {RouterTestingModule} from '@angular/router/testing';

import {ClarityModule} from '@clr/angular';

import {SignInService} from '../../services/sign-in.service';
import {AccountCreationComponent} from '../account-creation/component';
import {AppComponent} from '../app/component';
import {InvitationKeyComponent} from '../invitation-key/component';
import {RoutingSpinnerComponent} from '../routing-spinner/component';

import {ProfileServiceStub} from 'testing/stubs/profile-service-stub';

import {AuthDomainService, CohortsService, ProfileService} from 'generated';

describe('AppComponent', () => {

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      imports: [
        RouterTestingModule,
        FormsModule,
        ClarityModule.forRoot()
      ],
      declarations: [
        AccountCreationComponent,
        AppComponent,
        InvitationKeyComponent,
        RoutingSpinnerComponent,
      ],
      providers: [
        { provide: AuthDomainService, useValue: {} },
        { provide: SignInService, useValue: {} },
        { provide: CohortsService, useValue: {} },
        { provide: ProfileService, useValue: new ProfileServiceStub() }
      ] }).compileComponents();
  }));

  it('should create the app', async(() => {
    const fixture = TestBed.createComponent(AppComponent);
    const app = fixture.debugElement.componentInstance;
    expect(app).toBeTruthy();
  }));

});
