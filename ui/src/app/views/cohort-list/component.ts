import {Component, OnInit, ViewChild} from '@angular/core';

import {WorkspaceData} from 'app/resolvers/workspace';
import {currentWorkspaceStore, navigate} from 'app/utils/navigation';
import {ConfirmDeleteModalComponent} from 'app/views/confirm-delete-modal/component';

import {
  CohortsService,
  RecentResource,
  WorkspaceAccessLevel,
} from 'generated';

import {convertToResources, ResourceType} from 'app/utils/resourceActions';

import {cohortsApi} from 'app/services/swagger-fetch-clients';


@Component({
  styleUrls: ['../../styles/buttons.css',
    '../../styles/cards.css',
    './component.css'],
  templateUrl: './component.html',
})
export class CohortListComponent implements OnInit {
  accessLevel: WorkspaceAccessLevel;
  resourceList: RecentResource[] = [];
  cohortsLoading = true;
  wsNamespace: string;
  wsId: string;

  @ViewChild(ConfirmDeleteModalComponent)
  deleteModal: ConfirmDeleteModalComponent;

  constructor(
    private cohortsService: CohortsService,
  ) {}

  ngOnInit(): void {
    const ws = currentWorkspaceStore.getValue();
    this.accessLevel = ws.accessLevel;
    this.wsNamespace = ws.namespace;
    this.wsId = ws.id;
    this.reloadCohorts();
  }

  reloadCohorts(): void {
    this.cohortsLoading = true;
    this.resourceList = [];
    cohortsApi().getCohortsInWorkspace(this.wsNamespace, this.wsId)
      .then(
        resp => {
          this.resourceList = convertToResources(resp.items, this.wsNamespace,
            this.wsId, this.accessLevel, ResourceType.COHORT);
          this.cohortsLoading = false;
        },
        error => {
          this.cohortsLoading = false;
        });
  }

  buildCohort(): void {
    navigate(['/workspaces', this.wsNamespace, this.wsId, 'cohorts', 'build']);
  }

  get writePermission(): boolean {
    return this.accessLevel === WorkspaceAccessLevel.OWNER
      || this.accessLevel === WorkspaceAccessLevel.WRITER;
  }
}
