import {Component, Input, OnChanges, OnDestroy, OnInit} from '@angular/core';
import {ActivatedRoute, Router} from '@angular/router';
import {
  DomainType,
  PageFilterType,
} from 'generated';
import {Subscription} from 'rxjs/Subscription';
import {CohortReviewService} from '../../../generated';
import {typeToTitle} from '../../cohort-search/utils';
import {ReviewStateService} from '../review-state.service';

/* The most common column types */
const itemDate = {
  name: 'itemDate',
  classNames: ['date-col'],
  displayName: 'Date',
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
const valueNumber = {
  name: 'valueNumber',
  displayName: 'Value As Number',
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
const dataRoute = {
  name: 'route',
  displayName: 'Route',
};
const units = {
  name: 'units',
  displayName: 'Units',
};
const labRefRange = {
  name: 'labRefRange',
  displayName: 'Lab Reference Range',
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
      itemDate, standardName, standardCode, ageAtEvent, visitType, numMentions,
      firstMention, lastMention, valueSource, sourceName, sourceCode, sourceVocabulary
    ],
    reverseEnum: {
      itemDate: itemDate,
      standardName: standardName,
      standardCode: standardCode,
      age: ageAtEvent,
      visitType: visitType,
      numMentions: numMentions,
      firstMention: firstMention,
      lastMention: lastMention,
      valueSource: valueSource,
      sourceName: sourceName,
      sourceCode: sourceCode,
      sourceVocabulary: sourceVocabulary,
    }
  };

  // TODO add Surveys tab when we have data to show
  readonly tabs = [{
    name: 'Conditions',
    domain: DomainType.CONDITION,
    filterType: PageFilterType.ReviewFilter,
    columns: [
      itemDate, sourceName, sourceCode, sourceVocabulary, ageAtEvent, numMentions,
      firstMention, lastMention, standardCode, standardName, standardVocabulary, visitId
    ],
    reverseEnum: {
      itemDate: itemDate,
      sourceName: sourceName,
      sourceCode: sourceCode,
      sourceVocabulary: sourceVocabulary,
      age: ageAtEvent,
      numMentions: numMentions,
      firstMention: firstMention,
      lastMention: lastMention,
      standardCode: standardCode,
      standardName: standardName,
      standardVocabulary: standardVocabulary,
      visitId: visitId,
    }
  }, {
    name: 'Procedures',
    domain: DomainType.PROCEDURE,
    filterType: PageFilterType.ReviewFilter,
    columns: [
      itemDate, sourceName, sourceCode, sourceVocabulary, ageAtEvent, numMentions,
      firstMention, lastMention, standardCode, standardName, standardVocabulary, visitId
    ],
    reverseEnum: {
      itemDate: itemDate,
      sourceName: sourceName,
      sourceCode: sourceCode,
      sourceVocabulary: sourceVocabulary,
      age: ageAtEvent,
      numMentions: numMentions,
      firstMention: firstMention,
      lastMention: lastMention,
      standardCode: standardCode,
      standardName: standardName,
      standardVocabulary: standardVocabulary,
      visitId: visitId,
    }
  }, {
    name: 'Drugs',
    domain: DomainType.DRUG,
    filterType: PageFilterType.ReviewFilter,
    columns: [
      itemDate, standardName, standardCode, ageAtEvent, numMentions, firstMention,
      lastMention, quantity, refills, strength, dataRoute, sourceName, sourceCode,
      sourceVocabulary, visitId
    ],
    reverseEnum: {
      itemDate: itemDate,
      standardName: standardName,
      standardCode: standardCode,
      age: ageAtEvent,
      numMentions: numMentions,
      firstMention: firstMention,
      lastMention: lastMention,
      quantity: quantity,
      refills: refills,
      strength: strength,
      route: dataRoute,
      sourceName: sourceName,
      sourceCode: sourceCode,
      sourceVocabulary: sourceVocabulary,
      visitId: visitId,
    }
  }, {
    name: 'Measurements',
    domain: DomainType.MEASUREMENT,
    filterType: PageFilterType.ReviewFilter,
    columns: [
      itemDate, standardName, standardCode, standardVocabulary, valueConcept, valueNumber,
      valueSource, units, ageAtEvent, labRefRange, sourceName, sourceCode, visitId
    ],
    reverseEnum: {
      itemDate: itemDate,
      standardName: standardName,
      standardCode: standardCode,
      standardVocabulary: standardVocabulary,
      valueConcept: valueConcept,
      valueNumber: valueNumber,
      valueSource: valueSource,
      units: units,
      age: ageAtEvent,
      labRefRange: labRefRange,
      sourceName: sourceName,
      sourceCode: sourceCode,
      visitId: visitId,
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
      itemDate, standardName, standardCode, standardVocabulary, valueConcept, valueNumber,
      valueSource, units, ageAtEvent
    ],
    reverseEnum: {
      itemDate: itemDate,
      standardName: standardName,
      standardCode: standardCode,
      standardVocabulary: standardVocabulary,
      valueConcept: valueConcept,
      valueNumber: valueNumber,
      valueSource: valueSource,
      units: units,
      age: ageAtEvent,
    }
  }];

  constructor(
    private state: ReviewStateService,
    private route: ActivatedRoute,
    private router: Router,
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
        this.clickedParticipantId? this.participantsId = this.clickedParticipantId
          :this.participantsId = participants.participantId;
      });
    this.getDomainsParticipantsData();
  }

  ngOnInit() {
    this.getSubscribedData();
  }


  getDomainsParticipantsData() {
    const {ns, wsid, cid} = this.route.parent.snapshot.params;
    const cdrid = +(this.route.parent.snapshot.data.workspace.cdrVersionId);
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

