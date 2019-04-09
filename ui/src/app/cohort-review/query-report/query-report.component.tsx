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
  container: {
    width: '100%',
    marginLeft: 'auto',
    marginRight: 'auto',
    paddingLeft: '0.5rem',
    paddingRight: '0.5rem',
  },
  row: {
    display: 'flex',
    flexWrap: 'wrap',
    marginRight: '-.5rem',
    marginLeft: '-.5rem'
  },
  col: {
    position: 'relative',
    minHeight: '1px',
    width: '100%',
    paddingLeft: '0.5rem',
    paddingRight: '0.5rem',
  },
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
const col6 = {
  ...styles.col, flex: '0 0 50%', maxWidth: '50%'
};
const demoTitle = {
  ...styles.chartTitle,
  marginLeft: '0.4rem',
  paddingBottom: '0.5rem'
};

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
          <div style={styles.container}>
            <div style={styles.row}>
              <div style={{...styles.col, flex: '0 0 50%', maxWidth: '50%'}}>
                <div style={styles.container}>
                  <div style={styles.row}>
                    <div style={{...styles.col, flex: '0 0 50%', maxWidth: '50%'}}>
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
                    <div style={col6}>
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
                    <div style={{...styles.col, flex: '0 0 100%', maxWidth: '100%'}}>
                      // query-cohort-definition
                    </div>
                  </div>
                </div>
              </div>
              <div className='stats-left-padding'
                style={{...styles.col, flex: '0 0 50%', maxWidth: '50%'}}>
                // app-descriptive-stats
              </div>
            </div>
          </div>
          // ov charts
          <div style={{...styles.container, margin: 0}}>
            <div style={styles.row}>
              <div style={{...styles.col, flex: '0 0 100%', maxWidth: '100%'}}>
                <div style={{...styles.container, margin: 0}}>
                  <div style={styles.row}>
                    <div style={{...styles.col, flex: '0 0 100%', maxWidth: '100%'}}>
                      <div>
                        <span style={styles.chartTitle}>Charts</span>
                      </div>
                    </div>
                  </div>
                </div>
                <div style={{...styles.container, margin: 0}}>
                  <div style={{...styles.row, paddingTop: '1rem'}}>
                    <div style={{...styles.col, flex: '0 0 66.66667%', maxWidth: '66.66667%'}}>
                      <div style={demoTitle}>Demographics</div>
                      <div style={styles.graphBorder}>
                        {data && <ComboChart mode={'stacked'} data={data} />}
                        {loading && <SpinnerOverlay />}
                      </div>
                    </div>
                    <div style={{...styles.col, flex: '0 0 83.33333%', maxWidth: '83.33333%'}}>
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
