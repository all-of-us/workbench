import {Location} from '@angular/common';
import {Component, ElementRef, Input, OnInit, ViewChild} from '@angular/core';
import {ActivatedRoute} from '@angular/router';
import {Observable} from 'rxjs/Observable';

import {ProfileStorageService} from 'app/services/profile-storage.service';
import {ServerConfigService} from 'app/services/server-config.service';

import {
  ShareWorkspaceResponse,
  UserRole,
  Workspace,
  WorkspaceAccessLevel,
  WorkspaceResponse,
  WorkspacesService,
} from 'generated';

@Component({
  selector: 'app-workspace-share',
  styleUrls: ['./component.css',
    '../../styles/buttons.css'],
  templateUrl: './component.html',
})
export class WorkspaceShareComponent implements OnInit {
  @Input('workspace') workspace: Workspace;
  toShare = '';
  selectedPermission = 'Select Permission';
  roleNotSelected = false;
  @Input('accessLevel') accessLevel: WorkspaceAccessLevel;
  selectedAccessLevel: WorkspaceAccessLevel;
  notFound = false;
  userEmail: string;
  usersLoading = true;
  userNotFound = false;
  workspaceUpdateConflictError = false;
  public sharing = false;
  @ViewChild('usernameSharingInput') input: ElementRef;
  gsuiteDomain: string;

  constructor(
      private locationService: Location,
      private route: ActivatedRoute,
      public profileStorageService: ProfileStorageService,
      private workspacesService: WorkspacesService,
      private serverConfigService: ServerConfigService
  ) {
    serverConfigService.getConfig().subscribe((config) => {
      this.gsuiteDomain = config.gsuiteDomain;
    });
  }

  ngOnInit(): void {
    this.profileStorageService.profile$.subscribe((profile) => {
      this.usersLoading = false;
      this.userEmail = profile.username;
    });
  }

  setAccess(dropdownSelected: string): void {
    this.selectedPermission = dropdownSelected;
    if (dropdownSelected === 'Owner') {
      this.selectedAccessLevel = WorkspaceAccessLevel.OWNER;
    } else if (dropdownSelected === 'Writer') {
      this.selectedAccessLevel = WorkspaceAccessLevel.WRITER;
    } else {
      this.selectedAccessLevel = WorkspaceAccessLevel.READER;
    }
    this.roleNotSelected = false;
  }

  convertToEmail(username: string): string {
    if (username.endsWith('@' + this.gsuiteDomain)) {
      return username;
    }
    return username + '@' + this.gsuiteDomain;
  }


  addCollaborator(): void {
    if (this.selectedAccessLevel === undefined) {
      this.roleNotSelected = true;
      return;
    }
    if (!this.usersLoading) {
      const email = this.convertToEmail(this.toShare);
      const role = this.selectedAccessLevel;
      if (this.checkUnique(email, role)) {
        this.usersLoading = true;
        // A user can only have one role on a workspace so we replace them in the list
        const updateList = Array.from(this.workspace.userRoles)
          .filter(r => r.email !== email);
        updateList.push({
          email: email,
          role: role
        });
        this.workspacesService.shareWorkspace(
          this.workspace.namespace,
          this.workspace.id, {
            workspaceEtag: this.workspace.etag,
            items: updateList
          }).subscribe(
          (resp: ShareWorkspaceResponse) => {
            this.workspace.etag = resp.workspaceEtag;
            this.usersLoading = false;
            this.workspace.userRoles = resp.items;
            this.toShare = '';
            this.input.nativeElement.focus();
          },
          (error) => {
            if (error.status === 400) {
              this.userNotFound = true;
            } else if (error.status === 409) {
              this.workspaceUpdateConflictError = true;
            }
            this.usersLoading = false;
          }
        );
      }
    }
  }

  removeCollaborator(user: UserRole): void {
    if (!this.usersLoading) {
      this.usersLoading = true;
      const updateList = Array.from(this.workspace.userRoles);
      const position = updateList.findIndex((userRole) => {
        if (user.email === userRole.email) {
          return true;
        } else {
          return false;
        }
      });
      updateList.splice(position, 1);
      this.workspacesService.shareWorkspace(this.workspace.namespace,
          this.workspace.id, {
            workspaceEtag: this.workspace.etag,
            items: updateList}).subscribe(
        (resp: ShareWorkspaceResponse) => {
          this.workspace.etag = resp.workspaceEtag;
          this.usersLoading = false;
          this.workspace.userRoles = resp.items;
        },
        (error) => {
          this.usersLoading = false;
          if (error.status === 409) {
           this.workspaceUpdateConflictError = true;
         }
        }
      );
    }
  }

  reloadWorkspace(): Observable<WorkspaceResponse> {
    const obs: Observable<WorkspaceResponse> = this.workspacesService.getWorkspace(
      this.workspace.namespace,
      this.workspace.id);
    obs.subscribe(
      (workspaceResponse) => {
        this.accessLevel = workspaceResponse.accessLevel;
        this.workspace = workspaceResponse.workspace;
      },
      (error) => {
        if (error.status === 404) {
          this.notFound = true;
        }
      }
    );
    return obs;
  }

  reloadConflictingWorkspace(): void {
    this.reloadWorkspace().subscribe(() => this.resetWorkspaceEditor());
  }

  resetWorkspaceEditor(): void {
    this.workspaceUpdateConflictError = false;
    this.usersLoading = false;
  }

  open(): void {
    this.sharing = true;
  }

  get hasPermission(): boolean {
    return this.accessLevel === WorkspaceAccessLevel.OWNER;
  }

  navigateBack(): void {
    this.locationService.back();
  }

  // Checks for an email + role combination in the current list of user roles.
  checkUnique(email: String, role: WorkspaceAccessLevel): boolean {
    return Array.from(this.workspace.userRoles)
      .filter(r => r.email === email)
      .filter(r => r.role === role)
      .length === 0;
  }

}
