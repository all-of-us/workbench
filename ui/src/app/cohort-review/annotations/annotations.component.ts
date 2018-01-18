/* tslint:disable:no-unused-variable */
// TODO (jms) - this is a stub, when written, make sure it fully passes linting
import {Component, OnInit} from '@angular/core';
import {Observable} from 'rxjs/Observable';

import {ReviewStateService} from '../review-state.service';

import {
  CohortAnnotationDefinition,
  CohortAnnotationDefinitionService,
  ParticipantCohortAnnotation,
} from 'generated';

interface Annotation {
  definition: CohortAnnotationDefinition;
  value: ParticipantCohortAnnotation;
}

/*
 * Curried predicate function that matches CohortAnnotationDefinitions to
 * ParticipantCohortAnnotations - byDefnId(definition)(value) => true/false.
 */
const byDefnId =
  ({cohortAnnotationDefinitionId: defnId}: Annotation['definition']) =>
  ({annotationDefinitionId: valId}: Annotation['value']): boolean =>
  (defnId === valId);

/*
 * Curried ParticipantCohortAnnotation factory - generates a blank value, given
 * a participant and a cohort review id
 */
const valueFactory =
  ([participantId, cohortReviewId]) =>
  (): ParticipantCohortAnnotation =>
  (<ParticipantCohortAnnotation>{participantId, cohortReviewId});

@Component({
  selector: 'app-annotations',
  templateUrl: './annotations.component.html',
  styleUrls: ['./annotations.component.css'],
})
export class AnnotationsComponent implements OnInit {
  private annotations$: Observable<Annotation[]>;
  private verbosity = false;

  constructor(
    private state: ReviewStateService,
    private annotationAPI: CohortAnnotationDefinitionService,
  ) {}

  ngOnInit() {
    const defs$ = this.state.annotationDefinitions$;
    const vals$ = this.state.annotationValues$;

    const factory$ = Observable.combineLatest(
        this.state.participant$.pluck('participantId'),
        this.state.review.pluck('cohortReviewId'))
      .map(vals => valueFactory(vals));

    this.annotations$ = Observable
      .combineLatest(defs$, vals$, factory$)
      .map(([defs, vals, factoryFunc]) =>
        defs.map(definition => {
          const value = vals.find(byDefnId(definition)) || factoryFunc();
          return <Annotation>{definition, value};
        }));
  }
}
