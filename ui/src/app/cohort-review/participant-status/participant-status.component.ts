import {Component, OnDestroy, OnInit} from '@angular/core';
import {FormControl} from '@angular/forms';
import {ActivatedRoute} from '@angular/router';
import {Observable} from 'rxjs/Observable';
import {Subscription} from 'rxjs/Subscription';

import {Participant} from '../participant.model';
import {ReviewStateService} from '../review-state.service';

import {
  CohortReview,
  CohortReviewService,
  CohortStatus,
  ModifyCohortStatusRequest,
  ParticipantCohortStatus,
} from 'generated';

@Component({
  selector: 'app-participant-status',
  templateUrl: './participant-status.component.html',
})
export class ParticipantStatusComponent implements OnInit, OnDestroy {

  readonly CohortStatus = CohortStatus;

  statusControl = new FormControl();
  subscription: Subscription;
  changingStatus = false;

  private _participant: Participant | null;

  cohortStatusList(): Array<any> {
    const statuses = new Array<any>();
      statuses.push({
        key: '',
        value: 'Select a Status'});
      statuses.push({
        key: CohortStatus.EXCLUDED,
        value: Participant.formatStatusForText(CohortStatus.EXCLUDED)});
      statuses.push({
        key: CohortStatus.INCLUDED,
        value: Participant.formatStatusForText(CohortStatus.INCLUDED)});
      statuses.push({
        key: CohortStatus.NEEDSFURTHERREVIEW,
        value: Participant.formatStatusForText(CohortStatus.NEEDSFURTHERREVIEW)
      });

    return statuses;
  }

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

  constructor(
    private state: ReviewStateService,
    private reviewAPI: CohortReviewService,
    private route: ActivatedRoute,
  ) {}

  ngOnInit() {
    this.subscription = this.state.participant$
      .subscribe(participant => {
        this.participant = participant;
        this.statusControl.setValue(this.participant
            ? this.participant.status : '', {emitEvent: false});
      });

    const participantId = this.state.participant$
      .filter(participant => participant !== null)
      .map(participant => participant.id);

    const statusChanger = this.statusControl.valueChanges
      .withLatestFrom(participantId)
      .switchMap(this.callApi)
      .subscribe(this.emit);

    this.subscription.add(statusChanger);
  }

  ngOnDestroy() {
    this.subscription.unsubscribe();
  }

  private callApi = ([status, pid]): Observable<ParticipantCohortStatus> => {
    if (status === '') {
      return Observable.of(this.participant);
    }
    this.changingStatus = true;
    const request = <ModifyCohortStatusRequest>{status};
    const {ns, wsid, cid} = this.route.snapshot.params;
    const cdrid = this.route.snapshot.data.workspace.cdrVersionId;

    return this.reviewAPI.updateParticipantCohortStatus(ns, wsid, cid, cdrid, pid, request);
  }

  private emit = (newStatus: ParticipantCohortStatus) => {
    this.changingStatus = false;
    const participant = Participant.fromStatus(newStatus);
    this.state.participant.next(participant);

    /* TODO (jms) replace this with an action to refresh the table? */
    this.state.review$
      .take(1)
      .map((review: CohortReview) => {
        const index = review.participantCohortStatuses.findIndex(
          ({participantId}) => participantId === newStatus.participantId
        );
        if (index >= 0) {
          review.participantCohortStatuses.splice(index, 1, newStatus);
        }
        return review;
      })
      .subscribe(r => this.state.review.next(r));
  }
}
