import {Component, EventEmitter, Input, Output} from '@angular/core';
import {FormControl, FormGroup, Validators} from '@angular/forms';

import {ReviewStateService} from 'app/cohort-review/review-state.service';
import {urlParamsStore} from 'app/utils/navigation';

import {
  AnnotationType,
  CohortAnnotationDefinition,
  CohortAnnotationDefinitionService,
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

  @Input() loadAnnotationDefinitions: Function;
  @Output() onFinish = new EventEmitter<boolean>();
  posting = false;
  enumValues = <string[]>[];
  annotationOptions = '';

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
  ) {}

  create(): void {
    this.posting = true;
    const {ns, wsid, cid} = urlParamsStore.getValue();

    const request = <CohortAnnotationDefinition>{
      cohortId: cid,
      columnName: this.name.value,
      annotationType: this.kind.value,
    };

    if (this.isEnum) {
      const val = this.addValue.value;
      const hasVal = this.enumValues.includes(val);
      if (val && val !== '' && !hasVal) {
        request.enumValues = [...this.enumValues, val];
      } else {
        request.enumValues = [...this.enumValues];
      }
    }

    this.annotationAPI
      .createCohortAnnotationDefinition(ns, wsid, cid, request)
      .switchMap(_ => this.loadAnnotationDefinitions())
      .subscribe(_ => {
        this.open = false;
        this.form.reset();
        this.posting = false;
        this.annotationOptions = '';
        // this.isEnum = false;
        this.kind.patchValue('');
        this.enumValues.length = 0;
        this.name.patchValue('');
        this.onFinish.emit(true);
        this.addValue.reset();
      });
  }
  get open() {
    return this.state.annotationManagerOpen.getValue();
  }

  set open(value: boolean) {
    this.state.annotationManagerOpen.next(value);
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
    const val = this.addValue.value;
    const isEmptyEnum = this.isEnum && !val && !(this.enumValues.length > 0);
    return (this.form.invalid || isEmptyEnum);
  }

  selectDropdownChange(val) {
    this.annotationOptions = val.displayName ;
    this.kind.patchValue(val.value);
  }

  closeModal() {
    this.form.reset();
    this.annotationOptions = '';
    this.open = false;
    this.kind.patchValue('');
    this.enumValues.length = 0;
    this.name.patchValue('');
  }

}
