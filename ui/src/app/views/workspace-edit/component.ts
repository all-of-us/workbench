import {Location} from '@angular/common';
import {Component, OnInit} from '@angular/core';
import {ActivatedRoute, Router} from '@angular/router';
import {Observable} from 'rxjs/Observable';

import {ErrorHandlingService} from 'app/services/error-handling.service';
import {isBlank} from 'app/utils';

import {
  CloneWorkspaceResponse,
  DataAccessLevel,
  ProfileService,
  Workspace,
  WorkspaceAccessLevel,
  WorkspaceResponse,
  WorkspacesService
} from 'generated';

export enum WorkspaceEditMode { Create = 1, Edit = 2, Clone = 3 }

@Component({
  styleUrls: ['./component.css'],
  templateUrl: './component.html',
})
export class WorkspaceEditComponent implements OnInit {
  // Defines the supported modes for the workspace edit component.
  // Unfortunately, it is not currently possible to define an enum directly
  // within a class in Typescript, so make do with this type alias.
  Mode = WorkspaceEditMode;

  mode: WorkspaceEditMode;
  workspace: Workspace;
  workspaceId: string;
  oldWorkspaceName: string;
  oldWorkspaceNamespace: string;
  savingWorkspace = false;
  nameNotEntered = false;
  notFound = false;
  workspaceCreationError = false;
  workspaceUpdateError = false;
  workspaceUpdateConflictError = false;
  private accessLevel: WorkspaceAccessLevel;
  constructor(
      private errorHandlingService: ErrorHandlingService,
      private locationService: Location,
      private route: ActivatedRoute,
      private workspacesService: WorkspacesService,
      private profileService: ProfileService,
      private router: Router,
  ) {}

  ngOnInit(): void {
    this.workspace = {
      name: '',
      description: '',
      dataAccessLevel: DataAccessLevel.Registered,
      // TODO - please set this properly
      cdrVersionId: '1',
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
    this.mode = WorkspaceEditMode.Edit;
    if (this.route.routeConfig.data.mode) {
      this.mode = this.route.routeConfig.data.mode;
    }

    if (this.mode === WorkspaceEditMode.Create || this.mode === WorkspaceEditMode.Clone) {
      // There is a new workspace to be created via this flow.
      this.accessLevel = WorkspaceAccessLevel.OWNER;
      this.errorHandlingService.retryApi(this.profileService.getMe()).subscribe(profile => {
        this.workspace.namespace = profile.freeTierBillingProjectName;
      });
    }
    if (this.mode === WorkspaceEditMode.Edit || this.mode === WorkspaceEditMode.Clone) {
      // There is an existing workspace referenced in this flow.
      this.oldWorkspaceNamespace = this.route.snapshot.params['ns'];
      this.oldWorkspaceName = this.route.snapshot.params['wsid'];
      this.loadWorkspace();
    }
  }

  loadWorkspace(): Observable<WorkspaceResponse> {
    const obs: Observable<WorkspaceResponse> = this.workspacesService.getWorkspace(
      this.oldWorkspaceNamespace, this.oldWorkspaceName);
    obs.subscribe(
      (resp) => {
        if (this.mode === WorkspaceEditMode.Edit) {
          this.workspace = resp.workspace;
          this.accessLevel = resp.accessLevel;
        } else if (this.mode === WorkspaceEditMode.Clone) {
          this.workspace.name = 'Clone of ' + resp.workspace.name;
          this.workspace.description = resp.workspace.description;
        }
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
    this.loadWorkspace().subscribe(() => {
      this.resetWorkspaceEditor();
    });
  }

  resetWorkspaceEditor(): void {
    this.workspaceCreationError = false;
    this.workspaceUpdateError = false;
    this.workspaceUpdateConflictError = false;
    this.savingWorkspace = false;
  }

  validateForm(): boolean {
    if (this.savingWorkspace) {
      return false;
    }
    this.nameNotEntered = isBlank(this.workspace.name);
    if (this.nameNotEntered) {
      return false;
    }
    return true;
  }

  addWorkspace(): void {
    if (!this.validateForm()) {
      return;
    }
    this.savingWorkspace = true;
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

  updateWorkspace(): void {
    if (!this.validateForm()) {
      return;
    }
    this.savingWorkspace = true;
    this.errorHandlingService.retryApi(this.workspacesService.updateWorkspace(
      this.oldWorkspaceNamespace,
      this.oldWorkspaceName,
      {workspace: this.workspace}))
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

  cloneWorkspace(): void {
    if (!this.validateForm()) {
      return;
    }
    this.savingWorkspace = true;
    this.errorHandlingService.retryApi(this.workspacesService.cloneWorkspace(
      this.oldWorkspaceNamespace,
      this.oldWorkspaceName, {
        workspace: this.workspace,
      }))
      .subscribe(
        (r: CloneWorkspaceResponse) => {
          this.router.navigate(['/workspace', r.workspace.namespace, r.workspace.id]);
        },
        () => {
          // Only expected errors are transient, so allow the user to try again.
          this.resetWorkspaceEditor();
        });
  }

  get hasPermission(): boolean {
    return this.accessLevel === WorkspaceAccessLevel.OWNER
        || this.accessLevel === WorkspaceAccessLevel.WRITER;
  }
}
