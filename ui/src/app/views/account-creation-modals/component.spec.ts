import {ComponentFixture, fakeAsync, TestBed, tick} from '@angular/core/testing';
import {FormsModule} from '@angular/forms';

import {ClarityModule} from '@clr/angular';

import {ProfileService} from 'generated';

import {ProfileServiceStub} from 'testing/stubs/profile-service-stub';

import {
  updateAndTick
} from '../../../testing/test-helpers';

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
        { provide: ProfileService, useValue: new ProfileServiceStub() },
      ]
    }).compileComponents().then(() => {
      fixture = TestBed.createComponent(AccountCreationModalsComponent);
      tick();
    });
  }));

  it('should render', fakeAsync(() => {
    updateAndTick(fixture);
    expect(fixture).toBeTruthy();
  }));
});
