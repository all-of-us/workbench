import {Component, OnInit, ViewChild} from '@angular/core';
import {ActivatedRoute, Router} from '@angular/router';

import {WorkspaceData} from 'app/resolvers/workspace';

import {BugReportComponent} from 'app/views/bug-report/component';
import {ConfirmDeleteModalComponent} from 'app/views/confirm-delete-modal/component';
import {WorkspaceShareComponent} from 'app/views/workspace-share/component';

import {
  Workspace,
  WorkspaceAccessLevel,
  WorkspacesService,
} from 'generated';

@Component({
  selector: 'app-workspace-nav-bar',
  styleUrls: ['../../styles/buttons.css',
              '../../styles/headers.css',
              './component.css'],
  templateUrl: './component.html',
})
export class WorkspaceNavBarComponent implements OnInit {
  @ViewChild(WorkspaceShareComponent)
  shareModal: WorkspaceShareComponent;


  @ViewChild(ConfirmDeleteModalComponent)
  deleteModal: ConfirmDeleteModalComponent;

  workspace: Workspace;
  wsId: string;
  wsNamespace: string;
  private accessLevel: WorkspaceAccessLevel;
  deleting = false;
  workspaceDeletionError = false;

  @ViewChild(BugReportComponent)
  bugReportComponent: BugReportComponent;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private workspacesService: WorkspacesService
  ) {}

  ngOnInit(): void {
    const wsData: WorkspaceData = this.route.snapshot.data.workspace;
    this.workspace = wsData;
    this.accessLevel = wsData.accessLevel;
    const {approved, reviewRequested} = this.workspace.researchPurpose;
    this.wsNamespace = this.route.snapshot.params['ns'];
    this.wsId = this.route.snapshot.params['wsid'];
  }

  delete(workspace: Workspace): void {
    this.deleting = true;
    this.workspacesService.deleteWorkspace(
      workspace.namespace, workspace.id).subscribe(() => {
        this.router.navigate(['/workspaces']);
    }, () => {
      this.workspaceDeletionError = true;
    });
  }

  receiveDelete($event): void {
    this.delete($event);
  }

  confirmDelete(): void {
    this.deleteModal.open();
  }

  share(): void {
    this.shareModal.open();
  }

  get writePermission(): boolean {
    return this.accessLevel === WorkspaceAccessLevel.OWNER
      || this.accessLevel === WorkspaceAccessLevel.WRITER;
  }

  get ownerPermission(): boolean {
    return this.accessLevel === WorkspaceAccessLevel.OWNER;
  }

  submitWorkspaceDeleteBugReport(): void {
    this.workspaceDeletionError = false;
    this.bugReportComponent.reportBug();
    this.bugReportComponent.bugReport.shortDescription = 'Could not delete workspace.';
  }
}
