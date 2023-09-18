import * as React from 'react';
import { RouteComponentProps, withRouter } from 'react-router-dom';
import * as fp from 'lodash/fp';
import { TabPanel, TabView } from 'primereact/tabview';

import { CohortReview, Domain, FilterColumns } from 'generated/fetch';

import { SpinnerOverlay } from 'app/components/spinners';
import { domainToTitle } from 'app/pages/data/cohort/utils';
import { DetailTabTable } from 'app/pages/data/cohort-review/detail-tab-table.component';
import { IndividualParticipantsCharts } from 'app/pages/data/cohort-review/individual-participants-charts';
import { filterStateStore } from 'app/services/review-state.service';
import { cohortReviewApi } from 'app/services/swagger-fetch-clients';
import {
  hasNewValidProps,
  reactStyles,
  withCurrentCohortReview,
  withCurrentWorkspace,
} from 'app/utils';
import { triggerEvent } from 'app/utils/analytics';
import { MatchParams } from 'app/utils/stores';
import { WorkspaceData } from 'app/utils/workspace-data';

const styles = reactStyles({
  container: {
    width: '100%',
    margin: '0.75rem 0',
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
    minHeight: '12rem',
    width: '100%',
    paddingLeft: '0.75rem',
    paddingRight: '0.75rem',
    flex: '0 0 33.33333%',
    maxWidth: '33.33333%',
  },
});

const css = `
  body .p-tabview .p-tabview-panels {
    background-color: transparent;
    border: 0;
    padding: 0;
  }
  body .p-tabview.p-tabview-top .p-tabview-nav li,
  body .p-tabview.p-tabview-top .p-tabview-nav li:focus {
    padding: 0.571em 0.7em;
    box-shadow: none;
  }
  body .p-tabview.p-tabview-top .p-tabview-nav li a {
    padding: 0;
  }
  body .p-tabview.p-tabview-top .p-tabview-nav li:hover,
  body .p-tabview.p-tabview-top .p-tabview-nav li:not(.p-highlight):not(.p-disabled):hover a,
  body .p-tabview.p-tabview-top .p-tabview-nav li a,
  body .p-tabview.p-tabview-top .p-tabview-nav li a:hover {
    background-color: transparent;
    color: #2691D0;
    font-size: 14px;
    border: 0;
  }
  body .p-tabview.p-tabview-top .p-tabview-nav li.p-highlight:hover,
  body .p-tabview.p-tabview-top .p-tabview-nav li.p-highlight:hover a,
  body .p-tabview.p-tabview-top .p-tabview-nav li.p-highlight a,
  body .p-tabview.p-tabview-top .p-tabview-nav li.p-highlight a:hover,
  body .p-tabview.p-tabview-top .p-tabview-nav li.p-highlight a:focus {
    background: transparent;
    color: #2691D0;
    font-weight: bold;
    border: 0;
    box-shadow: none;
  }
  body .p-tabview.p-tabview-top .p-tabview-nav li.p-highlight:hover a,
  body .p-tabview.p-tabview-top .p-tabview-nav li.p-highlight a,
  body .p-tabview.p-tabview-top .p-tabview-nav li.p-highlight a:hover,
  body .p-tabview.p-tabview-top .p-tabview-nav li.p-highlight a:focus {
    border-bottom: 3px solid #2691D0;
  }
`;

/* The most common column types */
const itemDate = {
  name: 'itemDate',
  classNames: ['date-col'],
  displayName: 'Date',
  filter: FilterColumns.START_DATETIME,
};
const itemTime = {
  name: 'itemTime',
  classNames: ['time-col'],
  displayName: 'Time',
};
const domain = {
  name: 'domain',
  displayName: 'Domain',
  filter: FilterColumns.DOMAIN,
};
const standardVocabulary = {
  name: 'standardVocabulary',
  classNames: ['vocab-col'],
  displayName: 'Standard Vocabulary',
  filter: FilterColumns.STANDARD_VOCABULARY,
};
const standardName = {
  name: 'standardName',
  displayName: 'Standard Name',
  filter: FilterColumns.STANDARD_NAME,
};
const standardCode = {
  name: 'standardCode',
  displayName: 'Standard Code',
  filter: FilterColumns.STANDARD_CODE,
};
const sourceVocabulary = {
  name: 'sourceVocabulary',
  classNames: ['vocab-col'],
  displayName: 'Source Vocabulary',
  filter: FilterColumns.SOURCE_VOCABULARY,
};
const sourceName = {
  name: 'sourceName',
  displayName: 'Source Name',
  filter: FilterColumns.SOURCE_NAME,
};
const sourceCode = {
  name: 'sourceCode',
  displayName: 'Source Code',
  filter: FilterColumns.SOURCE_CODE,
};
const value = {
  name: 'value',
  displayName: 'Value',
  filter: FilterColumns.VALUE_AS_NUMBER,
};
const ageAtEvent = {
  name: 'ageAtEvent',
  displayName: 'Age At Event',
  filter: FilterColumns.AGE_AT_EVENT,
};
const visitType = {
  name: 'visitType',
  displayName: 'Visit Type',
  filter: FilterColumns.VISIT_TYPE,
};
const numMentions = {
  name: 'numMentions',
  displayName: 'Number Of Mentions',
  filter: FilterColumns.NUM_MENTIONS,
};
const firstMention = {
  name: 'firstMention',
  displayName: 'Date First Mention',
  filter: FilterColumns.FIRST_MENTION,
};
const lastMention = {
  name: 'lastMention',
  displayName: 'Date Last Mention',
  filter: FilterColumns.LAST_MENTION,
};
const survey = {
  name: 'survey',
  displayName: 'Survey Name',
  filter: FilterColumns.SURVEY_NAME,
};
const question = {
  name: 'question',
  displayName: 'Question',
  filter: FilterColumns.QUESTION,
};
const answer = {
  name: 'answer',
  displayName: 'Answer',
  filter: FilterColumns.ANSWER,
};
const graph = {
  name: 'graph',
  displayName: ' ',
};

const tabs = [
  {
    name: 'All Events',
    domain: Domain.ALL_EVENTS,
    columns: {
      standard: [
        itemDate,
        visitType,
        standardCode,
        standardVocabulary,
        standardName,
        value,
        domain,
        ageAtEvent,
      ],
      source: [
        itemDate,
        visitType,
        sourceCode,
        sourceVocabulary,
        sourceName,
        value,
        domain,
        ageAtEvent,
      ],
    },
  },
  {
    name: 'Conditions',
    domain: Domain.CONDITION,
    columns: {
      standard: [
        itemDate,
        standardCode,
        standardVocabulary,
        standardName,
        ageAtEvent,
        visitType,
      ],
      source: [
        itemDate,
        sourceCode,
        sourceVocabulary,
        sourceName,
        ageAtEvent,
        visitType,
      ],
    },
  },
  {
    name: 'Procedures',
    domain: Domain.PROCEDURE,
    columns: {
      standard: [
        itemDate,
        standardCode,
        standardVocabulary,
        standardName,
        ageAtEvent,
        visitType,
      ],
      source: [
        itemDate,
        sourceCode,
        sourceVocabulary,
        sourceName,
        ageAtEvent,
        visitType,
      ],
    },
  },
  {
    name: 'Drugs',
    domain: Domain.DRUG,
    columns: {
      standard: [
        itemDate,
        standardName,
        ageAtEvent,
        numMentions,
        firstMention,
        lastMention,
        visitType,
      ],
      source: [
        itemDate,
        standardName,
        ageAtEvent,
        numMentions,
        firstMention,
        lastMention,
        visitType,
      ],
    },
  },
  {
    name: 'Observations',
    domain: Domain.OBSERVATION,
    columns: {
      standard: [
        itemDate,
        standardName,
        standardCode,
        standardVocabulary,
        ageAtEvent,
        visitType,
      ],
      source: [
        itemDate,
        sourceName,
        sourceCode,
        sourceVocabulary,
        ageAtEvent,
        visitType,
      ],
    },
  },
  {
    name: 'Physical Measurements',
    domain: Domain.PHYSICAL_MEASUREMENT,
    columns: {
      standard: [
        itemDate,
        standardCode,
        standardVocabulary,
        standardName,
        value,
        ageAtEvent,
      ],
      source: [
        itemDate,
        sourceCode,
        sourceVocabulary,
        sourceName,
        value,
        ageAtEvent,
      ],
    },
  },
  {
    name: 'Labs',
    domain: Domain.LAB,
    columns: {
      standard: [
        itemDate,
        itemTime,
        standardName,
        graph,
        value,
        ageAtEvent,
        visitType,
      ],
      source: [
        itemDate,
        itemTime,
        standardName,
        graph,
        value,
        ageAtEvent,
        visitType,
      ],
    },
  },
  {
    name: 'Vitals',
    domain: Domain.VITAL,
    columns: {
      standard: [
        itemDate,
        itemTime,
        standardName,
        graph,
        value,
        ageAtEvent,
        visitType,
      ],
      source: [
        itemDate,
        itemTime,
        standardName,
        graph,
        value,
        ageAtEvent,
        visitType,
      ],
    },
  },
  {
    name: 'Surveys',
    domain: Domain.SURVEY,
    columns: {
      standard: [itemDate, survey, question, answer],
      source: [itemDate, survey, question, answer],
    },
  },
];

const domainList = [
  Domain[Domain.CONDITION],
  Domain[Domain.PROCEDURE],
  Domain[Domain.DRUG],
];
const EVENT_CATEGORY = 'Review Individual';

interface Props extends RouteComponentProps<MatchParams> {
  cohortReview: CohortReview;
  workspace: WorkspaceData;
}

interface State {
  activeTab: number;
  chartData: any;
  conditionTitle: string;
  filterState: any;
  updateState: number;
}

export const DetailTabs = fp.flow(
  withCurrentCohortReview(),
  withCurrentWorkspace(),
  withRouter
)(
  class extends React.Component<Props, State> {
    private subscription;
    constructor(props: any) {
      super(props);
      this.state = {
        activeTab: 0,
        chartData: {},
        conditionTitle: null,
        filterState: null,
        updateState: 0,
      };
      this.filteredData = this.filteredData.bind(this);
    }

    componentDidMount() {
      this.subscription = filterStateStore.subscribe((filterState) => {
        let { updateState } = this.state;
        // this.vocab = filterState.vocab;
        updateState++;
        this.setState({ filterState, updateState });
      });
      this.loadParticipantChartData();
    }

    componentDidUpdate(prevProps) {
      if (
        hasNewValidProps(this.props, prevProps, [(p) => p.match.params.pid])
      ) {
        this.loadParticipantChartData();
      }
    }

    componentWillUnmount() {
      this.subscription.unsubscribe();
    }

    loadParticipantChartData() {
      const { ns, wsid, pid, crid } = this.props.match.params;
      fp.map(async (domainName: string) => {
        this.setState((prevState) => ({
          chartData: {
            ...prevState.chartData,
            [domainName]: {
              loading: true,
              conditionTitle: '',
              items: [],
            },
          },
        }));
        const { items } = await cohortReviewApi().getParticipantChartData(
          ns,
          wsid,
          +crid,
          +pid,
          domainName,
          10
        );
        this.setState((prevState) => ({
          chartData: {
            ...prevState.chartData,
            [domainName]: {
              loading: false,
              conditionTitle: domainToTitle(domainName),
              items: items,
            },
          },
        }));
      })(domainList);
    }

    filteredData(_domain: string, checkedItems: any) {
      const { filterState } = this.state;
      filterState[_domain] = checkedItems;
      filterStateStore.next(filterState);
    }

    tabChange = (e: any) => {
      const tab = e.index === 0 ? 'Summary' : tabs[e.index - 1].name;
      triggerEvent(EVENT_CATEGORY, 'Click', `${tab} - Review Individual`);
      this.setState({ activeTab: e.index });
    };

    chartHover = (data: any) => {
      if (data.conditionTitle) {
        triggerEvent(
          EVENT_CATEGORY,
          'hover',
          `${data.conditionTitle} Chart - Review Individual`
        );
      }
    };

    render() {
      const { activeTab, chartData, filterState, updateState } = this.state;
      return (
        <React.Fragment>
          <style>{css}</style>
          <TabView
            style={{ padding: 0 }}
            activeIndex={activeTab}
            onTabChange={this.tabChange}
          >
            <TabPanel header='Summary'>
              <div style={styles.container}>
                <div style={styles.row}>
                  {domainList.map((dom, d) => {
                    return (
                      <div key={d} style={styles.col}>
                        {chartData[dom] && (
                          <div
                            onMouseEnter={() => this.chartHover(chartData[dom])}
                          >
                            {chartData[dom].loading && <SpinnerOverlay />}
                            {!chartData[dom].loading &&
                              !chartData[dom].items.length && (
                                <div>
                                  There are no {chartData[dom].conditionTitle}{' '}
                                  to show for this participant.
                                </div>
                              )}
                            <IndividualParticipantsCharts
                              chartData={chartData[dom]}
                            />
                          </div>
                        )}
                      </div>
                    );
                  })}
                </div>
              </div>
            </TabPanel>
            {tabs.map((tab, t) => {
              return (
                <TabPanel key={t} header={tab.name}>
                  {filterState && (
                    <DetailTabTable
                      getFilteredData={this.filteredData}
                      filterState={filterState}
                      updateState={updateState}
                      tabName={tab.name}
                      columns={tab.columns[filterState.vocab]}
                      domain={tab.domain}
                      participantId={this.props.match.params.pid}
                    />
                  )}
                </TabPanel>
              );
            })}
          </TabView>
        </React.Fragment>
      );
    }
  }
);
