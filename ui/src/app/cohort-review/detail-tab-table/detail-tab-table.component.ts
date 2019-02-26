import {Component, Input, OnDestroy, OnInit} from '@angular/core';
import {ClrDatagridComparatorInterface} from '@clr/angular';
import * as fp from 'lodash/fp';
import {Observable} from 'rxjs/Observable';
import {Subscription} from 'rxjs/Subscription';

import {currentWorkspaceStore, urlParamsStore} from 'app/utils/navigation';
import {
  CohortReviewService, PageFilterRequest, SortOrder
} from 'generated';

class SortByColumn implements ClrDatagridComparatorInterface<any> {
  compare(a: any, b: any) {
    return (a.numMentions === null ? 999999 : parseInt(a.numMentions, 10))
        - (b.numMentions === null ? 999999 : parseInt(b.numMentions, 10));
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
  filtered = [];
  constructor(
    private reviewApi: CohortReviewService,
  ) {}

  ngOnInit() {
    this.subscription = Observable
      .combineLatest(urlParamsStore, currentWorkspaceStore)
      .map(([{ns, wsid, cid, pid}, {cdrVersionId}]) => ({ns, wsid, cid, pid, cdrVersionId}))
      .distinctUntilChanged(fp.isEqual)
      .switchMap(({ns, wsid, cid, pid, cdrVersionId}) => {
        this.loading = true;
        this.data = [];
        return this.reviewApi.getParticipantData(ns, wsid, cid, +cdrVersionId, pid, {
          page: 0,
          pageSize: 10000,
          sortOrder: SortOrder.Asc,
          sortColumn: this.columns[0].name,
          pageFilterType: this.filterType,
          domain: this.domain,
        } as PageFilterRequest);
      }).subscribe(({items, count}) => {
        this.data = items;
        this.totalCount = count;
        this.loading = false;
      });
  }

  isFiltered(event) {
    if (event.action === 'add') {
      this.filtered.push(event.column);
    } else {
      this.filtered =
        this.filtered.filter(col => col !== event.column);
    }
  }

  isSelected(column) {
    return this.filtered.includes(column);
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
