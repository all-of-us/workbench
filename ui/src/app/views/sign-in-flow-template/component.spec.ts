import {ComponentFixture, fakeAsync, TestBed, tick} from '@angular/core/testing';

import {ClarityModule} from '@clr/angular';

import {ServerConfigServiceStub} from 'testing/stubs/server-config-service-stub';

import {
  updateAndTick
} from '../../../testing/test-helpers';

import {ServerConfigService} from '../../services/server-config.service';

import {SignInServiceStub} from '../../../testing/stubs/sign-in-service-stub';
import {SignInService} from '../../services/sign-in.service';
import {SignInTemplateComponent} from './/component';

describe('PageTemplateSignedOutComponent', () => {
  let fixture: ComponentFixture<SignInTemplateComponent>;
  const signInService: SignInService;

  beforeEach(fakeAsync(() => {
    TestBed.configureTestingModule({
      imports: [
        ClarityModule.forRoot()
      ],
      declarations: [
        SignInTemplateComponent,
      ],
      providers: [
        {
          provide: ServerConfigService,
          useValue: new ServerConfigServiceStub({
            gsuiteDomain: 'fake-research-aou.org'
          })
        }, { provide: SignInService, useValue: new SignInServiceStub() }
      ]
    }).compileComponents().then(() => {
      fixture = TestBed.createComponent(SignInTemplateComponent);
      tick();
    });
  }));

  // it('should render', fakeAsync(() => {
  //   updateAndTick(fixture);
  //   expect(fixture).toBeTruthy();
  // }));
});
