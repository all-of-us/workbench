import {ComponentFixture, fakeAsync, TestBed, tick} from '@angular/core/testing';

import {ClarityModule} from '@clr/angular';

import {ServerConfigServiceStub} from 'testing/stubs/server-config-service-stub';

import {
  updateAndTick
} from 'testing/test-helpers';

import {ServerConfigService} from 'app/services/server-config.service';

import {PageTemplateSignedOutComponent} from 'app/pages/login/page-template-signed-out/component';

describe('PageTemplateSignedOutComponent', () => {
  let fixture: ComponentFixture<PageTemplateSignedOutComponent>;
  beforeEach(fakeAsync(() => {
    TestBed.configureTestingModule({
      imports: [
        ClarityModule.forRoot()
      ],
      declarations: [
        PageTemplateSignedOutComponent,
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
      fixture = TestBed.createComponent(PageTemplateSignedOutComponent);
      tick();
    });
  }));

  it('should render', fakeAsync(() => {
    updateAndTick(fixture);
    expect(fixture).toBeTruthy();
  }));
});
