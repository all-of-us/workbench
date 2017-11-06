import {Location} from '@angular/common';
import {Component, OnInit} from '@angular/core';
import {Router, ActivatedRoute} from '@angular/router';

import {ErrorHandlingService} from 'app/services/error-handling.service';
import {isBlank} from 'app/utils';

import {DataAccessLevel} from 'generated';
import {UserRole} from 'generated';
import {Workspace} from 'generated';
import {WorkspacesService} from 'generated';

@Component({
  styleUrls: ['./component.css'],
  templateUrl: './component.html',
})
export class WorkspaceShareComponent implements OnInit {
  workspace: Workspace;
  loadingWorkspace = true;
  toShare = '';
  selectedPermission = 'Select Permission';

  constructor(
      private errorHandlingService: ErrorHandlingService,
      private locationService: Location,
      private router: Router,
      private route: ActivatedRoute,
      private workspacesService: WorkspacesService,
  ) {}

  ngOnInit(): void {
    this.errorHandlingService.retryApi(
        this.workspacesService.getWorkspace(
            this.route.snapshot.params['ns'],
            this.route.snapshot.params['wsid']))
      .subscribe((workspace) => {
        this.loadingWorkspace = false;
        this.workspace = workspace;
        console.log(workspace);
      }
    );
  }

  navigateBack(): void {
    this.locationService.back();
  }

  addCollaborator(): void {
    this.workspace.userRoles.items.push({user: this.toShare, role: this.selectedPermission});
    this.workspacesService.shareWorkspace(this.workspace.namespace,
        this.workspace.id, this.workspace.userRoles).subscribe(
      () => {
        console.log('Successfully Shared.');
      }
    );
  }

  removeCollaborator(user: UserRole): void {
    const position = this.workspace.userRoles.items.findIndex((userRole) => {
      if (user.user === userRole.user) {
        return true;
      } else {
        return false;
      }
    });
    this.workspace.userRoles.items.splice(position, 1);
  }
}
