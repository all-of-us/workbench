import {Component, OnInit} from '@angular/core';

import {ErrorHandlingService} from 'app/services/error-handling.service';

import {
  ResearchPurposeReviewRequest,
  Workspace,
  WorkspacesService,
} from 'generated';


/**
 * Review Workspace research purposes. Users with the REVIEW_RESEARCH_PURPOSE permission use this
 * to view other users' workspaces for which a review has been requested, and approve/reject them.
 */
// TODO(RW-85) Design this UI. Current implementation is a rough sketch.
@Component({
  templateUrl: './component.html',
  styleUrls: ['./component.css']
})
export class ReviewComponent implements OnInit {
  workspaces: Workspace[] = [];
  contentLoaded = false;

  constructor(
      private errorHandlingService: ErrorHandlingService,
      private workspacesService: WorkspacesService
  ) {}

  ngOnInit(): void {
    this.errorHandlingService.retryApi(this.workspacesService.getWorkspacesForReview())
        .subscribe(
            workspacesResp => {
              for (const ws of workspacesResp.items) {
                this.workspaces.push(ws);
              }
              this.contentLoaded = true;
            });
  }

  approve(workspace: Workspace, approved: boolean): void {
    const request = <ResearchPurposeReviewRequest>{
      approved: approved,
    };
    this.errorHandlingService.retryApi(this.workspacesService.reviewWorkspace(
        workspace.namespace, workspace.id, request))
        .subscribe(
            resp => {
              const i = this.workspaces.indexOf(workspace, 0);
              if (i >= 0) {
                this.workspaces.splice(i, 1);
              }
            });
  }
}
