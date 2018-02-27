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
  styleUrls: ['./participant-status.component.css']
})
export class ParticipantStatusComponent implements OnInit, OnDestroy {

  readonly CohortStatus = CohortStatus;
  readonly cohortStatusList = [{
    key: '',
    value: 'Select a Status',
  }, {
    key: CohortStatus.EXCLUDED,
    value: Participant.formatStatusForText(CohortStatus.EXCLUDED)
  }, {
    key: CohortStatus.INCLUDED,
    value: Participant.formatStatusForText(CohortStatus.INCLUDED)
  }, {
    key: CohortStatus.NEEDSFURTHERREVIEW,
    value: Participant.formatStatusForText(CohortStatus.NEEDSFURTHERREVIEW)
  }];

  statusControl = new FormControl();
  subscription: Subscription;
  participant: Participant | null;

  constructor(private state: ReviewStateService,
              private reviewAPI: CohortReviewService,
              private route: ActivatedRoute) {
  }

  ngOnInit() {
    this.subscription = this.state.participant$
      .subscribe(participant => {
        this.participant = participant;
        const status = this.participant ? this.participant.status : '';
        this.statusControl.setValue(status, {emitEvent: false});
      });

    const participantId = this.state.participant$
      .filter(participant => participant !== null)
      .map(participant => participant.id);

    const statusChanger = this.statusControl.valueChanges
      .filter(status => status !== '')
      .withLatestFrom(participantId)
      .switchMap(this.callApi)
      .subscribe(this.emit);

    this.subscription.add(statusChanger);
  }

  ngOnDestroy() {
    this.subscription.unsubscribe();
  }

  private callApi = ([status, pid]): Observable<ParticipantCohortStatus> => {
    const request = <ModifyCohortStatusRequest>{status};
    const {ns, wsid, cid} = this.route.snapshot.params;
    const cdrid = this.route.snapshot.data.workspace.cdrVersionId;

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
