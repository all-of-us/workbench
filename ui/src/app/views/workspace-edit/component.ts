import {Location} from '@angular/common';
import {Component, OnInit} from '@angular/core';
import {Router, ActivatedRoute} from '@angular/router';

import {ErrorHandlingService} from 'app/services/error-handling.service';
import {WorkspaceComponent} from 'app/views/workspace/component';
import {isBlank} from 'app/utils';

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
  buttonClicked = false;
  valueNotEntered = false;
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
      dataAccessLevel: Workspace.DataAccessLevelEnum.Registered,
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
    if (this.route.routeConfig.data.title === 'Create Workspace') {
      this.adding = true;
    } else {
      this.oldWorkspaceNamespace = this.route.snapshot.url[1].path;
      this.oldWorkspaceName = this.route.snapshot.url[2].path;
      this.workspacesService.getWorkspace(this.oldWorkspaceNamespace,
          this.oldWorkspaceName)
        .subscribe((workspace) => {
          this.workspace = workspace;
        }
      );
    }

  }

  addWorkspace(): void {
    if (!this.buttonClicked) {
      if (isBlank(this.workspace.name)) {
        this.valueNotEntered = true;
        const nameArea = document.getElementsByClassName('name-area')[0];
        nameArea.classList.add('validation-error');
      } else {
        this.buttonClicked = true;
        this.valueNotEntered = false;
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
    this.buttonClicked = false;
  }

  updateWorkspace(): void {
    if (!this.buttonClicked) {
      if (isBlank(this.workspace.name)) {
        this.valueNotEntered = true;
        const nameArea = document.getElementsByClassName('name-area')[0];
        nameArea.classList.add('validation-error');
      } else {
        this.buttonClicked = true;
        this.valueNotEntered = false;
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
