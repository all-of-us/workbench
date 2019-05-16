import { async, ComponentFixture, TestBed } from '@angular/core/testing';
import {ReactiveFormsModule} from '@angular/forms';
import {BrowserAnimationsModule} from '@angular/platform-browser/animations';
import { RouterTestingModule } from '@angular/router/testing';
import {ClarityModule} from '@clr/angular';

import {cohortReviewStore} from 'app/cohort-review/review-state.service';
import {currentCohortStore} from 'app/utils/navigation';
import {CohortReviewService} from 'generated';
import {cohortReviewStub} from 'testing/stubs/cohort-review-service-stub';
import {CreateReviewPage} from './create-review-modal';

describe('CreateReviewPage', () => {
  let component: CreateReviewPage;
  let fixture: ComponentFixture<CreateReviewPage>;

  beforeEach(async(() => {

    TestBed.configureTestingModule({
      declarations: [ CreateReviewPage ],
      imports: [
        BrowserAnimationsModule,
        ClarityModule,
        ReactiveFormsModule,
        RouterTestingModule.withRoutes([])
      ],
      providers: [
        {provide: CohortReviewService, useValue: {}},
      ],
    })
      .compileComponents();
    currentCohortStore.next({
      name: '',
      criteria: '',
      type: '',
    });
    cohortReviewStore.next(cohortReviewStub);
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(CreateReviewPage);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
