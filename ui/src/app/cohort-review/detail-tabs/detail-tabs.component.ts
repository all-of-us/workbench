import {Component} from '@angular/core';
import {ActivatedRoute} from '@angular/router';

import {
  CohortReviewService,
  PageFilterType,
  ParticipantConditionsColumns,
  ParticipantDrugsColumns,
  ParticipantMasterColumns,
  ParticipantObservationsColumns,
  ParticipantProceduresColumns,
} from 'generated';

/* The most common column types */
const itemDate = {
  name: 'itemDate',
  classNames: ['date-col'],
  displayName: 'Date',
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
    'measurements',
    'visits',
    'physical-measurements',
    'ppi',
    'device',
  ];

  readonly allEvents = {
    name: 'All Events',
    filterType: PageFilterType.ParticipantMasters,
    columns: [
      itemDate, domain, standardVocabulary, standardName, sourceVocabulary, sourceValue,
    ],
    reverseEnum: {
      itemDate: ParticipantMasterColumns.ItemDate,
      domain: ParticipantMasterColumns.Domain,
      standardVocabulary: ParticipantMasterColumns.StandardVocabulary,
      standardName: ParticipantMasterColumns.StandardName,
      sourceValue: ParticipantMasterColumns.SourceValue,
      sourceVocabulary: ParticipantMasterColumns.SourceVocabulary,
    }
  };

  readonly tabs = [{
    name: 'Conditions',
    filterType: PageFilterType.ParticipantConditions,
    columns: [
      itemDate, standardVocabulary, standardName, sourceVocabulary, sourceValue,
    ],
    reverseEnum: {
      itemDate: ParticipantConditionsColumns.ItemDate,
      standardVocabulary: ParticipantConditionsColumns.StandardVocabulary,
      standardName: ParticipantConditionsColumns.StandardName,
      sourceValue: ParticipantConditionsColumns.SourceValue,
      sourceVocabulary: ParticipantConditionsColumns.SourceVocabulary,
    }
  }, {
    name: 'Procedures',
    filterType: PageFilterType.ParticipantProcedures,
    columns: [
      itemDate, standardVocabulary, standardName, sourceVocabulary, sourceValue, ageAtEvent,
    ],
    reverseEnum: {
      itemDate: ParticipantProceduresColumns.ItemDate,
      standardVocabulary: ParticipantProceduresColumns.StandardVocabulary,
      standardName: ParticipantProceduresColumns.StandardName,
      sourceValue: ParticipantProceduresColumns.SourceValue,
      sourceVocabulary: ParticipantProceduresColumns.SourceVocabulary,
      age: ParticipantProceduresColumns.Age,
    }
  }, {
    name: 'Drugs',
    filterType: PageFilterType.ParticipantDrugs,
    columns: [
      itemDate, standardVocabulary, standardName, sourceVocabulary, sourceValue, ageAtEvent,
    ],
    reverseEnum: {
      itemDate: ParticipantDrugsColumns.ItemDate,
      standardVocabulary: ParticipantDrugsColumns.StandardVocabulary,
      standardName: ParticipantDrugsColumns.StandardName,
      sourceValue: ParticipantDrugsColumns.SourceValue,
      sourceVocabulary: ParticipantDrugsColumns.SourceVocabulary,
      age: ParticipantDrugsColumns.Age,
      signature: ParticipantDrugsColumns.Signature,
    }
  }, {
    name: 'Observations',
    filterType: PageFilterType.ParticipantObservations,
    columns: [
      itemDate, standardVocabulary, standardName, sourceVocabulary, sourceValue, ageAtEvent,
    ],
    reverseEnum: {
      itemDate: ParticipantObservationsColumns.ItemDate,
      standardVocabulary: ParticipantObservationsColumns.StandardVocabulary,
      standardName: ParticipantObservationsColumns.StandardName,
      sourceValue: ParticipantObservationsColumns.SourceValue,
      sourceVocabulary: ParticipantObservationsColumns.SourceVocabulary,
      sourceName: ParticipantObservationsColumns.SourceName,
      age: ParticipantObservationsColumns.Age,
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
    ).subscribe(details => this.details = details);
  }
}
