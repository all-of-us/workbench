import {Component, OnDestroy, OnInit, ViewChild} from '@angular/core';
import {ErrorHandlingService} from 'app/services/error-handling.service';
import {ProfileStorageService} from 'app/services/profile-storage.service';
import {Subscription} from 'rxjs/Subscription';

import {navigate} from 'app/utils/navigation';
import {WorkspacePermissions} from 'app/utils/workspace-permissions';

import {ToolTipComponent} from 'app/views/tooltip/component';
import {
  BillingProjectStatus,
  ErrorResponse,
  Workspace,
  WorkspaceAccessLevel,
  WorkspacesService
} from 'generated';


@Component({
  styleUrls: ['./component.css',
    '../../styles/buttons.css',
    '../../styles/tooltip.css',
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

  accessLevel: WorkspaceAccessLevel;
  username: string;

  @ViewChild(ToolTipComponent)
  toolTip: ToolTipComponent;

  deleting = false;
  confirmDeleting = false;
  sharing = false;
  workspaceDeletionError = false;
  workspace: Workspace = {name: ''};

  bugReportOpen: boolean;
  bugReportDescription = '';

  constructor(
    private profileStorageService: ProfileStorageService,
    private workspacesService: WorkspacesService,
  ) {
    this.receiveDelete = this.receiveDelete.bind(this);
    this.closeConfirmDelete = this.closeConfirmDelete.bind(this);
    this.closeShare = this.closeShare.bind(this);
    this.closeBugReport = this.closeBugReport.bind(this);
  }

  ngOnInit(): void {
    this.workspacesLoading = true;
    this.profileSubscription = this.profileStorageService.profile$.subscribe(
      (profile) => {
        this.username = profile.username;
        this.twoFactorEnabled = profile.twoFactorEnabled;
        if (this.firstSignIn === undefined) {
          this.firstSignIn = new Date(profile.firstSignInTime);
        }
        if (profile.freeTierBillingProjectStatus === BillingProjectStatus.Ready) {

          // Only once we know the billing project status do we request/display
          // workspaces for two reasons:
          // - If the FC user is not yet initialized, getWorkspaces() (called
          //   inside reload workspaces) may fail with a 401.
          // - While the billing project is being initialized, we want to keep the
          //   big spinner on the page to provide obvious messaging to the user
          //   about the expected wait time.
          this.billingProjectInitialized = true;
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
    navigate(['workspaces/build']);
  }

  reloadWorkspaces(): void {
    this.workspacesService.getWorkspaces()
      .subscribe(
        workspacesReceived => {
          workspacesReceived.items.sort((a, b) => a.workspace.name.localeCompare(b.workspace.name));
          this.workspaceList = workspacesReceived
            .items
            .map(w => new WorkspacePermissions(w));
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
    this.closeConfirmDelete();
    this.workspacesService.deleteWorkspace(workspace.namespace, workspace.id).subscribe(() => {
      this.reloadWorkspaces();
    }, (error) => {
      this.workspaceDeletionError = true;
    });
  }

  receiveDelete(): void {
    this.delete(this.workspace);
  }

  openConfirmDelete(workspace: Workspace): void {
    this.workspace = workspace;
    this.confirmDeleting = true;
  }

  closeConfirmDelete(): void {
    this.confirmDeleting = false;
  }

  openShare(workspace: Workspace, accessLevel: WorkspaceAccessLevel): void {
    this.workspace = workspace;
    this.accessLevel = accessLevel;
    this.sharing = true;
  }

  closeShare(): void {
    this.sharing = false;
    this.reloadWorkspaces();
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

  submitWorkspaceDeleteBugReport(): void {
    this.workspaceDeletionError = false;
    this.bugReportDescription = 'Could not delete workspace.';
    this.bugReportOpen = true;
  }

  closeBugReport(): void {
    this.bugReportOpen = false;
  }
}
