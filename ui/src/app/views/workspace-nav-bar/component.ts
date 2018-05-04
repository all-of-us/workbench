import {Component, OnInit, ViewChild} from '@angular/core';
import {ActivatedRoute, Router} from '@angular/router';

import {WorkspaceData, WorkspaceStorageService} from 'app/services/workspace-storage.service';

import {WorkspaceShareComponent} from 'app/views/workspace-share/component';

import {
  Workspace,
  WorkspaceAccessLevel,
  WorkspacesService,
} from 'generated';

@Component({
  selector: 'app-workspace-nav-bar',
  styleUrls: ['../../styles/buttons.css',
              '../../styles/headers.css',
              './component.css'],
  templateUrl: './component.html',
})
export class WorkspaceNavBarComponent implements OnInit {
  @ViewChild(WorkspaceShareComponent)
  shareModal: WorkspaceShareComponent;

  workspace: Workspace;
  wsId: string;
  wsNamespace: string;
  awaitingReview = false;
  private accessLevel: WorkspaceAccessLevel;
  deleting = false;
  workspaceLoaded = false;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private workspacesService: WorkspacesService,
    private workspaceStorageService: WorkspaceStorageService,
  ) {}

  ngOnInit(): void {
    this.workspaceStorageService.activeWorkspace$.subscribe((wsData: WorkspaceData) => {
      this.workspace = wsData;
      this.accessLevel = wsData.accessLevel;
      const {approved, reviewRequested} = this.workspace.researchPurpose;
      this.awaitingReview = reviewRequested && !approved;
      this.workspaceLoaded = true;
    });

    this.wsNamespace = this.route.snapshot.params['ns'];
    this.wsId = this.route.snapshot.params['wsid'];
    this.workspaceStorageService.reloadIfNew(this.wsNamespace, this.wsId);
  }

  delete(): void {
    this.deleting = true;
    this.workspacesService.deleteWorkspace(
      this.workspace.namespace, this.workspace.id).subscribe(() => {
        this.router.navigate(['/']);
    });
  }

  share(): void {
    this.shareModal.open();
  }

  get writePermission(): boolean {
    return this.accessLevel === WorkspaceAccessLevel.OWNER
      || this.accessLevel === WorkspaceAccessLevel.WRITER;
  }

  get ownerPermission(): boolean {
    return this.accessLevel === WorkspaceAccessLevel.OWNER;
  }
}
