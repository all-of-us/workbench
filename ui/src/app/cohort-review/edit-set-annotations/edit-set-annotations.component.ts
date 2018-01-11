import {Component, ViewChild} from '@angular/core';
import {FormControl, FormGroup, Validators} from '@angular/forms';
import {ActivatedRoute} from '@angular/router';
import {Observable} from 'rxjs/Observable';

import {ReviewStateService} from '../review-state.service';

import {
  AnnotationType,
  CohortAnnotationDefinition,
  CohortAnnotationDefinitionService,
} from 'generated';

@Component({
  selector: 'app-edit-set-annotations',
  templateUrl: './edit-set-annotations.component.html',
  styleUrls: ['./edit-set-annotations.component.css']
})
export class EditSetAnnotationsComponent {
  @ViewChild('modal') modal;
  readonly kinds = AnnotationType;
  private posting = false;

  form = new FormGroup({});

  constructor(
    private annotationAPI: CohortAnnotationDefinitionService,
    private state: ReviewStateService,
    private route: ActivatedRoute,
  ) { }

  close(): void {
    this.modal.close();
    this.state.isEditingAnnotations.next(false);
  }

  finish(): void {
    if (!this.form.valid) { return; }
    this.close();
  }
}
