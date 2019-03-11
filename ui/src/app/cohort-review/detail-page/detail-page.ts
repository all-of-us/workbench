import {Component, OnDestroy, OnInit} from '@angular/core';
import * as fp from 'lodash/fp';
import {Observable} from 'rxjs/Observable';
import {Subscription} from 'rxjs/Subscription';

import {Participant} from 'app/cohort-review/participant.model';
import {currentWorkspaceStore, urlParamsStore} from 'app/utils/navigation';
import {CohortAnnotationDefinition, CohortAnnotationDefinitionService, CohortReviewService, ParticipantCohortAnnotation} from 'generated';

@Component({
  templateUrl: './detail-page.html',
  styleUrls: ['./detail-page.css']
})
export class DetailPage implements OnInit, OnDestroy {

  sidebarOpen = true;
  creatingDefinition = false;
  editingDefinitions = false;
  participant: Participant;
  annotations: ParticipantCohortAnnotation[];
  annotationDefinitions: CohortAnnotationDefinition[];
  subscriptions: Subscription[] = [];
  constructor(
    private reviewAPI: CohortReviewService,
    private defsAPI: CohortAnnotationDefinitionService
  ) {
    this.setAnnotations = this.setAnnotations.bind(this);
    this.openCreateDefinitionModal = this.openCreateDefinitionModal.bind(this);
    this.closeCreateDefinitionModal = this.closeCreateDefinitionModal.bind(this);
    this.definitionCreated = this.definitionCreated.bind(this);
    this.openEditDefinitionsModal = this.openEditDefinitionsModal.bind(this);
    this.closeEditDefinitionsModal = this.closeEditDefinitionsModal.bind(this);
    this.setAnnotationDefinitions = this.setAnnotationDefinitions.bind(this);
    this.setParticipant = this.setParticipant.bind(this);
  }

  ngOnInit() {
    this.subscriptions.push(Observable
      .combineLatest(urlParamsStore, currentWorkspaceStore)
      .map(([{ns, wsid, cid, pid}, {cdrVersionId}]) => ({ns, wsid, cid, pid, cdrVersionId}))
      .distinctUntilChanged(fp.isEqual)
      .switchMap(({ns, wsid, cid, pid, cdrVersionId}) => {
        return Observable.forkJoin(
          this.reviewAPI
            .getParticipantCohortStatus(ns, wsid, +cid, +cdrVersionId, +pid)
            .do(ps => {
              this.participant = Participant.fromStatus(ps);
            }),
          this.reviewAPI
            .getParticipantCohortAnnotations(ns, wsid, +cid, +cdrVersionId, +pid)
            .do(({items}) => {
              this.annotations = items;
            }),
          this.loadAnnotationDefinitions()
        );
      })
      .subscribe()
    );
  }

  loadAnnotationDefinitions() {
    const {ns, wsid, cid} = urlParamsStore.getValue();
    return this.defsAPI.getCohortAnnotationDefinitions(ns, wsid, +cid)
      .do(({items}) => {
        this.annotationDefinitions = items;
      });
  }

  setAnnotations(v) {
    this.annotations = v;
  }

  ngOnDestroy() {
    this.subscriptions.forEach(s => s.unsubscribe());
  }

  get angleDir() {
    return this.sidebarOpen ? 'right' : 'left';
  }

  toggleSidebar() {
    this.sidebarOpen = !this.sidebarOpen;
  }

  openCreateDefinitionModal() {
    this.creatingDefinition = true;
  }

  closeCreateDefinitionModal() {
    this.creatingDefinition = false;
  }

  definitionCreated(ad) {
    this.creatingDefinition = false;
    this.annotationDefinitions = this.annotationDefinitions.concat([ad]);
  }

  openEditDefinitionsModal() {
    this.editingDefinitions = true;
  }

  closeEditDefinitionsModal() {
    this.editingDefinitions = false;
  }

  setAnnotationDefinitions(v) {
    this.annotationDefinitions = v;
  }

  setParticipant(v) {
    this.participant = v;
  }
}
