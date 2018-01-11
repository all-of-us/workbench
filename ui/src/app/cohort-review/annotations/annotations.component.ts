import {Component, OnInit} from '@angular/core';
import {ActivatedRoute} from '@angular/router';

import {ReviewStateService} from '../review-state.service';

import {
  CohortReviewService,
  CohortAnnotationDefinitionService,
} from 'generated';

@Component({
  selector: 'app-annotations',
  templateUrl: './annotations.component.html',
})
export class AnnotationsComponent implements OnInit {
  private annotations$;

  constructor(
    private state: ReviewStateService,
    private annotationAPI: CohortAnnotationDefinitionService,
  ) {}

  ngOnInit() {
    this.annotations$ = this.state.annotationDefinitions$;
  }
}
