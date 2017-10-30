import {Location} from '@angular/common';
import {Component, OnInit} from '@angular/core';
import {Router, ActivatedRoute} from '@angular/router';

import {ErrorHandlingService} from 'app/services/error-handling.service';
import {WorkspaceComponent} from 'app/views/workspace/component';
import {isBlank} from 'app/utils';

import {DataAccessLevel} from 'generated';
import {Workspace} from 'generated';
import {WorkspacesService} from 'generated';

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

  constructor(
      private errorHandlingService: ErrorHandlingService,
      private locationService: Location,
      private router: Router,
      private route: ActivatedRoute,
      private workspacesService: WorkspacesService,
  ) {}

  ngOnInit(): void {
    this.workspace = {
      name: '',
      description: '',
      dataAccessLevel: DataAccessLevel.Registered,
      /**
       * TODO: use the free billing project created for the user once registration work is done
       */
      namespace: 'all-of-us-broad',
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
    } else {
      this.oldWorkspaceNamespace = this.route.snapshot.params['ns'];
      this.oldWorkspaceName = this.route.snapshot.params['wsid'];
      this.workspacesService.getWorkspace(this.oldWorkspaceNamespace,
          this.oldWorkspaceName)
        .subscribe((workspace) => {
          this.workspace = workspace;
        }
      );
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
  navigateBack(): void {
    this.locationService.back();
  }

  resetWorkspaceCreation(): void {
    this.workspaceCreationError = false;
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
              this.workspaceCreationError = true;
            });
      }
    }
  }
}
