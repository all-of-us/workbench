import {Injectable} from '@angular/core';
import {BehaviorSubject} from 'rxjs/BehaviorSubject';
import {ReplaySubject} from 'rxjs/ReplaySubject';

import {Participant} from './participant.model';

import {
  Cohort,
  CohortAnnotationDefinition,
  CohortReview,
} from 'generated';

@Injectable()
export class ReviewStateService {
  /* Data Subjects */
  review = new ReplaySubject<CohortReview>(1);
  cohort = new ReplaySubject<Cohort>(1);
  participant = new ReplaySubject<Participant | null>(1);
  annotationDefinitions = new ReplaySubject<CohortAnnotationDefinition[]>(1);

  /* Observable views on the data Subjects */
  review$ = this.review.asObservable();
  cohort$ = this.cohort.asObservable();
  participant$ = this.participant.asObservable();
  annotationDefinitions$ = this.annotationDefinitions.asObservable();

  /* Flags */
  sidebarOpen = new BehaviorSubject<boolean>(false);
  sidebarOpen$ = this.sidebarOpen.asObservable();
  annotationsOpen = new BehaviorSubject<boolean>(false);
  annotationsOpen$ = this.annotationsOpen.asObservable();
}
