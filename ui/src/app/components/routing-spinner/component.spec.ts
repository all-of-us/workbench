import {ComponentFixture, fakeAsync, TestBed, tick} from '@angular/core/testing';
import {RouterTestingModule} from '@angular/router/testing';

import {ClarityModule} from '@clr/angular';

import {
  updateAndTick
} from 'testing/test-helpers';

import {RoutingSpinnerComponent} from 'app/components/routing-spinner/component';
import {registerApiClient} from 'app/services/swagger-fetch-clients';
import {ConfigApi} from 'generated/fetch';
import {ConfigApiStub} from 'testing/stubs/config-api-stub';

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
      ]
    }).compileComponents().then(() => {
      fixture = TestBed.createComponent(RoutingSpinnerComponent);
      tick();
    });

    registerApiClient(ConfigApi, new ConfigApiStub());
  }));

  it('should render', fakeAsync(() => {
    updateAndTick(fixture);
    expect(fixture).toBeTruthy();
  }));
});
