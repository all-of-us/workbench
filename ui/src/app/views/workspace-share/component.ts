import {Location} from '@angular/common';
import {Component, ElementRef, OnInit, ViewChild} from '@angular/core';
import {ActivatedRoute} from '@angular/router';

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
  updateList: Array<UserRole>;
  usersLoading = true;
  userNotFound = false;
  userNotFoundEmail = '';
  inputNotInitialized = true;
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
        this.workspacesService.getWorkspace(
            this.route.snapshot.params['ns'],
            this.route.snapshot.params['wsid']))
      .subscribe((workspace) => {
        this.errorHandlingService.retryApi(
            this.profileService.getMe()).subscribe(profile => {
          this.usersLoading = false;
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

  convertToEmail(username: string): string {
    return username + '@fake-research-aou.org';
  }

  inputChange(): void {
    this.userNotFound = false;
  }

  addCollaborator(): void {
    if (this.inputNotInitialized) {
      this.inputNotInitialized = false;
      this.input.nativeElement.addEventListener('keydown', () => {
        this.userNotFound = false;
      });
    }
    this.usersLoading = true;
    this.updateList = Array.from(this.workspace.userRoles);
    this.updateList.push({email: this.convertToEmail(this.toShare),
        role: this.accessLevel});

    this.errorHandlingService.retryApi(
      this.workspacesService.shareWorkspace(
        this.workspace.namespace,
        this.workspace.id, {
          workspaceEtag: this.workspace.etag,
          items: this.updateList})).subscribe(
      (resp: ShareWorkspaceResponse) => {
        this.workspace.etag = resp.workspaceEtag;
        this.usersLoading = false;
        this.workspace.userRoles = this.updateList;
        this.toShare = '';
        this.input.nativeElement.focus();
      },
      (error) => {
        if (error.status === 400) {
          this.userNotFound = true;
        }
        this.usersLoading = false;
      }
    );
  }

  removeCollaborator(user: UserRole): void {
    this.usersLoading = true;
    this.updateList = Array.from(this.workspace.userRoles);
    const position = this.updateList.findIndex((userRole) => {
      if (user.email === userRole.email) {
        return true;
      } else {
        return false;
      }
    });

    this.updateList.splice(position, 1);
    this.workspacesService.shareWorkspace(this.workspace.namespace,
        this.workspace.id, {
          workspaceEtag: this.workspace.etag,
          items: this.updateList}).subscribe(
      (resp: ShareWorkspaceResponse) => {
        this.workspace.etag = resp.workspaceEtag;
        this.usersLoading = false;
        this.workspace.userRoles = this.updateList;
      },
      (error) => {
        this.usersLoading = false;
      }
    );
  }
}
