import {Component, OnInit} from '@angular/core';
import {ActivatedRoute} from '@angular/router';
import {FormArray, FormBuilder} from '@angular/forms';

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
  private form;

  constructor(
    private state: ReviewStateService,
    private annotationAPI: CohortAnnotationDefinitionService,
    private formBuilder: FormBuilder,
  ) {}

  ngOnInit() {
    this.annotations$ = this.state.annotationDefinitions$;
  }
}
