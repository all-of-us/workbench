import {Component, Input, OnInit, OnDestroy} from '@angular/core';
import {FormControl} from '@angular/forms';
import {Subscription} from 'rxjs/Subscription';

import {ReviewStateService} from '../review-state.service';

import {
  AnnotationType,
  CohortAnnotationDefinition,
  CohortReviewService,
} from 'generated';

@Component({
  selector: 'app-annotation-form',
  templateUrl: './annotation-form.component.html',
  styleUrls: ['./annotation-form.component.css']
})
export class AnnotationFormComponent {
  @Input() definition: CohortAnnotationDefinition;
  @Input() verbose: boolean;

  private control = new FormControl();
  private subscription: Subscription;
  private expandText = false;
  readonly AnnotationType = AnnotationType;

  constructor(
    private state: ReviewStateService,
    private reviewAPI: CohortReviewService,
  ) {}

  get machineName() {
    return this.definition.columnName.split(' ').join('-');
  }

  get datatype() {
    return this.verbose
      ? ` (${this.definition.annotationType})`
      : '';
  }

  toggleExpandText() {
    this.expandText = !this.expandText;
  }

  edit() {
    console.log(`Editing ${this.definition.cohortAnnotationDefinitionId}`);
  }
}
