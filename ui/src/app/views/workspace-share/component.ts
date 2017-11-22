import {Location} from '@angular/common';
import {Component, ElementRef, OnInit, ViewChild} from '@angular/core';
import {ActivatedRoute} from '@angular/router';
import {Observable} from 'rxjs/Observable';

import {ErrorHandlingService} from 'app/services/error-handling.service';

import {ProfileService} from 'generated';
import {UserRole} from 'generated';
import {ShareWorkspaceResponse} from 'generated';
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
  usersLoading = true;
  userNotFound = false;
  userNotFoundEmail = '';
  workspaceUpdateConflictError = false;
  @ViewChild('usernameSharingInput') input: ElementRef;

  constructor(
      private errorHandlingService: ErrorHandlingService,
      private locationService: Location,
      private route: ActivatedRoute,
      private profileService: ProfileService,
      private workspacesService: WorkspacesService,
  ) {}

  ngOnInit(): void {
    this.errorHandlingService.retryApi(
        this.loadWorkspace())
      .subscribe((workspace) => {
        this.errorHandlingService.retryApi(
            this.profileService.getMe()).subscribe(profile => {
          this.usersLoading = false;
          this.loadingWorkspace = false;
          this.userEmail = profile.username;
        });
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

  convertToEmail(username: string): string {
    return username + '@fake-research-aou.org';
  }


  addCollaborator(): void {
    if (!this.usersLoading) {
      this.usersLoading = true;
      const updateList = Array.from(this.workspace.userRoles);
      updateList.push({email: this.convertToEmail(this.toShare),
          role: this.accessLevel});

      this.errorHandlingService.retryApi(
        this.workspacesService.shareWorkspace(
          this.workspace.namespace,
          this.workspace.id, {
            workspaceEtag: this.workspace.etag,
            items: updateList})).subscribe(
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

  loadWorkspace(): Observable<Workspace> {
    const obs: Observable<Workspace> = this.workspacesService.getWorkspace(
      this.route.snapshot.params['ns'],
      this.route.snapshot.params['wsid']);
    obs.subscribe((workspace) => {
        this.workspace = workspace;
    });
    return obs;
  }

  reloadConflictingWorkspace(): void {
    this.loadWorkspace().subscribe(() => this.resetWorkspaceEditor());
  }

  resetWorkspaceEditor(): void {
    this.workspaceUpdateConflictError = false;
    this.usersLoading = false;
  }
}
