/* tslint:disable:no-unused-variable */
// TODO (jms) - this is a stub, when written, make sure it fully passes linting
import {Component, Input} from '@angular/core';
import {FormControl} from '@angular/forms';
import {Subscription} from 'rxjs/Subscription';

import {ReviewStateService} from '../review-state.service';

import {
  AnnotationType,
  CohortAnnotationDefinition,
  CohortReviewService,
} from 'generated';

@Component({
  selector: 'app-participant-annotation',
  templateUrl: './participant-annotation.component.html',
  styleUrls: ['./participant-annotation.component.css']
})
export class ParticipantAnnotationComponent {
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
    this.state.annotationMgrState.next({
      open: true,
      mode: 'edit',
      defn: this.definition
    });
  }
}
