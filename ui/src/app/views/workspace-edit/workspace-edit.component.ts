import {Component, OnInit} from '@angular/core';
import {Router, ActivatedRoute} from '@angular/router';

import {Workspace} from 'generated';
import {WorkspacesService} from 'generated';
import {WorkspaceComponent} from 'app/views/workspace/workspace.component';

@Component({
  styleUrls: ['./workspace-edit.component.css'],
  templateUrl: './workspace-edit.component.html',
})
export class WorkspaceEditComponent implements OnInit {
  workspace: Workspace;
  workspaceId: string;
  adding = false;
  buttonClicked = false;
  constructor(
      private router: Router,
      private route: ActivatedRoute,
      private workspacesService: WorkspacesService,
  ) {}

  ngOnInit(): void {
    this.workspace = {
      name: '',
      description: '',
      dataAccessLevel: Workspace.DataAccessLevelEnum.Registered};
  }

  addWorkspace(): void {
    if (!this.buttonClicked) {
      this.buttonClicked = true;
      this.workspacesService
          .createWorkspace(
              this.workspace)
          .retry(2)
          .subscribe(cohorts => this.router.navigate(['../..'], {relativeTo : this.route}));
    }
  }
}
