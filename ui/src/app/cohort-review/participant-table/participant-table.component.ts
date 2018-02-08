import {Component, OnDestroy, OnInit} from '@angular/core';
import {ActivatedRoute, Router} from '@angular/router';
import {ClrDatagridStateInterface} from '@clr/angular';
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
  ParticipantCohortStatusColumns as Columns,
  ParticipantCohortStatusesRequest as Request,
  SortOrder,
} from 'generated';

@Component({
  selector: 'app-participant-table',
  templateUrl: './participant-table.component.html',
})
export class ParticipantTableComponent implements OnInit, OnDestroy {

  readonly columnsEnum = {
    participantId: Columns.ParticipantId,
    gender: Columns.Gender,
    race: Columns.Race,
    ethnicity: Columns.Ethnicity,
    birthDate: Columns.BirthDate,
    status: Columns.Status
  };

  participants: Participant[];

  review: CohortReview;
  loading: boolean;
  subscription: Subscription;

  constructor(
    private reviewAPI: CohortReviewService,
    private state: ReviewStateService,
    private route: ActivatedRoute,
    private router: Router,
  ) {}

  ngOnInit() {
    this.loading = false;
    // console.log('Route for participant table: ');
    // console.dir(this.route);

    this.subscription = this.route.data.subscribe(data => {
      this.participants = data.participants.map(Participant.fromStatus);
    });

    this.subscription.add(this.route.parent.data.subscribe(data => {
      this.review = data.review;
    }));
  }

  ngOnDestroy() {
    this.subscription.unsubscribe();
  }

  refresh(state: ClrDatagridStateInterface) {
    // console.log('Datagrid state: ');
    // console.dir(state);

    const query = <Request>{};

    query.page = Math.floor(state.page.from / state.page.size) + 1;
    query.pageSize = state.page.size;

    if (state.sort) {
      const sortby = <string>(state.sort.by);
      query.sortColumn = this.columnsEnum[sortby];
      query.sortOrder = state.sort.reverse
        ? SortOrder.Desc
        : SortOrder.Asc;
    }

    if (state.filters) {
      // TODO(jms) - do filter stuff here
    }

    this.router.navigate(['.'], {relativeTo: this.route, queryParams: query});
  }
}
