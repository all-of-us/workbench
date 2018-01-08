import {Injectable} from '@angular/core';
import {BehaviorSubject} from 'rxjs/BehaviorSubject';
import {ReplaySubject} from 'rxjs/ReplaySubject';

import {Participant} from './participant.model';

import {
  Cohort,
  CohortReview,
} from 'generated';

@Injectable()
export class ReviewStateService {
  review = new ReplaySubject<CohortReview>(1);
  cohort = new ReplaySubject<Cohort>(1);
  participant = new ReplaySubject<Participant | null>(1);
  sidebarOpen = new BehaviorSubject<boolean>(false);

  review$ = this.review.asObservable();
  cohort$ = this.cohort.asObservable();
  participant$ = this.participant.asObservable();
  sidebarOpen$ = this.sidebarOpen.asObservable();
}
