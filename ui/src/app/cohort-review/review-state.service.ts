import {BehaviorSubject} from 'rxjs/BehaviorSubject';

import {
  CohortReview,
} from 'generated/fetch';

const initialFilterState = {
  global: {
    date: {
      min: null,
      max: null
    },
    age: {
      min: '',
      max: ''
    },
    visits: null
  },
  tabs: {
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
  },
  vocab: 'standard',
};

export const cohortReviewStore = new BehaviorSubject<CohortReview>(undefined);
export const visitsFilterOptions = new BehaviorSubject<Array<any>>(null);
export const filterStateStore =
  new BehaviorSubject<any>(JSON.parse(JSON.stringify(initialFilterState)));
