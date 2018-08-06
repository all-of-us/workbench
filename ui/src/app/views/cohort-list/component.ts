import {Component, OnDestroy, OnInit, ViewChild} from '@angular/core';
import {ActivatedRoute, Router} from '@angular/router';

import {WorkspaceData} from 'app/resolvers/workspace';
import {ConfirmDeleteModalComponent} from 'app/views/confirm-delete-modal/component';

import {
  Cohort,
  CohortsService,
  Workspace,
  WorkspaceAccessLevel,
} from 'generated';

@Component({
  styleUrls: ['../../styles/buttons.css',
    '../../styles/cards.css',
    './component.css'],
  templateUrl: './component.html',
})
export class CohortListComponent implements OnInit, OnDestroy {
  accessLevel: WorkspaceAccessLevel;
  awaitingReview: boolean;
  cohortList: Cohort[] = [];
  workspace: Workspace;
  cohortsLoading = true;
  cohortsError = false;
  wsNamespace: string;
  wsId: string;
  cohortInFocus: Cohort;


  @ViewChild(ConfirmDeleteModalComponent)
  deleteModal: ConfirmDeleteModalComponent;

  constructor(
    private route: ActivatedRoute,
    private cohortsService: CohortsService,
    private router: Router,
  ) {
    const wsData: WorkspaceData = this.route.snapshot.data.workspace;
    this.workspace = wsData;
    this.accessLevel = wsData.accessLevel;
    const {approved, reviewRequested} = this.workspace.researchPurpose;
    this.awaitingReview = reviewRequested && !approved;
  }

  ngOnInit(): void {
    this.wsNamespace = this.route.snapshot.params['ns'];
    this.wsId = this.route.snapshot.params['wsid'];
    this.reloadCohorts();
  }

  reloadCohorts(): void {
    this.cohortsLoading = true;
    this.cohortsService.getCohortsInWorkspace(this.wsNamespace, this.wsId)
      .subscribe(
        cohortsReceived => {
          this.cohortList = cohortsReceived.items.map(function(cohorts) {
            return cohorts;
          });
          this.cohortsLoading = false;
        },
        error => {
          this.cohortsLoading = false;
          this.cohortsError = true;
        });
  }

  ngOnDestroy(): void {

  }

  buildCohort(): void {
    if (!this.awaitingReview) {
      this.router.navigate(['build'], {relativeTo: this.route});
    }
  }

  openCohort(cohort: Cohort): void {
    if (!this.actionsDisabled) {
      this.router.navigate([cohort.id, 'review'], {relativeTo: this.route});
    }
  }

  public deleteCohort(cohort: Cohort): void {
    this.cohortsService.deleteCohort(this.wsNamespace, this.wsId, cohort.id).subscribe(() => {
      this.cohortList.splice(
        this.cohortList.indexOf(cohort), 1);
      this.deleteModal.close();
    });
  }

  confirmDelete(cohort: Cohort): void {
    this.cohortInFocus = cohort;
    this.deleteModal.open();
  }

  receiveDelete($event): void {
    this.deleteCohort($event);
  }

  get writePermission(): boolean {
    return this.accessLevel === WorkspaceAccessLevel.OWNER
      || this.accessLevel === WorkspaceAccessLevel.WRITER;
  }

  get actionsDisabled(): boolean {
    return !this.writePermission || this.awaitingReview;
  }
}
