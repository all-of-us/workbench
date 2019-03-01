import {BehaviorSubject} from 'rxjs/BehaviorSubject';

import {
  CohortReview,
} from 'generated';

export const cohortReviewStore = new BehaviorSubject<CohortReview>(undefined);

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
export const filterStateStore = new BehaviorSubject<any>(initialfilterState);
