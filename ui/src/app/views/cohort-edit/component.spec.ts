import {async, ComponentFixture, TestBed} from '@angular/core/testing';
import {ReactiveFormsModule} from '@angular/forms';
import {ActivatedRoute, Router} from '@angular/router';
import {ClarityModule} from '@clr/angular';
import {Observable} from 'rxjs/Observable';

import {CohortEditComponent} from './component';

import {CohortsServiceStub} from 'testing/stubs/cohort-service-stub';

import {CohortsService} from 'generated';

class RouterStub {
  navigate(...args) {
    return args;
  }
}

class ActivatedRouteStub {
  snapshot: any;

  constructor(cohortStub: CohortsServiceStub) {
    const cohort = cohortStub.cohorts[0];
    const workspace = cohortStub.workspaces[0];
    this.snapshot = {data: {cohort, workspace}};
  }
}

describe('CohortEditComponent', () => {
  let component: CohortEditComponent;
  let fixture: ComponentFixture<CohortEditComponent>;

  beforeEach(async(() =>
    TestBed.configureTestingModule({
      imports: [
        ClarityModule.forRoot(),
        ReactiveFormsModule,
      ],
      declarations: [CohortEditComponent],
      providers: [
        { provide: Router, useClass: RouterStub },
        { provide: CohortsService, useClass: CohortsServiceStub },
        {
          provide: ActivatedRoute,
          deps: [CohortsServiceStub],
          useClass: ActivatedRouteStub
        },
        CohortsServiceStub,
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
