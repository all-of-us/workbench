import {Component, OnDestroy, OnInit} from '@angular/core';
import {ActivatedRoute} from '@angular/router';
import {State} from '@clr/angular';
import {Subscription} from 'rxjs/Subscription';

import {Participant} from '../participant.model';
import {ReviewStateService} from '../review-state.service';

const CDR_VERSION = 1;
const getProperty = filt => (<{property: string, value: string}>filt).property;
const getValue = filt => (<{property: string, value: string}>filt).value;

import {
  CohortReview,
  CohortReviewService,
  ParticipantCohortStatus,
  ParticipantCohortStatusColumns,
  ParticipantCohortStatusesRequest,
  SortOrder
} from 'generated';

@Component({
  selector: 'app-participant-table',
  templateUrl: './participant-table.component.html',
})
export class ParticipantTableComponent implements OnInit, OnDestroy {
  DUMMY_DATA: Participant[];

  review: CohortReview;
  loading: boolean;
  subscription: Subscription;

  constructor(
    private reviewAPI: CohortReviewService,
    private state: ReviewStateService,
    private route: ActivatedRoute,
  ) {}

  ngOnInit() {
    this.loading = false;

    this.subscription = this.state.review$
      .do(review => this.review = review)
      .pluck('participantCohortStatuses')
      .map(statusSet =>
        (<ParticipantCohortStatus[]>statusSet).map(Participant.makeRandomFromExisting))
      .subscribe(val => this.DUMMY_DATA = <Participant[]>val);
  }

  ngOnDestroy() {
    this.subscription.unsubscribe();
  }

  refresh(state: State) {
    const {ns, wsid, cid} = this.route.parent.snapshot.params;

    const page = Math.floor(state.page.from / state.page.size);
    const pageSize = state.page.size;

    let sortColumn = state.sort && {
      participantId: ParticipantCohortStatusColumns.ParticipantId,
      gender: ParticipantCohortStatusColumns.Gender,
      race: ParticipantCohortStatusColumns.Race,
      ethnicity: ParticipantCohortStatusColumns.Ethnicity,
      birthDate: ParticipantCohortStatusColumns.BirthDate,
      status: ParticipantCohortStatusColumns.Status
    }[<string>(state.sort.by)];

    sortColumn = sortColumn || ParticipantCohortStatusColumns.ParticipantId;

    let sortOrder = state.sort &&
      (state.sort.reverse ? SortOrder.Desc : SortOrder.Asc);

    sortOrder = sortOrder || SortOrder.Asc;

    const filterColumns = state.filters && state.filters.map(getProperty);
    const filterValues = state.filters && state.filters.map(getValue);

    console.dir(state);

    setTimeout(() => this.loading = true, 0);
      const request = {
          page: page,
          pageSize: pageSize,
          sortColumn: sortColumn,
          sortOrder: sortOrder
      };
      // TODO: build filters list here
      console.dir(request);
    this.reviewAPI.getParticipantCohortStatuses(ns, wsid, cid, CDR_VERSION,
        request)
      .do(r => this.loading = false)
      .subscribe(review => this.state.review.next(review));
  }
}
