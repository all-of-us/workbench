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
  participant: Participant;
  annotations: ParticipantCohortAnnotation[];
  annotationDefinitions: CohortAnnotationDefinition[];
  subscriptions: Subscription[] = [];
  constructor(
    private reviewAPI: CohortReviewService,
    private defsAPI: CohortAnnotationDefinitionService
  ) {
    this.loadAnnotationDefinitions = this.loadAnnotationDefinitions.bind(this);
    this.setAnnotations = this.setAnnotations.bind(this);
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
}
