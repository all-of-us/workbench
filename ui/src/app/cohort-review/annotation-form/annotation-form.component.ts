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

  constructor(
    private state: ReviewStateService,
    private annotationAPI: CohortAnnotationDefinitionService,
  ) {}

  get name() {
    return this.definition.columnName.split(' ').join('-');
  }

  get kind() {
    return {
      [AnnotationType.STRING]:    'text',
      [AnnotationType.BOOLEAN]:   'checkbox',
      [AnnotationType.INTEGER]:   'number',
      [AnnotationType.DATE]:      'date',
      [AnnotationType.TIMESTAMP]: 'text',  // what should this be?
    }[this.definition.annotationType];
  }
}
