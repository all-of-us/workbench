import {Component} from '@angular/core';
import {ActivatedRoute} from '@angular/router';

import {
    CohortReviewService,
    DomainType,
    PageFilterType,
    ReviewColumns,
} from 'generated';

/* The most common column types */
const itemDate = {
  name: 'itemDate',
  classNames: ['date-col'],
  displayName: 'Start Date',
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
const sourceValue = {
  name: 'sourceValue',
  displayName: 'Source Value',
};
const sourceCode = {
  name: 'sourceCode',
  displayName: 'Source Code',
};
const ageAtEvent = {
  name: 'age',
  notField: true,
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
const numberOfMentions = {
  name: 'numberOfMentions',
  displayName: 'Number Of Mentions',
};
const dateFirstMention = {
  name: 'dateFirstMention',
  displayName: 'Date First Mention',
};
const dateLastMention = {
  name: 'dateLastMention',
  displayName: 'Date Last Mention',
};
const quantity = {
  name: 'quantity',
  displayName: 'Quantity',
};
const refills = {
  name: 'refills',
  displayName: 'Refills',
};
const strength = {
  name: 'strength',
  displayName: 'Strength',
};
const route = {
  name: 'route',
  displayName: 'Route',
};


@Component({
  selector: 'app-detail-tabs',
  templateUrl: './detail-tabs.component.html',
  styleUrls: ['./detail-tabs.component.css']
})
export class DetailTabsComponent {

  readonly stubs = [
    'physical-measurements',
    'ppi',
  ];

  readonly allEvents = {
    name: 'All Events',
    domain: DomainType.Master,
    filterType: PageFilterType.ReviewFilter,
    columns: [
      itemDate, standardName, standardCode, ageAtEvent, visitType, numberOfMentions,
        dateFirstMention, dateLastMention, sourceValue, sourceName, sourceCode, sourceVocabulary
    ],
    reverseEnum: {
      itemDate: itemDate,
      standardName: standardName,
      standardCode: standardCode,
      age: ageAtEvent,
      visitType: visitType,
      numberOfMentions: numberOfMentions,
      dateFirstMention: dateFirstMention,
      dateLastMention: dateLastMention,
      sourceValue: sourceValue,
      sourceName: sourceName,
      sourceCode: sourceCode,
      sourceVocabulary: sourceVocabulary,
    }
  };

  readonly tabs = [{
    name: 'Conditions',
    domain: DomainType.Condition,
    filterType: PageFilterType.ReviewFilter,
    columns: [
      itemDate, sourceName, sourceCode, sourceVocabulary, ageAtEvent, numberOfMentions,
      dateFirstMention, dateLastMention, standardCode, standardName, standardVocabulary, visitId
    ],
    reverseEnum: {
      itemDate: itemDate,
      sourceName: sourceName,
      sourceCode: sourceCode,
      sourceVocabulary: sourceVocabulary,
      age: ageAtEvent,
      numberOfMentions: numberOfMentions,
      dateFirstMention: dateFirstMention,
      dateLastMention: dateLastMention,
      standardCode: standardCode,
      standardName: standardName,
      standardVocabulary: standardVocabulary,
      visitId: visitId,
    }
  }, {
    name: 'Procedures',
    domain: DomainType.Procedure,
    filterType: PageFilterType.ReviewFilter,
    columns: [
      itemDate, sourceName, sourceCode, sourceVocabulary, ageAtEvent, numberOfMentions,
      dateFirstMention, dateLastMention, standardCode, standardName, standardVocabulary, visitId
    ],
    reverseEnum: {
      itemDate: itemDate,
      sourceName: sourceName,
      sourceCode: sourceCode,
      sourceVocabulary: sourceVocabulary,
      age: ageAtEvent,
      numberOfMentions: numberOfMentions,
      dateFirstMention: dateFirstMention,
      dateLastMention: dateLastMention,
      standardCode: standardCode,
      standardName: standardName,
      standardVocabulary: standardVocabulary,
      visitId: visitId,
    }
  }, {
    name: 'Drugs',
    domain: DomainType.Drug,
    filterType: PageFilterType.ReviewFilter,
    columns: [
      itemDate, standardName, standardCode, ageAtEvent, numberOfMentions, dateFirstMention,
        dateLastMention, quantity, refills, strength, route, sourceName, sourceCode, sourceVocabulary, visitId
    ],
    reverseEnum: {
      itemDate: itemDate,
      standardVocabulary: standardVocabulary,
      standardName: standardName,
      sourceValue: sourceValue,
      sourceVocabulary: sourceVocabulary,
      age: ageAtEvent,
      signature: signature,
    }
  }, {
    name: 'Observations',
    domain: DomainType.Observation,
    filterType: PageFilterType.ReviewFilter,
    columns: [
      itemDate, standardVocabulary, standardName, sourceVocabulary, sourceValue, ageAtEvent,
    ],
    reverseEnum: {
      itemDate: itemDate,
      standardVocabulary: standardVocabulary,
      standardName: standardName,
      sourceValue: sourceValue,
      sourceVocabulary: sourceVocabulary,
      sourceName: sourceName,
      age: ageAtEvent,
    }
  }, {
    name: 'Visits',
    domain: DomainType.Visit,
    filterType: PageFilterType.ReviewFilter,
    columns: [
      itemDate, endDate, standardVocabulary, standardName, sourceVocabulary,
      sourceValue, ageAtEvent,
    ],
    reverseEnum: {
      itemDate: itemDate,
      endDate: endDate,
      standardVocabulary: standardVocabulary,
      standardName: standardName,
      sourceValue: sourceValue,
      sourceVocabulary: sourceVocabulary,
      sourceName: sourceName,
      age: ageAtEvent,
    }
  }, {
    name: 'Devices',
    domain: DomainType.Device,
    filterType: PageFilterType.ReviewFilter,
    columns: [
      itemDate, standardVocabulary, standardName, sourceVocabulary,
      sourceValue, ageAtEvent,
    ],
    reverseEnum: {
      itemDate: itemDate,
      standardVocabulary: standardVocabulary,
      standardName: standardName,
      sourceValue: sourceValue,
      sourceVocabulary: sourceVocabulary,
      sourceName: sourceName,
      age: ageAtEvent,
    }
  }, {
    name: 'Measurements',
    domain: DomainType.Measurement,
    filterType: PageFilterType.ReviewFilter,
    columns: [
      itemDate, standardVocabulary, standardName, sourceVocabulary,
      sourceValue, ageAtEvent,
    ],
    reverseEnum: {
      itemDate: itemDate,
      standardVocabulary: standardVocabulary,
      standardName: standardName,
      sourceValue: sourceValue,
      sourceVocabulary: sourceVocabulary,
      sourceName: sourceName,
      age: ageAtEvent,
    }
  }];

  detailsLoading = false;
  details;

  constructor(
    private route: ActivatedRoute,
    private reviewApi: CohortReviewService,
  ) {}

  detailView(datum) {
    this.detailsLoading = true;
    const {participant} = this.route.snapshot.data;
    const {cohort, workspace} = this.route.parent.snapshot.data;
    this.reviewApi.getDetailParticipantData(
      workspace.namespace,
      workspace.id,
      cohort.id,
      workspace.cdrVersionId,
      datum.dataId,
      datum.domain
    ).subscribe(
        details => {
          this.details = details;
          this.detailsLoading = false;
        }
    );
  }
}
