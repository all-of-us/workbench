import {Component, OnDestroy, OnInit} from '@angular/core';
import {ActivatedRoute, Router} from '@angular/router';
import {ClrDatagridStateInterface} from '@clr/angular';
import {Subscription} from 'rxjs/Subscription';

import {ChoiceFilterComponent} from '../choice-filter/choice-filter.component';
import {Participant} from '../participant.model';
import {ReviewStateService} from '../review-state.service';

import {
  Cohort,
  CohortReview,
  CohortReviewService,
  ConceptIdName,
  Filter,
  Operator,
  ParticipantCohortStatus,
  ParticipantCohortStatusColumns as Columns,
  ParticipantCohortStatusesRequest as Request,
  ParticipantDemographics,
  SortOrder,
  Workspace,
} from 'generated';

const CDR_VERSION = 1;

function isChoiceFilter(filter): filter is ChoiceFilterComponent {
  return (filter instanceof ChoiceFilterComponent);
}


@Component({
  selector: 'app-participant-table',
  templateUrl: './participant-table.component.html',
})
export class ParticipantTableComponent implements OnInit, OnDestroy {

  readonly ColumnEnum = Columns;
  readonly ReverseColumnEnum = {
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
  concepts: ParticipantDemographics;
  genders: string[] = [];
  races: string[] = [];
  ethnicities: string[] = [];

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

    const {concepts} = this.route.snapshot.data;
    this.concepts = concepts;
    this.races = this.extractDemographics(concepts.raceList);
    this.genders = this.extractDemographics(concepts.genderList);
    this.ethnicities = this.extractDemographics(concepts.ethnicityList);
  }

  ngOnDestroy() {
    this.subscription.unsubscribe();
  }

  refresh(state: ClrDatagridStateInterface) {
    setTimeout(() => this.loading = true, 0);
    console.log('Datagrid state: ');
    console.dir(state);

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
      query.sortColumn = this.ReverseColumnEnum[sortby];
      query.sortOrder = state.sort.reverse
        ? SortOrder.Desc
        : SortOrder.Asc;
    }

    if (state.filters) {
      for (const filter of state.filters) {
        if (isChoiceFilter(filter)) {
          query.filters.items.push(...filter.toFilters());
        } else {
          const {property, value} = <Partial<Filter>>filter;
          const operator = Operator.Equal;
          query.filters.items.push(<Filter>{property, value, operator});
        }
      }
    }

    const {ns, wsid, cid} = this.pathParams;

    console.log('Participant page request parameters:');
    console.dir(query);

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

  private extractDemographics(arr: ConceptIdName[]): string[] {
    const names = arr.map(item => item.conceptName);
    const vals = new Set<string>(names);
    return Array.from(vals);
  }
}
