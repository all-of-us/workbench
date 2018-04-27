import {Location} from '@angular/common';
import {Component, ElementRef, OnInit, ViewChild} from '@angular/core';
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
  workspace: Workspace = {
    name: '',
    userRoles: []
  };
  loadingWorkspace = true;
  loadWorkspaceFinished = false;
  toShare = '';
  selectedPermission = 'Select Permission';
  roleNotSelected = false;
  private accessLevel: WorkspaceAccessLevel;
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
      if (this.loadWorkspaceFinished === true) {
        this.loadingWorkspace = false;
      }
      this.userEmail = profile.username;
    });

    this.loadWorkspace().subscribe((workspace) => {
      this.loadWorkspaceFinished = true;
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
      this.usersLoading = true;
      const updateList = Array.from(this.workspace.userRoles);
      updateList.push({
        email: this.convertToEmail(this.toShare),
        role: this.selectedAccessLevel
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
            this.workspace.userRoles = updateList;
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
          this.workspace.userRoles = updateList;
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

  loadWorkspace(): Observable<WorkspaceResponse> {
    const obs: Observable<WorkspaceResponse> = this.workspacesService.getWorkspace(
      this.route.snapshot.params['ns'],
      this.route.snapshot.params['wsid']);
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
    this.loadWorkspace().subscribe(() => this.resetWorkspaceEditor());
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

}
