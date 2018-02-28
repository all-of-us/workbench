import {Component, OnInit} from '@angular/core';

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
  styleUrls: ['./component.css']
})
export class AdminReviewWorkspaceComponent implements OnInit {
  workspaces: Workspace[] = [];
  contentLoaded = false;

  constructor(
      private workspacesService: WorkspacesService
  ) {}

  ngOnInit(): void {
    this.workspacesService.getWorkspacesForReview()
        .subscribe(
            workspacesResp => {
              this.workspaces = workspacesResp.items;
              this.contentLoaded = true;
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
            });
  }
}
