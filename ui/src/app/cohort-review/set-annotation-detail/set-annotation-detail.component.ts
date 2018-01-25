import {Component, OnDestroy, OnInit} from '@angular/core';
import {FormArray, FormBuilder, FormGroup, Validators} from '@angular/forms';
import {ActivatedRoute} from '@angular/router';
import {Observable} from 'rxjs/Observable';
import {Subscription} from 'rxjs/Subscription';

import {
  AnnotationManagerState,
  ReviewStateService,
} from '../review-state.service';

import {
  AnnotationType,
  CohortAnnotationDefinition,
  CohortAnnotationDefinitionService,
  ModifyCohortAnnotationDefinitionRequest,
} from 'generated';

@Component({
  selector: 'app-set-annotation-detail',
  templateUrl: './set-annotation-detail.component.html',
  styleUrls: ['./set-annotation-detail.component.css']
})
export class SetAnnotationDetailComponent implements OnInit, OnDestroy {
  readonly kinds = AnnotationType;
  private posting = false;
  private defn: CohortAnnotationDefinition | null;
  private mode: AnnotationManagerState['mode'];
  private form: FormGroup;
  private subscription: Subscription;

  get name() { return this.form.get('name'); }
  get kind() { return this.form.get('kind'); }
  get editing() { return this.mode === 'edit'; }

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

  ngOnInit() {
    this.subscription = this.state.annotationMgrState
      .subscribe(({open, mode, defn}) => {
        this.mode = mode;
        this.defn = defn;

        if (this.editing) {
          this._setFromDefn();
          this.kind.disable();
        }
      });
  }

  ngOnDestroy() {
    this.subscription.unsubscribe();
  }

  private _setFromDefn(): void {
    this.name.setValue(this.defn.columnName);
    this.kind.setValue(this.defn.annotationType);
    const vals = this.defn.enumValues || [];
    const controls = vals.map(value => this.fb.group({value}));
    this.form.setControl('enumValues', this.fb.array(controls));
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

  clear(): void {
    this.editing
      ? this._setFromDefn()
      : this.form.reset();
  }

  get clearButtonText(): string {
    return this.editing ? 'Revert' : 'Clear';
  }

  save(): void {
    const call = this.editing
      ? this._update()
      : this._create();

    this.posting = true;

    call.switchMap(_ => this._fetchAll())
      .do(defns => this._broadcast(defns))
      .do(_ => this.posting = false)
      .subscribe(_ => this.toOverview());
  }

  get saveButtonText(): string {
    return this.editing ? 'Save' : 'Create';
  }

  get canSave(): boolean {
    return this.editing ? this._editsAreValid : this.form.valid;
  }

  get _editsAreValid(): boolean {
    if (!this.form.valid) { return false; }
    let somethingChanged = false;

    const name = this.defn && this.defn.columnName;
    if (name !== this.name.value) {
      somethingChanged = true;
    }

    if (this.kind.value === this.kinds.ENUM) {
      const dvals = (this.defn && this.defn.enumValues) || [];
      const fvals = this.enumValues.controls.map(c => c.value.value);
      if (dvals.length !== fvals.length) {
        somethingChanged = true;
      }
      for (let i = 0; i < dvals.length; i++) {
        if (dvals[i] !== fvals[i]) {
          somethingChanged = true;
        }
      }
    }

    return somethingChanged;
  }

  toOverview(): void {
    this.state.annotationMgrState.next({
      open: true,
      mode: 'overview'
    });
  }

  private _create(): Observable<CohortAnnotationDefinition> {
    const {ns, wsid, cid} = this.route.snapshot.params;
    const request = <CohortAnnotationDefinition>{
      cohortId: cid,
      columnName: this.name.value,
      annotationType: this.kind.value,
    };
    return this.annotationAPI
      .createCohortAnnotationDefinition(ns, wsid, cid, request);
  }

  private _update(): Observable<CohortAnnotationDefinition> {
    const {ns, wsid, cid} = this.route.snapshot.params;
    const id = this.defn.cohortAnnotationDefinitionId;
    const request = <ModifyCohortAnnotationDefinitionRequest>{
      columnName: this.name.value
    };
    return this.annotationAPI
      .updateCohortAnnotationDefinition(ns, wsid, cid, id, request);
  }

  private _fetchAll(): Observable<CohortAnnotationDefinition[]> {
    const {ns, wsid, cid} = this.route.snapshot.params;
    const call = this.annotationAPI
      .getCohortAnnotationDefinitions(ns, wsid, cid)
      .pluck('items');
    return <Observable<CohortAnnotationDefinition[]>>call;
  }

  private _broadcast(defns: CohortAnnotationDefinition[]): void {
    this.state.annotationDefinitions.next(defns);
  }
}
