import {ComponentFixture, fakeAsync, TestBed, tick} from '@angular/core/testing';
import {RouterTestingModule} from '@angular/router/testing';

import {ClarityModule} from '@clr/angular';

import {
  updateAndTick
} from 'testing/test-helpers';

import {InitialErrorComponent} from 'app/pages/initial-error/component';
import {registerApiClient} from "app/services/swagger-fetch-clients";
import { ConfigApi } from 'generated/fetch';
import {ConfigApiStub} from "testing/stubs/config-api-stub";

describe('InitialErrorComponent', () => {
  let fixture: ComponentFixture<InitialErrorComponent>;
  beforeEach(fakeAsync(() => {
    TestBed.configureTestingModule({
      imports: [
        RouterTestingModule,
        ClarityModule.forRoot()
      ],
      declarations: [
        InitialErrorComponent,
      ]
    }).compileComponents().then(() => {
      fixture = TestBed.createComponent(InitialErrorComponent);
      tick();
    });

    registerApiClient(ConfigApi, new ConfigApiStub());
  }));

  it('should render', fakeAsync(() => {
    updateAndTick(fixture);
    expect(fixture).toBeTruthy();
  }));
});
