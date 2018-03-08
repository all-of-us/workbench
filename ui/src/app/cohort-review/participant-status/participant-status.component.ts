import {Component, Input, OnDestroy, OnInit} from '@angular/core';
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
  styleUrls: ['./participant-status.component.css']
})
export class ParticipantStatusComponent implements OnInit, OnDestroy {
  private _participant: Participant;

  @Input() set participant(p: Participant) {
    this._participant = p;
    const status = p.status === CohortStatus.NOTREVIEWED ? null : p.status;
    this.statusControl.setValue(status, {emitEvent: false});
  }

  get participant() {
    return this._participant;
  }

  readonly cohortStatusList = [{
      value: CohortStatus.INCLUDED,
      display: Participant.formatStatusForText(CohortStatus.INCLUDED)
    }, {
      value: CohortStatus.EXCLUDED,
      display: Participant.formatStatusForText(CohortStatus.EXCLUDED)
    }, {
      value: CohortStatus.NEEDSFURTHERREVIEW,
      display: Participant.formatStatusForText(CohortStatus.NEEDSFURTHERREVIEW)
  }];

  statusControl = new FormControl();
  subscription: Subscription;

  get noStatus() {
    return this.participant.status === CohortStatus.NOTREVIEWED;
  }

  get validStatuses() {
    return this.cohortStatusList.map(obj => obj.value);
  }

  constructor(
    private state: ReviewStateService,
    private reviewAPI: CohortReviewService,
    private route: ActivatedRoute,
  ) {}

  ngOnInit() {
    this.subscription = this.statusControl.valueChanges
      .filter(status => this.validStatuses.includes(status))
      .switchMap(this.callApi)
      .subscribe(this.emit);
  }

  ngOnDestroy() {
    this.subscription.unsubscribe();
  }

  private callApi = (status): Observable<ParticipantCohortStatus> => {
    const pid = this.participant.id;
    const request = <ModifyCohortStatusRequest>{status};
    const {ns, wsid, cid} = this.route.parent.snapshot.params;
    const cdrid = this.route.parent.snapshot.data.workspace.cdrVersionId;

    return this.reviewAPI.updateParticipantCohortStatus(ns, wsid, cid, cdrid, pid, request);
  }

  private emit = (newStatus: ParticipantCohortStatus) => {
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
