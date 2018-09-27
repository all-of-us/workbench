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
  @Input('accessLevel') accessLevel: WorkspaceAccessLevel;
  @ViewChild('usernameSharingInput') input: ElementRef;

  toShare = '';
  notFound = false;
  userEmail: string;
  usersLoading = true;
  userNotFound = false;
  workspaceUpdateConflictError = false;
  public sharing = false;
  gsuiteDomain: string;
  userRolesList: UserRole[] = [];

  // All new stuff to handle autocomplete.
  searchTerm: string;
  searchTermChanged = new Subject<string>();
  userResponse: UserResponse;
  autocompleteUsers: User[] = [];
  autocompleteNoResults = false;
  autocompleteLoading = false;

  constructor(private userService: UserService,
              private locationService: Location,
              private route: ActivatedRoute,
              public profileStorageService: ProfileStorageService,
              private workspacesService: WorkspacesService,
              private serverConfigService: ServerConfigService) {
    serverConfigService.getConfig().subscribe((config) => {
      this.gsuiteDomain = config.gsuiteDomain;
    });
    this.searchTermChanged
        .debounceTime(300)
        .distinctUntilChanged()
        .subscribe(model => {
          this.searchTerm = model;
          this.userSearch(this.searchTerm);
        });
  }

  ngOnInit(): void {
    this.profileStorageService.profile$.subscribe((profile) => {
      this.usersLoading = false;
      this.userEmail = profile.username;
    });
  }

  save(): void {
    if (this.usersLoading) {
      return;
    }
    this.usersLoading = true;
    this.workspace.userRoles = this.userRolesList;
    this.workspacesService.shareWorkspace(
        this.workspace.namespace,
        this.workspace.id, {
          workspaceEtag: this.workspace.etag,
          items: this.workspace.userRoles
        }).subscribe(
        (resp: ShareWorkspaceResponse) => {
          this.workspace.etag = resp.workspaceEtag;
          this.usersLoading = false;
          this.workspace.userRoles = resp.items;
          this.toShare = '';
          this.input.nativeElement.focus();
          this.searchTerm = '';
          this.closeModal();
        },
        (error) => {
          if (error.status === 400) {
            this.userNotFound = true;
          } else if (error.status === 409) {
            this.workspaceUpdateConflictError = true;
          }
          this.usersLoading = false;
        });
  }

  removeCollaborator(user: UserRole): void {
    if (!this.usersLoading) {
      this.usersLoading = true;
      const position = this.userRolesList.findIndex((userRole) => {
        if (user.email === userRole.email) {
          return true;
        } else {
          return false;
        }
      });
      this.userRolesList.splice(position, 1);
      this.usersLoading = false;
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

  addCollaborator(user: User): void {
    this.toShare = user.email;
    this.searchTerm = '';
    this.autocompleteLoading = false;
    this.autocompleteNoResults = false;
    this.autocompleteUsers = [];
    const userRole: UserRole = {
      givenName: user.givenName,
      familyName: user.familyName,
      email: user.email,
      role: WorkspaceAccessLevel.READER
    };
    this.userRolesList.splice(0, 0, userRole);
  }

  searchTermChangedEvent($event: string) {
    this.autocompleteLoading = true;
    this.searchTermChanged.next($event);
  }

  userSearch(value: string): void {
    this.autocompleteLoading = true;
    this.autocompleteNoResults = false;
    this.autocompleteUsers = [];

    if (!this.searchTerm.trim()) {
      this.autocompleteLoading = false;
      this.autocompleteNoResults = true;
      return;
    }
    this.userService.user(this.searchTerm).subscribe((response) => {
      this.autocompleteLoading = false;
      this.userResponse = response;
      response.users = response.users.filter(user => {
        return this.checkUnique(user.email, user.familyName, user.givenName);
      });
      this.autocompleteUsers = response.users.splice(0, 4);
      if (this.autocompleteUsers.length === 0) {
        this.autocompleteNoResults = true;
      }
    }, () => {
      this.autocompleteLoading = false;
    });
  }

  open(): void {
    this.userRolesList = [];
    this.sharing = true;
    this.userRolesList = this.workspace.userRoles;
  }

  closeModal() {
    this.toShare = '';
    this.searchTerm = '';
    this.sharing = false;
    this.userRolesList = [];
  }

  reloadConflictingWorkspace(): void {
    this.reloadWorkspace().subscribe(() => this.resetWorkspaceEditor());
  }

  resetWorkspaceEditor(): void {
    this.workspaceUpdateConflictError = false;
    this.usersLoading = false;
  }


  get hasPermission(): boolean {
    return this.accessLevel === WorkspaceAccessLevel.OWNER;
  }

  navigateBack(): void {
    this.locationService.back();
  }

  checkUnique(email: String, familyName: String, givenName: String): boolean {
    return Array.from(this.userRolesList)
        .filter(r => r.email === email)
        .filter(r => r.familyName === familyName)
        .filter(r => r.givenName === givenName)
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

}
