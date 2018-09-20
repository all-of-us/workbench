import {NgRedux} from '@angular-redux/store';
import {Component, OnDestroy, OnInit} from '@angular/core';
import {ActivatedRoute} from '@angular/router';
import {CohortBuilderService, CohortReview, CohortReviewService, DemoChartInfoListResponse, DomainType, SearchRequest} from 'generated';
import {fromJS, List} from 'immutable';
import {Subscription} from 'rxjs/Subscription';
import {
    CohortSearchActions,
    CohortSearchState,
    isChartLoading,
    isDomainNameExists
} from '../../cohort-search/redux';
import {typeToTitle} from '../../cohort-search/utils';
import {ReviewStateService} from '../review-state.service';


@Component({
    selector: 'app-overview-charts',
    templateUrl: './overview-page.html',
    styleUrls: ['./overview-page.css'],

})
export class OverviewPage implements OnInit, OnDestroy {
  openChartContainer = false;
  demoGraph = false;
  data = List();
  typesList = [DomainType[DomainType.CONDITION],
              DomainType[DomainType.PROCEDURE],
              DomainType[DomainType.MEASUREMENT],
              DomainType[DomainType.LAB]];
  title: string;
  showTitle = false;
  loading: any;
  domainItems = [];
  spinner = false;
  selectedCohortName: string;
  review: CohortReview;
  totalParticipantCount: number;
  isCancelTimerInitiated: any = false;
  domainTitle: '';
  trackClickedDomains = false;
  private subscription: Subscription;

  constructor(
    private ngRedux: NgRedux<CohortSearchState>,
    private chartAPI: CohortBuilderService,
    private state: ReviewStateService,
    private route: ActivatedRoute,
  ) {}

  ngOnInit() {
     this.selectedCohortName = this.route.parent.snapshot.data.cohort.name;
      this.spinner = true;
      const {cdrVersionId} = this.route.parent.snapshot.data.workspace;
      this.subscription = this.state.cohort$
          .map(({criteria}) => <SearchRequest>(JSON.parse(criteria)))
          .switchMap(request => this.chartAPI.getDemoChartInfo(cdrVersionId, request))
          .map(response => (<DemoChartInfoListResponse>response).items)
          .subscribe(data => {
              this.data = fromJS(data);
               this.spinner = false;
          });
      this.subscription = this.state.review$.subscribe(review => {
          this.review = review;
          this.totalParticipantCount = review.matchedParticipantCount;

      });
      this.getDemoCharts();
      this.openChartContainer = true;
  }


  getDemoCharts () {
      this.spinner = true;
       setTimeout(() => {
          this.demoGraph = true;
          if (this.data.size) {
              this.spinner = false;
          }
       }, 1000);
          this.showTitle = false;
          this.domainTitle = '';
          this.isCancelTimerInitiated = false;
  }

  getDifferentCharts(names) {
    this.demoGraph = false;
    this.domainTitle = names;
    this.fetchChartsData(names);
    this.title = typeToTitle(names);
    return this.title;
  }

  fetchChartsData(name) {
    this.trackClickedDomains = isDomainNameExists(name)(this.ngRedux.getState());
    this.demoGraph = false;
    this.spinner = true;
    this.showTitle = false;
    const domain = name;
    const limit = 10;
    const {ns, wsid, cid} = this.route.parent.snapshot.params;
    const cdrid = +(this.route.parent.snapshot.data.workspace.cdrVersionId);
    if (this.trackClickedDomains) {
       setTimeout(() => {
        this.spinner = false;
         this.getCharts(name);
       }, 2000);
    } else {
      this.actions.fetchReviewChartsData(ns, wsid, cid, cdrid, domain, limit);
      this.getCharts(name);
    }

  }

  getCharts(name) {
    const loadingReviewCohortData = this.ngRedux
        .select(isChartLoading(name))
        .filter(domain => !!domain)
        .subscribe(loading => {
            this.loading = loading;
            const totalCount = this.loading.toJS().count;
            if (name === this.domainTitle) {
              this.spinner = false;
              this.showTitle = true;
              this.domainItems = this.loading.toJS().items;
              this.domainItems.forEach(itemCount => {
              const percentCount = ((itemCount.count / totalCount) * 100);
              Object.assign(itemCount, {percentCount: percentCount});
           });
          }
        });
    this.subscription = loadingReviewCohortData;
  }

  ngOnDestroy() {
    this.subscription.unsubscribe();
  }

}

