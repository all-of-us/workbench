import {Component} from '@angular/core';
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
  selector: 'app-set-annotation-detail',
  templateUrl: './set-annotation-detail.component.html',
  styleUrls: ['./set-annotation-detail.component.css']
})
export class SetAnnotationDetailComponent {
  readonly kinds = AnnotationType;
  private posting = false;

  form = new FormGroup({
    name: new FormControl('', Validators.required),
    kind: new FormControl('', Validators.required),
  });

  get name() { return this.form.get('name'); }
  get kind() { return this.form.get('kind'); }

  constructor(
    private annotationAPI: CohortAnnotationDefinitionService,
    private state: ReviewStateService,
    private route: ActivatedRoute,
  ) { }

  clear(): void {
    this.form.reset();
  }

  finish(): void {
    if (!this.form.valid) { return; }
    const {ns, wsid, cid} = this.route.snapshot.params;
    const request = <CohortAnnotationDefinition>{
      cohortId: cid,
      columnName: this.name.value,
      annotationType: this.kind.value,
    };

    const allDefns$ = this.annotationAPI
      .getCohortAnnotationDefinitions(ns, wsid, cid)
      .pluck('items');

    const broadcast = (defns: CohortAnnotationDefinition[]) =>
      this.state.annotationDefinitions.next(defns);

    this.posting = true;
    this.annotationAPI
      .createCohortAnnotationDefinition(ns, wsid, cid, request)
      .switchMap(_ => allDefns$)
      .do(broadcast)
      .do(_ => this.posting = false)
      .subscribe(_ => this.clear());
  }
}
