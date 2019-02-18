import {Component, Input, OnChanges, OnDestroy, OnInit} from '@angular/core';
import {ActivatedRoute} from '@angular/router';
import {ReviewStateService} from 'app/cohort-review/review-state.service';
import {typeToTitle} from 'app/cohort-search/utils';
import {currentWorkspaceStore, urlParamsStore} from 'app/utils/navigation';
import {CohortReviewService} from 'generated';
import {
  DomainType,
  PageFilterType,
} from 'generated';
import {Subscription} from 'rxjs/Subscription';

/* The most common column types */
const itemDate = {
  name: 'itemDate',
  classNames: ['date-col'],
  displayName: 'Date',
};
const itemTime = {
  name: 'itemDate',
  classNames: ['time-col'],
  displayName: 'Time',
};
const endDate = {
  name: 'endDate',
  classNames: ['date-col'],
  displayName: 'End Date',
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
const signature = {
  name: 'signature',
  displayName: 'Signature',
};
const valueConcept = {
  name: 'valueConcept',
  displayName: 'Concept Value',
};
const valueSource = {
  name: 'valueSource',
  displayName: 'Source Value',
};
const value = {
  name: 'value',
  displayName: 'Value',
};
const sourceCode = {
  name: 'sourceCode',
  displayName: 'Source Code',
};
const ageAtEvent = {
  name: 'ageAtEvent',
  displayName: 'Age At Event',
};
const visitType = {
  name: 'visitType',
  displayName: 'Visit Type',
};
const visitId = {
  name: 'visitId',
  displayName: 'Visit ID',
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

const dose = {
  name: 'dose',
  displayName: 'Dose',
};
const refills = {
  name: 'refills',
  displayName: 'Refills',
};
const strength = {
  name: 'strength',
  displayName: 'Strength',
};
const dataRoute = {
  name: 'route',
  displayName: 'Route',
};
const unit = {
  name: 'unit',
  displayName: 'Units',
};
const refRange = {
  name: 'refRange',
  displayName: 'Reference Range',
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
export class DetailTabsComponent implements OnChanges, OnInit, OnDestroy {
  subscription: Subscription;
  data;
  participantsId: any;
  chartData = {};
  domainList = [DomainType[DomainType.CONDITION],
    DomainType[DomainType.PROCEDURE],
    DomainType[DomainType.DRUG]];
  conditionTitle: string;
  chartLoadedSpinner = false;
  @Input() clickedParticipantId: number;
  summaryActive = false;
  readonly allEvents = {
    name: 'All Events',
    domain: DomainType.ALLEVENTS,
    filterType: PageFilterType.ReviewFilter,
    columns: [
      itemDate, visitType, standardCode, standardVocabulary, standardName, sourceCode,
      sourceVocabulary, sourceName, dataRoute, dose, strength, value, unit, refRange,
      domain, ageAtEvent, numMentions, firstMention, lastMention
    ],
    reverseEnum: {
      Date: itemDate,
      Time: itemDate,
      visitType: visitType,
      standardCode: standardCode,
      standardVocabulary: standardVocabulary,
      standardName: standardName,
      sourceCode: sourceCode,
      sourceVocabulary: sourceVocabulary,
      sourceName: sourceName,
      dataRoute: dataRoute,
      quantity: dose,
      strength: strength,
      value: value,
      unit: unit,
      refRange: refRange,
      domain: domain,
      age: ageAtEvent,
      numMentions: numMentions,
      firstMention: firstMention,
      lastMention: lastMention,
    }
  };

  readonly tabs = [{
    name: 'Conditions',
    domain: DomainType.CONDITION,
    filterType: PageFilterType.ReviewFilter,
    columns: [
      itemDate, standardCode, standardVocabulary, standardName, sourceCode, sourceVocabulary,
      sourceName, ageAtEvent, visitType
    ],
    reverseEnum: {
      itemDate: itemDate,
      standardCode: standardCode,
      standardVocabulary: standardVocabulary,
      standardName: standardName,
      sourceCode: sourceCode,
      sourceVocabulary: sourceVocabulary,
      sourceName: sourceName,
      age: ageAtEvent,
      visitType: visitType,
    }
  }, {
    name: 'Procedures',
    domain: DomainType.PROCEDURE,
    filterType: PageFilterType.ReviewFilter,
    columns: [
      itemDate, standardCode, standardVocabulary, standardName, sourceCode, sourceVocabulary,
      sourceName, ageAtEvent, visitType
    ],
    reverseEnum: {
      itemDate: itemDate,
      standardCode: standardCode,
      standardVocabulary: standardVocabulary,
      standardName: standardName,
      sourceCode: sourceCode,
      sourceVocabulary: sourceVocabulary,
      sourceName: sourceName,
      age: ageAtEvent,
      visitType: visitType,
    }
  }, {
    name: 'Drugs',
    domain: DomainType.DRUG,
    filterType: PageFilterType.ReviewFilter,
    columns: [
      itemDate, standardName, dataRoute, dose, strength, ageAtEvent, numMentions,
      firstMention, lastMention, visitType
    ],
    reverseEnum: {
      itemDate: itemDate,
      standardName: standardName,
      dataRoute: dataRoute,
      dose: dose,
      strength: strength,
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
      itemDate, standardName, standardCode, standardVocabulary, ageAtEvent, sourceName,
      sourceCode, sourceVocabulary, visitId
    ],
    reverseEnum: {
      itemDate: itemDate,
      standardName: standardName,
      standardCode: standardCode,
      standardVocabulary: standardVocabulary,
      age: ageAtEvent,
      sourceName: sourceName,
      sourceCode: sourceCode,
      sourceVocabulary: sourceVocabulary,
      visitId: visitId,
    }
  }, {
    name: 'Physical Measurements',
    domain: DomainType.PHYSICALMEASURE,
    filterType: PageFilterType.ReviewFilter,
    columns: [
      itemDate, standardCode, standardVocabulary, standardName, value, unit, ageAtEvent
    ],
    reverseEnum: {
      itemDate: itemDate,
      standardCode: standardCode,
      standardVocabulary: standardVocabulary,
      standardName: standardName,
      value: value,
      unit: unit,
      age: ageAtEvent,
    }
  }, {
    name: 'Lab',
    domain: DomainType.LAB,
    filterType: PageFilterType.ReviewFilter,
    columns: [
      itemDate, itemTime, standardName, value, unit, refRange, ageAtEvent, visitType
    ],
    reverseEnum: {
      itemDate: itemDate,
      itemTime: itemTime,
      standardName: standardName,
      value: value,
      unit: unit,
      refRange: refRange,
      age: ageAtEvent,
      visitType: visitType
    }
  }, {
    name: 'Vital',
    domain: DomainType.VITAL,
    filterType: PageFilterType.ReviewFilter,
    columns: [
      itemDate, standardName, value, unit, refRange, ageAtEvent, visitType
    ],
    reverseEnum: {
      itemDate: itemDate,
      itemTime: itemTime,
      standardName: standardName,
      value: value,
      unit: unit,
      refRange: refRange,
      age: ageAtEvent,
      visitType: visitType
    }
  }, {
    name: 'Survey',
    domain: DomainType.SURVEY,
    filterType: PageFilterType.ReviewFilter,
    columns: [
      survey, question, answer
    ],
    reverseEnum: {
      survey: survey,
      question: question,
      answer: answer
    }
  }];

  constructor(
    private state: ReviewStateService,
    private route: ActivatedRoute,
    private reviewAPI: CohortReviewService,
  ) {}


  ngOnChanges() {
    if (this.clickedParticipantId && this.participantsId !== this.clickedParticipantId) {
      this.chartLoadedSpinner = true;
      if (this.summaryActive) {
        this.getSubscribedData();
      }
    }
  }

  getSubscribedData() {
    if (this.subscription) {
      this.subscription.unsubscribe();
    }
    this.subscription = this.route.data.map(({participant}) => participant)
      .subscribe(participants => {
        this.participantsId = this.clickedParticipantId || participants.participantId;
      });
    this.getDomainsParticipantsData();
  }

  ngOnInit() {
    this.getSubscribedData();
  }


  getDomainsParticipantsData() {
    const {ns, wsid, cid} = urlParamsStore.getValue();
    const cdrid = +(currentWorkspaceStore.getValue().cdrVersionId);
    const limit = 10;

    this.domainList.map(domainName => {
      this.chartData[domainName] = {
        conditionTitle: '',
        loading: true,
        items: []
      };
      const getParticipantsDomainData = this.reviewAPI.getParticipantChartData(ns, wsid, cid, cdrid,
        this.participantsId , domainName, limit)
        .subscribe(data => {
          const participantsData = data;
          this.chartData[domainName].items = participantsData.items;
          this.chartData[domainName].conditionTitle = typeToTitle(domainName);
          this.chartData[domainName].loading = false;
        });
      this.subscription.add(getParticipantsDomainData);
    });
  }

  ngOnDestroy() {
    this.subscription.unsubscribe();
  }
}
