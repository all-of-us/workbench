import {Component, OnInit} from '@angular/core';
import {Observable} from 'rxjs/Observable';

import {ReviewStateService} from '../review-state.service';

import {
  CohortAnnotationDefinition,
  ParticipantCohortAnnotation,
} from 'generated';

interface Annotation {
  definition: CohortAnnotationDefinition;
  value: ParticipantCohortAnnotation;
}

/*
 * Curried predicate function that matches CohortAnnotationDefinitions to
 * ParticipantCohortAnnotations - byDefinitionId(definition)(value) => true/false.
 */
const byDefinitionId =
  ({cohortAnnotationDefinitionId}: CohortAnnotationDefinition) =>
  ({cohortAnnotationDefinitionId: annotationDefinitionId}: ParticipantCohortAnnotation): boolean =>
  (cohortAnnotationDefinitionId === annotationDefinitionId);

/*
 * Curried ParticipantCohortAnnotation factory - generates a blank value, given
 * a participant and a cohort review id
 */
const valueFactory =
  ([participantId, cohortReviewId]) =>
  (): ParticipantCohortAnnotation =>
  (<ParticipantCohortAnnotation>{participantId, cohortReviewId});

/*
 * The identity function (useful for filtering objects by truthiness)
 */
const identity = obj => obj;

@Component({
  selector: 'app-annotation-list',
  templateUrl: './annotation-list.component.html',
  styleUrls: ['./annotation-list.component.css'],
})
export class AnnotationListComponent implements OnInit {
  annotations$: Observable<Annotation[]>;

  /* Determines if the children should show the datatype of the annotation */
  showDataType = false;

  constructor(private state: ReviewStateService) {}

  ngOnInit() {
    /* All four of these get filtered for existence; they must all exist for
     * this component to make any sense
     */
    const defs$ = this.state.annotationDefinitions$.filter(identity);
    const vals$ = this.state.annotationValues$.filter(identity);
    const pid$ = this.state.participant$.filter(identity).pluck('participantId');
    const rid$ = this.state.review$.filter(identity).pluck('cohortReviewId');

    const factory$ = Observable
      .combineLatest(pid$, rid$)
      .map(vals => valueFactory(vals));

    this.annotations$ = Observable
      .combineLatest(defs$, vals$, factory$)
      .map(([defs, vals, factoryFunc]) =>
        defs.map(definition => {
          const value = vals.find(byDefinitionId(definition)) || factoryFunc();
          return <Annotation>{definition, value};
        }));
  }

  openManager(): void {
    this.state.annotationManagerOpen.next(true);
  }
}
