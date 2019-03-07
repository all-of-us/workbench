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
  summaryActive = false;
  filterState: any;
  vocab: string;
  readonly allEvents = {
    name: 'All Events',
    domain: DomainType.ALLEVENTS,
    filterType: PageFilterType.ReviewFilter,
    columns: {
      standard: [
        itemDate, visitType, standardCode, standardVocabulary, standardName, value,
        domain, ageAtEvent
      ],
      source: [
        itemDate, visitType, sourceCode, sourceVocabulary, sourceName, value, domain, ageAtEvent
      ],
    }
  };

  readonly tabs = [{
    name: 'Conditions',
    domain: DomainType.CONDITION,
    filterType: PageFilterType.ReviewFilter,
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
    filterType: PageFilterType.ReviewFilter,
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
    filterType: PageFilterType.ReviewFilter,
    columns: {
      standard: [
        itemDate, standardName, ageAtEvent, numMentions, firstMention, lastMention, visitType
      ],
      source: [
        itemDate, sourceName, ageAtEvent, numMentions, firstMention, lastMention, visitType
      ],
    }
  }, {
    name: 'Observations',
    domain: DomainType.OBSERVATION,
    filterType: PageFilterType.ReviewFilter,
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
    domain: DomainType.PHYSICALMEASURE,
    filterType: PageFilterType.ReviewFilter,
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
    filterType: PageFilterType.ReviewFilter,
    columns: {
      standard: [
        itemDate, itemTime, standardName, value, ageAtEvent, visitType
      ],
      source: [
        itemDate, itemTime, sourceName, value, ageAtEvent, visitType
      ],
    }
  }, {
    name: 'Vitals',
    domain: DomainType.VITAL,
    filterType: PageFilterType.ReviewFilter,
    columns: {
      standard: [
        itemDate, itemTime, standardName, value, ageAtEvent, visitType
      ],
      source: [
        itemDate, itemTime, sourceName, value, ageAtEvent, visitType
      ],
    }
  }, {
    name: 'Surveys',
    domain: DomainType.SURVEY,
    filterType: PageFilterType.ReviewFilter,
    columns: {
      standard: [
        itemDate, survey, question, answer
      ],
      source: [
        itemDate, survey, question, answer
      ],
    }
  }];

  constructor(
    private reviewAPI: CohortReviewService,
  ) {}

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
      this.vocab = filterState.vocab;
      this.filterState = filterState;
    }));
  }

  ngOnDestroy() {
    this.subscription.unsubscribe();
  }
}
