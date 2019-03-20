import {BehaviorSubject} from 'rxjs/BehaviorSubject';

import {
  CohortReview,
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
    participantId: null,
    gender: ['Select All'],
    race: ['Select All'],
    ethnicity: ['Select All'],
    status: ['Select All'],
  },
  tabs: {
    ALL_EVENTS: {
      standardVocabulary: ['Select All'],
      sourceVocabulary: ['Select All'],
      domain: ['Select All'],
    },
    PROCEDURE: {
      standardVocabulary: ['Select All'],
      sourceVocabulary: ['Select All'],
    },
    CONDITION: {
      standardVocabulary: ['Select All'],
      sourceVocabulary: ['Select All'],
    },
    OBSERVATION: {
      standardVocabulary: ['Select All'],
      sourceVocabulary: ['Select All'],
    },
    PHYSICAL_MEASURE: {
      standardVocabulary: ['Select All'],
      sourceVocabulary: ['Select All'],
    },
  },
  vocab: 'standard',
};

export const cohortReviewStore = new BehaviorSubject<CohortReview>(undefined);
export const visitsFilterOptions = new BehaviorSubject<Array<any>>(null);
export const filterStateStore =
  new BehaviorSubject<any>(JSON.parse(JSON.stringify(initialFilterState)));
export const vocabOptions = new BehaviorSubject<any>(null);
export const demoOptions = new BehaviorSubject<any>(null);
