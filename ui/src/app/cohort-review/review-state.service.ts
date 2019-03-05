import {BehaviorSubject} from 'rxjs/BehaviorSubject';

import {
  CohortReview,
} from 'generated/fetch';

export const cohortReviewStore = new BehaviorSubject<CohortReview>(undefined);
