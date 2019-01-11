import {ComponentFixture, fakeAsync, TestBed, tick} from '@angular/core/testing';

import {ClarityModule} from '@clr/angular';

import {ServerConfigServiceStub} from 'testing/stubs/server-config-service-stub';

import {
  updateAndTick
} from '../../../testing/test-helpers';

import {ServerConfigService} from '../../services/server-config.service';

import {SignInTemplateComponent} from './/component';

describe('PageTemplateSignedOutComponent', () => {
  let fixture: ComponentFixture<SignInTemplateComponent>;
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
        },
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
