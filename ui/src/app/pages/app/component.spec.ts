import {CUSTOM_ELEMENTS_SCHEMA} from '@angular/core';
import {async, TestBed} from '@angular/core/testing';
import {FormsModule} from '@angular/forms';
import {RouterTestingModule} from '@angular/router/testing';
import {ClarityModule} from '@clr/angular';

import {AppComponent} from 'app/pages/app/component';
import {SignInService} from 'app/services/sign-in.service';
import {registerApiClient} from 'app/services/swagger-fetch-clients';
import {ProfileApiStub} from 'testing/stubs/profile-api-stub';
import {ConfigApiStub} from 'testing/stubs/config-api-stub';

import {ConfigApi, ProfileApi} from 'generated/fetch';

describe('AppComponent', () => {

  beforeEach(async(() => {
    registerApiClient(ProfileApi, new ProfileApiStub());
    registerApiClient(ConfigApi, new ConfigApiStub());
    TestBed.configureTestingModule({
      imports: [
        RouterTestingModule,
        FormsModule,
        ClarityModule.forRoot()
      ],
      declarations: [
        AppComponent
      ],
      schemas: [CUSTOM_ELEMENTS_SCHEMA],
      providers: [
        { provide: SignInService, useValue: {} },
      ] }).compileComponents();
  }));

  it('should create the app', async(() => {
    const fixture = TestBed.createComponent(AppComponent);
    const app = fixture.debugElement.componentInstance;
    expect(app).toBeTruthy();
  }));

});
