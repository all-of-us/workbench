import {async, ComponentFixture, TestBed} from '@angular/core/testing';
import {ReactiveFormsModule} from '@angular/forms';
import {ActivatedRoute, Router} from '@angular/router';
import {ClarityModule} from '@clr/angular';

import {EditModalComponent} from './component';

import {CohortsServiceStub} from 'testing/stubs/cohort-service-stub';

import {CohortsService, ConceptSetsService} from 'generated';

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

describe('CohortEditModalComponent', () => {
  let component: EditModalComponent;
  let fixture: ComponentFixture<EditModalComponent>;

  beforeEach(async(() =>
    TestBed.configureTestingModule({
      imports: [
        ClarityModule.forRoot(),
        ReactiveFormsModule,
      ],
      declarations: [EditModalComponent],
      providers: [
        { provide: Router, useClass: RouterStub },
        { provide: CohortsService, useClass: CohortsServiceStub },
        { provider: ConceptSetsService },
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
    fixture = TestBed.createComponent(EditModalComponent);
    component = fixture.componentInstance;
  });

  it('Should render without errors', () => {
    // fully initializes the component for rendering
    fixture.detectChanges();
    expect(component).toBeTruthy();
  });
});
