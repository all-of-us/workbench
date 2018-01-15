/* tslint:disable:no-unused-variable */
// TODO (jms) - this is a stub, when written, make sure it fully passes linting
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
  private annotations$: Observable<CohortAnnotationDefinition[]> =
    this.state.annotationDefinitions$;

  private verbosity = false;

  constructor(
    private state: ReviewStateService,
    private annotationAPI: CohortAnnotationDefinitionService,
  ) {}
}
