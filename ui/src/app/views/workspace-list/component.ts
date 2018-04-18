import {Component, OnDestroy, OnInit} from '@angular/core';
import {ActivatedRoute, Router} from '@angular/router';
import {ErrorHandlingService} from 'app/services/error-handling.service';

import {
  BillingProjectStatus,
  ErrorResponse,
  Profile,
  ProfileService,
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

  billingProjectInitialized = false;
  billingProjectQuery: NodeJS.Timer;
  errorText: string;
  workspaceList: WorkspaceResponse[] = [];
  workspacesLoading = false;
  workspaceAccessLevel = WorkspaceAccessLevel;
  firstSignIn: Date;
  constructor(
      private profileService: ProfileService,
      private route: ActivatedRoute,
      private router: Router,
      private workspacesService: WorkspacesService,
  ) {}
  ngOnInit(): void {
    this.workspacesLoading = true;
    this.profileService.getMe().subscribe((profile: Profile) => {
      this.firstSignIn = new Date(profile.firstSignInTime);
      if (profile.freeTierBillingProjectStatus === BillingProjectStatus.Ready) {
        this.billingProjectInitialized = true;
      } else {
        this.billingProjectQuery = setTimeout(() => {
          this.queryBillingStatus();
        }, 10000);
      }
    });
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
  }

  ngOnDestroy(): void {
    clearTimeout(this.billingProjectQuery);
  }

  queryBillingStatus(): void {
    // TODO (blrubenstein): When we have a home page, move this to the
    //      home page and change from tooltip to more descriptive message.
    this.profileService.getMe().subscribe((profile: Profile) => {
      if (profile.freeTierBillingProjectStatus === BillingProjectStatus.Ready) {
        this.billingProjectInitialized = true;
      } else {
        this.billingProjectQuery = setTimeout(() => {
          this.queryBillingStatus();
        }, 10000);
      }
    });
  }

  addWorkspace(): void {
    this.router.navigate(['workspace/build'], {relativeTo : this.route});
  }

  get twoFactorBannerEnabled() {
    if (this.firstSignIn == null) {
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
