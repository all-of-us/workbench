import {Component, Input} from '@angular/core';
import {FormControl} from '@angular/forms';

import {ReviewStateService} from '../review-state.service';
import {
  AnnotationType,
  CohortAnnotationDefinition,
  CohortAnnotationDefinitionService,
} from 'generated';

@Component({
  selector: 'app-annotation-form',
  templateUrl: './annotation-form.component.html',
  styleUrls: ['./annotation-form.component.css']
})
export class AnnotationFormComponent {
  @Input() definition: CohortAnnotationDefinition;
  control = new FormControl();
  AnnotationType = AnnotationType;

  constructor(
    private state: ReviewStateService,
    private annotationAPI: CohortAnnotationDefinitionService,
  ) {}

  get name() {
    return this.definition.columnName.split(' ').join('-');
  }
}
