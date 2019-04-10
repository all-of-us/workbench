import {BehaviorSubject} from 'rxjs/BehaviorSubject';

import {
  CohortReview, CohortStatus,
} from 'generated/fetch';

export const initialFilterState = {
  global: {
    dateMin: null,
    dateMax: null,
    ageMin: '',
    ageMax: '',
    visits: null
  },
  participants: {
    PARTICIPANTID: '',
    GENDER: ['Select All'],
    RACE: ['Select All'],
    ETHNICITY: ['Select All'],
    DECEASED: ['1', '0', 'Select All'],
    STATUS: [
      CohortStatus.INCLUDED,
      CohortStatus.EXCLUDED,
      CohortStatus.NEEDSFURTHERREVIEW,
      CohortStatus.NOTREVIEWED,
      'Select All'
    ]
  },
  tabs: {
    ALL_EVENTS: {
      standardCode: ['Select All'],
      sourceCode: ['Select All'],
      standardVocabulary: ['Select All'],
      sourceVocabulary: ['Select All'],
      domain: ['Select All'],
      sourceName: null,
      standardName: null,
      value: null,
    },
    PROCEDURE: {
      standardCode: ['Select All'],
      sourceCode: ['Select All'],
      standardVocabulary: ['Select All'],
      sourceVocabulary: ['Select All'],
      sourceName: null,
      standardName: null,
    },
    CONDITION: {
      standardCode: ['Select All'],
      sourceCode: ['Select All'],
      standardVocabulary: ['Select All'],
      sourceVocabulary: ['Select All'],
      sourceName: null,
      standardName: null,
    },
    OBSERVATION: {
      standardCode: ['Select All'],
      sourceCode: ['Select All'],
      standardVocabulary: ['Select All'],
      sourceVocabulary: ['Select All'],
      sourceName: null,
      standardName: null,
    },
    PHYSICAL_MEASUREMENT: {
      standardCode: ['Select All'],
      sourceCode: ['Select All'],
      standardVocabulary: ['Select All'],
      sourceVocabulary: ['Select All'],
      sourceName: null,
      standardName: null,
      value: null,
    },
    LAB: {
      itemTime: null,
      sourceName: null,
      standardName: null,
      value: null
    },
    VITAL: {
      itemTime: null,
      sourceName: null,
      standardName: null,
    },
    SURVEY: {
      survey: null
    }
  },
  vocab: 'standard',
};

export const cohortReviewStore = new BehaviorSubject<CohortReview>(undefined);
export const visitsFilterOptions = new BehaviorSubject<Array<any>>(null);
export const filterStateStore =
  new BehaviorSubject<any>(JSON.parse(JSON.stringify(initialFilterState)));
export const vocabOptions = new BehaviorSubject<any>(null);
export const multiOptions = new BehaviorSubject<any>(null);
