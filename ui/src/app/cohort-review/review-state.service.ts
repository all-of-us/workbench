import {Injectable} from '@angular/core';
import {BehaviorSubject} from 'rxjs/BehaviorSubject';
import {ReplaySubject} from 'rxjs/ReplaySubject';

import {Participant} from './participant.model';

import {
  Cohort,
  CohortAnnotationDefinition,
  CohortReview,
  ParticipantCohortAnnotation,
} from 'generated';

@Injectable()
export class ReviewStateService {
  /* Data Subjects */
  review = new ReplaySubject<CohortReview>(1);
  cohort = new ReplaySubject<Cohort>(1);
  annotationDefinitions = new ReplaySubject<CohortAnnotationDefinition[]>(1);

  /* Observable views on the data Subjects */
  review$ = this.review.asObservable();
  cohort$ = this.cohort.asObservable();
  annotationDefinitions$ = this.annotationDefinitions.asObservable();

  /* Flags */
  annotationManagerOpen = new BehaviorSubject<boolean>(false);
}
