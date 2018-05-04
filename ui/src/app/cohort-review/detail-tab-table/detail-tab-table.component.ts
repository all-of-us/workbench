import {Component, Input, OnDestroy, OnInit} from '@angular/core';
import {ActivatedRoute} from '@angular/router';
import {ClrDatagridStateInterface} from '@clr/angular';
import {fromJS} from 'immutable';
import {Observable} from 'rxjs/Observable';
import {Subscription} from 'rxjs/Subscription';

import {WorkspaceStorageService} from 'app/services/workspace-storage.service';

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
  cdrId: number;
  loading = false;

  data;
  request;
  totalCount: number;
  apiCaller: (any) => Observable<any>;
  subscription: Subscription;

  constructor(
    private route: ActivatedRoute,
    private reviewApi: CohortReviewService,
    private workspaceStorageService: WorkspaceStorageService,
  ) {}

  ngOnInit() {
    console.dir(this.route);

    this.workspaceStorageService.activeWorkspace$.subscribe((workspace) => {
      this.cdrId = parseInt(workspace.cdrVersionId, 10);
    });
    this.workspaceStorageService.reloadIfNew(
      this.route.snapshot.parent.params['ns'],
      this.route.snapshot.parent.params['wsid']);

    this.subscription = this.route.data
      .map(({participant}) => participant)
      .withLatestFrom(
        this.route.parent.data.map(({cohort}) => cohort),
      )
      .distinctUntilChanged()
      .subscribe(([participant, cohort]) => {
        this.loading = true;

        this.apiCaller = (request) => this.reviewApi.getParticipantData(
          this.route.snapshot.parent.params['ns'],
          this.route.snapshot.parent.params['wsid'],
          cohort.id,
          this.cdrId,
          participant.participantId,
          request
        );

        this.request = <PageFilterRequest>{
          page: 0,
          pageSize: 50,
          includeTotal: true,
          sortOrder: SortOrder.Asc,
          sortColumn: this.reverseEnum[this.columns[0].name],
          pageFilterType: this.filterType,
        };

        // console.log(`Fetching tab ${this.tabname} from ngOnInit`);
        this.callApi();
      });
  }

  ngOnDestroy() {
    this.subscription.unsubscribe();
  }

  callApi() {
    this.loading = true;
    this.apiCaller(this.request).subscribe(resp => {
      this.data = resp.items;
      this.totalCount = resp.count;
      this.request = resp.pageRequest;
      this.request.pageFilterType = this.filterType;
      this.loading = false;
    });
  }

  update(state: ClrDatagridStateInterface) {
    // console.log('Datagrid state: ');
    // console.dir(state);
    const page = Math.floor(state.page.from / state.page.size);
    const pageSize = state.page.size;
    const oldRequest = {...this.request};
    const newRequest = {...this.request, page, pageSize};

    if (state.sort) {
      const sortby = <string>(state.sort.by);
      newRequest.sortColumn = this.reverseEnum[sortby];
      newRequest.sortOrder = state.sort.reverse ? SortOrder.Desc : SortOrder.Asc;
    }

    /* If the old state is the same as the new state, do nothing */
    if (fromJS(oldRequest).equals(fromJS(newRequest))) {
      return ;
    }

    this.request = newRequest;
    // console.log(`Fetching tab ${this.tabname} from clrDgRefresh`);
    this.callApi();
  }
}
