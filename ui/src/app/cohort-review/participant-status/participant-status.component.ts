import {Component, OnDestroy, OnInit} from '@angular/core';
import {FormControl} from '@angular/forms';
import {Observable} from 'rxjs/Observable';
import {Subscription} from 'rxjs/Subscription';

import {ReviewStateService} from '../review-state.service';

import {
  CohortReviewService,
  CohortStatus,
  ModifyCohortStatusRequest,
  ParticipantCohortStatus,
} from 'generated';

@Component({
  selector: 'app-participant-status',
  templateUrl: './participant-status.component.html',
  styleUrls: ['./participant-status.component.css']
})
export class ParticipantStatusComponent implements OnInit, OnDestroy {

  readonly CohortStatus = CohortStatus;
  statusControl = new FormControl();

  private _participant: ParticipantCohortStatus | undefined;

  set participant(value) {
    this._participant = value;
    if (value !== null) {
      this.statusControl.enable({emitEvent: false});
    } else {
      this.statusControl.disable({emitEvent: false});
    }
  }

  get participant() {
    return this._participant;
  }

  private subscription: Subscription;
  private changingStatus = false;

  constructor(
    private state: ReviewStateService,
    private reviewAPI: CohortReviewService,
  ) {}

  ngOnInit() {
    this.subscription = this.state.participant.subscribe(
      participant => this.participant = participant
    );

    const participantId = this.state.participant
      .filter(participant => participant !== null)
      .map(participant => participant.participantId);

    const statusChanger = this.statusControl.valueChanges
      .withLatestFrom(participantId, this.state.context)
      .switchMap(this._callApi)
      .subscribe(this._emit);

    this.subscription.add(statusChanger);
  }

  ngOnDestroy() {
    this.subscription.unsubscribe();
  }

  private _emit = (newStatus: ParticipantCohortStatus) => {
    this.changingStatus = false;
    this.state.participant.next(newStatus);
    this.state.review
      .take(1)
      .map(review => {
        const index = review.participantCohortStatuses.findIndex(
          ({participantId}) => participantId === newStatus.participantId
        );
        if (index >= 0) {
          review.participantCohortStatuses.splice(index, 1, newStatus);
        }
        return review;
      })
      .subscribe(this.state.review);
  }

  private _callApi = ([status, participantId, context]): Observable<ParticipantCohortStatus> => {
    this.changingStatus = true;
    const request = <ModifyCohortStatusRequest>{status};
    const {workspaceNamespace, workspaceId, cohortId, cdrVersion} = context;

    return this.reviewAPI.updateParticipantCohortStatus(
      workspaceNamespace,
      workspaceId,
      cohortId,
      cdrVersion,
      participantId,
      request
    );
  }
}
