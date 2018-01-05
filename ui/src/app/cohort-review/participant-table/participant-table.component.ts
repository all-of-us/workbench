import {Component, OnInit} from '@angular/core';
import {ActivatedRoute} from '@angular/router';
import {State} from 'clarity-angular';

import {Participant} from '../participant.model';
import {ReviewStateService} from '../review-state.service';

import {
  CohortReview,
  CohortReviewService,
  ParticipantCohortStatus
} from 'generated';

@Component({
  selector: 'app-participant-table',
  templateUrl: './participant-table.component.html',
})
export class ParticipantTableComponent implements OnInit {
  DUMMY_DATA: Participant[];

  review: CohortReview;
  loading: boolean;

  constructor(
    private reviewAPI: CohortReviewService,
    private state: ReviewStateService,
    private route: ActivatedRoute,
  ) {}

  ngOnInit() {
    this.loading = false;

    this.state.review$
      .do(review => this.review = review)
      .pluck('participantCohortStatuses')
      .map(statusSet =>
        (<ParticipantCohortStatus[]>statusSet).map(Participant.makeRandomFromExisting))
      .subscribe(val => this.DUMMY_DATA = <Participant[]>val);
  }

  refresh(state: State) {
    const CDR_VERSION = 1;
    const {ns, wsid, cid} = this.route.parent.snapshot.params;
    const size = state.page.size;
    const page = Math.floor(state.page.from / size);

    console.dir(state);
    console.dir(this.route);

    setTimeout(() => this.loading = true, 0);
    this.reviewAPI.getParticipantCohortStatuses(ns, wsid, cid, CDR_VERSION, page, size)
      .do(r => this.loading = false)
      .subscribe(review => this.state.review.next(review));
  }
}
