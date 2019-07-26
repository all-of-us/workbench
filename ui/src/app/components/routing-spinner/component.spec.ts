import {ComponentFixture, fakeAsync, TestBed, tick} from '@angular/core/testing';
import {RouterTestingModule} from '@angular/router/testing';

import {ClarityModule} from '@clr/angular';

import {ServerConfigServiceStub} from 'testing/stubs/server-config-service-stub';

import {
  updateAndTick
} from 'testing/test-helpers';

import {ServerConfigService} from 'app/services/server-config.service';

import {RoutingSpinnerComponent} from 'app/components/routing-spinner/component';

describe('RoutingSpinnerComponent', () => {
  let fixture: ComponentFixture<RoutingSpinnerComponent>;
  beforeEach(fakeAsync(() => {
    TestBed.configureTestingModule({
      imports: [
        RouterTestingModule,
        ClarityModule.forRoot()
      ],
      declarations: [
        RoutingSpinnerComponent,
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
      fixture = TestBed.createComponent(RoutingSpinnerComponent);
      tick();
    });
  }));

  it('should render', fakeAsync(() => {
    updateAndTick(fixture);
    expect(fixture).toBeTruthy();
  }));
});
