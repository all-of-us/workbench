import {TestBed, async} from '@angular/core/testing';
import {Title} from '@angular/platform-browser';
import {RouterTestingModule} from '@angular/router/testing';

import {BehaviorSubject} from 'rxjs/BehaviorSubject';
import {ClarityModule} from 'clarity-angular';

import {AppComponent} from 'app/views/app/component';
import {SignInService} from 'app/services/sign-in.service';
import {ProfileServiceStub} from 'testing/stubs/profile-service-stub';

import {CohortsService} from 'generated';
import {ProfileService} from 'generated';

describe('AppComponent', () => {

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      imports: [
        RouterTestingModule,
        ClarityModule.forRoot()
      ],
      declarations: [
        AppComponent
      ],
      providers: [
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
