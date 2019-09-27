import * as fp from 'lodash/fp';
import * as React from 'react';

import {domainToTitle} from 'app/cohort-search/utils';
import {SpinnerOverlay} from 'app/components/spinners';
import {DetailTabTable} from 'app/pages/data/cohort-review/detail-tab-table.component';
import {IndividualParticipantsCharts} from 'app/pages/data/cohort-review/individual-participants-charts';
import {cohortReviewStore, filterStateStore} from 'app/services/review-state.service';
import {cohortReviewApi} from 'app/services/swagger-fetch-clients';
import {reactStyles, withCurrentWorkspace} from 'app/utils';
import {triggerEvent} from 'app/utils/analytics';
import {urlParamsStore} from 'app/utils/navigation';
import {WorkspaceData} from 'app/utils/workspace-data';
import {DomainType} from 'generated/fetch';
import {TabPanel, TabView} from 'primereact/tabview';
import {Observable} from 'rxjs/Observable';
import {from} from 'rxjs/observable/from';

const styles = reactStyles({
  container: {
    width: '100%',
    margin: '0.5rem 0',
    paddingLeft: '0.5rem',
    paddingRight: '0.5rem',
  },
  row: {
    display: 'flex',
    flexWrap: 'wrap',
    marginRight: '-.5rem',
    marginLeft: '-.5rem',
  },
  col: {
    position: 'relative',
    minHeight: '8rem',
    width: '100%',
    paddingLeft: '0.5rem',
    paddingRight: '0.5rem',
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
};
const itemTime = {
  name: 'itemTime',
  classNames: ['time-col'],
  displayName: 'Time',
};
const domain = {
  name: 'domain',
  displayName: 'Domain',
};
const standardVocabulary = {
  name: 'standardVocabulary',
  classNames: ['vocab-col'],
  displayName: 'Standard Vocabulary',
};
const standardName = {
  name: 'standardName',
  displayName: 'Standard Name',
};
const standardCode = {
  name: 'standardCode',
  displayName: 'Standard Code',
};
const sourceVocabulary = {
  name: 'sourceVocabulary',
  classNames: ['vocab-col'],
  displayName: 'Source Vocabulary',
};
const sourceName = {
  name: 'sourceName',
  displayName: 'Source Name',
};
const sourceCode = {
  name: 'sourceCode',
  displayName: 'Source Code',
};
const value = {
  name: 'value',
  displayName: 'Value',
};
const ageAtEvent = {
  name: 'ageAtEvent',
  displayName: 'Age At Event',
};
const visitType = {
  name: 'visitType',
  displayName: 'Visit Type',
};
const numMentions = {
  name: 'numMentions',
  displayName: 'Number Of Mentions',
};
const firstMention = {
  name: 'firstMention',
  displayName: 'Date First Mention',
};
const lastMention = {
  name: 'lastMention',
  displayName: 'Date Last Mention',
};
const survey = {
  name: 'survey',
  displayName: 'Survey Name',
};
const question = {
  name: 'question',
  displayName: 'Question',
};
const answer = {
  name: 'answer',
  displayName: 'Answer',
};
const graph = {
  name: 'graph',
  displayName: ' '
};

const tabs = [
  {
    name: 'All Events',
    domain: DomainType.ALLEVENTS,
    columns: {
      standard: [
        itemDate, visitType, standardCode, standardVocabulary, standardName, value,
        domain, ageAtEvent
      ],
      source: [
        itemDate, visitType, sourceCode, sourceVocabulary, sourceName, value, domain, ageAtEvent
      ],
    }
  }, {
    name: 'Conditions',
    domain: DomainType.CONDITION,
    columns: {
      standard: [
        itemDate, standardCode, standardVocabulary, standardName, ageAtEvent, visitType
      ],
      source: [
        itemDate, sourceCode, sourceVocabulary, sourceName, ageAtEvent, visitType
      ],
    }
  }, {
    name: 'Procedures',
    domain: DomainType.PROCEDURE,
    columns: {
      standard: [
        itemDate, standardCode, standardVocabulary, standardName, ageAtEvent, visitType
      ],
      source: [
        itemDate, sourceCode, sourceVocabulary, sourceName, ageAtEvent, visitType
      ],
    }
  }, {
    name: 'Drugs',
    domain: DomainType.DRUG,
    columns: {
      standard: [
        itemDate, standardName, ageAtEvent, numMentions, firstMention, lastMention, visitType
      ],
      source: [
        itemDate, standardName, ageAtEvent, numMentions, firstMention, lastMention, visitType
      ],
    }
  }, {
    name: 'Observations',
    domain: DomainType.OBSERVATION,
    columns: {
      standard: [
        itemDate, standardName, standardCode, standardVocabulary, ageAtEvent, visitType
      ],
      source: [
        itemDate, sourceName, sourceCode, sourceVocabulary, ageAtEvent, visitType
      ],
    }
  }, {
    name: 'Physical Measurements',
    domain: DomainType.PHYSICALMEASUREMENT,
    columns: {
      standard: [
        itemDate, standardCode, standardVocabulary, standardName, value, ageAtEvent
      ],
      source: [
        itemDate, sourceCode, sourceVocabulary, sourceName, value, ageAtEvent
      ],
    }
  }, {
    name: 'Labs',
    domain: DomainType.LAB,
    columns: {
      standard: [
        itemDate, itemTime, standardName, graph, value, ageAtEvent, visitType
      ],
      source: [
        itemDate, itemTime, standardName, graph, value, ageAtEvent, visitType
      ],
    }
  }, {
    name: 'Vitals',
    domain: DomainType.VITAL,
    columns: {
      standard: [
        itemDate, itemTime, standardName, graph, value, ageAtEvent, visitType
      ],
      source: [
        itemDate, itemTime, standardName, graph, value, ageAtEvent, visitType
      ],
    }
  }, {
    name: 'Surveys',
    domain: DomainType.SURVEY,
    columns: {
      standard: [
        itemDate, survey, question, answer
      ],
      source: [
        itemDate, survey, question, answer
      ],
    }
  }
];

const domainList = [
  DomainType[DomainType.CONDITION],
  DomainType[DomainType.PROCEDURE],
  DomainType[DomainType.DRUG]
];
const EVENT_CATEGORY = 'Review Individual';

interface Props {
  workspace: WorkspaceData;
}

interface State {
  activeTab: number;
  chartData: any;
  conditionTitle: string;
  filterState: any;
  participantId: number;
  updateState: number;
}

export const DetailTabs = withCurrentWorkspace()(
  class extends React.Component<Props, State> {
    private subscription;
    constructor(props: any) {
      super(props);
      this.state = {
        activeTab: 0,
        chartData: {},
        conditionTitle: null,
        filterState: null,
        participantId: null,
        updateState: 0,
      };
      this.filteredData = this.filteredData.bind(this);
    }

    componentDidMount() {
      this.subscription = urlParamsStore.distinctUntilChanged(fp.isEqual)
        .filter(({pid}) => !!pid)
        .switchMap(({ns, wsid, cid, pid}) => {
          const chartData = {};
          return Observable.forkJoin(
            ...domainList.map(domainName => {
              chartData[domainName] = {
                loading: true,
                conditionTitle: '',
                items: []
              };
              this.setState({chartData, participantId: pid});
              return from(cohortReviewApi()
                .getParticipantChartData(ns, wsid,
                  cohortReviewStore.getValue().cohortReviewId, pid, domainName, 10))
                .do(({items}) => {
                  chartData[domainName] = {
                    loading: false,
                    conditionTitle: domainToTitle(domainName),
                    items
                  };
                  this.setState({chartData});
                });
            })
          );
        })
        .subscribe();

      filterStateStore.subscribe(filterState => {
        let {updateState} = this.state;
        // this.vocab = filterState.vocab;
        updateState++;
        this.setState({filterState, updateState});
      });
    }

    componentWillUnmount() {
      this.subscription.unsubscribe();
    }

    filteredData(_domain: string, checkedItems: any) {
      const {filterState} = this.state;
      filterState[_domain] = checkedItems;
      filterStateStore.next(filterState);
    }

    tabChange = (e: any) => {
      const tab = e.index === 0 ? 'Summary' : tabs[e.index - 1].name;
      triggerEvent(EVENT_CATEGORY, 'Click', `${tab} - Review Individual`);
      this.setState({activeTab: e.index});
    }

    chartHover = (data: any) => {
      if (data.conditionTitle) {
        triggerEvent(EVENT_CATEGORY, 'hover', `${data.conditionTitle} Chart - Review Individual`);
      }
    }

    render() {
      const {activeTab, chartData, filterState, participantId, updateState} = this.state;
      return <React.Fragment>
        <style>{css}</style>
        <TabView style={{padding: 0}} activeIndex={activeTab} onTabChange={this.tabChange}>
          <TabPanel header='Summary'>
            <div style={styles.container}>
              <div style={styles.row}>
                {domainList.map((dom, d) => {
                  return <div key={d} style={styles.col}>
                    {chartData[dom] && <div onMouseEnter={() => this.chartHover(chartData[dom])}>
                      {chartData[dom].loading && <SpinnerOverlay/>}
                      {!chartData[dom].loading && !chartData[dom].items.length && <div>
                        There are no {chartData[dom].conditionTitle} to show for this participant.
                      </div>}
                      <IndividualParticipantsCharts chartData={chartData[dom]}/>
                    </div>}
                  </div>;
                })}
              </div>
            </div>
          </TabPanel>
          {tabs.map((tab, t) => {
            return <TabPanel key={t} header={tab.name}>
              {filterState && <DetailTabTable
                getFilteredData={this.filteredData}
                filterState={filterState}
                updateState={updateState}
                tabName={tab.name}
                columns={tab.columns[filterState.vocab]}
                domain={tab.domain}
                participantId={participantId}
              />}
            </TabPanel>;
          })}
        </TabView>
      </React.Fragment>;
    }
  }
);
