import {Location} from '@angular/common';
import {Component, OnInit} from '@angular/core';
import {ActivatedRoute} from '@angular/router';
import {Observable} from 'rxjs/Observable';

import {ErrorHandlingService} from 'app/services/error-handling.service';
import {isBlank} from 'app/utils';

import {
  DataAccessLevel,
  ProfileService,
  Workspace,
  WorkspaceAccessLevel,
  WorkspaceResponse,
  WorkspacesService
} from 'generated';

@Component({
  styleUrls: ['./component.css'],
  templateUrl: './component.html',
})
export class WorkspaceEditComponent implements OnInit {
  workspace: Workspace;
  workspaceId: string;
  oldWorkspaceName: string;
  oldWorkspaceNamespace: string;
  adding = false;
  savingWorkspace = false;
  nameNotEntered = false;
  workspaceCreationError = false;
  workspaceUpdateError = false;
  workspaceUpdateConflictError = false;
  notFound = false;
  accessLevel: WorkspaceAccessLevel;
  insufficientPermissions = true;
  constructor(
      private errorHandlingService: ErrorHandlingService,
      private locationService: Location,
      private route: ActivatedRoute,
      private workspacesService: WorkspacesService,
      private profileService: ProfileService,
  ) {}

  ngOnInit(): void {
    this.workspace = {
      name: '',
      description: '',
      dataAccessLevel: DataAccessLevel.Registered,
      researchPurpose: {
        diseaseFocusedResearch: false,
        methodsDevelopment: false,
        controlSet: false,
        aggregateAnalysis: false,
        ancestry: false,
        commercialPurpose: false,
        population: false,
        reviewRequested: false
      }};
    if (this.route.routeConfig.data.adding) {
      this.adding = true;
      this.errorHandlingService.retryApi(this.profileService.getMe()).subscribe(profile => {
        this.workspace.namespace = profile.freeTierBillingProjectName;
      });
      this.accessLevel = WorkspaceAccessLevel.OWNER;
    } else {
      this.oldWorkspaceNamespace = this.route.snapshot.params['ns'];
      this.oldWorkspaceName = this.route.snapshot.params['wsid'];
      this.loadWorkspace();
    }
  }

  addWorkspace(): void {
    if (!this.savingWorkspace) {
      if (isBlank(this.workspace.name)) {
        this.nameNotEntered = true;
      } else {
        this.savingWorkspace = true;
        this.nameNotEntered = false;
        this.errorHandlingService.retryApi(
          this.workspacesService.createWorkspace(this.workspace))
            .subscribe(
              () => {
                this.navigateBack();
              },
              (error) => {
                this.workspaceCreationError = true;
              });
      }
    }
  }

  loadWorkspace(): Observable<WorkspaceResponse> {
    const obs: Observable<WorkspaceResponse> = this.workspacesService.getWorkspace(
      this.oldWorkspaceNamespace, this.oldWorkspaceName);
    obs.subscribe(
      (workspaceResponse) => {
        this.accessLevel = workspaceResponse.accessLevel;
        this.workspace = workspaceResponse.workspace;
      },
      (error) => {
        if (error.status === 404) {
          this.notFound = true;
        }
      }
    );
    return obs;
  }

  navigateBack(): void {
    this.locationService.back();
  }

  reloadConflictingWorkspace(): void {
    this.loadWorkspace().subscribe(() => this.resetWorkspaceEditor());
  }

  resetWorkspaceEditor(): void {
    this.workspaceCreationError = false;
    this.workspaceUpdateError = false;
    this.workspaceUpdateConflictError = false;
    this.savingWorkspace = false;
  }

  updateWorkspace(): void {
    if (!this.savingWorkspace) {
      if (isBlank(this.workspace.name)) {
        this.nameNotEntered = true;
      } else {
        this.savingWorkspace = true;
        this.nameNotEntered = false;
        this.errorHandlingService.retryApi(this.workspacesService.updateWorkspace(
            this.oldWorkspaceNamespace,
            this.oldWorkspaceName,
            this.workspace))
          .subscribe(
            () => {
              this.navigateBack();
            },
            (error) => {
              if (error.status === 409) {
                this.workspaceUpdateConflictError = true;
              } else {
                this.workspaceUpdateError = true;
              }
            });
      }
    }
  }
  hasPermission(): boolean {
    return this.accessLevel === WorkspaceAccessLevel.OWNER
        || this.accessLevel === WorkspaceAccessLevel.WRITER;
  }
}
