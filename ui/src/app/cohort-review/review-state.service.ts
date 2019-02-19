import {Injectable} from '@angular/core';
import {BehaviorSubject} from 'rxjs/BehaviorSubject';
import {ReplaySubject} from 'rxjs/ReplaySubject';

import {
  Cohort,
  CohortAnnotationDefinition,
  CohortReview,
} from 'generated';

@Injectable()
export class ReviewStateService {
  /* Flags */
  annotationManagerOpen = new BehaviorSubject<boolean>(false);
  editAnnotationManagerOpen = new BehaviorSubject<boolean>(false);
}

export const cohortReviewStore = new BehaviorSubject<CohortReview>(undefined);
export const annotationDefinitionsStore =
  new BehaviorSubject<CohortAnnotationDefinition[]>(undefined);
