import {ComponentFixture, fakeAsync, TestBed} from '@angular/core/testing';
import {ReactiveFormsModule} from '@angular/forms';
import {ClarityModule} from '@clr/angular';
import {CreateReviewPage} from 'app/cohort-review/create-review-page/create-review-page';
import {Participant} from 'app/cohort-review/participant.model';
import {Observable} from 'rxjs/Observable';
import {ParticipantStatusComponent} from './participant-status.component';

import {CohortReviewService, CohortStatus} from 'generated';


class ApiSpy {
  updateParticipantCohortStatus = jasmine
    .createSpy('updateParticipantCohortStatus')
    .and
    .returnValue(Observable.of({participantId: 1,
      status: CohortStatus.INCLUDED,
      birthDate: '12-31-1969'}));
}

const participant: Participant = new Participant({
  participantId: 1,
  status: CohortStatus.NOTREVIEWED,
  birthDate: '12-31-1969'
});

describe('ParticipantStatusComponent', () => {
  let component: ParticipantStatusComponent;
  let fixture: ComponentFixture<ParticipantStatusComponent>;
  let cohortReviewService: CohortReviewService;

  beforeEach(fakeAsync(() => {
    TestBed.configureTestingModule({
      declarations: [CreateReviewPage, ParticipantStatusComponent],
      imports: [ReactiveFormsModule, ClarityModule],
      providers: [
        {provide: CohortReviewService, useValue: new ApiSpy()},
      ],
    }).compileComponents().then((resp) => {
      fixture = TestBed.createComponent(ParticipantStatusComponent);

      component = fixture.componentInstance;
      component.participant = participant;
      fixture.detectChanges();

      cohortReviewService = TestBed.get(CohortReviewService);
    });
  }));

  it('Should render', () => {
    expect(component).toBeTruthy();
  });
});
