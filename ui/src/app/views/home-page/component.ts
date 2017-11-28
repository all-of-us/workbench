import {Component, OnInit} from '@angular/core';
import {ActivatedRoute, Router} from '@angular/router';
import {Comparator, StringFilter} from 'clarity-angular';

import {ErrorHandlingService} from 'app/services/error-handling.service';

import {
  Workspace,
  WorkspaceResponse,
  WorkspacesService
} from 'generated';

/*
* Search filters used by the workspace data table to
* determine which of the cohorts loaded into client side memory
* are displayed.
*/
class WorkspaceNameFilter implements StringFilter<Workspace> {
  accepts(workspace: Workspace, search: string): boolean {
    return workspace.name.toLowerCase().indexOf(search) >= 0;
  }
}

class WorkspaceNameComparator implements Comparator<Workspace> {
  compare(a: Workspace, b: Workspace) {
    return a.name.localeCompare(b.name);
  }
}

// TODO: Change to research purpose?
class WorkspaceResearchPurposeFilter implements StringFilter<Workspace> {
  accepts(workspace: Workspace, search: string): boolean {
    return workspace.description.toLowerCase().indexOf(search) >= 0;
  }
}


@Component({
  styleUrls: ['./component.css'],
  templateUrl: './component.html',
})
export class HomePageComponent implements OnInit {

  /* tslint:disable:no-unused-variable */
  private workspaceNameFilter = new WorkspaceNameFilter();
  private workspaceResearchPurposeFilter = new WorkspaceResearchPurposeFilter();
  private workspaceNameComparator = new WorkspaceNameComparator();
  /* tslint:enable:no-unused-variable */

  workspaceList: WorkspaceResponse[] = [];
  workspacesLoading = false;
  constructor(
      private route: ActivatedRoute,
      private errorHandlingService: ErrorHandlingService,
      private router: Router,
      private workspacesService: WorkspacesService,
  ) {}
  ngOnInit(): void {
    this.workspacesLoading = true;
    this.errorHandlingService.retryApi(this.workspacesService
        .getWorkspaces())
        .subscribe(
            workspacesReceived => {
              this.workspaceList = workspacesReceived.items;
              this.workspacesLoading = false;
            },
            error => {
              // TODO: Add Error Message.
              this.workspacesLoading = false;
            });
  }

  addWorkspace(): void {
    this.router.navigate(['workspace/build'], {relativeTo : this.route});
  }
}
