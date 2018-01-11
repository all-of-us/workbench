import {Component} from '@angular/core';
import {Observable} from 'rxjs/Observable';

import {ReviewStateService} from '../review-state.service';

import {
  CohortAnnotationDefinition,
  CohortAnnotationDefinitionService,
} from 'generated';

@Component({
  selector: 'app-annotations',
  templateUrl: './annotations.component.html',
  styleUrls: ['./annotations.component.css'],
})
export class AnnotationsComponent {
  /* tslint:disable-next-line:no-unused-variable */
  private annotations$: Observable<CohortAnnotationDefinition[]> =
    this.state.annotationDefinitions$;

  constructor(
    private state: ReviewStateService,
    // TODO(jms) - plug in per-participant annotation updates
    /* tslint:disable-next-line:no-unused-variable */
    private annotationAPI: CohortAnnotationDefinitionService,
  ) {}
}
