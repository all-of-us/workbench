import {Component, OnInit, ViewChild} from '@angular/core';
import {ActivatedRoute, Router} from '@angular/router';
import {Observable} from 'rxjs/Observable';

import {WorkspaceData} from 'app/resolvers/workspace';
import {ConfirmDeleteModalComponent} from 'app/views/confirm-delete-modal/component';

import {
  Cohort,
  CohortListResponse,
  CohortsService,
  RecentResource,
  Workspace,
  WorkspaceAccessLevel,
} from 'generated';

import {convertToResources, ResourceType} from 'app/utils/resourceActions';

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
    private route: ActivatedRoute,
    private cohortsService: CohortsService,
    private router: Router,
  ) {
    const wsData: WorkspaceData = this.route.snapshot.data.workspace;
    this.accessLevel = wsData.accessLevel;
  }

  ngOnInit(): void {
    this.wsNamespace = this.route.snapshot.params['ns'];
    this.wsId = this.route.snapshot.params['wsid'];
    this.reloadCohorts();
  }

  reloadCohorts(): void {
    this.cohortsLoading = true;
    this.resourceList = [];
    this.cohortsService.getCohortsInWorkspace(this.wsNamespace, this.wsId)
      .subscribe(
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
    this.router.navigate(['build'], {relativeTo: this.route});
  }

  get writePermission(): boolean {
    return this.accessLevel === WorkspaceAccessLevel.OWNER
      || this.accessLevel === WorkspaceAccessLevel.WRITER;
  }
}
