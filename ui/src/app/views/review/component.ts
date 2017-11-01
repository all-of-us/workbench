import {Component, OnInit, Inject} from '@angular/core';
import {Router, ActivatedRoute} from '@angular/router';
import {StringFilter, Comparator} from 'clarity-angular';
import {DOCUMENT} from '@angular/platform-browser';
import {Observable} from 'rxjs/Observable';

import {ErrorHandlingService} from 'app/services/error-handling.service';

import {Workspace} from 'generated';
import {WorkspacesService} from 'generated';
import {ResearchPurposeReviewRequest} from 'generated';

@Component({
  templateUrl: './component.html',
})
export class ReviewComponent implements OnInit {
  workspaces: Workspace[] = [];
  contentLoaded = false;

  constructor(
      private router: Router,
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
    var request = <ResearchPurposeReviewRequest>{
      approved: approved,
    };
    this.errorHandlingService.retryApi(this.workspacesService.reviewWorkspace(
        workspace.namespace, workspace.name, request))
        .subscribe(
            resp => {
              var i = this.workspaces.indexOf(workspace, 0);
              if (i >= 0) {
                this.workspaces.splice(i, 1);
              }
            });
  }
}
