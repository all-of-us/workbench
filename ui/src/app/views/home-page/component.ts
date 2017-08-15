import {Component, OnInit} from '@angular/core';
import {Router, ActivatedRoute} from '@angular/router';
import {StringFilter} from 'clarity-angular';

import {Workspace} from 'generated';
import {WorkspacesService} from 'generated';
import {Repository} from 'app/models/repository';
import {RepositoryService} from 'app/services/repository.service';
import {User} from 'app/models/user';
import {UserService} from 'app/services/user.service';
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
class WorkspaceDescriptionFilter implements StringFilter<Workspace> {
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
  private workspaceDescriptionFilter = new WorkspaceDescriptionFilter();

  repositories: Repository[] = [];
  user: User;  // to detect if logged in
  // TODO: Replace with real data/workspaces.
  // TODO: Implement API side workspace detection.
  workspaceList: Workspace[] = [];

  constructor(
      private router: Router,
      private route: ActivatedRoute,
      private userService: UserService,
      private repositoryService: RepositoryService,
      private workspacesService: WorkspacesService
  ) {}
  ngOnInit(): void {
    this.userService.getLoggedInUser().then(user => this.user = user);
    this.workspacesService
        .getWorkspaces()
        .retry(2)
        .subscribe(
            workspacesReceived => {
              this.workspaceList = workspacesReceived.items;
            });

  }

  goToWorkspace(namespace: string, id: string): void {
    this.router.navigate(['workspaces/' + namespace + '/' + id], {relativeTo : this.route});
  }
}
