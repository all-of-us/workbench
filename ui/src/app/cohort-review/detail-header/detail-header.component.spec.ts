import { async, ComponentFixture, TestBed } from '@angular/core/testing';
import {ClarityModule} from '@clr/angular';
import {CohortReviewService} from 'generated';

import {Participant} from 'app/cohort-review/participant.model';
import {cohortReviewStore} from 'app/cohort-review/review-state.service';
import {cohortReviewStub} from 'testing/stubs/cohort-review-service-stub';
import {DetailHeaderComponent} from './detail-header.component';

describe('DetailHeaderComponent', () => {
  let component: DetailHeaderComponent;
  let fixture: ComponentFixture<DetailHeaderComponent>;

  beforeEach(async(() => {

    TestBed.configureTestingModule({
      declarations: [ DetailHeaderComponent ],
      imports: [ClarityModule],
      providers: [
        {provide: CohortReviewService, useValue: {}},
      ],
    })
      .compileComponents();
    cohortReviewStore.next(cohortReviewStub);
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(DetailHeaderComponent);
    component = fixture.componentInstance;
    component.participant = <Participant> {id: 1};
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
