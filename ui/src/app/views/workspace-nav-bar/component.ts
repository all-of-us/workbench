import {Component, OnDestroy, OnInit, ViewChild} from '@angular/core';
import {ActivatedRoute, NavigationEnd, Router} from '@angular/router';

import {WorkspaceData} from 'app/resolvers/workspace';

import {currentWorkspaceStore} from 'app/utils/navigation';
import {BugReportComponent} from 'app/views/bug-report/component';
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
export class WorkspaceNavBarComponent implements OnInit, OnDestroy {
  @ViewChild(WorkspaceShareComponent)
  shareModal: WorkspaceShareComponent;

  workspace: Workspace;
  wsId: string;
  wsNamespace: string;
  accessLevel: WorkspaceAccessLevel;
  deleting = false;
  workspaceDeletionError = false;
  tabPath: string;
  display = true;
  confirmDeleting = false;

  @ViewChild(BugReportComponent)
  bugReportComponent: BugReportComponent;

  private subscriptions = [];

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private workspacesService: WorkspacesService
  ) {}

  ngOnInit(): void {
    const handleData = (data) => {
      const workspace = <WorkspaceData> data.workspace;
      currentWorkspaceStore.next(workspace);
      this.workspace = workspace;
      this.accessLevel = workspace.accessLevel;
    };
    handleData(this.route.snapshot.data);
    this.subscriptions.push(this.route.data.subscribe(handleData));

    const handleParams = (params) => {
      this.wsNamespace = params['ns'];
      this.wsId = params['wsid'];
    };
    handleParams(this.route.snapshot.params);
    this.subscriptions.push(this.route.params.subscribe(handleParams));

    this.tabPath = this.getTabPath();
    this.display = this.shouldDisplay();
    this.subscriptions.push(
      this.router.events.filter(event => event instanceof NavigationEnd)
        .subscribe(event => {
          this.tabPath = this.getTabPath();
          this.display = this.shouldDisplay();
        }));
  }

  ngOnDestroy() {
    currentWorkspaceStore.next(undefined);
    for (const s of this.subscriptions) {
      s.unsubscribe();
    }
  }

  private shouldDisplay(): boolean {
    let leaf = this.route.snapshot;
    while (leaf.firstChild != null) {
      leaf = leaf.firstChild;
    }
    return !leaf.data.minimizeChrome;
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
        this.router.navigate(['/workspaces']);
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
    this.shareModal.open();
  }

  get writePermission(): boolean {
    return this.accessLevel === WorkspaceAccessLevel.OWNER
      || this.accessLevel === WorkspaceAccessLevel.WRITER;
  }

  get ownerPermission(): boolean {
    return this.accessLevel === WorkspaceAccessLevel.OWNER;
  }

  submitWorkspaceDeleteBugReport(): void {
    this.workspaceDeletionError = false;
    this.bugReportComponent.reportBug();
    this.bugReportComponent.bugReport.shortDescription = 'Could not delete workspace.';
  }
}
