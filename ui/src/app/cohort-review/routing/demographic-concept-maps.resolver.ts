import {Injectable} from '@angular/core';
import {ActivatedRouteSnapshot, Resolve} from '@angular/router';
import {Observable} from 'rxjs/Observable';
import {ReplaySubject} from 'rxjs/ReplaySubject';

import {WorkspaceStorageService} from 'app/services/workspace-storage.service';

import {
  CohortBuilderService,
  ConceptIdName,
  ParticipantDemographics,
} from 'generated';

@Injectable()
export class DemographicConceptMapsResolver implements Resolve<ParticipantDemographics> {
  private cdrId: number;

  constructor(
    private builderAPI: CohortBuilderService,
    private workspaceStorageService: WorkspaceStorageService,
  ) {
    this.workspaceStorageService.activeWorkspace$.subscribe((workspace) => {
      this.cdrId = parseInt(workspace.cdrVersionId, 10);
    });
  }

  resolve(route: ActivatedRouteSnapshot): Observable<ParticipantDemographics> {
    return this.workspaceStorageService.reloadIfNew(
      route.parent.params['ns'], route.parent.params['wsid'])
      .switchMap(() => this.workspaceStorageService.activeWorkspace$)
      .switchMap(cdrId => this.builderAPI.getParticipantDemographics(this.cdrId))
      .first();
  }
}
