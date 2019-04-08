import {ComboChart} from 'app/cohort-common/combo-chart/combo-chart.component';
import {ParticipantsCharts} from 'app/cohort-review/participants-charts/participants-charts';
import {cohortReviewStore} from 'app/cohort-review/review-state.service';
import {SpinnerOverlay} from 'app/components/spinners';
import {cdrVersionsApi, cohortBuilderApi} from 'app/services/swagger-fetch-clients';
import {WorkspaceData} from 'app/services/workspace-storage.service';
import {withCurrentWorkspace} from 'app/utils';
import {currentCohortStore} from 'app/utils/navigation';
import {DomainType, SearchRequest} from 'generated/fetch';
import {fromJS} from 'immutable';
import * as React from 'react';

const css = `
  .container {
    min-width: 100%;
  }
  .report-background {
    background-color: white;
    padding-top: 1rem;
    margin-top: 0.5rem;
  }
  .report-background .query-title {
    font-size: 16px;
    font-weight: 600;
    color: #262262;
    line-height: 22px;
  }
  .report-background .query-content {
    font-size: 13px;
    color: black;
    line-height: 30px;
    padding-bottom: 0.6rem;
  }
  .header-border .btn-margin {
    margin-top : 0.7rem;
  }
  .temporal-container {
    padding-left: 1rem;
    margin-top: 1rem;
    margin-bottom: 1rem;
  }
  .stats-left-padding {
    padding-left: 6rem;
  }
  @media print{
    .doNotPrint{
      display:none !important;
      -webkit-print-color-adjust: exact;
    }
    .page-break {
      page-break-inside:auto;
    }
    .stats-left-padding {
      padding-left: 2rem;
    }
  }
`;

const domains = [DomainType[DomainType.CONDITION],
  DomainType[DomainType.PROCEDURE],
  DomainType[DomainType.DRUG],
  DomainType[DomainType.LAB]];

export interface QueryReportProps {
  workspace: WorkspaceData;
}
export interface QueryReportState {
  cdrName: string;
  data: any;
  loading: boolean;
}

export const QueryReport = withCurrentWorkspace()(
  class extends React.Component<QueryReportProps, QueryReportState> {
    cohort = currentCohortStore.getValue();
    review = cohortReviewStore.getValue();

    constructor(props: any) {
      super(props);
      this.state = {
        cdrName: null,
        data: null,
        loading: true
      };
    }

    componentDidMount() {
      const {cdrVersionId} = this.props.workspace;
      cdrVersionsApi().getCdrVersions().then(resp => {
        const cdrName = resp.items.find(
          v => v.cdrVersionId === this.review.cdrVersionId.toString()
        ).name;
        this.setState({cdrName});
      });
      const request = (JSON.parse(this.review.cohortDefinition)) as SearchRequest;
      cohortBuilderApi().getDemoChartInfo(+cdrVersionId, request)
        .then(response => {
          this.setState({data: fromJS((response).items), loading: false});
        });
    }

    render() {
      const {cdrName, data, loading} = this.state;
      return <React.Fragment>
        <style>{css}</style>
        <div className='report-background'>
          <div className='container'>
            <div className='row'>
              <div className='col-sm-6 col-xs-6'>
                <div className='container'>
                  <div className='row'>
                    <div className='col-sm-6 col-xs-6'>
                      <div className='query-title'>
                        Cohort Name
                      </div>
                      <div className='query-content'>
                        {this.review.cohortName}
                      </div>
                      <div className='query-title'>
                        Created By
                      </div>
                      <div className='query-content'>
                        {this.cohort.creator}
                      </div>
                    </div>
                    <div className='col-sm-6 col-xs-6'>
                      <div className='query-title'>
                        Date created
                      </div>
                      <div className='query-content'>
                        {this.cohort.creationTime}
                      </div>
                      <div className='query-title'>
                        Dataset
                      </div>
                      <div className='query-content'>
                        {cdrName}
                      </div>
                    </div>
                    <div className='col-sm-12 col-xs-12'>
                      // query-cohort-definition
                    </div>
                  </div>
                </div>
              </div>
              <div className=' col-sm-6 col-xs-6 stats-left-padding'>
                // app-descriptive-stats
              </div>
            </div>
          </div>
          // ov charts
          <div className='container container-margin'>
            <div className='row'>
              <div className='col-sm-12 col-xs-12'>
                <div className='container container-margin'>
                  <div className='row'>
                    <div className='col-sm-12 col-xs-12'>
                      <div>
                        <span className='chart-title'>Charts</span>
                      </div>
                    </div>
                  </div>
                </div>
                <div className='container container-margin'>
                  <div className='row'>
                    <div className='col-sm-8 col-xs-8 col-lg-8 col-xl-8'>
                      <div className='chart-container'>
                        <div className='demography-title '>Demographics</div>
                        <div className='graph-border'>
                          <div className='demo-chart-heading'>
                            Participants by Gender, Age Range & Race
                          </div>
                          {data && <ComboChart mode={'stacked'} data={data} />}
                          {loading && <SpinnerOverlay />}
                        </div>
                      </div>
                    </div>
                    <div className='col-sm-10 col-xs-10 col-lg-10 col-xl-10'>
                      <div className='chart-container'>
                        {domains.map((domain, i) => (
                          <div key={i}>
                            <ParticipantsCharts domain={domain}/>
                          </div>
                        ))}
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </React.Fragment>;
    }
  }
);
