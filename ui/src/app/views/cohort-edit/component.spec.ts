import {async, ComponentFixture, TestBed} from '@angular/core/testing';
import {FormsModule} from '@angular/forms';
import {ActivatedRoute, Router} from '@angular/router';
import {ClarityModule} from '@clr/angular';
import {Observable} from 'rxjs/Observable';

import {ErrorHandlingService} from '../../services/error-handling.service';
import {CohortEditComponent} from './component';

import {CohortsServiceStub} from 'testing/stubs/cohort-service-stub';
import {ErrorHandlingServiceStub} from 'testing/stubs/error-handling-service-stub';

import {CohortsService} from 'generated';

class RouterStub {
  navigate(...args) {
    return args;
  }
}

class ActivatedRouteStub {
  params = Observable.of({
    ns: 'test-namespace',
    wsid: 'test-workspace-id',
    cid: 1
  });
}

describe('CohortEditComponent', () => {
  let component: CohortEditComponent;
  let fixture: ComponentFixture<CohortEditComponent>;

  beforeEach(async(() =>
    TestBed.configureTestingModule({
      imports: [
        ClarityModule.forRoot(),
        FormsModule,
      ],
      declarations: [CohortEditComponent],
      providers: [
        { provide: Router, useClass: RouterStub },
        { provide: ActivatedRoute, useClass: ActivatedRouteStub },
        { provide: CohortsService, useClass: CohortsServiceStub },
        { provide: ErrorHandlingService, useClass: ErrorHandlingServiceStub },
      ]
    }).compileComponents()
  ));

  beforeEach(() => {
    fixture = TestBed.createComponent(CohortEditComponent);
    component = fixture.componentInstance;
  });

  it('Should render without errors', () => {
    // fully initializes the component for rendering
    fixture.detectChanges();
    expect(component).toBeTruthy();
  });
});
