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
} from '../../cohort-search/redux';
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
  private subscription: Subscription;

  constructor(
    private ngRedux: NgRedux<CohortSearchState>,
    private chartAPI: CohortBuilderService,
    private reviewAPI: CohortReviewService,
    private state: ReviewStateService,
    private route: ActivatedRoute,
    private actions: CohortSearchActions,
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
        this.demoGraph = true;
        this.showTitle = false;
        this.title = '';
    }

    getDifferentCharts(names) {
        this.demoGraph = false;
        this.title = names;
        this.fetchChartsData(names);
        return this.title;
    }

    fetchChartsData(name) {
      this.spinner = true;
      this.showTitle = false;
      const domain = name;
      const limit = 10;
      const {ns, wsid, cid} = this.route.parent.snapshot.params;
      const cdrid = +(this.route.parent.snapshot.data.workspace.cdrVersionId);
      setTimeout(() => {
            this.actions.fetchReviewChartsData(ns, wsid, cid, cdrid, domain, limit);
            this.getCharts(name);
        }, 1000);
    }

    getCharts(name) {
        const loadingReviewCohortData = this.ngRedux
            .select(isChartLoading(name))
            .filter(domain => !!domain)
            .subscribe(loading => {
                this.spinner = false;
                this.showTitle = true;
                this.loading = loading;
                const totalCount = this.loading.toJS().count;
                this.domainItems = this.loading.toJS().items;
                this.domainItems.forEach(itemCount => {
                    const percentCount = ((itemCount.count / totalCount) * 100);
                    Object.assign(itemCount, {percentCount: percentCount});
                });
            });
        this.subscription = loadingReviewCohortData;
    }

  ngOnDestroy() {
    this.subscription.unsubscribe();
  }
}

