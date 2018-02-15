import {Component, OnDestroy, OnInit} from '@angular/core';
import {ActivatedRoute} from '@angular/router';
import {Observable} from 'rxjs/Observable';
import {Subscription} from 'rxjs/Subscription';

import {ReviewStateService} from '../review-state.service';

import {
  CohortAnnotationDefinition,
  CohortAnnotationDefinitionService,
} from 'generated';

type DefnId = CohortAnnotationDefinition['cohortAnnotationDefinitionId'];

@Component({
  selector: 'app-annotation-manager',
  templateUrl: './annotation-manager.component.html',
  styleUrls: ['./annotation-manager.component.css']
})
export class AnnotationManagerComponent {

  subscription: Subscription;

  annotations$: Observable<CohortAnnotationDefinition[]> =
    this.state.annotationDefinitions$;


  posting: boolean = false;
  editSet: Set<DefnId> = new Set<DefnId>();
  mode: 'list' | 'create' = 'list';

  constructor(
    private annotationAPI: CohortAnnotationDefinitionService,
    private route: ActivatedRoute,
    private state: ReviewStateService,
  ) {}

  /* Edit the annotation name functions */
  isEditing(defn: CohortAnnotationDefinition): boolean {
    return this.editSet.has(defn.cohortAnnotationDefinitionId);
  }

  setEditing(defn: CohortAnnotationDefinition): void {
    this.editSet.add(defn.cohortAnnotationDefinitionId);
  }

  doneEditing(defn: CohortAnnotationDefinition): void {
    this.editSet.delete(defn.cohortAnnotationDefinitionId);
  }

  /* Delete the annotation definition */
  delete(defn: CohortAnnotationDefinition): void {
    const {ns, wsid, cid} = this.route.snapshot.params;
    const id = defn.cohortAnnotationDefinitionId;
    this.posting = true;

    this.annotationAPI
      .deleteCohortAnnotationDefinition(ns, wsid, cid, id)
      .switchMap(_ => this.annotationAPI
        .getCohortAnnotationDefinitions(ns, wsid, cid)
        .pluck('items'))
      .do((defns: CohortAnnotationDefinition[]) =>
        this.state.annotationDefinitions.next(defns))
      .subscribe(_ => this.posting = false);
  }

  /* Create a new annotation definition - delegates to child component */
  add()       { this.mode = 'create'; }
  onFinish()  { this.mode = 'list'; }


  get open() {
    return this.state.annotationManagerOpen.getValue();
  }

  set open(value: boolean) {
    this.state.annotationManagerOpen.next(value);
  }

  get twoThirds() {
    return `${Math.floor((window.innerHeight / 3) * 2)}px`;
  }
}
