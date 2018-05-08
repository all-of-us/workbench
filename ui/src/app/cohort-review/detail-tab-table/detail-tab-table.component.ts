import {Component, Input, OnDestroy, OnInit} from '@angular/core';
import {ActivatedRoute} from '@angular/router';
import {ClrDatagridStateInterface} from '@clr/angular';
import {fromJS} from 'immutable';
import {Observable} from 'rxjs/Observable';
import {Subscription} from 'rxjs/Subscription';

import {CohortReviewService, PageFilterRequest, SortOrder} from 'generated';

@Component({
  selector: 'app-detail-tab-table',
  templateUrl: './detail-tab-table.component.html',
  styleUrls: ['./detail-tab-table.component.css']
})
export class DetailTabTableComponent implements OnInit, OnDestroy {
  @Input() tabname;
  @Input() columns;
  @Input() filterType;
  @Input() reverseEnum;
  loading = false;

  data;
  totalCount: number;
  subscription: Subscription;

  readonly pageSize = 25;

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
      .switchMap(([participant, cohort, workspace]) =>
        this.reviewApi.getParticipantData(
          workspace.namespace,
          workspace.id,
          cohort.id,
          workspace.cdrVersionId,
          participant.participantId,
          <PageFilterRequest>{
            page: 0,
            pageSize: this.pageSize,
            includeTotal: true,
            sortOrder: SortOrder.Asc,
            sortColumn: this.reverseEnum[this.columns[0].name],
            pageFilterType: this.filterType,
          }
      ))
      .subscribe(resp => {
        this.data = resp.items;
        this.totalCount = resp.count;
        this.loading = false;
      });
  }

  ngOnDestroy() {
    this.subscription.unsubscribe();
  }
}
