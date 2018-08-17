import {Component, EventEmitter, Output, OnChanges, Input} from '@angular/core';
import {FormControl, FormGroup, Validators} from '@angular/forms';
import {ActivatedRoute} from '@angular/router';

import {ReviewStateService} from '../review-state.service';

import {
  AnnotationType,
  CohortAnnotationDefinition,
  CohortAnnotationDefinitionService,
  ModifyCohortAnnotationDefinitionRequest,
} from 'generated';
import {Observable} from "rxjs/Observable";
import {ParticipantCohortAnnotation} from "../../../generated";
interface Annotation {
    definition: CohortAnnotationDefinition;
    value: ParticipantCohortAnnotation;
}
/*
 * Curried predicate function that matches CohortAnnotationDefinitions to
 * ParticipantCohortAnnotations - byDefinitionId(definition)(value) => true/false.
 */
const byDefinitionId =
    ({cohortAnnotationDefinitionId}: CohortAnnotationDefinition) =>
        ({cohortAnnotationDefinitionId: annotationDefinitionId}: ParticipantCohortAnnotation): boolean =>
            (cohortAnnotationDefinitionId === annotationDefinitionId);

/*
 * Curried ParticipantCohortAnnotation factory - generates a blank value, given
 * a participant and a cohort review id
 */
const valueFactory =
    ([participantId, cohortReviewId]) =>
        (): ParticipantCohortAnnotation =>
            (<ParticipantCohortAnnotation>{participantId, cohortReviewId});

/*
 * The identity function (useful for filtering objects by truthiness)
 */
const identity = obj => obj;
@Component({
  selector: 'app-set-annotation-create',
  templateUrl: './set-annotation-create.component.html',
  styleUrls: ['./set-annotation-create.component.css']
})
export class SetAnnotationCreateComponent implements OnChanges {
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
    @Input() participant;
  @Output() onFinish = new EventEmitter<boolean>();
  posting = false;
  enumValues = <string[]>[];
  annotationOptions: string = '';
  showDataType = false;
    annotations$: Observable<Annotation[]>;
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

    ngOnChanges(changes) {
        const defs$ = this.state.annotationDefinitions$.filter(identity);
        const factory$ = this.state.review$.filter(identity).pluck('cohortReviewId')
            .map(rid => valueFactory([this.participant.id, rid]));

        this.annotations$ = Observable
            .combineLatest(defs$, factory$)
            .map(([defs, factoryFunc]) =>
                defs.map(definition => {
                    const vals = this.participant.annotations;
                    const value = vals.find(byDefinitionId(definition)) || factoryFunc();
                    return <Annotation>{definition, value};
                }))
            .do(console.dir);
    }

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
        this.open = false;
        this.posting = false;
        this.onFinish.emit(true);
      });
  }

  cancel() {

    this.onFinish.emit(true);
  }
    get open() {
        return this.state.annotationManagerOpen.getValue();
    }

    set open(value: boolean) {
        this.state.annotationManagerOpen.next(value);
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

    selectDropdownChange(data){
        this.annotationOptions = data.displayName;
        this.kind.patchValue(data.value);
    }

}
