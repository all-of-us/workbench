import {Injectable} from '@angular/core';
import {ReplaySubject} from 'rxjs/ReplaySubject';

import {
  Cohort,
  CohortReview,
  ParticipantCohortStatus
} from 'generated';

interface RouteContext {
  workspaceNamespace?: string;
  workspaceId?: string;
  cohortId?: number;
  cdrVersion?: number;
}

@Injectable()
export class ReviewStateService {
  review = new ReplaySubject<CohortReview>(1);
  cohort = new ReplaySubject<Cohort>(1);
  participant = new ReplaySubject<ParticipantCohortStatus | null>(1);
  context = new ReplaySubject<RouteContext>(1);
}
