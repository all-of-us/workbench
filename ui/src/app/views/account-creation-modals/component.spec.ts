import {ComponentFixture, fakeAsync, TestBed, tick} from '@angular/core/testing';
import {FormsModule} from '@angular/forms';

import {ClarityModule} from '@clr/angular';

import {
  ProfileApi
} from 'generated/fetch';

import {ProfileApiStub} from 'testing/stubs/profile-service-stub';

import {
  updateAndTick
} from 'testing/test-helpers';

import {AccountCreationModalsComponent} from './component';

describe('AccountCreationModalsComponent', () => {
  let fixture: ComponentFixture<AccountCreationModalsComponent>;
  beforeEach(fakeAsync(() => {
    TestBed.configureTestingModule({
      imports: [
        FormsModule,
        ClarityModule.forRoot()
      ],
      declarations: [
        AccountCreationModalsComponent
      ],
      providers: [
        { provide: ProfileApi, useValue: new ProfileApiStub() },
      ]
    }).compileComponents().then(() => {
      fixture = TestBed.createComponent(AccountCreationModalsComponent);
      tick();
    });
  }));

  fit('should render', fakeAsync(() => {
    updateAndTick(fixture);
    expect(fixture).toBeTruthy();
  }));
});
