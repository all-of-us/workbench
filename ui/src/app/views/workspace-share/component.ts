import {Location} from '@angular/common';
import {Component, OnInit} from '@angular/core';
import {Router, ActivatedRoute} from '@angular/router';

import {ErrorHandlingService} from 'app/services/error-handling.service';
import {isBlank} from 'app/utils';

import {DataAccessLevel} from 'generated';
import {ProfileService} from 'generated';
import {UserRole} from 'generated';
import {UserRoleList} from 'generated';
import {Workspace} from 'generated';
import {WorkspaceAccessLevel} from 'generated';
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
  accessLevel: WorkspaceAccessLevel;
  userEmail: string;
  constructor(
      private errorHandlingService: ErrorHandlingService,
      private locationService: Location,
      private router: Router,
      private route: ActivatedRoute,
      private profileService: ProfileService,
      private workspacesService: WorkspacesService,
  ) {}

  ngOnInit(): void {
    this.errorHandlingService.retryApi(
        this.workspacesService.getWorkspace(
            this.route.snapshot.params['ns'],
            this.route.snapshot.params['wsid']))
      .subscribe((workspace) => {
        this.errorHandlingService.retryApi(
            this.profileService.getMe()).subscribe(profile => {
          this.loadingWorkspace = false;
          this.userEmail = profile.username;
        });
        this.workspace = workspace;
      }
    );
  }

  navigateBack(): void {
    this.locationService.back();
  }

  setAccess(dropdownSelected: string): void {
    this.selectedPermission = dropdownSelected;
    if (dropdownSelected === 'Owner') {
      this.accessLevel = WorkspaceAccessLevel.OWNER;
    } else if (dropdownSelected === 'Writer') {
      this.accessLevel = WorkspaceAccessLevel.WRITER;
    } else {
      this.accessLevel = WorkspaceAccessLevel.READER;
    }
  }

  addCollaborator(): void {
    this.workspace.userRoles.push({email: this.toShare, role: this.accessLevel});
    const userRoleList: UserRoleList = {items: this.workspace.userRoles};
    this.workspacesService.shareWorkspace(this.workspace.namespace,
        this.workspace.id, userRoleList).subscribe(
      () => {
      }
    );
  }

  removeCollaborator(user: UserRole): void {
    const position = this.workspace.userRoles.findIndex((userRole) => {
      if (user.email === userRole.email) {
        return true;
      } else {
        return false;
      }
    });
    this.workspace.userRoles.splice(position, 1);
    const userRoleList: UserRoleList = {items: this.workspace.userRoles};
    this.workspacesService.shareWorkspace(this.workspace.namespace,
        this.workspace.id, userRoleList).subscribe(
      () => {
      }
    );
  }
}
