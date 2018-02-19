import {Component, EventEmitter, OnDestroy, OnInit, Output} from '@angular/core';
import {FormArray, FormBuilder, FormGroup, Validators} from '@angular/forms';
import {ActivatedRoute} from '@angular/router';
import {Observable} from 'rxjs/Observable';
import {Subscription} from 'rxjs/Subscription';

import {ReviewStateService} from '../review-state.service';

import {
  AnnotationType,
  CohortAnnotationDefinition,
  CohortAnnotationDefinitionService,
  ModifyCohortAnnotationDefinitionRequest,
} from 'generated';

@Component({
  selector: 'app-set-annotation-create',
  templateUrl: './set-annotation-create.component.html',
  styleUrls: ['./set-annotation-create.component.css']
})
export class SetAnnotationCreateComponent {
  readonly kinds = AnnotationType;
  private posting = false;
  @Output() onFinish = new EventEmitter<boolean>();
  private defn: CohortAnnotationDefinition | null;
  private form: FormGroup;
  private subscription: Subscription;

  get name() { return this.form.get('name'); }
  get kind() { return this.form.get('kind'); }

  constructor(
    private annotationAPI: CohortAnnotationDefinitionService,
    private state: ReviewStateService,
    private route: ActivatedRoute,
    private fb: FormBuilder,
  ) {
    this.form = this.fb.group({
      name: ['', Validators.required],
      kind: ['', Validators.required],
      enumValues: this.fb.array([]),
    });
  }

  get enumValues(): FormArray {
    return this.form.get('enumValues') as FormArray;
  }

  addEnumValue(): void {
    this.enumValues.push(this.fb.group({
      value: ['', Validators.required]
    }));
  }

  removeEnumValue(index: number): void {
    this.enumValues.removeAt(index);
  }

  create(): void {
    this.posting = true;
    const {ns, wsid, cid} = this.route.snapshot.params;

    const request = <CohortAnnotationDefinition>{
      cohortId: cid,
      columnName: this.name.value,
      annotationType: this.kind.value,
    };

    this.annotationAPI
      .createCohortAnnotationDefinition(ns, wsid, cid, request)
      .switchMap(_ => this.annotationAPI
        .getCohortAnnotationDefinitions(ns, wsid, cid)
        .pluck('items'))
      .do((defns: CohortAnnotationDefinition[]) =>
        this.state.annotationDefinitions.next(defns))
      .subscribe(_ => {
        this.posting = false;
        this.form.reset();
        this.onFinish.emit(true);
      });
  }

  cancel() {
    // this.form.reset();
    this.onFinish.emit(true);
  }
}
