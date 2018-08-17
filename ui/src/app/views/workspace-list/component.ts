import {Component, OnDestroy, OnInit, ViewChild} from '@angular/core';
import {ActivatedRoute, Router} from '@angular/router';
import {ErrorHandlingService} from 'app/services/error-handling.service';
import {ProfileStorageService} from 'app/services/profile-storage.service';
import {Subscription} from 'rxjs/Subscription';
import {Workspace} from '../../../generated';
import {WorkspacePermissions} from '../../utils/workspacePermissions';
import {ConfirmDeleteModalComponent} from '../confirm-delete-modal/component';
import {WorkspaceShareComponent} from '../workspace-share/component';

import {
  BillingProjectStatus,
  ErrorResponse,
  WorkspaceAccessLevel,
  WorkspacesService
} from 'generated';


@Component({
  styleUrls: ['./component.css',
              '../../styles/buttons.css',
              '../../styles/cards.css'],
  templateUrl: './component.html',
})
export class WorkspaceListComponent implements OnInit, OnDestroy {

  // TODO: Consider moving profile load to be in a resolver - currently we have
  // a degenerate third undefined state for this boolean where we don't yet know
  // whether billing has been initialized.
  billingProjectInitialized: boolean;
  billingProjectQuery: NodeJS.Timer;
  errorText: string;
  workspaceList: WorkspacePermissions[] = [];
  workspacesLoading = false;
  firstSignIn: Date;
  twoFactorEnabled: boolean;
  private profileSubscription: Subscription;

  // All the things related to sharing a workspace
  @ViewChild(WorkspaceShareComponent)
  shareModal: WorkspaceShareComponent;
  // TODO This is necessary to placate the sharing template - figure out how to remove it
  selectedWorkspace: Workspace = {name: ''};
  accessLevel: WorkspaceAccessLevel;

  // All the things related to deleting a workspace
  @ViewChild(ConfirmDeleteModalComponent)
  deleteModal: ConfirmDeleteModalComponent;
  deleting = false;
  workspaceDeletionError = false;
  resource: Workspace;
  // TODO This is necessary to placate the delete error template - figure out how to remove it
  workspace: Workspace = {name: ''};

  constructor(
      private profileStorageService: ProfileStorageService,
      private route: ActivatedRoute,
      private router: Router,
      private workspacesService: WorkspacesService,
  ) {}

  ngOnInit(): void {
    this.workspacesLoading = true;
    this.profileSubscription = this.profileStorageService.profile$.subscribe(
      (profile) => {
        this.twoFactorEnabled = profile.twoFactorEnabled;
        if (this.firstSignIn === undefined) {
          this.firstSignIn = new Date(profile.firstSignInTime);
        }
        if (profile.freeTierBillingProjectStatus === BillingProjectStatus.Ready) {
          this.billingProjectInitialized = true;
          // Only once we know the billing project status do we request/display
          // workspaces for two reasons:
          // - If the FC user is not yet initialized, getWorkspaces() may fail
          //   with a 401.
          // - While the billing project is being initialized, we want to keep the
          //   big spinner on the page to provide obvious messaging to the user
          //   about the expected wait time.
          this.reloadWorkspaces();
          // This may execute synchronously, no guarantee this has been assigned above yet.
          if (this.profileSubscription) {
            this.profileSubscription.unsubscribe();
          }
        } else {
          this.billingProjectInitialized = false;
          this.billingProjectQuery = setTimeout(() => {
            this.profileStorageService.reload();
          }, 10000);
        }
      });
    this.profileStorageService.reload();
  }

  ngOnDestroy(): void {
    if (this.billingProjectQuery) {
      clearTimeout(this.billingProjectQuery);
    }
    this.profileSubscription.unsubscribe();
  }

  addWorkspace(): void {
    this.router.navigate(['workspaces/build']);
  }

  reloadWorkspaces(): void {
    this.workspacesService.getWorkspaces()
      .subscribe(
        workspacesReceived => {
          workspacesReceived.items.sort(function(a, b) {
            return a.workspace.name.localeCompare(b.workspace.name);
          });
          this.workspaceList = workspacesReceived
            .items
            .map( w => WorkspacePermissions.create(w));
          this.workspacesLoading = false;
        },
        error => {
          const response: ErrorResponse = ErrorHandlingService.convertAPIError(error);
          this.errorText = (response.message) ? response.message : '';
        });
  }

  delete(workspace: Workspace): void {
    this.workspaceDeletionError = false;
    this.deleting = true;
    this.workspaceList = [];
    this.workspacesLoading = true;
    this.deleteModal.close();
    this.workspacesService.deleteWorkspace(workspace.namespace, workspace.id).subscribe(() => {
      this.reloadWorkspaces();
    }, (error) => {
      this.workspaceDeletionError = true;
    });
  }

  receiveDelete(workspace: Workspace): void {
    this.delete(workspace);
  }

  confirmDelete(workspace: Workspace): void {
    this.resource = workspace;
    this.deleteModal.open();
  }

  share(workspace: Workspace, accessLevel: WorkspaceAccessLevel): void {
    this.selectedWorkspace = workspace;
    this.accessLevel = accessLevel;
    this.shareModal.open();
  }

  get twoFactorBannerEnabled() {
    if (this.firstSignIn === undefined) {
      return false;
    }

    if (this.twoFactorEnabled === true) {
      return false;
    }

    // Don't show the banner after 1 week as their account would
    // have been disabled had they not enabled 2-factor auth.
    if (new Date().getTime() - this.firstSignIn.getTime() > 1 * 7 * 24 * 60 * 60 * 1000) {
      return false;
    }
    return true;
  }
}
