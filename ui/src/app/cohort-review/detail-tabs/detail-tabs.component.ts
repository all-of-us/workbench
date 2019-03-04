import {Component, OnDestroy, OnInit} from '@angular/core';
import * as fp from 'lodash/fp';

import {filterStateStore} from 'app/cohort-review/review-state.service';
import {typeToTitle} from 'app/cohort-search/utils';
import {currentWorkspaceStore, urlParamsStore} from 'app/utils/navigation';
import {CohortReviewService} from 'generated';
import {
  DomainType,
  PageFilterType,
} from 'generated';
import {Observable} from 'rxjs/Observable';
import {Subscription} from 'rxjs/Subscription';

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
  displayName: 'Vocabulary',
};
const standardName = {
  name: 'standardName',
  displayName: 'Name',
};
const standardCode = {
  name: 'standardCode',
  displayName: 'Code',
};

/*
  * TODO - uncomment below code when we will have source code and standard code radio button filter.
  */
//
// const sourceVocabulary = {
//   name: 'sourceVocabulary',
//   classNames: ['vocab-col'],
//   displayName: 'Source Vocabulary',
// };
// const sourceName = {
//   name: 'sourceName',
//   displayName: 'Source Name',
// };
// const sourceCode = {
//   name: 'sourceCode',
//   displayName: 'Source Code',
// };

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

const initialfilterState = {
  ALL_EVENTS: {
    standardVocabulary: [],
    domain: [],
  },
  PROCEDURE: {
    standardVocabulary: [],
  },
  CONDITION: {
    standardVocabulary: [],
  },
  OBSERVATION: {
    standardVocabulary: [],
  },
  PHYSICAL_MEASURE: {
    standardVocabulary: [],
  },
};

@Component({
  selector: 'app-detail-tabs',
  templateUrl: './detail-tabs.component.html',
  styleUrls: ['./detail-tabs.component.css']
})
export class DetailTabsComponent implements OnInit, OnDestroy {
  subscription: Subscription;
  participantId: any;
  chartData = {};
  domainList = [DomainType[DomainType.CONDITION],
    DomainType[DomainType.PROCEDURE],
    DomainType[DomainType.DRUG]];
  conditionTitle: string;
  chartLoadedSpinner = false;
  summaryActive = false;
  filterState: any;
  readonly allEvents = {
    name: 'All Events',
    domain: DomainType.ALLEVENTS,
    filterType: PageFilterType.ReviewFilter,
    columns: [
      visitType, standardCode, standardVocabulary, standardName, value,
      domain, ageAtEvent
    ],
    reverseEnum: {
      visitType: visitType,
      standardCode: standardCode,
      standardVocabulary: standardVocabulary,
      standardName: standardName,
      value: value,
      domain: domain,
      age: ageAtEvent,
    },
  };

  readonly tabs = [{
    name: 'Conditions',
    domain: DomainType.CONDITION,
    filterType: PageFilterType.ReviewFilter,
    columns: [
      standardCode, standardVocabulary, standardName, ageAtEvent, visitType
    ],
    reverseEnum: {
      standardName: standardName,
      standardCode: standardCode,
      standardVocabulary: standardVocabulary,
      age: ageAtEvent,
      visitType: visitType,
    },
  }, {
    name: 'Procedures',
    domain: DomainType.PROCEDURE,
    filterType: PageFilterType.ReviewFilter,
    columns: [
      standardCode, standardVocabulary, standardName, ageAtEvent, visitType
    ],
    reverseEnum: {
      standardName: standardName,
      standardCode: standardCode,
      standardVocabulary: standardVocabulary,
      age: ageAtEvent,
      visitType: visitType,
    }
  }, {
    name: 'Drugs',
    domain: DomainType.DRUG,
    filterType: PageFilterType.ReviewFilter,
    columns: [
      itemDate, standardName, ageAtEvent, numMentions,
      firstMention, lastMention, visitType
    ],
    reverseEnum: {
      itemDate: itemDate,
      standardName: standardName,
      age: ageAtEvent,
      numMentions: numMentions,
      firstMention: firstMention,
      lastMention: lastMention,
      visitType: visitType,
    }
  }, {
    name: 'Observations',
    domain: DomainType.OBSERVATION,
    filterType: PageFilterType.ReviewFilter,
    columns: [
      itemDate, standardName, standardCode, standardVocabulary, ageAtEvent, visitType
    ],
    reverseEnum: {
      itemDate: itemDate,
      standardName: standardName,
      standardCode: standardCode,
      standardVocabulary: standardVocabulary,
      age: ageAtEvent,
      visitType: visitType,
    }
  }, {
    name: 'Physical Measurements',
    domain: DomainType.PHYSICALMEASURE,
    filterType: PageFilterType.ReviewFilter,
    columns: [
      itemDate, standardCode, standardVocabulary, standardName, value, ageAtEvent
    ],
    reverseEnum: {
      itemDate: itemDate,
      standardCode: standardCode,
      standardVocabulary: standardVocabulary,
      standardName: standardName,
      value: value,
      age: ageAtEvent,
    }
  }, {
    name: 'Labs',
    domain: DomainType.LAB,
    filterType: PageFilterType.ReviewFilter,
    columns: [
      itemDate, itemTime, standardName, value, ageAtEvent, visitType
    ],
    reverseEnum: {
      itemDate: itemDate,
      itemTime: itemTime,
      standardName: standardName,
      value: value,
      age: ageAtEvent,
      visitType: visitType
    }
  }, {
    name: 'Vitals',
    domain: DomainType.VITAL,
    filterType: PageFilterType.ReviewFilter,
    columns: [
      itemDate, itemTime, standardName, value, ageAtEvent, visitType
    ],
    reverseEnum: {
      itemDate: itemDate,
      itemTime: itemTime,
      standardName: standardName,
      value: value,
      age: ageAtEvent,
      visitType: visitType
    }
  }, {
    name: 'Surveys',
    domain: DomainType.SURVEY,
    filterType: PageFilterType.ReviewFilter,
    columns: [
      itemDate, survey, question, answer
    ],
    reverseEnum: {
      itemDate: itemDate,
      survey: survey,
      question: question,
      answer: answer
    }
  }];

  constructor(
    private reviewAPI: CohortReviewService,
  ) {
    this.filteredData = this.filteredData.bind(this);
  }

  ngOnInit() {
    this.subscription = Observable
      .combineLatest(urlParamsStore, currentWorkspaceStore)
      .map(([{ns, wsid, cid, pid}, {cdrVersionId}]) => ({ns, wsid, cid, pid, cdrVersionId}))
      .distinctUntilChanged(fp.isEqual)
      .switchMap(({ns, wsid, cid, pid, cdrVersionId}) => {
        this.participantId = pid;
        return Observable.forkJoin(
          ...this.domainList.map(domainName => {
            this.chartData[domainName] = {
              loading: true,
              conditionTitle: '',
              items: []
            };
            return this.reviewAPI
              .getParticipantChartData(ns, wsid, cid, cdrVersionId, pid, domainName, 10)
              .do(({items}) => {
                this.chartData[domainName] = {
                  loading: false,
                  conditionTitle: typeToTitle(domainName),
                  items
                };
              });
          })
        );
      })
      .subscribe();

    this.subscription.add(filterStateStore.subscribe(filterState => {
      this.filterState = filterState === null ? initialfilterState : filterState;
    }));
  }

  filteredData(_domain: string, checkedItems: any) {
    this.filterState[_domain] = checkedItems;
    filterStateStore.next(this.filterState);
  }

  ngOnDestroy() {
    this.subscription.unsubscribe();
  }
}
