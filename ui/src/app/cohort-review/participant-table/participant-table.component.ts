import {Component, OnDestroy, OnInit} from '@angular/core';
import {ActivatedRoute, Router} from '@angular/router';
import {ClrDatagridStateInterface} from '@clr/angular';
import {Subscription} from 'rxjs/Subscription';

import {Participant} from '../participant.model';
import {ReviewStateService} from '../review-state.service';

const CDR_VERSION = 1;

import {
  Cohort,
  CohortReview,
  CohortReviewService,
  Filter,
  Operator,
  ParticipantCohortStatus,
  ParticipantCohortStatusColumns as Columns,
  ParticipantCohortStatusesRequest as Request,
  SortOrder,
  Workspace,
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
    this.subscription = this.state.review$.subscribe(review => {
      this.review = review;
      this.participants = review.participantCohortStatuses.map(Participant.fromStatus);
    });
  }

  ngOnDestroy() {
    this.subscription.unsubscribe();
  }

  refresh(state: ClrDatagridStateInterface) {
    setTimeout(() => this.loading = true, 0);
    // console.log('Datagrid state: ');
    // console.dir(state);

    /* Populate the query with page / pagesize and then defaults */
    const query = <Request>{
      page: Math.floor(state.page.from / state.page.size),
      pageSize: state.page.size,
      sortColumn: Columns.ParticipantId,
      sortOrder: SortOrder.Asc,
      filters: {items: []},
    };

    if (state.sort) {
      const sortby = <string>(state.sort.by);
      query.sortColumn = this.columnsEnum[sortby];
      query.sortOrder = state.sort.reverse
        ? SortOrder.Desc
        : SortOrder.Asc;
    }

    if (state.filters) {
      query.filters.items = <Filter[]>(state.filters.map(
        ({property, value}: any) => (<Filter>{property, value, operator: Operator.Equal})
      ));
    }

    const {ns, wsid, cid} = this.pathParams;

    // console.log('Participant page request parameters:');
    // console.dir(query);

    return this.reviewAPI
      .getParticipantCohortStatuses(ns, wsid, cid, CDR_VERSION, query)
      .do(_ => this.loading = false)
      .subscribe(review => this.state.review.next(review));
  }

  private get pathParams() {
    const paths = this.route.snapshot.pathFromRoot;
    const params: any = paths.reduce((p, r) => ({...p, ...r.params}), {});

    const ns: Workspace['namespace'] = params.ns;
    const wsid: Workspace['id'] = params.wsid;
    const cid: Cohort['id'] = +(params.cid);
    return {ns, wsid, cid};
  }
}
