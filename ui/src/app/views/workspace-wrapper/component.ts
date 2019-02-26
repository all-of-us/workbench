import {Component, OnDestroy, OnInit, ViewChild} from '@angular/core';
import {ActivatedRoute, NavigationEnd, Router} from '@angular/router';

import {WorkspaceData} from 'app/resolvers/workspace';

import {currentWorkspaceStore, navigate, routeConfigDataStore} from 'app/utils/navigation';
import {WorkspaceShareComponent} from 'app/views/workspace-share/component';

import {
  Workspace,
  WorkspaceAccessLevel,
  WorkspacesService,
} from 'generated';

@Component({
  styleUrls: ['../../styles/buttons.css',
    '../../styles/headers.css'],
  templateUrl: './component.html',
})
export class WorkspaceWrapperComponent implements OnInit, OnDestroy {
  @ViewChild(WorkspaceShareComponent)
  shareModal: WorkspaceShareComponent;

  workspace: Workspace;
  accessLevel: WorkspaceAccessLevel;
  deleting = false;
  sharing = false;
  workspaceDeletionError = false;
  tabPath: string;
  displayNavBar = true;
  confirmDeleting = false;
  username: string;

  bugReportOpen: boolean;
  bugReportDescription = '';

  private subscriptions = [];

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private workspacesService: WorkspacesService
  ) {
    this.share = this.share.bind(this);
    this.closeShare = this.closeShare.bind(this);
    this.openConfirmDelete = this.openConfirmDelete.bind(this);
    this.receiveDelete = this.receiveDelete.bind(this);
    this.closeConfirmDelete = this.closeConfirmDelete.bind(this);
    this.closeBugReport = this.closeBugReport.bind(this);
  }

  ngOnInit(): void {
    const handleData = (data) => {
      const workspace = <WorkspaceData> data.workspace;
      currentWorkspaceStore.next(workspace);
      this.workspace = workspace;
      this.accessLevel = workspace.accessLevel;
    };
    handleData(this.route.snapshot.data);
    this.subscriptions.push(this.route.data.subscribe(handleData));

    this.tabPath = this.getTabPath();

    this.subscriptions.push(
      this.router.events.filter(event => event instanceof NavigationEnd)
        .subscribe(event => {
          this.tabPath = this.getTabPath();
        }));
    this.subscriptions.push(routeConfigDataStore.subscribe(({minimizeChrome}) => {
      this.displayNavBar = !minimizeChrome;
    }));
  }

  ngOnDestroy() {
    currentWorkspaceStore.next(undefined);
    for (const s of this.subscriptions) {
      s.unsubscribe();
    }
  }

  private getTabPath(): string {
    const child = this.route.firstChild;
    if (!child) {
      return '';
    }
    const path = child.routeConfig.path;
    if (!path.includes('/')) {
      return path;
    }
    return path.slice(0, path.indexOf('/'));
  }

  delete(workspace: Workspace): void {
    this.deleting = true;
    this.workspacesService.deleteWorkspace(
      workspace.namespace, workspace.id).subscribe(() => {
        navigate(['/workspaces']);
      }, () => {
        this.workspaceDeletionError = true;
      });
  }

  receiveDelete(): void {
    this.delete(this.workspace);
  }

  openConfirmDelete(): void {
    this.confirmDeleting = true;
  }

  closeConfirmDelete(): void {
    this.confirmDeleting = false;
  }

  share(): void {
    this.sharing = true;
  }

  closeShare(): void {
    this.sharing = false;
    // TODO: RW-1919 - remove this
    window.location.reload();
  }

  submitWorkspaceDeleteBugReport(): void {
    this.workspaceDeletionError = false;
    // this.bugReportComponent.reportBug();
    this.bugReportDescription = 'Could not delete workspace.';
    this.bugReportOpen = true;
  }

  closeBugReport(): void {
    this.bugReportOpen = false;
  }
}
