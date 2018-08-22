import {ComponentFixture, fakeAsync, TestBed} from '@angular/core/testing';
import {ReactiveFormsModule} from '@angular/forms';
import {ActivatedRoute} from '@angular/router';
import {ClarityModule} from '@clr/angular';
import {CohortReviewService, CohortStatus} from 'generated';
import {ParticipantStatusComponent} from './participant-status.component';
import {Participant} from '../participant.model';
import {Observable} from 'rxjs/Observable';
import {ReviewStateService} from '../review-state.service';
import {updateAndTick} from '../../../testing/test-helpers';

class StubRoute {
  snapshot = {
    params: {
      ns: 'workspaceNamespace',
      wsid: 'workspaceId',
      cid: 1
    },
    data: {
      workspace: {cdrVersionId: 1}
    }
  };
}

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
  let reviewStateService: ReviewStateService;
  let cohortReviewService: CohortReviewService;

  beforeEach(fakeAsync(() => {
    TestBed.configureTestingModule({
      imports: [ReactiveFormsModule, ClarityModule],
      declarations: [ParticipantStatusComponent],
      providers: [
        {provide: ReviewStateService, useClass: ReviewStateService},
        {provide: CohortReviewService, useValue: new ApiSpy()},
        {provide: ActivatedRoute, useClass: StubRoute}
      ],
    }).compileComponents().then((resp) => {
      fixture = TestBed.createComponent(ParticipantStatusComponent);

      component = fixture.componentInstance;
      component.participant = participant;
      fixture.detectChanges();

      reviewStateService = TestBed.get(ReviewStateService);
      cohortReviewService = TestBed.get(CohortReviewService);
    });
  }));

  it('Should render', () => {
    expect(component).toBeTruthy();
  });
});
