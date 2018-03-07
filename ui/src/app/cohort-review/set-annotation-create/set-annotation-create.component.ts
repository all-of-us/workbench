import {Component, EventEmitter, Output} from '@angular/core';
import {FormControl, FormGroup, Validators} from '@angular/forms';
import {ActivatedRoute} from '@angular/router';

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
  readonly datatypes = [{
    value: AnnotationType.STRING,
    displayName: 'Text'
  }, {
    value: AnnotationType.ENUM,
    displayName: 'Enumeration'
  }, {
    value: AnnotationType.DATE,
    displayName: 'Date'
  }, {
    value: AnnotationType.BOOLEAN,
    displayName: 'Boolean'
  }, {
    value: AnnotationType.INTEGER,
    displayName: 'Integer'
  }];

  @Output() onFinish = new EventEmitter<boolean>();
  posting = false;
  enumValues = <string[]>[];

  form = new FormGroup({
    name: new FormControl('', Validators.required),
    kind: new FormControl('', Validators.required),
    addValue: new FormControl(),
  });

  get name() { return this.form.get('name'); }
  get kind() { return this.form.get('kind'); }
  get addValue() { return this.form.get('addValue'); }

  constructor(
    private annotationAPI: CohortAnnotationDefinitionService,
    private state: ReviewStateService,
    private route: ActivatedRoute,
  ) {}

  create(): void {
    this.posting = true;
    const {ns, wsid, cid} = this.route.snapshot.params;

    const request = <CohortAnnotationDefinition>{
      cohortId: cid,
      columnName: this.name.value,
      annotationType: this.kind.value,
    };

    if (this.isEnum) {
      request.enumValues = [...this.enumValues];
    }

    this.annotationAPI
      .createCohortAnnotationDefinition(ns, wsid, cid, request)
      .switchMap(_ => this.annotationAPI
        .getCohortAnnotationDefinitions(ns, wsid, cid)
        .pluck('items'))
      .do((defns: CohortAnnotationDefinition[]) =>
        this.state.annotationDefinitions.next(defns))
      .subscribe(_ => {
        this.posting = false;
        this.onFinish.emit(true);
      });
  }

  cancel() {
    this.onFinish.emit(true);
  }

  addEnumValue() {
    const val = this.addValue.value;
    const hasVal = this.enumValues.includes(val);
    if (val && val !== '' && !hasVal) {
      this.enumValues = [...this.enumValues, val];
      this.addValue.reset();
    }
  }

  removeEnumValue(val: string) {
    this.enumValues = this.enumValues.filter(v => v !== val);
  }

  get isEnum() {
    return this.kind.value === AnnotationType.ENUM;
  }

  get formIsInvalid() {
    const isEmptyEnum = this.isEnum && !(this.enumValues.length > 0);
    return (this.form.invalid || isEmptyEnum);
  }
}
