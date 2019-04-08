import {ComboChart} from 'app/cohort-common/combo-chart/combo-chart.component';
import {ParticipantsCharts} from 'app/cohort-review/participants-charts/participants-charts';
import {cohortReviewStore} from 'app/cohort-review/review-state.service';
import {SpinnerOverlay} from 'app/components/spinners';
import {cdrVersionsApi, cohortBuilderApi} from 'app/services/swagger-fetch-clients';
import {WorkspaceData} from 'app/services/workspace-storage.service';
import {reactStyles, withCurrentWorkspace} from 'app/utils';
import {currentCohortStore} from 'app/utils/navigation';
import {DomainType, SearchRequest} from 'generated/fetch';
import {fromJS} from 'immutable';
import * as React from 'react';

const css = `
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

const styles = reactStyles({
  reportBackground: {
    backgroundColor: 'white',
    paddingTop: '1rem',
    marginTop: '0.5rem',
  },
  queryTitle: {
    fontSize: '16px',
    fontWeight: 600,
    color: '#262262',
    lineHeight: '22px',
  },
  queryContent: {
    fontSize: '13px',
    color: 'black',
    lineHeight: '30px',
    paddingBottom: '0.6rem',
  },
  containerMargin: {
    margin: 0,
    minWidth: '100%'
  },
  chartTitle: {
    marginLeft: '0.4rem',
    paddingBottom: '0.5rem',
    fontSize: '16px',
    fontWeight: 600,
    color: '#262262',
    lineHeight: '22px',
  },
  graphBorder: {
    minHeight: '10rem',
    padding: '0.3rem',
  }
});

const demoTitle = {
  ...styles.chartTitle,
  marginLeft: '0.4rem',
  paddingBottom: '0.5rem'
}

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
        <div style={styles.reportBackground}>
          <div className='container' style={{minWidth: '100%'}}>
            <div className='row'>
              <div className='col-sm-6 col-xs-6'>
                <div className='container' style={{minWidth: '100%'}}>
                  <div className='row'>
                    <div className='col-sm-6 col-xs-6'>
                      <div style={styles.queryTitle}>
                        Cohort Name
                      </div>
                      <div style={styles.queryContent}>
                        {this.review.cohortName}
                      </div>
                      <div style={styles.queryTitle}>
                        Created By
                      </div>
                      <div style={styles.queryContent}>
                        {this.cohort.creator}
                      </div>
                    </div>
                    <div className='col-sm-6 col-xs-6'>
                      <div style={styles.queryTitle}>
                        Date created
                      </div>
                      <div style={styles.queryContent}>
                        {this.cohort.creationTime}
                      </div>
                      <div style={styles.queryTitle}>
                        Dataset
                      </div>
                      <div style={styles.queryContent}>
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
          <div className='container' style={styles.containerMargin}>
            <div className='row'>
              <div className='col-sm-12 col-xs-12'>
                <div className='container' style={styles.containerMargin}>
                  <div className='row'>
                    <div className='col-sm-12 col-xs-12'>
                      <div>
                        <span style={styles.chartTitle}>Charts</span>
                      </div>
                    </div>
                  </div>
                </div>
                <div className='container' style={styles.containerMargin}>
                  <div className='row' style={{paddingTop: '1rem'}}>
                    <div className='col-sm-8 col-xs-8 col-lg-8 col-xl-8'>
                      <div style={demoTitle}>Demographics</div>
                      <div style={styles.graphBorder}>
                        {data && <ComboChart mode={'stacked'} data={data} />}
                        {loading && <SpinnerOverlay />}
                      </div>
                    </div>
                    <div className='col-sm-10 col-xs-10 col-lg-10 col-xl-10'>
                      {domains.map((domain, i) => (
                        <div key={i} style={{minHeight: '10rem', position: 'relative'}}>
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
      </React.Fragment>;
    }
  }
);
