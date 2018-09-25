import {Component, OnDestroy, OnInit, ViewChild} from '@angular/core';
import {ActivatedRoute, Router} from '@angular/router';
import {Observable} from 'rxjs/Observable';

import {WorkspaceData} from 'app/resolvers/workspace';
import {CohortEditModalComponent} from 'app/views/cohort-edit-modal/component';
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
export class CohortListComponent implements OnInit, OnDestroy {
  accessLevel: WorkspaceAccessLevel;
  cohortList: Cohort[] = [];
  resourceList: RecentResource[] = [];
  workspace: Workspace;
  cohortsLoading = true;
  cohortsError = false;
  wsNamespace: string;
  wsId: string;
  cohortInFocus: Cohort;


  @ViewChild(ConfirmDeleteModalComponent)
  deleteModal: ConfirmDeleteModalComponent;

  @ViewChild(CohortEditModalComponent)
  editModal: CohortEditModalComponent;

  constructor(
    private route: ActivatedRoute,
    private cohortsService: CohortsService,
    private router: Router,
  ) {
    const wsData: WorkspaceData = this.route.snapshot.data.workspace;
    this.workspace = wsData;
    this.accessLevel = wsData.accessLevel;
  }

  ngOnInit(): void {
    this.wsNamespace = this.route.snapshot.params['ns'];
    this.wsId = this.route.snapshot.params['wsid'];
    this.reloadCohorts();
  }

  reloadCohorts(): Observable<CohortListResponse> {
    this.cohortsLoading = true;
    this.cohortList = [];
    const call = this.cohortsService.getCohortsInWorkspace(this.wsNamespace, this.wsId);
    call
      .subscribe(
        cohortsReceived => {
          this.cohortList = cohortsReceived.items.map(function(cohorts) {
            return cohorts;
          });
          this.resourceList = convertToResources(this.cohortList, this.wsNamespace,
            this.wsId, this.accessLevel, ResourceType.COHORT);
          this.cohortsLoading = false;
        },
        error => {
          this.cohortsLoading = false;
          this.cohortsError = true;
        });
    return call;
  }

  ngOnDestroy(): void {

  }

  buildCohort(): void {
    this.router.navigate(['build'], {relativeTo: this.route});
  }

  public deleteCohort(cohort: Cohort): void {
    this.cohortsService.deleteCohort(this.wsNamespace, this.wsId, cohort.id).subscribe(() => {
      this.cohortList.splice(
        this.cohortList.indexOf(cohort), 1);
      this.deleteModal.close();
    });
  }

  get writePermission(): boolean {
    return this.accessLevel === WorkspaceAccessLevel.OWNER
      || this.accessLevel === WorkspaceAccessLevel.WRITER;
  }

  get actionsDisabled(): boolean {
    return !this.writePermission;
  }

  updateFinished(): void {
    this.editModal.close();
    this.reloadCohorts();
  }
}
