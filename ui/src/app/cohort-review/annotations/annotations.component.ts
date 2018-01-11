import {Component} from '@angular/core';
import {ActivatedRoute} from '@angular/router';
import {Observable} from 'rxjs/Observable';

import {ReviewStateService} from '../review-state.service';

import {
  CohortReviewService,
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

  constructor(
    private state: ReviewStateService,
    private annotationAPI: CohortAnnotationDefinitionService,
  ) {}
}
