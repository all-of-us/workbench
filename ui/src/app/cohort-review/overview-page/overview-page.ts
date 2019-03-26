import {Component, EventEmitter, OnInit, Output} from '@angular/core';
import {cohortReviewStore} from 'app/cohort-review/review-state.service';
import {cohortBuilderApi, cohortReviewApi} from 'app/services/swagger-fetch-clients';
import {currentWorkspaceStore, urlParamsStore} from 'app/utils/navigation';
import {DemoChartInfoListResponse, DomainType, SearchRequest} from 'generated/fetch';
import {CohortReview} from 'generated/fetch';
import {fromJS, List} from 'immutable';
import {from} from 'rxjs/observable/from';


@Component({
  selector: 'app-overview-charts',
  templateUrl: './overview-page.html',
  styleUrls: ['./overview-page.css'],

})
export class OverviewPage implements OnInit {
  openChartContainer = false;
  demoGraph = false;
  @Output() dataItems = new EventEmitter<any>();
  data = List();
  typesList = [DomainType[DomainType.CONDITION],
    DomainType[DomainType.PROCEDURE],
    DomainType[DomainType.DRUG],
    DomainType[DomainType.LAB]];
  title: string;
  showTitle = false;
  review: CohortReview;
  totalParticipantCount: number;
  loading = false;
  domainsData = {};
  totalCount: any;
  constructor() {}

  ngOnInit() {
    const workspace = currentWorkspaceStore.getValue();
    this.review = cohortReviewStore.getValue();
    const request = <SearchRequest>(JSON.parse(this.review.cohortDefinition));
    this.loading = true;
    cohortBuilderApi().getDemoChartInfo(+workspace.cdrVersionId, request)
      .then(response => {
        this.data = fromJS((<DemoChartInfoListResponse>response).items);
        this.dataItems.emit(this.data);
        this.loading = false;
      });
    this.totalParticipantCount = this.review.matchedParticipantCount;
    this.openChartContainer = true;
    this.fetchChartsData();
  }


  fetchChartsData() {
    this.demoGraph = false;
    this.showTitle = false;
    const limit = 10;
    const cdrid = +(currentWorkspaceStore.getValue().cdrVersionId);
    const {ns, wsid, cid} = urlParamsStore.getValue();
    this.typesList.map(domainName => {
      this.domainsData[domainName] = {
        conditionTitle: '',
        loading: true
      };
      cohortReviewApi().getCohortChartData(ns, wsid, cid, cdrid, domainName, limit)
        .then(data => {
          const chartData = data;
          this.totalCount = chartData.count;
          this.domainsData[domainName] = chartData.items;
          this.domainsData[domainName].loading = false;
        });
    });
  }
}
