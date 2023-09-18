import * as React from 'react';
import { RouteComponentProps, withRouter } from 'react-router-dom';
import * as fp from 'lodash/fp';

import {
  AgeType,
  CdrVersionTiersResponse,
  Cohort,
  CohortDefinition,
  DemoChartInfo,
  Domain,
  GenderSexRaceOrEthType,
  SortOrder,
} from 'generated/fetch';

import { ComboChart } from 'app/components/combo-chart.component';
import { SpinnerOverlay } from 'app/components/spinners';
import { WithSpinnerOverlayProps } from 'app/components/with-spinner-overlay';
import { CohortDefinitionComponent } from 'app/pages/data/cohort-review/cohort-definition.component';
import { ParticipantsCharts } from 'app/pages/data/cohort-review/participants-charts';
import {
  cohortBuilderApi,
  cohortReviewApi,
  cohortsApi,
} from 'app/services/swagger-fetch-clients';
import colors, { colorWithWhiteness } from 'app/styles/colors';
import {
  reactStyles,
  withCdrVersions,
  withCurrentCohort,
  withCurrentWorkspace,
} from 'app/utils';
import { findCdrVersion } from 'app/utils/cdr-versions';
import { currentCohortStore, NavigationProps } from 'app/utils/navigation';
import { MatchParams } from 'app/utils/stores';
import { withNavigation } from 'app/utils/with-navigation-hoc';
import { WorkspaceData } from 'app/utils/workspace-data';
import outdated from 'assets/icons/outdated.svg';
import moment from 'moment';

const css = `
  .stats-left-padding {
    padding-left: 9rem;
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
      padding-left: 3rem;
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
    paddingLeft: '0.75rem',
    paddingRight: '0.75rem',
  },
  row: {
    display: 'flex',
    flexWrap: 'wrap',
    marginRight: '-.75rem',
    marginLeft: '-.75rem',
  },
  col: {
    position: 'relative',
    minHeight: '1px',
    width: '100%',
    paddingLeft: '0.75rem',
    paddingRight: '0.75rem',
  },
  reportBackground: {
    backgroundColor: colors.white,
    paddingTop: '1.5rem',
    marginTop: '0.75rem',
  },
  queryHeader: {
    fontSize: '18px',
    fontWeight: 600,
    color: colors.primary,
    lineHeight: '24px',
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
    paddingBottom: '0.9rem',
  },
  containerMargin: {
    margin: 0,
    minWidth: '100%',
  },
  chartTitle: {
    marginLeft: '0.6rem',
    paddingBottom: '0.75rem',
    fontSize: '16px',
    fontWeight: 600,
    color: colors.primary,
    lineHeight: '22px',
  },
  graphBorder: {
    minHeight: '15rem',
    marginLeft: '23%',
    padding: '0.45rem',
  },
  groupHeader: {
    backgroundColor: colorWithWhiteness(colors.light, -0.5),
    padding: '0.3rem',
    color: colors.primary,
    marginTop: '1.5rem',
  },
  groupText: {
    display: 'inline-block',
    fontWeight: 600,
    fontSize: '14px',
    textTransform: 'capitalize',
  },
  groupContent: {
    paddingTop: '0.3rem',
    paddingLeft: '1.125rem',
    color: colors.primary,
    fontSize: '13px',
  },
  print: {
    color: colors.accent,
    cursor: 'pointer',
  },
  printDisabled: {
    color: colors.light,
    cursor: 'not-allowed',
  },
});
const columns = {
  col2: {
    ...styles.col,
    flex: '0 0 16.66667%',
    maxWidth: '16.66667%',
  },
  col3: {
    ...styles.col,
    flex: '0 0 25%',
    maxWidth: '25%',
  },
  col4: {
    ...styles.col,
    flex: '0 0 33.33333%',
    maxWidth: '33.33333%',
  },
  col6: {
    ...styles.col,
    flex: '0 0 50%',
    maxWidth: '50%',
  },
  col7: {
    ...styles.col,
    flex: '0 0 58.33333%',
    maxWidth: '58.33333%',
  },
  col8: {
    ...styles.col,
    flex: '0 0 67.66667%',
    maxWidth: '67.66667%',
  },
  col10: {
    ...styles.col,
    flex: '0 0 83.33333%',
    maxWidth: '83.33333%',
  },
  col12: {
    ...styles.col,
    flex: '0 0 100%',
    maxWidth: '100%',
  },
};
const demoTitle = {
  ...styles.chartTitle,
  marginLeft: '0.6rem',
  paddingBottom: '0.75rem',
};

const domains = [
  Domain[Domain.CONDITION],
  Domain[Domain.PROCEDURE],
  Domain[Domain.DRUG],
  Domain[Domain.LAB],
];

const chartTypes = [
  GenderSexRaceOrEthType.ETHNICITY,
  GenderSexRaceOrEthType.GENDER,
  GenderSexRaceOrEthType.RACE,
  GenderSexRaceOrEthType.SEXATBIRTH,
];

export interface QueryReportProps
  extends WithSpinnerOverlayProps,
    NavigationProps,
    RouteComponentProps<MatchParams> {
  cdrVersionTiersResponse: CdrVersionTiersResponse;
  cohort: Cohort;
  workspace: WorkspaceData;
}
export interface QueryReportState {
  cdrName: string;
  data: DemoChartInfo[];
  dateCreated: string;
  displayCohort: Cohort;
  groupedData: any;
  chartsLoading: boolean;
  cohortDefinition: CohortDefinition;
  cohortLoading: boolean;
  outdatedDefinition: boolean;
  participantCount: number;
}

export const QueryReport = fp.flow(
  withCdrVersions(),
  withCurrentCohort(),
  withCurrentWorkspace(),
  withNavigation,
  withRouter
)(
  class extends React.Component<QueryReportProps, QueryReportState> {
    constructor(props: any) {
      super(props);
      this.state = {
        cdrName: null,
        data: null,
        dateCreated: null,
        displayCohort: null,
        groupedData: null,
        chartsLoading: true,
        cohortDefinition: null,
        cohortLoading: true,
        outdatedDefinition: false,
        participantCount: null,
      };
    }

    async componentDidMount() {
      const {
        cdrVersionTiersResponse,
        workspace: { cdrVersionId },
        hideSpinner,
      } = this.props;
      hideSpinner();
      const { ns, wsid } = this.props.match.params;
      const cohortDefinition = await this.getRequestFromCohort();
      const cdrName = findCdrVersion(
        cdrVersionId,
        cdrVersionTiersResponse
      ).name;
      this.setState({ cdrName });
      const demoChartData = {};
      await Promise.all([
        ...chartTypes.map((chartType) =>
          cohortBuilderApi()
            .findDemoChartInfo(
              ns,
              wsid,
              chartType.toString(),
              AgeType.AGE.toString(),
              cohortDefinition
            )
            .then((demoChartInfo) => {
              demoChartData[chartType] = demoChartInfo.items;
            })
        ),
        cohortBuilderApi()
          .countParticipants(ns, wsid, cohortDefinition)
          .then((participantCount) => this.setState({ participantCount })),
      ]);
      this.groupChartData(demoChartData);
      this.setState({
        data: demoChartData[GenderSexRaceOrEthType.GENDER],
        chartsLoading: false,
        cohortDefinition,
      });
    }

    async getRequestFromCohort() {
      const { cohort } = this.props;
      const { ns, wsid, cid, crid } = this.props.match.params;
      let request: CohortDefinition;
      if (cohort?.id === +cid) {
        this.setState({ cohortLoading: false });
        if (crid) {
          request = await this.getRequestFromCohortReview();
        } else {
          this.setState({
            dateCreated: moment(cohort.creationTime).format('YYYY-MM-DD'),
          });
          request = JSON.parse(cohort.criteria);
        }
      } else {
        await cohortsApi()
          .getCohort(ns, wsid, +cid)
          .then(async (cohortResponse) => {
            currentCohortStore.next(cohortResponse);
            if (crid) {
              request = await this.getRequestFromCohortReview();
            } else {
              this.setState({
                dateCreated: moment(cohortResponse.creationTime).format(
                  'YYYY-MM-DD'
                ),
              });
              request = JSON.parse(cohortResponse.criteria);
            }
            this.setState({ cohortLoading: false });
          });
      }
      return request;
    }

    async getRequestFromCohortReview() {
      const {
        cohort,
        match: {
          params: { ns, wsid, crid },
        },
      } = this.props;
      const filterRequest = { page: 0, pageSize: 0, sortOrder: SortOrder.ASC };
      let request: CohortDefinition;
      await cohortReviewApi()
        .getParticipantCohortStatuses(ns, wsid, +crid, filterRequest)
        .then(({ cohortReview }) => {
          request = JSON.parse(cohortReview.cohortDefinition);
          this.setState({
            cohortLoading: false,
            dateCreated: moment(cohortReview.creationTime).format('YYYY-MM-DD'),
            outdatedDefinition:
              cohortReview.creationTime < cohort.lastModifiedTime,
          });
        });
      return request;
    }

    groupChartData(data: any) {
      const groupedData = { AGE: {} };
      chartTypes.forEach((chartType, index) => {
        groupedData[chartType] = data[chartType].reduce((acc, typeInfo) => {
          if (acc[typeInfo.name]) {
            acc[typeInfo.name].count += typeInfo.count;
          } else {
            acc[typeInfo.name] = { name: typeInfo.name, count: typeInfo.count };
          }
          // Get the age range totals on the first iteration
          if (index === 0) {
            if (groupedData.AGE[typeInfo.ageRange]) {
              groupedData.AGE[typeInfo.ageRange].count += typeInfo.count;
            } else {
              groupedData.AGE[typeInfo.ageRange] = {
                name: typeInfo.ageRange,
                count: typeInfo.count,
              };
            }
          }
          return acc;
        }, {});
      });
      this.setState({ groupedData });
    }

    goBack() {
      const { ns, wsid, cid, crid } = this.props.match.params;
      this.props.navigate([
        'workspaces',
        ns,
        wsid,
        'data',
        'cohorts',
        cid,
        'reviews',
        crid,
      ]);
    }

    render() {
      const { cohort } = this.props;
      const {
        cdrName,
        data,
        dateCreated,
        groupedData,
        chartsLoading,
        cohortDefinition,
        cohortLoading,
        outdatedDefinition,
        participantCount,
      } = this.state;
      return (
        <React.Fragment>
          <style>{css}</style>
          {cohort && (
            <button
              style={styles.backBtn}
              type='button'
              onClick={() => this.goBack()}
            >
              Back to review sets
            </button>
          )}
          {cohortLoading && <SpinnerOverlay />}
          {cohort && (
            <div style={styles.reportBackground}>
              <div style={styles.container}>
                <div style={styles.row}>
                  <div style={columns.col6}>
                    <div style={styles.container}>
                      {outdatedDefinition && (
                        <div style={styles.queryHeader}>
                          Cohort Snapshot{' '}
                          <span
                            style={{
                              color: colors.warning,
                              fontSize: '14px',
                              fontWeight: 'normal',
                            }}
                          >
                            <img
                              src={outdated}
                              title='Outdated Cohort Definition'
                            />{' '}
                            Outdated
                          </span>
                        </div>
                      )}
                      <div style={styles.row}>
                        <div style={columns.col6}>
                          <div style={styles.queryTitle}>Cohort Name</div>
                          <div style={styles.queryContent}>{cohort.name}</div>
                          <div style={styles.queryTitle}>Created By</div>
                          <div style={styles.queryContent}>
                            {!!cohort ? cohort.creator : ''}
                          </div>
                        </div>
                        <div style={columns.col6}>
                          <div style={styles.queryTitle}>
                            Date {outdatedDefinition && <span>snapshot</span>}{' '}
                            created
                          </div>
                          <div style={styles.queryContent}>{dateCreated}</div>
                          <div style={styles.queryTitle}>Dataset</div>
                          <div style={styles.queryContent}>{cdrName}</div>
                        </div>
                        <div style={columns.col12}>
                          {!!cohortDefinition && (
                            <CohortDefinitionComponent
                              cohortDefinition={cohortDefinition}
                            />
                          )}
                        </div>
                      </div>
                    </div>
                  </div>
                  <div className='stats-left-padding' style={columns.col6}>
                    <div style={styles.container}>
                      <div style={styles.row}>
                        <div style={{ ...columns.col10, ...styles.queryTitle }}>
                          Descriptive Statistics
                        </div>
                        {/* TODO uncomment print icon when we know how we want to display the data download policy*/}
                        {/* <ClrIcon*/}
                        {/*  className='is-solid'*/}
                        {/*  style={{...columns.col2,*/}
                        {/*    ...(groupedData ? styles.print : styles.printDisabled)}}*/}
                        {/*  onClick={() => groupedData && window.print()}*/}
                        {/*  disabled={!groupedData}*/}
                        {/*  shape='printer'*/}
                        {/*  size={32} />*/}
                      </div>
                    </div>
                    {groupedData &&
                      Object.keys(groupedData).map((group, g) => (
                        <div key={g}>
                          <div style={styles.container}>
                            <div
                              style={{
                                ...styles.container,
                                ...styles.groupHeader,
                              }}
                            >
                              <div
                                style={{
                                  ...columns.col7,
                                  ...styles.groupText,
                                  textTransform: 'capitalize',
                                }}
                              >
                                {group.replace(/_/g, ' ').toLowerCase()}
                              </div>
                              {g === 0 && (
                                <div
                                  style={{
                                    ...columns.col2,
                                    ...styles.groupText,
                                  }}
                                >
                                  Total
                                </div>
                              )}
                              {g === 0 && (
                                <div
                                  style={{
                                    ...columns.col3,
                                    ...styles.groupText,
                                  }}
                                >
                                  % of Cohort
                                </div>
                              )}
                            </div>
                          </div>
                          {Object.values(groupedData[group]).map(
                            ({ count, name }, r) => (
                              <div key={r} style={styles.container}>
                                <div
                                  style={{
                                    ...styles.row,
                                    ...styles.groupContent,
                                  }}
                                >
                                  <div style={columns.col7}>{name}</div>
                                  <div style={columns.col2}>
                                    {count.toLocaleString()}
                                  </div>
                                  <div style={columns.col3}>
                                    {(count / participantCount) * 100 < 1
                                      ? '<1'
                                      : Math.round(
                                          (count / participantCount) * 100
                                        )}
                                    %
                                  </div>
                                </div>
                              </div>
                            )
                          )}
                        </div>
                      ))}
                    {chartsLoading && <SpinnerOverlay />}
                  </div>
                </div>
              </div>
              <div style={{ ...styles.container, margin: 0 }}>
                <div style={styles.row}>
                  <div
                    style={{
                      ...styles.col,
                      flex: '0 0 100%',
                      maxWidth: '100%',
                    }}
                  >
                    <div style={{ ...styles.container, margin: 0 }}>
                      <div style={styles.row}>
                        <div
                          style={{
                            ...styles.col,
                            flex: '0 0 100%',
                            maxWidth: '100%',
                          }}
                        >
                          <div>
                            <span style={styles.chartTitle}>Charts</span>
                          </div>
                        </div>
                      </div>
                    </div>
                    <div style={{ ...styles.container, margin: 0 }}>
                      <div style={{ ...styles.row, paddingTop: '1.5rem' }}>
                        <div
                          style={{
                            ...styles.col,
                            flex: '0 0 75%',
                            maxWidth: '75%',
                          }}
                        >
                          <div style={demoTitle}>Demographics</div>
                          <div style={styles.graphBorder}>
                            {data && (
                              <ComboChart
                                legendTitle='Age'
                                mode={'stacked'}
                                data={data}
                              />
                            )}
                            {chartsLoading && <SpinnerOverlay />}
                          </div>
                        </div>
                        <div
                          style={{
                            ...styles.col,
                            flex: '0 0 83.33333%',
                            maxWidth: '83.33333%',
                          }}
                        >
                          {!!cohort &&
                            domains.map((domain, i) => (
                              <div
                                key={i}
                                style={{
                                  minHeight: '15rem',
                                  position: 'relative',
                                }}
                              >
                                <ParticipantsCharts
                                  domain={domain}
                                  searchRequest={JSON.parse(cohort.criteria)}
                                />
                              </div>
                            ))}
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          )}
        </React.Fragment>
      );
    }
  }
);
