import {ComponentFixture, fakeAsync, TestBed, tick} from '@angular/core/testing';

import {ClarityModule} from '@clr/angular';

import {ErrorHandlingServiceStub} from 'testing/stubs/error-handling-service-stub';
import {ServerConfigServiceStub} from 'testing/stubs/server-config-service-stub';
import {StatusCheckServiceStub} from 'testing/stubs/status-check-service-stub';

import {
  updateAndTick
} from 'testing/test-helpers';

import {ErrorHandlingService} from 'app/services/error-handling.service';
import {ServerConfigService} from 'app/services/server-config.service';
import {StatusCheckService} from 'app/services/status-check.service';


import {ErrorHandlerComponent} from 'app/pages/error-handler/component';

describe('ErrorHandlerComponent', () => {
  let fixture: ComponentFixture<ErrorHandlerComponent>;
  beforeEach(fakeAsync(() => {
    TestBed.configureTestingModule({
      imports: [
        ClarityModule.forRoot()
      ],
      declarations: [
        ErrorHandlerComponent,
      ],
      providers: [
        {provide: ErrorHandlingService, useValue: new ErrorHandlingServiceStub()},
        {
          provide: ServerConfigService,
          useValue: new ServerConfigServiceStub({
            gsuiteDomain: 'fake-research-aou.org'
          })
        },
        {provide: StatusCheckService, useValue: new StatusCheckServiceStub()}
      ]
    }).compileComponents().then(() => {
      fixture = TestBed.createComponent(ErrorHandlerComponent);
      tick();
    });
  }));

  it('should render', fakeAsync(() => {
    updateAndTick(fixture);
    expect(fixture).toBeTruthy();
  }));
});
