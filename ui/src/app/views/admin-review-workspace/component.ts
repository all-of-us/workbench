import {Component, OnInit, ViewChild} from '@angular/core';

import {BugReportComponent} from 'app/views/bug-report/component';

import {
  ResearchPurposeReviewRequest,
  Workspace,
  WorkspacesService,
} from 'generated';


/**
 * Review Workspaces. Users with the REVIEW_RESEARCH_PURPOSE permission use this
 * to view other users' workspaces for which a review has been requested, and approve/reject them.
 */
// TODO(RW-85) Design this UI. Current implementation is a rough sketch.
@Component({
  templateUrl: './component.html',
  styleUrls: ['./component.css',
              '../../styles/errors.css']
})
export class AdminReviewWorkspaceComponent implements OnInit {
  workspaces: Workspace[] = [];
  contentLoaded = false;
  fetchingWorkspacesError = false;
  reviewedWorkspace: Workspace;
  reviewError = false;

  @ViewChild(BugReportComponent)
  bugReportComponent: BugReportComponent;
  constructor(
      private workspacesService: WorkspacesService
  ) {}

  ngOnInit(): void {
    this.workspacesService.getWorkspacesForReview()
        .subscribe(
            workspacesResp => {
              this.workspaces = workspacesResp.items;
              this.contentLoaded = true;
            }, () => {
              this.fetchingWorkspacesError = true;
            });
  }

  approve(workspace: Workspace, approved: boolean): void {
    const request = <ResearchPurposeReviewRequest> {approved};
    this.workspacesService.reviewWorkspace(
        workspace.namespace, workspace.id, request)
        .subscribe(
            resp => {
              const i = this.workspaces.indexOf(workspace, 0);
              if (i >= 0) {
                this.workspaces.splice(i, 1);
              }
            }, () => {
              const i = this.workspaces.indexOf(workspace, 0);
              if (i >= 0) {
                this.reviewedWorkspace = this.workspaces[i];
              }
              this.reviewError = true;
            });
  }

  submitFetchingWorkspacesBugReport(): void {
    this.bugReportComponent.reportBug();
    this.bugReportComponent.bugReport.shortDescription =
        'Could not fetch workspaces for approval';
  }

  submitReviewWorkspaceBugReport(): void {
    this.reviewError = false;
    this.bugReportComponent.reportBug();
    this.bugReportComponent.bugReport.shortDescription =
        'Could not review workspace: \'' + this.reviewedWorkspace.namespace + '/' +
        this.reviewedWorkspace.name + '\'';
  }
}
