import {ComponentFixture, fakeAsync, TestBed} from '@angular/core/testing';
import {ReactiveFormsModule} from '@angular/forms';
import {ActivatedRoute} from '@angular/router';
import {Observable} from 'rxjs/Observable';

import {ParticipantStatusComponent} from './participant-status.component';

import {CohortReviewService, CohortStatus} from 'generated';
import {ReviewStateService} from '../review-state.service';

import {updateAndTick} from '../../../testing/test-helpers';
import {Participant} from '../participant.model';

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
    .returnValue(Observable.of({participantId: 1, status: CohortStatus.INCLUDED, birthDate: 1}));
}

const PARTICIPANT: Participant =
  new Participant({participantId: 1, status: CohortStatus.NOTREVIEWED, birthDate: 1});

describe('ParticipantStatusComponent', () => {
  let component: ParticipantStatusComponent;
  let fixture: ComponentFixture<ParticipantStatusComponent>;
  let reviewStateService: ReviewStateService;
  let cohortReviewService: CohortReviewService;

  beforeEach(fakeAsync(() => {
    TestBed.configureTestingModule({
      imports: [ReactiveFormsModule],
      declarations: [ParticipantStatusComponent],
      providers: [
        {provide: ReviewStateService, useClass: ReviewStateService},
        {provide: CohortReviewService, useValue: new ApiSpy()},
        {provide: ActivatedRoute, useClass: StubRoute}
      ],
    }).compileComponents().then((resp) => {
      fixture = TestBed.createComponent(ParticipantStatusComponent);

      component = fixture.componentInstance;

      reviewStateService = TestBed.get(ReviewStateService);
      cohortReviewService = TestBed.get(CohortReviewService);
    });
  }));

  it('Should render', () => {
    expect(component).toBeTruthy();
  });

  it('Should make api call for save', fakeAsync(() => {
    component.ngOnInit();
    expect(component.participant).toBe(null);
    reviewStateService.participant.next(PARTICIPANT);
    fixture.detectChanges();
    expect(component.participant).toBe(PARTICIPANT);
    expect(component.statusControl.value).toBe(CohortStatus.NOTREVIEWED);

    // Set up an API spy
    const spy = fixture.debugElement.injector.get(CohortReviewService);

    component.statusControl.setValue(CohortStatus.INCLUDED);
    updateAndTick(fixture);
    expect(spy.updateParticipantCohortStatus).toHaveBeenCalled();
  }));
});
