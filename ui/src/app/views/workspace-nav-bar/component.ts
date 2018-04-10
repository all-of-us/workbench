import {Component, Inject, OnDestroy, OnInit} from '@angular/core';
import {DOCUMENT} from '@angular/platform-browser';
import {ActivatedRoute, Router} from '@angular/router';

import {WorkspaceData} from 'app/resolvers/workspace';

import {
    Workspace,
    WorkspaceAccessLevel,
    WorkspacesService,
} from 'generated';

@Component({
    selector : 'app-workspace-nav-bar',
    styleUrls: ['../../styles/buttons.css',
                '../../styles/headers.css',
                './component.css'],
    templateUrl: './component.html',
})
export class WorkspaceNavBarComponent implements OnInit {
    dropdownOpen = false;

    workspace: Workspace;
    wsId: string;
    wsNamespace: string;
    awaitingReview = false;
    private accessLevel: WorkspaceAccessLevel;
    deleting = false;


    constructor(
      private route: ActivatedRoute,
      private router: Router,
      private workspacesService: WorkspacesService,
      @Inject(DOCUMENT) private document: any
    ) {
        const wsData: WorkspaceData = this.route.snapshot.data.workspace;
        this.workspace = wsData;
        this.accessLevel = wsData.accessLevel;
        const {approved, reviewRequested} = this.workspace.researchPurpose;
        this.awaitingReview = reviewRequested && !approved;
    }

    ngOnInit(): void {
        this.wsNamespace = this.route.snapshot.params['ns'];
        this.wsId = this.route.snapshot.params['wsid'];
    }

    edit(): void {
        this.router.navigate(['workspace', this.wsNamespace, this.wsId, 'edit']);
    }

    clone(): void {
        this.router.navigate(['workspace', this.wsNamespace, this.wsId, 'clone']);
    }

    share(): void {
        this.router.navigate(['workspace', this.wsNamespace, this.wsId, 'share']);
    }

    delete(): void {
        this.deleting = true;
        this.workspacesService.deleteWorkspace(
          this.workspace.namespace, this.workspace.id).subscribe(() => {
            this.router.navigate(['/']);
        });
    }

    buildCohort(): void {
        if (!this.awaitingReview) {
            this.router.navigate(['workspace', this.wsNamespace, this.wsId, 'cohorts', 'build']);
        }
    }

    navigateWorkspaceHome(): void {
        this.router.navigate(['workspace', this.wsNamespace, this.wsId]);
    }

    get writePermission(): boolean {
        return this.accessLevel === WorkspaceAccessLevel.OWNER
          || this.accessLevel === WorkspaceAccessLevel.WRITER;
    }

    get ownerPermission(): boolean {
        return this.accessLevel === WorkspaceAccessLevel.OWNER;
    }
}
