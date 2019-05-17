import {CUSTOM_ELEMENTS_SCHEMA} from '@angular/core';
import {async, TestBed} from '@angular/core/testing';
import {FormsModule} from '@angular/forms';
import {RouterTestingModule} from '@angular/router/testing';

import {ClarityModule} from '@clr/angular';

import {ServerConfigService} from "app/services/server-config.service";
import {SignInService} from 'app/services/sign-in.service';
import {AppComponent} from 'app/views/app/component';
import {PageTemplateSignedOutComponent} from 'app/views/page-template-signed-out/component';

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
        AppComponent,
        PageTemplateSignedOutComponent
      ],
      schemas: [CUSTOM_ELEMENTS_SCHEMA],
      providers: [
        { provide: AuthDomainService, useValue: {} },
        { provide: SignInService, useValue: {} },
        { provide: CohortsService, useValue: {} },
        { provide: ServerConfigService, useValue: {} },
        { provide: ProfileService, useValue: new ProfileServiceStub() }
      ] }).compileComponents();
  }));

  it('should create the app', async(() => {
    const fixture = TestBed.createComponent(AppComponent);
    const app = fixture.debugElement.componentInstance;
    expect(app).toBeTruthy();
  }));

});
