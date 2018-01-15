import {Component, OnDestroy, OnInit} from '@angular/core';
import {FormBuilder, FormGroup, Validators} from '@angular/forms';
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
  selector: 'app-set-annotation-detail',
  templateUrl: './set-annotation-detail.component.html',
  styleUrls: ['./set-annotation-detail.component.css']
})
export class SetAnnotationDetailComponent implements OnInit, OnDestroy {
  readonly kinds = AnnotationType;
  private posting = false;
  private editing = false;
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
  ) { }

  ngOnInit() {
    this.subscription = this.state.annotationMgrState
      .subscribe(({open, mode, defn}) => {
        this.editing = mode === 'edit';
        this.defn = defn;

        if (this.editing) {
          this.form = this.fb.group({
            name: [defn.columnName, Validators.required],
            kind: [{value: defn.annotationType, disabled: true}, Validators.required]
          });
        } else {
          this.form = this.fb.group({
            name: ['', Validators.required],
            kind: ['', Validators.required],
          });
        }
      });
  }

  ngOnDestroy() {
    this.subscription.unsubscribe();
  }

  private _setForm(): void {
    this.name.setValue(this.defn.columnName);
    this.kind.setValue(this.defn.annotationType);
  }

  clear(): void {
    this.editing
      ? this._setForm()
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

  get savingIsDisabled(): boolean {
    const disabled = !this.form.valid;
    const noChange = (this.editing && this.defn)
      ? this.name.value === this.defn.columnName
      : false;
    return disabled || noChange;
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
