import {Component, OnDestroy, OnInit} from '@angular/core';
import {ActivatedRoute, Router} from '@angular/router';
import {ErrorHandlingService} from 'app/services/error-handling.service';
import {ProfileStorageService} from 'app/services/profile-storage.service';
import {Subscription} from 'rxjs/Subscription';

import {
  BillingProjectStatus,
  ErrorResponse,
  WorkspaceAccessLevel,
  WorkspaceResponse,
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
  workspaceList: WorkspaceResponse[] = [];
  workspacesLoading = false;
  workspaceAccessLevel = WorkspaceAccessLevel;
  firstSignIn: Date;
  twoFactorEnabled: boolean;
  private profileSubscription: Subscription;

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
          this.workspacesService.getWorkspaces()
            .subscribe(
              workspacesReceived => {
                workspacesReceived.items.sort(function(a, b) {
                  return a.workspace.name.localeCompare(b.workspace.name);
                });
                this.workspaceList = workspacesReceived.items;
                this.workspacesLoading = false;
              },
              error => {
                const response: ErrorResponse = ErrorHandlingService.convertAPIError(error);
                this.errorText = (response.message) ? response.message : '';
              });
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
    this.router.navigate(['workspace/build'], {relativeTo : this.route});
  }

  get twoFactorBannerEnabled() {
    if (this.firstSignIn === null) {
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
