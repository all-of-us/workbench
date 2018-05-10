import {Component} from '@angular/core';
import {ActivatedRoute} from '@angular/router';

import {
    CohortReviewService,
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
const ageAtEvent = {
  name: 'age',
  notField: true,
  displayName: 'Age At Event',
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
    domain: 'Master',
    filterType: PageFilterType.ReviewFilter,
    columns: [
      itemDate, domain, standardVocabulary, standardName, sourceVocabulary, sourceValue,
    ],
    reverseEnum: {
      itemDate: itemDate,
      domain: domain,
      standardVocabulary: standardVocabulary,
      standardName: standardName,
      sourceValue: sourceValue,
      sourceVocabulary: sourceVocabulary,
    }
  };

  readonly tabs = [{
    name: 'Conditions',
    domain: 'Condition',
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
      age: ageAtEvent,
    }
  }, {
    name: 'Procedures',
    domain: 'Procedure',
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
      age: ageAtEvent,
    }
  }, {
    name: 'Drugs',
    domain: 'Drug',
    filterType: PageFilterType.ReviewFilter,
    columns: [
      itemDate, standardVocabulary, standardName, sourceVocabulary, sourceValue,
        ageAtEvent, signature,
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
    domain: 'Observation',
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
    domain: 'Visit',
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
    domain: 'Device',
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
    domain: 'Measurement',
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
