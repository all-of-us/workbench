import {BehaviorSubject} from 'rxjs/BehaviorSubject';

import {
  CohortReview,
} from 'generated';

export const cohortReviewStore = new BehaviorSubject<CohortReview>(undefined);
