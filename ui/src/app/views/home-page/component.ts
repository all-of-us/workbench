import {Component, OnInit, Inject} from '@angular/core';
import {Router, ActivatedRoute} from '@angular/router';
import {DOCUMENT} from '@angular/platform-browser';
import {StringFilter, Comparator} from 'clarity-angular';

import {retryApi} from 'app/utils';
import {WorkspaceComponent} from 'app/views/workspace/component';

import {Workspace} from 'generated';
import {WorkspacesService} from 'generated';
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
  private workspaceNameFilter = new WorkspaceNameFilter();
  private workspaceResearchPurposeFilter = new WorkspaceResearchPurposeFilter();
  private workspaceNameComparator = new WorkspaceNameComparator();
  workspaceList: Workspace[] = [];
  workspacesLoading = false;
  constructor(
      private router: Router,
      private route: ActivatedRoute,
      private workspacesService: WorkspacesService,
      @Inject(DOCUMENT) private document: any
  ) {}
  ngOnInit(): void {
    this.workspacesLoading = true;

    retryApi(this.workspacesService
        .getWorkspaces(), 3)
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
}
