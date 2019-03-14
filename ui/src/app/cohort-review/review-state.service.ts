import {BehaviorSubject} from 'rxjs/BehaviorSubject';

import {
  CohortReview,
} from 'generated/fetch';

export const cohortReviewStore = new BehaviorSubject<CohortReview>(undefined);
export const filterStateStore = new BehaviorSubject<any>(null);
export const vocabOptions = new BehaviorSubject<any>(null);
