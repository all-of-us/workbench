import {Location} from '@angular/common';
import {Component, ElementRef, Input, OnInit, ViewChild} from '@angular/core';
import {ActivatedRoute} from '@angular/router';
import {Observable} from 'rxjs/Observable';
import {Subject} from 'rxjs/Subject';

import {ProfileStorageService} from 'app/services/profile-storage.service';
import {ServerConfigService} from 'app/services/server-config.service';
import {isBlank} from 'app/utils';

import 'rxjs/add/operator/debounceTime';
import 'rxjs/add/operator/distinctUntilChanged';
import 'rxjs/add/operator/map';

import {
  ShareWorkspaceResponse,
  User,
  UserResponse,
  UserRole,
  UserService,
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
  toShareChanged = new Subject<string>();
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

  // All new stuff to handle autocomplete. TODO: Clean this up before PR
  searchTerm: string;
  // searchUpdated: Subject = new Subject();
  userResponse: UserResponse;
  autocompleteUsers: User[] = [];
  autocompleteNoResults = false;
  autocompleteLoading = false;
  selectedUser: User;

  constructor(
      private userService: UserService,
      private locationService: Location,
      private route: ActivatedRoute,
      public profileStorageService: ProfileStorageService,
      private workspacesService: WorkspacesService,
      private serverConfigService: ServerConfigService
  ) {
    serverConfigService.getConfig().subscribe((config) => {
      this.gsuiteDomain = config.gsuiteDomain;
    });
    this.toShareChanged
      .debounceTime(300) // wait 300 millisec after the last event before emitting last event
      .distinctUntilChanged() // only emit if value is different from previous value
      .subscribe(model => {
        this.toShare = model;

        // Call your function which calls API or do anything you would like do after a lag of 1 sec
        this.userSearch(this.toShare);
      });
  }

  ngOnInit(): void {
    this.profileStorageService.profile$.subscribe((profile) => {
      this.usersLoading = false;
      this.userEmail = profile.username;
    });
  }

  toShareChangedEvent($event: string) {
    this.autocompleteLoading = true;
    this.toShareChanged.next($event);
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

  clearDropdown(): void {
    this.autocompleteUsers = [];
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
        const updateList = this.workspace.userRoles
          .filter(r => r.email !== email)
          .map((userRole) => ({email: userRole.email, role: userRole.role}));
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
            this.selectedUser = undefined;
            this.toShare = '';
            this.searchTerm = '';
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
      const updateList = this.workspace.userRoles
        .map((userRole) => ({email: userRole.email, role: userRole.role}));
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

  closeModal() {
    this.selectedUser = undefined;
    this.toShare = '';
    this.searchTerm = '';
    this.sharing = false;
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

  get showSearchResults(): boolean {
    return !this.autocompleteLoading &&
      this.autocompleteUsers.length > 0 &&
      !isBlank(this.searchTerm);
  }

  get showAutocompleteNoResults(): boolean {
    return this.autocompleteNoResults &&
        !isBlank(this.searchTerm);
  }

  userSearch(value: string): void {
    this.autocompleteLoading = true;
    this.autocompleteNoResults = false;
    this.autocompleteUsers = [];
    this.selectedUser = null;

    if (!this.searchTerm.trim()) {
      this.autocompleteLoading = false;
      this.autocompleteNoResults = true;
      return;
    }
    this.userService.user(this.searchTerm).subscribe((response) => {
      this.autocompleteLoading = false;
      this.userResponse = response;
      // We want to limit to the first four results
      this.autocompleteUsers = response.users.splice(0, 4);
      if (this.autocompleteUsers.length === 0) {
        this.autocompleteNoResults = true;
      }
    }, () => {
      this.autocompleteLoading = false;
    });
  }

  selectUser(user: User): void {
    this.selectedUser = user;
    this.toShare = user.email;
    this.searchTerm = user.email;
    this.autocompleteLoading = false;
    this.autocompleteNoResults = false;
    this.autocompleteUsers = [];
  }

  clearSelectedUser() {
    this.selectedUser = undefined;
    this.toShare = '';
    this.searchTerm = '';
    this.autocompleteLoading = false;
    this.autocompleteNoResults = false;
    this.autocompleteUsers = [];
  }

}
