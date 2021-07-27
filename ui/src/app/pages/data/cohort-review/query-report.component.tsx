import {ComboChart} from 'app/components/combo-chart.component';
import {SpinnerOverlay} from 'app/components/spinners';
import {WithSpinnerOverlayProps} from 'app/components/with-spinner-overlay';
import {CohortDefinition} from 'app/pages/data/cohort-review/cohort-definition.component';
import {ParticipantsCharts} from 'app/pages/data/cohort-review/participants-charts';
import {cohortBuilderApi, cohortReviewApi, cohortsApi} from 'app/services/swagger-fetch-clients';
import colors, {colorWithWhiteness} from 'app/styles/colors';
import {reactStyles, withCdrVersions, withCurrentCohortReview, withCurrentWorkspace} from 'app/utils';
import {findCdrVersion} from 'app/utils/cdr-versions';
import {currentCohortReviewStore, navigate, urlParamsStore} from 'app/utils/navigation';
import {WorkspaceData} from 'app/utils/workspace-data';
import {
  AgeType,
  CdrVersionTiersResponse,
  Cohort,
  CohortReview,
  Domain,
  GenderOrSexType,
  SearchRequest,
  SortOrder
} from 'generated/fetch';
import * as fp from 'lodash/fp';
import * as moment from 'moment';
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
  backBtn: {
    padding: 0,
    border: 0,
    fontSize: '14px',
    color: colors.accent,
    background: 'transparent',
    cursor: 'pointer',
  },
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
    backgroundColor: colors.white,
    paddingTop: '1rem',
    marginTop: '0.5rem',
  },
  queryTitle: {
    fontSize: '16px',
    fontWeight: 600,
    color: colors.primary,
    lineHeight: '22px',
  },
  queryContent: {
    fontSize: '13px',
    color: colors.primary,
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
    color: colors.primary,
    lineHeight: '22px',
  },
  graphBorder: {
    minHeight: '10rem',
    marginLeft: '23%',
    padding: '0.3rem',
  },
  groupHeader: {
    backgroundColor: colorWithWhiteness(colors.light, -.5),
    padding: '0.2rem',
    color: colors.primary,
    marginTop: '1rem'
  },
  groupText: {
    display: 'inline-block',
    fontWeight: 600,
    fontSize: '14px',
    textTransform: 'capitalize'
  },
  groupContent: {
    paddingTop: '0.2rem',
    paddingLeft: '0.75rem',
    color: colors.primary,
    fontSize: '13px'
  },
  print: {
    color: colors.accent,
    cursor: 'pointer'
  },
  printDisabled: {
    color: colors.light,
    cursor: 'not-allowed'
  }
});
const columns = {
  col2: {
    ...styles.col,
    flex: '0 0 16.66667%',
    maxWidth: '16.66667%'
  },
  col3: {
    ...styles.col,
    flex: '0 0 25%',
    maxWidth: '25%'
  },
  col4: {
    ...styles.col,
    flex: '0 0 33.33333%',
    maxWidth: '33.33333%'
  },
  col6: {
    ...styles.col,
    flex: '0 0 50%',
    maxWidth: '50%'
  },
  col7: {
    ...styles.col,
    flex: '0 0 58.33333%',
    maxWidth: '58.33333%'
  },
  col8: {
    ...styles.col,
    flex: '0 0 67.66667%',
    maxWidth: '67.66667%'
  },
  col10: {
    ...styles.col,
    flex: '0 0 83.33333%',
    maxWidth: '83.33333%'
  },
  col12: {
    ...styles.col,
    flex: '0 0 100%',
    maxWidth: '100%'
  },
};
const demoTitle = {
  ...styles.chartTitle,
  marginLeft: '0.4rem',
  paddingBottom: '0.5rem'
};

const domains = [Domain[Domain.CONDITION],
  Domain[Domain.PROCEDURE],
  Domain[Domain.DRUG],
  Domain[Domain.LAB]];

export interface QueryReportProps extends WithSpinnerOverlayProps {
  cdrVersionTiersResponse: CdrVersionTiersResponse;
  cohortReview: CohortReview;
  workspace: WorkspaceData;
}
export interface QueryReportState {
  cdrName: string;
  cohort: Cohort;
  data: any;
  groupedData: any;
  chartsLoading: boolean;
  reviewLoading: boolean;
}

export const QueryReport = fp.flow(withCdrVersions(), withCurrentCohortReview(), withCurrentWorkspace())(
  class extends React.Component<QueryReportProps, QueryReportState> {
    constructor(props: any) {
      super(props);
      this.state = {
        cdrName: null,
        cohort: undefined,
        data: null,
        groupedData: null,
        chartsLoading: true,
        reviewLoading: true
      };
    }

    async componentDidMount() {
      const {cdrVersionTiersResponse, cohortReview, workspace: {cdrVersionId}, hideSpinner} = this.props;
      hideSpinner();
      const {ns, wsid, cid} = urlParamsStore.getValue();
      let request: SearchRequest;
      if (cohortReview) {
        this.setState({reviewLoading: false});
        request = (JSON.parse(cohortReview.cohortDefinition));
      } else {
        await cohortReviewApi().getParticipantCohortStatuses(ns, wsid, cid, +cdrVersionId, {
          page: 0,
          pageSize: 25,
          sortOrder: SortOrder.Asc
        }).then(({cohortReview: review}) => {
          request = (JSON.parse(review.cohortDefinition));
          this.setState({reviewLoading: false});
          currentCohortReviewStore.next(review);
        });
      }
      cohortsApi().getCohort(ns, wsid, cid).then(cohort => this.setState({cohort}));
      const cdrName = findCdrVersion(cdrVersionId, cdrVersionTiersResponse).name;
      this.setState({cdrName});
      cohortBuilderApi().findDemoChartInfo(ns, wsid, GenderOrSexType[GenderOrSexType.GENDER], AgeType[AgeType.AGE], request)
        .then(response => {
          this.groupChartData(response.items);
          this.setState({data: response.items, chartsLoading: false});
        });
    }

    groupChartData(data: any) {
      const groups = ['gender', 'ageRange', 'race'];
      const init = {gender: {}, ageRange: {}, race: {}};
      const groupedData = data.reduce((acc, i) => {
        groups.forEach(group => {
          const key = i[group];
          if (acc[group][key]) {
            acc[group][key].count += i.count;
          } else {
            acc[group][key] = {name: this.getFormattedName(key), count: i.count};
          }
        });
        return acc;
      }, init);
      this.setState({groupedData});
    }

    getFormattedName(name: string) {
      switch (name) {
        case 'F' :
          return 'Female';
        case 'M' :
          return 'Male';
        default:
          return name;
      }
    }

    goBack() {
      const {ns, wsid, cid} = urlParamsStore.getValue();
      navigate(['/workspaces', ns, wsid, 'data', 'cohorts', cid, 'review', 'participants']);
    }

    render() {
      const {cohortReview} = this.props;
      const {cdrName, cohort, data, groupedData, chartsLoading, reviewLoading} = this.state;
      // TODO can we use the creation time from the review instead of the cohort here?
      const created = !!cohort ? moment(cohort.creationTime).format('YYYY-MM-DD') : null;
      return <React.Fragment>
        <style>{css}</style>
        <button
          style={styles.backBtn}
          type='button'
          onClick={() => this.goBack()}>
          Back to review set
        </button>
        {reviewLoading && <SpinnerOverlay/>}
        {cohortReview && <div style={styles.reportBackground}>
          <div style={styles.container}>
            <div style={styles.row}>
              <div style={columns.col6}>
                <div style={styles.container}>
                  <div style={styles.row}>
                    <div style={columns.col6}>
                      <div style={styles.queryTitle}>
                        Cohort Name
                      </div>
                      <div style={styles.queryContent}>
                        {cohortReview.cohortName}
                      </div>
                      <div style={styles.queryTitle}>
                        Created By
                      </div>
                      <div style={styles.queryContent}>
                        {!!cohort ? cohort.creator : ''}
                      </div>
                    </div>
                    <div style={columns.col6}>
                      <div style={styles.queryTitle}>
                        Date created
                      </div>
                      <div style={styles.queryContent}>
                        {created}
                      </div>
                      <div style={styles.queryTitle}>
                        Dataset
                      </div>
                      <div style={styles.queryContent}>
                        {cdrName}
                      </div>
                    </div>
                    <div style={columns.col12}>
                      <CohortDefinition review={cohortReview}/>
                    </div>
                  </div>
                </div>
              </div>
              <div className='stats-left-padding'
                style={columns.col6}>
                <div style={styles.container}>
                  <div style={styles.row}>
                    <div style={{...columns.col10, ...styles.queryTitle}}>
                      Descriptive Statistics
                    </div>
                    {/*TODO uncomment print icon when we know how we want to display the data download policy*/}
                    {/*<ClrIcon*/}
                    {/*  className='is-solid'*/}
                    {/*  style={{...columns.col2,*/}
                    {/*    ...(groupedData ? styles.print : styles.printDisabled)}}*/}
                    {/*  onClick={() => groupedData && window.print()}*/}
                    {/*  disabled={!groupedData}*/}
                    {/*  shape='printer'*/}
                    {/*  size={32} />*/}
                </div>
              </div>
              {groupedData && Object.keys(groupedData).map((group, g) => (
                <div key={g}>
                  <div style={styles.container}>
                    <div style={{...styles.container, ...styles.groupHeader}}>
                      <div style={{...columns.col7, ...styles.groupText}}>
                        {group === 'ageRange' ? 'Age' : group}
                      </div>
                      {g === 0 && <div style={{...columns.col2, ...styles.groupText}}>
                        Total
                      </div>}
                      {g === 0 && <div style={{...columns.col3, ...styles.groupText}}>
                        % of Cohort
                      </div>}
                    </div>
                  </div>
                  {Object.keys(groupedData[group]).map((row, r) => (
                    <div key={r} style={styles.container}>
                      <div style={{...styles.row, ...styles.groupContent}}>
                        <div style={columns.col7}>
                          {groupedData[group][row].name}
                        </div>
                        <div style={columns.col2}>
                          {groupedData[group][row].count.toLocaleString()}
                        </div>
                        <div style={columns.col3}>
                          {Math.round(groupedData[group][row].count / cohortReview.matchedParticipantCount * 100)}%
                        </div>
                      </div>
                    </div>
                  ))}
                  </div>
                ))}
                {chartsLoading && <SpinnerOverlay />}
              </div>
            </div>
          </div>
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
                    <div style={{...styles.col, flex: '0 0 75%', maxWidth: '75%'}}>
                      <div style={demoTitle}>Demographics</div>
                      <div style={styles.graphBorder}>
                        {data && <ComboChart mode={'stacked'} data={data} />}
                        {chartsLoading && <SpinnerOverlay />}
                      </div>
                    </div>
                    <div style={{...styles.col, flex: '0 0 83.33333%', maxWidth: '83.33333%'}}>
                      {domains.map((domain, i) => (
                        <div key={i} style={{minHeight: '10rem', position: 'relative'}}>
                          <ParticipantsCharts domain={domain} review={cohortReview}/>
                        </div>
                      ))}
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>}
      </React.Fragment>;
    }
  }
);
