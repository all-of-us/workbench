import {Component, Input, OnDestroy, OnInit} from '@angular/core';
import {ActivatedRoute} from '@angular/router';
import {ClrDatagridComparatorInterface} from '@clr/angular';
import {fromJS} from 'immutable';
import {Observable} from 'rxjs/Observable';
import {Subscription} from 'rxjs/Subscription';

import {CohortReviewService, PageFilterRequest, ParticipantData, SortOrder} from 'generated';

class SortByColumn implements ClrDatagridComparatorInterface<ParticipantData> {
  compare(a: ParticipantData, b: ParticipantData) {
    return (a.numMentions === null ? 0 : parseInt(a.numMentions, 10))
        - (b.numMentions === null ? 0 : parseInt(b.numMentions, 10));
  }
}

@Component({
  selector: 'app-detail-tab-table',
  templateUrl: './detail-tab-table.component.html',
  styleUrls: ['./detail-tab-table.component.css']
})
export class DetailTabTableComponent implements OnInit, OnDestroy {
  @Input() tabname;
  @Input() columns;
  @Input() domain;
  @Input() filterType;
  @Input() reverseEnum;
  loading = false;

  data;
  totalCount: number;
  subscription: Subscription;

  readonly pageSize = 25;
  numMentionsSort = new SortByColumn();

  constructor(
    private route: ActivatedRoute,
    private reviewApi: CohortReviewService,
  ) {}

  ngOnInit() {
    this.subscription = this.route.data
      .map(({participant}) => participant)
      .withLatestFrom(
        this.route.data.map(({cohort}) => cohort),
        this.route.data.map(({workspace}) => workspace),
      )
      .distinctUntilChanged()
      .do(_ => this.loading = true)
      .switchMap(([participant, cohort, workspace]) => {
        this.data = [];
        return this.reviewApi.getParticipantData(
          workspace.namespace,
          workspace.id,
          cohort.id,
          workspace.cdrVersionId,
          participant.participantId,
          <PageFilterRequest>{
            page: 0,
            pageSize: 10000,
            sortOrder: SortOrder.Asc,
            sortColumn: this.columns[0].name,
            pageFilterType: this.filterType,
            domain: this.domain,
          }
        );
      })
      .subscribe(resp => {
        this.data = resp.items;
        this.totalCount = resp.count;
        this.loading = false;
      });
  }

  ngOnDestroy() {
    this.subscription.unsubscribe();
  }

  sortByColumn(column: string) {
    switch (column) {
      case 'numMentions':
        return this.numMentionsSort;
      default:
        return null;
    }
  }
}
